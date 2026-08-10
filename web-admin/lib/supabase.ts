import { createClient } from '@supabase/supabase-js';

function sanitizeSupabaseUrl(url: string): string {
  if (!url) return '';
  let cleaned = url.trim();
  // Remove trailing slashes
  while (cleaned.endsWith('/')) {
    cleaned = cleaned.slice(0, -1);
  }
  // Remove trailing /rest/v1 if present
  if (cleaned.endsWith('/rest/v1')) {
    cleaned = cleaned.slice(0, -8);
  }
  while (cleaned.endsWith('/')) {
    cleaned = cleaned.slice(0, -1);
  }
  return cleaned;
}

const rawUrl = (process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://aihnpdxpzwlhxfmyjmfa.supabase.co').trim();
const supabaseUrl = sanitizeSupabaseUrl(rawUrl);
const supabaseAnonKey = (process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpaG5wZHhwendsaHhmbXlqbWZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2MzA5NDYsImV4cCI6MjEwMDIwNjk0Nn0.SO8LOnAqUFmsQJ-meA3zPVlDN6JU6kJPRW1WAyfJsHw').trim();

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

