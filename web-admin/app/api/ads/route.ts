import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

const DEFAULT_URL = 'https://aihnpdxpzwlhxfmyjmfa.supabase.co';
const DEFAULT_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpaG5wZHhwendsaHhmbXlqbWZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2MzA5NDYsImV4cCI6MjEwMDIwNjk0Nn0.SO8LOnAqUFmsQJ-meA3zPVlDN6JU6kJPRW1WAyfJsHw';

function getSupabase() {
  const supabaseUrl = (process.env.NEXT_PUBLIC_SUPABASE_URL || DEFAULT_URL).trim();
  const supabaseKey = (process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || DEFAULT_KEY).trim();
  return createClient(supabaseUrl, supabaseKey);
}

export async function GET() {
  try {
    const supabase = getSupabase();
    const { data, error } = await supabase.from('company_ads').select('*').order('created_at', { ascending: false });
    if (error) {
      // If table doesn't exist yet, return empty list gracefully
      return NextResponse.json({ success: true, ads: [] });
    }
    return NextResponse.json({ success: true, ads: data || [] });
  } catch (err: any) {
    return NextResponse.json({ success: true, ads: [] });
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { company_name, headline, description, image_url, website_url, contact_phone, status } = body;

    if (!company_name || !headline) {
      return NextResponse.json({ error: 'Company Name and Headline are required.' }, { status: 400 });
    }

    const supabase = getSupabase();
    const { data, error } = await supabase
      .from('company_ads')
      .insert([
        {
          company_name,
          headline,
          description: description || '',
          image_url: image_url || '',
          website_url: website_url || '',
          contact_phone: contact_phone || '',
          status: status || 'active',
        },
      ])
      .select();

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    return NextResponse.json({ success: true, ad: data?.[0] }, { status: 201 });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || 'Internal Server Error' }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');

    if (!id) {
      return NextResponse.json({ error: 'Ad ID is required.' }, { status: 400 });
    }

    const supabase = getSupabase();
    const { error } = await supabase.from('company_ads').delete().eq('id', id);

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    return NextResponse.json({ success: true, message: 'Ad deleted successfully' });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || 'Internal Server Error' }, { status: 500 });
  }
}
