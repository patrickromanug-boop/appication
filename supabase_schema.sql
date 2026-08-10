-- SQL Schema for LS Services Job App (Uganda)
-- To be run in your Supabase SQL Editor

-- Enable UUID generation extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================================================================
-- 1. TABLES CREATION
-- =========================================================================

-- Profiles table (extends Supabase auth.users)
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    phone TEXT,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    education JSONB DEFAULT '[]'::jsonb, -- Array of objects: {institution, qualification, start_date, end_date}
    skills JSONB DEFAULT '[]'::jsonb, -- Array of strings
    experience JSONB DEFAULT '[]'::jsonb, -- Array of objects: {job_title, company, start_date, end_date, description}
    preferred_categories TEXT[] DEFAULT '{}',
    preferred_locations TEXT[] DEFAULT '{}',
    theme_preference TEXT NOT NULL DEFAULT 'system' CHECK (theme_preference IN ('light', 'dark', 'system')),
    hide_services_popup BOOLEAN NOT NULL DEFAULT FALSE,
    referral_code TEXT UNIQUE,
    referred_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- User Documents table
CREATE TABLE public.user_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    document_type TEXT NOT NULL, -- e.g., 'cv', 'academic_transcript', 'id_card'
    file_url TEXT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Locations table
CREATE TABLE public.locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Job Types table
CREATE TABLE public.job_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Categories table
CREATE TABLE public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Jobs table
CREATE TABLE public.jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    organization TEXT NOT NULL,
    location_id UUID NOT NULL REFERENCES public.locations(id) ON DELETE RESTRICT,
    category_id UUID NOT NULL REFERENCES public.categories(id) ON DELETE RESTRICT,
    job_type_id UUID NOT NULL REFERENCES public.job_types(id) ON DELETE RESTRICT,
    purpose TEXT NOT NULL,
    requirements TEXT NOT NULL,
    other_details TEXT,
    deadline DATE NOT NULL,
    official_link TEXT,
    opens_externally BOOLEAN NOT NULL DEFAULT FALSE,
    required_documents TEXT[] DEFAULT '{}',
    application_method TEXT NOT NULL DEFAULT 'auto_apply_supported' CHECK (application_method IN ('auto_apply_supported', 'requires_personal_account', 'email_only')),
    views_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'archived')),
    posted_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- CV Templates table
CREATE TABLE public.cv_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    layout_definition JSONB NOT NULL, -- Field placeholders mapped to profile fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Applications table
CREATE TABLE public.applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES public.jobs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    generated_cv_url TEXT,
    documents_attached JSONB DEFAULT '[]'::jsonb, -- Array of {document_id, file_url, document_type}
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'submitted', 'needs_more_info')),
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(job_id, user_id) -- Prevent duplicate applications
);

-- Subscriptions table
CREATE TABLE public.subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_tier TEXT NOT NULL DEFAULT 'free' CHECK (plan_tier IN ('trial', 'free', 'basic', 'premium', 'premium_pro')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('trial', 'active', 'expired')),
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    renewal_date TIMESTAMP WITH TIME ZONE,
    notif_daily_limit INTEGER, -- NULL = unlimited
    applies_monthly_limit INTEGER, -- NULL = unlimited
    categories_limit INTEGER, -- NULL = unlimited
    payment_provider_ref TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Push Tokens table
CREATE TABLE public.push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(user_id, token)
);

-- Referrals table
CREATE TABLE public.referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    referred_user_id UUID NOT NULL UNIQUE REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'completed')),
    reward_granted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Reported Jobs table
CREATE TABLE public.reported_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES public.jobs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'reviewed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Bookmarks table
CREATE TABLE public.bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES public.jobs(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(user_id, job_id)
);


-- =========================================================================
-- 2. AUTO-UPDATE TIMESTAMPS SETUP
-- =========================================================================

-- Trigger function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to tables with updated_at
CREATE TRIGGER update_profiles_modtime BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_user_documents_modtime BEFORE UPDATE ON public.user_documents FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_jobs_modtime BEFORE UPDATE ON public.jobs FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_cv_templates_modtime BEFORE UPDATE ON public.cv_templates FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_applications_modtime BEFORE UPDATE ON public.applications FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_subscriptions_modtime BEFORE UPDATE ON public.subscriptions FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_push_tokens_modtime BEFORE UPDATE ON public.push_tokens FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_referrals_modtime BEFORE UPDATE ON public.referrals FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_reported_jobs_modtime BEFORE UPDATE ON public.reported_jobs FOR EACH ROW EXECUTE PROCEDURE update_modified_column();


-- =========================================================================
-- 3. SEEDING SEED DATA
-- =========================================================================

-- Seed Job Types (Required)
INSERT INTO public.job_types (name) VALUES
('Full-time'),
('Part-time'),
('Contract'),
('Internship'),
('Volunteer'),
('Remote')
ON CONFLICT (name) DO NOTHING;

-- Seed Locations (Uganda)
INSERT INTO public.locations (name, latitude, longitude) VALUES
('Kampala', 0.3476, 32.5825),
('Entebbe', 0.0512, 32.4637),
('Jinja', 0.4244, 33.2042),
('Gulu', 2.7720, 32.3000),
('Mbarara', -0.6075, 30.6542),
('Remote (Uganda)', NULL, NULL)
ON CONFLICT (name) DO NOTHING;

-- Seed Categories
INSERT INTO public.categories (name) VALUES
('Engineering & IT'),
('Healthcare & Medical'),
('Sales & Marketing'),
('Education & Teaching'),
('Administration & HR'),
('Finance & Accounting'),
('Logistics & Transport'),
('Customer Support'),
('Hospitality & Tourism')
ON CONFLICT (name) DO NOTHING;


-- =========================================================================
-- 4. ROW LEVEL SECURITY (RLS) POLICIES
-- =========================================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.job_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cv_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.push_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reported_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookmarks ENABLE ROW LEVEL SECURITY;

-- Help function to check if the current user is an admin
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid() AND role = 'admin'
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- policies for: PROFILES
CREATE POLICY "Public profiles are viewable by anyone" ON public.profiles
    FOR SELECT USING (true);

CREATE POLICY "Users can update their own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- policies for: USER_DOCUMENTS
CREATE POLICY "Users can view their own documents" ON public.user_documents
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

CREATE POLICY "Users can manage their own documents" ON public.user_documents
    FOR ALL USING (auth.uid() = user_id);

-- policies for: LOCATIONS
CREATE POLICY "Locations are viewable by anyone" ON public.locations
    FOR SELECT USING (true);

CREATE POLICY "Only admins can manage locations" ON public.locations
    FOR ALL USING (public.is_admin());

-- policies for: JOB_TYPES
CREATE POLICY "Job types are viewable by anyone" ON public.job_types
    FOR SELECT USING (true);

CREATE POLICY "Only admins can manage job types" ON public.job_types
    FOR ALL USING (public.is_admin());

-- policies for: CATEGORIES
CREATE POLICY "Categories are viewable by anyone" ON public.categories
    FOR SELECT USING (true);

CREATE POLICY "Only admins can manage categories" ON public.categories
    FOR ALL USING (public.is_admin());

-- policies for: JOBS
CREATE POLICY "Active jobs are viewable by anyone" ON public.jobs
    FOR SELECT USING (status = 'active' OR public.is_admin());

CREATE POLICY "Only admins can insert/update jobs" ON public.jobs
    FOR ALL USING (public.is_admin());

-- policies for: CV_TEMPLATES
CREATE POLICY "CV templates are viewable by authenticated users" ON public.cv_templates
    FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Only admins can manage CV templates" ON public.cv_templates
    FOR ALL USING (public.is_admin());

-- policies for: APPLICATIONS
CREATE POLICY "Users can view their own applications" ON public.applications
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

CREATE POLICY "Users can insert their own applications" ON public.applications
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own applications" ON public.applications
    FOR UPDATE USING (auth.uid() = user_id OR public.is_admin());

-- policies for: SUBSCRIPTIONS
CREATE POLICY "Users can view their own subscription" ON public.subscriptions
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

-- policies for: PUSH_TOKENS
CREATE POLICY "Users can manage their own push tokens" ON public.push_tokens
    FOR ALL USING (auth.uid() = user_id);

-- policies for: REFERRALS
CREATE POLICY "Users can view their referrals" ON public.referrals
    FOR SELECT USING (auth.uid() = referrer_id OR auth.uid() = referred_user_id OR public.is_admin());

CREATE POLICY "Users can create referrals" ON public.referrals
    FOR INSERT WITH CHECK (auth.uid() = referrer_id);

-- policies for: REPORTED_JOBS
CREATE POLICY "Admins can view all reported jobs" ON public.reported_jobs
    FOR SELECT USING (public.is_admin());

CREATE POLICY "Authenticated users can submit reported jobs" ON public.reported_jobs
    FOR INSERT WITH CHECK (auth.role() = 'authenticated' AND auth.uid() = user_id);

-- policies for: BOOKMARKS
CREATE POLICY "Users can manage their own bookmarks" ON public.bookmarks
    FOR ALL USING (auth.uid() = user_id);


-- =========================================================================
-- 5. AUTOMATIC PROFILE & SUBSCRIPTION ON SIGN-UP TRIGGER
-- =========================================================================

-- Trigger function to handle auth user creation
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    ref_code TEXT;
    ref_by_id UUID;
    signup_full_name TEXT;
    signup_phone TEXT;
BEGIN
    -- 1. Determine referrals if applicable from metadata
    ref_by_id := NULL;
    IF (NEW.raw_user_meta_data->>'referred_by') IS NOT NULL THEN
        BEGIN
            ref_by_id := (NEW.raw_user_meta_data->>'referred_by')::UUID;
        EXCEPTION WHEN OTHERS THEN
            ref_by_id := NULL;
        END;
    END IF;

    -- 2. Generate unique referral code (8 random alphanumeric chars)
    ref_code := UPPER(SUBSTRING(MD5(NEW.id::text || clock_timestamp()::text) FROM 1 FOR 8));

    -- 3. Extract metadata
    signup_full_name := COALESCE(NEW.raw_user_meta_data->>'full_name', SPLIT_PART(NEW.email, '@', 1));
    signup_phone := NEW.raw_user_meta_data->>'phone';

    -- 4. Create user profile
    INSERT INTO public.profiles (
        id, 
        full_name, 
        phone, 
        role, 
        referral_code, 
        referred_by,
        theme_preference,
        hide_services_popup
    )
    VALUES (
        NEW.id,
        signup_full_name,
        signup_phone,
        -- Default to 'admin' for the owner / developer email if needed, or 'user'
        CASE 
            WHEN NEW.email = 'patrickromanug@gmail.com' THEN 'admin'
            ELSE 'user'
        END,
        ref_code,
        ref_by_id,
        'system',
        FALSE
    );

    -- 5. Create 14-day free trial subscription
    INSERT INTO public.subscriptions (
        user_id,
        plan_tier,
        status,
        trial_ends_at,
        renewal_date,
        notif_daily_limit,
        applies_monthly_limit,
        categories_limit
    )
    VALUES (
        NEW.id,
        'trial',
        'trial',
        timezone('utc'::text, now() + interval '14 days'),
        timezone('utc'::text, now() + interval '14 days'),
        5,    -- 5 daily job alerts limit for trial
        5,    -- 5 monthly job applications limit for trial
        3     -- 3 categories preference limit for trial
    );

    -- 6. Insert referral record if referred_by is valid
    IF ref_by_id IS NOT NULL THEN
        INSERT INTO public.referrals (referrer_id, referred_user_id, status, reward_granted)
        VALUES (ref_by_id, NEW.id, 'pending', FALSE)
        ON CONFLICT DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create the trigger
CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
