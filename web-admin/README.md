# LS Services Admin Portal (Next.js 14 App Router)

Management Web Dashboard for LS Services Platform (Uganda).

## 🚀 Quick Setup & Vercel Deployment

### 1. Project Structure
Ensure your repository `patrickromanug-boop/Ls-admin` contains these files directly at the root of your GitHub repository:

```
Ls-admin/
├── app/
│   ├── layout.tsx
│   ├── page.tsx
│   └── globals.css
├── lib/
│   └── supabase.ts
├── package.json
├── tsconfig.json
├── next.config.mjs
├── tailwind.config.ts
├── postcss.config.js
├── .env.example
└── .env.local
```

### 2. Environment Variables (.env.local)
Create a `.env.local` file with your Supabase keys:

```env
NEXT_PUBLIC_SUPABASE_URL=https://your-supabase-project.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-supabase-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-supabase-service-role-key
```

### 3. Deploy to Vercel
1. Import repository `patrickromanug-boop/Ls-admin` in Vercel.
2. Ensure **Framework Preset** is set to `Next.js`.
3. Set Root Directory to `./`.
4. Add your Environment Variables in Vercel project settings (`NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`).
5. Click **Deploy**!
