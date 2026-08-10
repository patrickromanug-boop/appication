-- =========================================================================
-- MIGRATION: LS SERVICES JOB APP NOTIFICATION SYSTEM
-- Run this in your Supabase SQL Editor to configure the matching and trigger system.
-- =========================================================================

-- 1. ADD NOTIFICATION PREFERENCE COLUMNS TO PROFILES
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS notify_all_jobs BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS notify_matching_preferences BOOLEAN NOT NULL DEFAULT FALSE;

-- Create an audit/log table to trace simulated push notification dispatches
CREATE TABLE IF NOT EXISTS public.notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    job_id UUID REFERENCES public.jobs(id) ON DELETE CASCADE,
    token TEXT,
    plan_tier TEXT,
    notification_type TEXT, -- 'instant' or 'daily_digest'
    status TEXT, -- 'sent', 'failed', 'queued_for_digest'
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable Row Level Security on Notification Logs
ALTER TABLE public.notification_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Admins can view notification logs" ON public.notification_logs
    FOR SELECT USING (public.is_admin());

-- =========================================================================
-- 2. MATCHING & FILTERING LOGIC: TRIGGER ON NEW JOB POSTED
-- =========================================================================

CREATE OR REPLACE FUNCTION public.notify_on_new_job_posted()
RETURNS TRIGGER AS $$
DECLARE
    user_rec RECORD;
    token_rec RECORD;
    job_category TEXT;
    job_location TEXT;
    is_matched BOOLEAN;
    sub_rec RECORD;
    notif_type TEXT;
    dispatch_status TEXT;
    dispatch_reason TEXT;
BEGIN
    -- 1. Fetch category and location names for comparison
    SELECT name INTO job_category FROM public.categories WHERE id = NEW.category_id;
    SELECT name INTO job_location FROM public.locations WHERE id = NEW.location_id;

    -- 2. Iterate through all profiles to find matches
    FOR user_rec IN 
        SELECT p.id, p.full_name, p.notify_all_jobs, p.notify_matching_preferences, p.preferred_categories, p.preferred_locations 
        FROM public.profiles p
    LOOP
        -- Fetch user's subscription details
        SELECT plan_tier, status INTO sub_rec 
        FROM public.subscriptions 
        WHERE user_id = user_rec.id;

        -- Fallback to free tier if no subscription found
        IF sub_rec.plan_tier IS NULL THEN
            sub_rec.plan_tier := 'free';
            sub_rec.status := 'active';
        END IF;

        -- Reset loop variables
        is_matched := FALSE;
        notif_type := 'instant';
        dispatch_status := 'queued_for_digest';
        dispatch_reason := '';

        -- SUBSCRIPTION TIER RULES:
        -- Free tier (post-trial): DAILY DIGEST only. No instant alerts.
        IF sub_rec.plan_tier = 'free' AND sub_rec.status != 'trial' THEN
            notif_type := 'daily_digest';
            dispatch_status := 'queued_for_digest';
            dispatch_reason := 'Free plan tier: job queued for 24h Daily Digest';
        ELSE
            -- Trial, Basic, Premium, Premium Pro get instant alerts
            notif_type := 'instant';
            dispatch_status := 'sent';
        END IF;

        -- MATCHING FILTER LOGIC:
        IF user_rec.notify_all_jobs = TRUE THEN
            is_matched := TRUE;
            dispatch_reason := COALESCE(dispatch_reason, 'User requested alerts for all new jobs');
        ELSIF user_rec.notify_matching_preferences = TRUE THEN
            -- Check if job's category or location is in user's preferences arrays
            IF (job_category = ANY(user_rec.preferred_categories)) OR 
               (job_location = ANY(user_rec.preferred_locations)) THEN
                is_matched := TRUE;
                dispatch_reason := COALESCE(dispatch_reason, 'Job matches user preferred category or location');
            ELSE
                is_matched := FALSE;
                dispatch_reason := 'Job category/location does not match user preferences';
            END IF;
        ELSE
            -- All alerts disabled for this user
            is_matched := FALSE;
            dispatch_reason := 'User disabled all job notifications';
        END IF;

        -- 3. DISPATCH PUSH NOTIFICATION (IF MATCHED)
        IF is_matched = TRUE THEN
            -- Fetch active push tokens for the user
            FOR token_rec IN 
                SELECT token FROM public.push_tokens WHERE user_id = user_rec.id
            LOOP
                -- LOG THE DISPATCH FOR VERIFICATION
                INSERT INTO public.notification_logs (
                    user_id,
                    job_id,
                    token,
                    plan_tier,
                    notification_type,
                    status,
                    reason
                ) VALUES (
                    user_rec.id,
                    NEW.id,
                    token_rec.token,
                    sub_rec.plan_tier,
                    notif_type,
                    dispatch_status,
                    dispatch_reason
                );

                -- NOTE ON EDGE FUNCTIONS:
                -- In production, you would perform an HTTP call to your Edge Function (e.g. Supabase Edge Functions or Firebase Cloud Messaging)
                -- using pg_net or webhook integrations like:
                -- PERFORM net.http_post(
                --     url := 'https://your-project.supabase.co/functions/v1/send-push',
                --     headers := '{"Content-Type": "application/json", "Authorization": "Bearer YOUR_SERVICE_ROLE_KEY"}',
                --     body := json_build_object('token', token_rec.token, 'title', NEW.title, 'body', NEW.organization, 'job_id', NEW.id)::text
                -- );
            END LOOP;
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. CREATE TRIGGER ON JOBS TABLE FOR NEW INSERTION
DROP TRIGGER IF EXISTS tr_notify_on_new_job_posted ON public.jobs;
CREATE TRIGGER tr_notify_on_new_job_posted
    AFTER INSERT ON public.jobs
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_on_new_job_posted();
