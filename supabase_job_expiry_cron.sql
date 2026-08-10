-- ==========================================
-- LS Services Job App - Daily Automatic Job Expiry
-- ==========================================
-- This script enables automated cleanups of expired job postings.
-- It can be executed directly within the Supabase SQL Editor.

-- 1. Create a security-definer function to safely archive expired jobs
CREATE OR REPLACE FUNCTION archive_expired_jobs()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  archived_count integer;
BEGIN
  -- Update job listings whose deadline date is strictly in the past
  -- and whose status is still 'active'
  UPDATE jobs
  SET status = 'archived'
  WHERE status = 'active'
    AND deadline < CURRENT_DATE;

  GET DIAGNOSTICS archived_count = ROW_COUNT;
  RAISE NOTICE 'Daily Cron Run: Successfully archived % expired job vacancies.', archived_count;
END;
$$;

-- 2. Enable the standard pg_cron extension (native to Supabase)
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- 3. Schedule the function to execute automatically at midnight (00:00 UTC) every day
-- This performs the cleanup in the database background without requiring client interactions
SELECT cron.schedule(
  'daily-job-expiry-cleanup',  -- Unique job schedule name
  '0 0 * * *',                 -- Cron expression: Midnight every day
  'SELECT archive_expired_jobs();'
);

-- ==========================================
-- Alternative: PostgreSQL pg_net HTTP Cron Job
-- ==========================================
-- If pg_cron is disabled on your tier, you can use Supabase Edge Functions with a daily trigger:
-- OR execute this simple periodic scheduler query if needed.
