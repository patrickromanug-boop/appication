import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'LS Services Admin Portal',
  description: 'Management portal for LS Services Jobs platform',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-slate-50 antialiased">{children}</body>
    </html>
  );
}
