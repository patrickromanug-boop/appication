'use client';

import { useState, useEffect } from 'react';
import { supabase } from '@/lib/supabase';
import { 
  Briefcase, 
  Users, 
  FileText, 
  Plus, 
  Search, 
  Bell, 
  CheckCircle, 
  Clock, 
  Send,
  Building2,
  Trash2,
  Megaphone,
  Globe,
  Phone,
  ShieldCheck,
  Image as ImageIcon,
  ExternalLink,
  Edit,
  Eye
} from 'lucide-react';

interface Job {
  id: string;
  title: string;
  organization: string;
  purpose: string;
  requirements: string;
  deadline: string;
  status: string;
  created_at: string;
}

interface Application {
  id: string;
  job_id: string;
  user_id: string;
  status: string;
  applied_at: string;
}

interface Profile {
  id: string;
  full_name: string;
  phone: string;
  role: string;
  created_at: string;
}

interface CompanyAd {
  id: string;
  company_name: string;
  headline: string;
  description: string;
  image_url: string;
  website_url: string;
  contact_phone: string;
  status: string;
  created_at: string;
}

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'jobs' | 'applications' | 'users' | 'ads'>('jobs');
  const [jobs, setJobs] = useState<Job[]>([]);
  const [applications, setApplications] = useState<Application[]>([]);
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [companyAds, setCompanyAds] = useState<CompanyAd[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Job Modal State
  const [showAddJobModal, setShowAddJobModal] = useState(false);
  const [newJobTitle, setNewJobTitle] = useState('');
  const [newJobOrg, setNewJobOrg] = useState('');
  const [newJobPurpose, setNewJobPurpose] = useState('');
  const [newJobRequirements, setNewJobRequirements] = useState('');
  const [newJobDeadline, setNewJobDeadline] = useState('');
  const [submittingJob, setSubmittingJob] = useState(false);

  // Ad Modal State
  const [showAddAdModal, setShowAddAdModal] = useState(false);
  const [adCompanyName, setAdCompanyName] = useState('');
  const [adHeadline, setAdHeadline] = useState('');
  const [adDescription, setAdDescription] = useState('');
  const [adImageUrl, setAdImageUrl] = useState('');
  const [adWebsiteUrl, setAdWebsiteUrl] = useState('');
  const [adContactPhone, setAdContactPhone] = useState('');
  const [submittingAd, setSubmittingAd] = useState(false);

  // View Details Modal State
  const [selectedJobDetails, setSelectedJobDetails] = useState<Job | null>(null);

  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const showNotification = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 4000);
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const { data: jobsData } = await supabase.from('jobs').select('*').order('created_at', { ascending: false });
      if (jobsData) setJobs(jobsData);

      const { data: appsData } = await supabase.from('applications').select('*').order('applied_at', { ascending: false });
      if (appsData) setApplications(appsData);

      const { data: profilesData } = await supabase.from('profiles').select('*').order('created_at', { ascending: false });
      if (profilesData) setProfiles(profilesData);

      const { data: adsData } = await supabase.from('company_ads').select('*').order('created_at', { ascending: false });
      if (adsData) setCompanyAds(adsData);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const openAddJobModal = () => {
    const futureDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    setNewJobDeadline(futureDate);
    setShowAddJobModal(true);
  };

  const handleCreateJob = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newJobTitle || !newJobOrg || !newJobDeadline) {
      alert('Please fill in required fields');
      return;
    }

    setSubmittingJob(true);
    try {
      let locationId: string | null = null;
      let categoryId: string | null = null;
      let jobTypeId: string | null = null;

      const { data: locs } = await supabase.from('locations').select('id').limit(1);
      if (locs && locs.length > 0) locationId = locs[0].id;

      const { data: cats } = await supabase.from('categories').select('id').limit(1);
      if (cats && cats.length > 0) categoryId = cats[0].id;

      const { data: types } = await supabase.from('job_types').select('id').limit(1);
      if (types && types.length > 0) jobTypeId = types[0].id;

      const jobPayload: any = {
        title: newJobTitle,
        organization: newJobOrg,
        purpose: newJobPurpose || 'Job opportunity published via Admin Portal',
        requirements: newJobRequirements || 'Minimum qualifications required.',
        deadline: newJobDeadline,
        status: 'active',
      };

      if (locationId) jobPayload.location_id = locationId;
      if (categoryId) jobPayload.category_id = categoryId;
      if (jobTypeId) jobPayload.job_type_id = jobTypeId;

      const { error } = await supabase.from('jobs').insert([jobPayload]);

      if (error) {
        throw new Error(error.message);
      }

      showNotification('✅ New job published successfully! Live in mobile app.');
      setShowAddJobModal(false);
      setNewJobTitle('');
      setNewJobOrg('');
      setNewJobPurpose('');
      setNewJobRequirements('');
      setNewJobDeadline('');
      fetchData();
    } catch (err: any) {
      alert('Error creating job: ' + (err.message || 'Check connection or Supabase settings.'));
    } finally {
      setSubmittingJob(false);
    }
  };

  const handleDeleteJob = async (jobId: string, jobTitle: string) => {
    if (!confirm(`Are you sure you want to permanently delete "${jobTitle}"?`)) return;

    try {
      const { error } = await supabase.from('jobs').delete().eq('id', jobId);
      if (error) throw new Error(error.message);

      showNotification('🗑️ Job deleted permanently.');
      fetchData();
    } catch (err: any) {
      alert('Error deleting job: ' + err.message);
    }
  };

  const handleCreateAd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!adCompanyName || !adHeadline) {
      alert('Company name and headline are required.');
      return;
    }

    setSubmittingAd(true);
    try {
      const { error } = await supabase.from('company_ads').insert([
        {
          company_name: adCompanyName,
          headline: adHeadline,
          description: adDescription || '',
          image_url: adImageUrl || '',
          website_url: adWebsiteUrl || '',
          contact_phone: adContactPhone || '',
          status: 'active',
        },
      ]);

      if (error) throw new Error(error.message);

      showNotification('🚀 Company advertisement published live!');
      setShowAddAdModal(false);
      setAdCompanyName('');
      setAdHeadline('');
      setAdDescription('');
      setAdImageUrl('');
      setAdWebsiteUrl('');
      setAdContactPhone('');
      fetchData();
    } catch (err: any) {
      alert('Error posting advertisement: ' + err.message);
    } finally {
      setSubmittingAd(false);
    }
  };

  const handleDeleteAd = async (adId: string) => {
    if (!confirm('Are you sure you want to delete this company ad?')) return;

    try {
      const { error } = await supabase.from('company_ads').delete().eq('id', adId);
      if (error) throw new Error(error.message);

      showNotification('🗑️ Advertisement removed.');
      fetchData();
    } catch (err: any) {
      alert('Error deleting ad: ' + err.message);
    }
  };

  const filteredJobs = (jobs || []).filter(j => 
    (j.title || '').toLowerCase().includes((searchTerm || '').toLowerCase()) || 
    (j.organization || '').toLowerCase().includes((searchTerm || '').toLowerCase())
  );

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col font-sans">
      {/* Toast Alert */}
      {toast && (
        <div className="fixed top-5 right-5 z-50 bg-emerald-600 text-white px-5 py-3 rounded-xl shadow-2xl flex items-center gap-3 animate-bounce">
          <Bell className="w-5 h-5" />
          <span className="font-medium text-sm">{toast}</span>
        </div>
      )}

      {/* Header Bar */}
      <header className="border-b border-slate-800 bg-slate-950/90 backdrop-blur sticky top-0 z-40 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white font-bold text-lg shadow-lg shadow-blue-500/20">
            LS
          </div>
          <div>
            <h1 className="text-lg font-bold text-white leading-none">LS Services Admin Portal</h1>
            <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> Admin Access • Uganda Operations
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={() => setShowAddAdModal(true)}
            className="bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 border border-purple-500/30 text-sm font-semibold px-4 py-2.5 rounded-xl flex items-center gap-2 transition-all"
          >
            <Megaphone className="w-4 h-4 text-purple-400" /> Post Company Ad
          </button>
          <button 
            onClick={openAddJobModal}
            className="bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold px-4 py-2.5 rounded-xl flex items-center gap-2 shadow-lg shadow-blue-600/30 transition-all hover:scale-105"
          >
            <Plus className="w-4 h-4" /> Post New Job
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        
        {/* Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Total Active Jobs</p>
              <h3 className="text-2xl font-black text-white mt-1">{jobs.length}</h3>
            </div>
            <div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center">
              <Briefcase className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Total Applications</p>
              <h3 className="text-2xl font-black text-white mt-1">{applications.length}</h3>
            </div>
            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <FileText className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Registered Candidates</p>
              <h3 className="text-2xl font-black text-white mt-1">{profiles.length}</h3>
            </div>
            <div className="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center">
              <Users className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/50 rounded-2xl p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Company Ads Active</p>
              <h3 className="text-2xl font-black text-white mt-1">{companyAds.length}</h3>
            </div>
            <div className="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <Megaphone className="w-6 h-6" />
            </div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-slate-800 gap-8">
          <button 
            onClick={() => setActiveTab('jobs')}
            className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-all ${
              activeTab === 'jobs' ? 'border-blue-500 text-blue-400' : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Briefcase className="w-4 h-4" /> Jobs ({jobs.length})
          </button>
          <button 
            onClick={() => setActiveTab('applications')}
            className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-all ${
              activeTab === 'applications' ? 'border-blue-500 text-blue-400' : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <FileText className="w-4 h-4" /> Applications ({applications.length})
          </button>
          <button 
            onClick={() => setActiveTab('users')}
            className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-all ${
              activeTab === 'users' ? 'border-blue-500 text-blue-400' : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Users className="w-4 h-4" /> Candidates ({profiles.length})
          </button>
          <button 
            onClick={() => setActiveTab('ads')}
            className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-all ${
              activeTab === 'ads' ? 'border-purple-500 text-purple-400' : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Megaphone className="w-4 h-4" /> Company Ads ({companyAds.length})
          </button>
        </div>

        {/* Search Bar */}
        <div className="relative">
          <Search className="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
          <input 
            type="text" 
            placeholder="Search job titles, organization names, candidates..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-800/80 border border-slate-700 text-white text-sm rounded-xl pl-12 pr-4 py-3 focus:outline-none focus:border-blue-500 transition-all placeholder-slate-500"
          />
        </div>

        {/* Content Section */}
        {loading ? (
          <div className="py-20 text-center text-slate-400 text-sm animate-pulse">Loading portal database records...</div>
        ) : (
          <div className="space-y-4">
            {/* JOBS TAB */}
            {activeTab === 'jobs' && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredJobs.length === 0 ? (
                  <div className="col-span-2 text-center py-12 text-slate-500 text-sm">No active jobs found in portal database.</div>
                ) : (
                  filteredJobs.map((job) => (
                    <div key={job.id} className="bg-slate-800/50 border border-slate-700/60 rounded-2xl p-5 hover:border-slate-600 transition-all flex flex-col justify-between">
                      <div>
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <span className="text-[10px] font-bold uppercase tracking-wider text-blue-400 bg-blue-500/10 px-2.5 py-1 rounded-md border border-blue-500/20">
                              {job.status}
                            </span>
                            <h4 className="text-base font-bold text-white mt-2">{job.title}</h4>
                            <p className="text-xs text-slate-400 flex items-center gap-1.5 mt-1">
                              <Building2 className="w-3.5 h-3.5 text-slate-500" /> {job.organization}
                            </p>
                          </div>
                          <span className="text-xs font-semibold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-md border border-amber-500/20 flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" /> Due: {job.deadline}
                          </span>
                        </div>

                        {/* Job Summary Section */}
                        <div className="mt-3 bg-slate-900/40 p-3 rounded-xl border border-slate-700/40">
                          <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Job Summary</p>
                          <p className="text-xs text-slate-300 line-clamp-2 leading-relaxed">{job.purpose || 'No summary provided.'}</p>
                        </div>

                        {/* Qualifications Section */}
                        <div className="mt-2 bg-slate-900/40 p-3 rounded-xl border border-slate-700/40">
                          <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Qualifications & Requirements</p>
                          <p className="text-xs text-slate-300 line-clamp-2 leading-relaxed">{job.requirements || 'Standard qualifications apply.'}</p>
                        </div>
                      </div>

                      {/* Action Bar */}
                      <div className="mt-4 pt-3 border-t border-slate-700/50 flex items-center justify-between">
                        <button 
                          onClick={() => setSelectedJobDetails(job)}
                          className="text-xs text-blue-400 hover:text-blue-300 font-semibold flex items-center gap-1"
                        >
                          <Eye className="w-3.5 h-3.5" /> View Full Details
                        </button>
                        
                        <button 
                          onClick={() => handleDeleteJob(job.id, job.title)}
                          className="text-xs text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 px-3 py-1.5 rounded-lg border border-red-500/20 font-semibold flex items-center gap-1 transition-all"
                        >
                          <Trash2 className="w-3.5 h-3.5" /> Delete Job
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {/* APPLICATIONS TAB */}
            {activeTab === 'applications' && (
              <div className="bg-slate-800/50 border border-slate-700/60 rounded-2xl overflow-hidden">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900/80 border-b border-slate-700 text-slate-400 uppercase font-semibold">
                    <tr>
                      <th className="px-6 py-4">Application ID</th>
                      <th className="px-6 py-4">Job Reference</th>
                      <th className="px-6 py-4">Candidate ID</th>
                      <th className="px-6 py-4">Status</th>
                      <th className="px-6 py-4">Submitted Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {applications.length === 0 ? (
                      <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-500">No applications registered yet.</td></tr>
                    ) : (
                      applications.map(app => (
                        <tr key={app.id || Math.random().toString()} className="hover:bg-slate-800/80">
                          <td className="px-6 py-4 font-mono text-slate-400">{app.id ? app.id.substring(0, 8) : '—'}...</td>
                          <td className="px-6 py-4 font-mono text-blue-400">{app.job_id ? app.job_id.substring(0, 8) : '—'}...</td>
                          <td className="px-6 py-4 font-mono text-purple-400">{app.user_id ? app.user_id.substring(0, 8) : '—'}...</td>
                          <td className="px-6 py-4">
                            <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2.5 py-1 rounded-md text-[10px] font-bold uppercase">
                              {app.status || 'Submitted'}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-slate-400">{app.applied_at ? new Date(app.applied_at).toLocaleDateString() : 'N/A'}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}

            {/* CANDIDATES TAB */}
            {activeTab === 'users' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {profiles.map(p => (
                  <div key={p.id || Math.random().toString()} className="bg-slate-800/50 border border-slate-700/60 rounded-2xl p-5">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-slate-700 flex items-center justify-center font-bold text-white text-sm">
                        {(p.full_name && p.full_name.length > 0) ? p.full_name[0].toUpperCase() : 'U'}
                      </div>
                      <div>
                        <h5 className="text-sm font-bold text-white">{p.full_name || 'Anonymous User'}</h5>
                        <span className="text-xs text-slate-400">{p.phone || 'No phone'}</span>
                      </div>
                    </div>
                    <div className="mt-4 pt-3 border-t border-slate-700/50 flex items-center justify-between text-xs text-slate-400">
                      <span>Role: <strong className="text-blue-400 capitalize">{p.role || 'Candidate'}</strong></span>
                      <span>Joined {p.created_at ? new Date(p.created_at).toLocaleDateString() : 'N/A'}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* COMPANY ADVERTISING TAB */}
            {activeTab === 'ads' && (
              <div className="space-y-4">
                <div className="flex justify-between items-center bg-purple-900/20 border border-purple-500/30 p-4 rounded-2xl">
                  <div>
                    <h3 className="text-sm font-bold text-purple-200">Company Promotional Banners & Ads</h3>
                    <p className="text-xs text-purple-300/80 mt-0.5">Advertise sponsor companies, featured training, or corporate placements directly inside the mobile app.</p>
                  </div>
                  <button 
                    onClick={() => setShowAddAdModal(true)}
                    className="bg-purple-600 hover:bg-purple-500 text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-lg shadow-purple-600/30"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Advertisement
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {companyAds.length === 0 ? (
                    <div className="col-span-2 text-center py-12 text-slate-500 text-sm">No active company advertisements. Click &quot;Add Advertisement&quot; to publish one!</div>
                  ) : (
                    companyAds.map(ad => (
                      <div key={ad.id} className="bg-slate-800/50 border border-slate-700/60 rounded-2xl p-5 space-y-3">
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex items-center gap-3">
                            {ad.image_url ? (
                              <img src={ad.image_url} alt={ad.company_name} className="w-12 h-12 rounded-xl object-cover border border-slate-700" />
                            ) : (
                              <div className="w-12 h-12 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400 flex items-center justify-center font-bold">
                                {ad.company_name[0] || 'A'}
                              </div>
                            )}
                            <div>
                              <h4 className="text-sm font-bold text-white">{ad.company_name}</h4>
                              <p className="text-xs text-purple-400 font-medium">{ad.headline}</p>
                            </div>
                          </div>
                          <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            {ad.status}
                          </span>
                        </div>

                        {ad.description && (
                          <p className="text-xs text-slate-300 leading-relaxed bg-slate-900/30 p-2.5 rounded-xl border border-slate-800">
                            {ad.description}
                          </p>
                        )}

                        <div className="flex items-center justify-between pt-2 border-t border-slate-700/40 text-xs text-slate-400">
                          <div className="flex items-center gap-3">
                            {ad.website_url && (
                              <a href={ad.website_url} target="_blank" rel="noreferrer" className="text-blue-400 flex items-center gap-1 hover:underline">
                                <Globe className="w-3 h-3" /> Website
                              </a>
                            )}
                            {ad.contact_phone && (
                              <span className="flex items-center gap-1 text-slate-300">
                                <Phone className="w-3 h-3 text-slate-400" /> {ad.contact_phone}
                              </span>
                            )}
                          </div>
                          <button 
                            onClick={() => handleDeleteAd(ad.id)}
                            className="text-red-400 hover:text-red-300 bg-red-500/10 px-2.5 py-1 rounded-lg border border-red-500/20 font-semibold flex items-center gap-1"
                          >
                            <Trash2 className="w-3 h-3" /> Delete
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* VIEW FULL JOB DETAILS MODAL */}
      {selectedJobDetails && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <span className="text-[10px] font-bold uppercase text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded">
                  {selectedJobDetails.status}
                </span>
                <h3 className="text-lg font-bold text-white mt-1">{selectedJobDetails.title}</h3>
                <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                  <Building2 className="w-3.5 h-3.5" /> {selectedJobDetails.organization}
                </p>
              </div>
              <button onClick={() => setSelectedJobDetails(null)} className="text-slate-400 hover:text-white text-lg font-bold">✕</button>
            </div>

            <div className="space-y-3 max-h-[60vh] overflow-y-auto pr-2">
              <div className="bg-slate-800/60 p-3 rounded-xl border border-slate-700/60">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Application Deadline</p>
                <p className="text-sm font-semibold text-amber-400 flex items-center gap-1.5">
                  <Clock className="w-4 h-4" /> {selectedJobDetails.deadline}
                </p>
              </div>

              <div className="bg-slate-800/60 p-4 rounded-xl border border-slate-700/60 space-y-1">
                <p className="text-xs font-bold text-blue-400 uppercase tracking-wider">Job Summary & Overview</p>
                <p className="text-xs text-slate-200 whitespace-pre-wrap leading-relaxed">{selectedJobDetails.purpose}</p>
              </div>

              <div className="bg-slate-800/60 p-4 rounded-xl border border-slate-700/60 space-y-1">
                <p className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Qualifications & Requirements</p>
                <p className="text-xs text-slate-200 whitespace-pre-wrap leading-relaxed">{selectedJobDetails.requirements}</p>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-between items-center">
              <button 
                onClick={() => {
                  handleDeleteJob(selectedJobDetails.id, selectedJobDetails.title);
                  setSelectedJobDetails(null);
                }}
                className="bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5"
              >
                <Trash2 className="w-3.5 h-3.5" /> Delete Job
              </button>

              <button 
                onClick={() => setSelectedJobDetails(null)}
                className="bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold px-4 py-2 rounded-xl"
              >
                Close Window
              </button>
            </div>
          </div>
        </div>
      )}

      {/* POST NEW JOB MODAL */}
      {showAddJobModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Briefcase className="w-5 h-5 text-blue-500" /> Post New Job Opportunity
              </h3>
              <button 
                onClick={() => setShowAddJobModal(false)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateJob} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Job Title *</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. Senior IT Officer / Field Logistics Coordinator" 
                  value={newJobTitle}
                  onChange={e => setNewJobTitle(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Organization / Company Name *</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. National Agricultural Research Organisation (NARO)" 
                  value={newJobOrg}
                  onChange={e => setNewJobOrg(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Application Deadline *</label>
                  <input 
                    type="date" 
                    required
                    value={newJobDeadline}
                    onChange={e => setNewJobDeadline(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Status</label>
                  <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl px-3 py-2.5 text-xs font-semibold flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4" /> Active Job Listing
                  </div>
                </div>
              </div>

              {/* Job Summary Section */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Job Summary & Key Duties *</label>
                <textarea 
                  rows={3}
                  required
                  placeholder="Provide a clear, detailed summary of the job role and key responsibilities..." 
                  value={newJobPurpose}
                  onChange={e => setNewJobPurpose(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              {/* Qualifications & Requirements Section */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Qualifications & Experience Requirements *</label>
                <textarea 
                  rows={3}
                  required
                  placeholder="• Degree in Computer Science or IT&#10;• Minimum 3 years relevant experience&#10;• Good communication and technical skills" 
                  value={newJobRequirements}
                  onChange={e => setNewJobRequirements(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-3">
                <button 
                  type="button" 
                  onClick={() => setShowAddJobModal(false)}
                  className="px-4 py-2.5 rounded-xl text-sm font-semibold text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={submittingJob}
                  className="bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold px-5 py-2.5 rounded-xl shadow-lg shadow-blue-600/30 flex items-center gap-2"
                >
                  {submittingJob ? 'Publishing...' : <><Send className="w-4 h-4" /> Publish Job Opportunity</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* POST COMPANY AD MODAL */}
      {showAddAdModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Megaphone className="w-5 h-5 text-purple-400" /> Create Company Advertisement
              </h3>
              <button onClick={() => setShowAddAdModal(false)} className="text-slate-400 hover:text-white text-lg font-bold">✕</button>
            </div>

            <form onSubmit={handleCreateAd} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Company / Sponsor Name *</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. Stanbic Bank Uganda / MTN Uganda" 
                  value={adCompanyName}
                  onChange={e => setAdCompanyName(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Headline / Offer Catchphrase *</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. Free Professional Resume Audit & Training Programs!" 
                  value={adHeadline}
                  onChange={e => setAdHeadline(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Banner Image / Logo URL</label>
                <input 
                  type="url" 
                  placeholder="https://example.com/banner-photo.jpg" 
                  value={adImageUrl}
                  onChange={e => setAdImageUrl(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500"
                />
                {adImageUrl && (
                  <div className="mt-2 p-2 bg-slate-950 rounded-xl border border-slate-800 flex items-center gap-3">
                    <img src={adImageUrl} alt="Preview" className="w-12 h-12 object-cover rounded-lg" onError={(e) => (e.currentTarget.style.display = 'none')} />
                    <span className="text-xs text-emerald-400 flex items-center gap-1"><ImageIcon className="w-3.5 h-3.5" /> Photo Preview Ready</span>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Ad Description & Offer Details</label>
                <textarea 
                  rows={2}
                  placeholder="Detail what the company is offering or advertising..." 
                  value={adDescription}
                  onChange={e => setAdDescription(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Website Link (Optional)</label>
                  <input 
                    type="url" 
                    placeholder="https://company.com" 
                    value={adWebsiteUrl}
                    onChange={e => setAdWebsiteUrl(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Contact Phone (Optional)</label>
                  <input 
                    type="text" 
                    placeholder="+256 700 000 000" 
                    value={adContactPhone}
                    onChange={e => setAdContactPhone(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500"
                  />
                </div>
              </div>

              <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-3">
                <button 
                  type="button" 
                  onClick={() => setShowAddAdModal(false)}
                  className="px-4 py-2.5 rounded-xl text-sm font-semibold text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={submittingAd}
                  className="bg-purple-600 hover:bg-purple-500 text-white text-sm font-semibold px-5 py-2.5 rounded-xl shadow-lg shadow-purple-600/30 flex items-center gap-2"
                >
                  {submittingAd ? 'Publishing...' : <><Send className="w-4 h-4" /> Publish Company Ad</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
