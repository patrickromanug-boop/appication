import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { title, organization, purpose, requirements, deadline, status, location_id, category_id, job_type_id } = body;

    if (!title || !organization || !deadline) {
      return NextResponse.json({ error: 'Title, organization, and deadline are required.' }, { status: 400 });
    }

    const DEFAULT_URL = 'https://aihnpdxpzwlhxfmyjmfa.supabase.co';
    const DEFAULT_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpaG5wZHhwendsaHhmbXlqbWZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2MzA5NDYsImV4cCI6MjEwMDIwNjk0Nn0.SO8LOnAqUFmsQJ-meA3zPVlDN6JU6kJPRW1WAyfJsHw';

    const supabaseUrl = (process.env.NEXT_PUBLIC_SUPABASE_URL || DEFAULT_URL).trim();
    // Use service role key if present to bypass RLS, otherwise use anon key or default
    const supabaseKey = (process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || DEFAULT_KEY).trim();

    if (!supabaseUrl || !supabaseKey) {
      return NextResponse.json({ error: 'Supabase URL or Key is missing in environment variables.' }, { status: 500 });
    }

    const supabase = createClient(supabaseUrl, supabaseKey);

    // 1. Ensure a default location exists if location_id not provided or valid
    let finalLocationId = location_id;
    if (!finalLocationId) {
      const { data: locs } = await supabase.from('locations').select('id').limit(1);
      if (locs && locs.length > 0) {
        finalLocationId = locs[0].id;
      } else {
        const { data: newLoc } = await supabase.from('locations').insert([{ name: 'Kampala, Uganda' }]).select();
        finalLocationId = newLoc?.[0]?.id;
      }
    }

    // 2. Ensure a default category exists
    let finalCategoryId = category_id;
    if (!finalCategoryId) {
      const { data: cats } = await supabase.from('categories').select('id').limit(1);
      if (cats && cats.length > 0) {
        finalCategoryId = cats[0].id;
      } else {
        const { data: newCat } = await supabase.from('categories').insert([{ name: 'General' }]).select();
        finalCategoryId = newCat?.[0]?.id;
      }
    }

    // 3. Ensure a default job type exists
    let finalJobTypeId = job_type_id;
    if (!finalJobTypeId) {
      const { data: types } = await supabase.from('job_types').select('id').limit(1);
      if (types && types.length > 0) {
        finalJobTypeId = types[0].id;
      } else {
        const { data: newType } = await supabase.from('job_types').insert([{ name: 'Full-Time' }]).select();
        finalJobTypeId = newType?.[0]?.id;
      }
    }

    // 4. Insert Job
    const { data, error } = await supabase
      .from('jobs')
      .insert([
        {
          title,
          organization,
          purpose: purpose || 'Job opportunity published via Admin Portal',
          requirements: requirements || 'Minimum qualifications required.',
          deadline,
          status: status || 'active',
          location_id: finalLocationId,
          category_id: finalCategoryId,
          job_type_id: finalJobTypeId,
        },
      ])
      .select();

    if (error) {
      return NextResponse.json({ error: error.message || 'Failed to insert job into Supabase.' }, { status: 400 });
    }

    return NextResponse.json({ success: true, job: data?.[0] }, { status: 201 });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || 'Internal Server Error' }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');

    if (!id) {
      return NextResponse.json({ error: 'Job ID is required.' }, { status: 400 });
    }

    const DEFAULT_URL = 'https://aihnpdxpzwlhxfmyjmfa.supabase.co';
    const DEFAULT_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpaG5wZHhwendsaHhmbXlqbWZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2MzA5NDYsImV4cCI6MjEwMDIwNjk0Nn0.SO8LOnAqUFmsQJ-meA3zPVlDN6JU6kJPRW1WAyfJsHw';

    const supabaseUrl = (process.env.NEXT_PUBLIC_SUPABASE_URL || DEFAULT_URL).trim();
    const supabaseKey = (process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || DEFAULT_KEY).trim();

    const supabase = createClient(supabaseUrl, supabaseKey);

    const { error } = await supabase.from('jobs').delete().eq('id', id);

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    return NextResponse.json({ success: true, message: 'Job deleted successfully' });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || 'Internal Server Error' }, { status: 500 });
  }
}

export async function PUT(request: Request) {
  try {
    const body = await request.json();
    const { id, title, organization, purpose, requirements, deadline, status } = body;

    if (!id) {
      return NextResponse.json({ error: 'Job ID is required.' }, { status: 400 });
    }

    const DEFAULT_URL = 'https://aihnpdxpzwlhxfmyjmfa.supabase.co';
    const DEFAULT_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpaG5wZHhwendsaHhmbXlqbWZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2MzA5NDYsImV4cCI6MjEwMDIwNjk0Nn0.SO8LOnAqUFmsQJ-meA3zPVlDN6JU6kJPRW1WAyfJsHw';

    const supabaseUrl = (process.env.NEXT_PUBLIC_SUPABASE_URL || DEFAULT_URL).trim();
    const supabaseKey = (process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || DEFAULT_KEY).trim();

    const supabase = createClient(supabaseUrl, supabaseKey);

    const { data, error } = await supabase
      .from('jobs')
      .update({
        title,
        organization,
        purpose,
        requirements,
        deadline,
        status,
      })
      .eq('id', id)
      .select();

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    return NextResponse.json({ success: true, job: data?.[0] });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || 'Internal Server Error' }, { status: 500 });
  }
}
