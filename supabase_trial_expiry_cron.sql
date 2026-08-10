-- ==========================================
-- LS Services Job App - Daily Subscription Trial Expiry Cron
-- ==========================================
-- This script schedules a daily cron job to check and transition expired trials.
-- It can be executed directly within the Supabase SQL Editor.

-- 1. Create a function to process trial expiries
CREATE OR REPLACE FUNCTION process_trial_expiries()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  expired_count integer;
BEGIN
  -- Update subscriptions whose trial has ended to the free tier with free tier defaults
  UPDATE subscriptions
  SET 
    plan_tier = 'free',
    status = 'active',
    notif_daily_limit = 5,
    applies_monthly_limit = 3,
    categories_limit = 2,
    renewal_date = NULL
  WHERE plan_tier = 'trial'
    AND trial_ends_at < timezone('utc'::text, now());

  GET DIAGNOSTICS expired_count = ROW_COUNT;
  RAISE NOTICE 'Daily Cron Run: Successfully converted % expired trials to the Free tier.', expired_count;
END;
$$;

-- 2. Schedule the function to execute automatically at midnight (00:05 UTC) every day
-- This performs the check in the database background without requiring client interactions
SELECT cron.schedule(
  'daily-trial-expiry-check',    -- Unique job schedule name
  '5 0 * * *',                   -- Cron expression: 00:05 UTC every day
  'SELECT process_trial_expiries();'
);
