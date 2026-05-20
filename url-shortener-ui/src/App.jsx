import React, { useState } from 'react';
import { Link2, ArrowRight, Copy, CheckCircle2, AlertCircle } from 'lucide-react';

function App() {
  const [longUrl, setLongUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [shortUrl, setShortUrl] = useState('');
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(false);

  // 🎯 DYNAMIC ENVIRONMENT GATEWAY: Reads your cloud URL on Vercel, or falls back to your local cluster port
  const BACKEND_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:30080";

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setShortUrl('');
    setCopied(false);
    setLoading(true);

    try {
      const response = await fetch(`${BACKEND_URL}/shorten`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          longUrl, 
          customAlias: customAlias.trim() || "" 
        }),
      });

      // 1. SUCCESS: Server responded with standard 200 OK plain text short code
      if (response.ok) {
        const shortCode = await response.text();
        setShortUrl(`${BACKEND_URL}/${shortCode}`); // Maps perfectly to redirection path
        setLongUrl('');
        setCustomAlias('');
        return;
      }

      // 2. ERROR CATCH: Parse structured JSON error payloads safely
      const errorData = await response.json().catch(() => null);
      
      if (errorData) {
        if (errorData.fallbackUrl) {
          setError(errorData.error);
          setShortUrl(`${BACKEND_URL}/${errorData.fallbackUrl}`);
        } else {
          setError(errorData.error || "Bad Request processing transaction.");
        }
      } else {
        setError("An unexpected network error occurred.");
      }

    } catch (err) {
      setError("Could not establish a connection to the backend server.");
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-slate-50/50">
      <div className="w-full max-w-xl bg-white rounded-2xl shadow-xl border border-slate-100 p-8">
        
        {/* Header Logo Component */}
        <div className="flex items-center gap-3 mb-8">
          <div className="p-3 bg-indigo-600 rounded-xl text-white shadow-lg shadow-indigo-100">
            <Link2 size={28} />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">SnapLink</h1>
            <p className="text-sm text-slate-500">Enterprise-grade high-speed URL shortener</p>
          </div>
        </div>

        {/* Input Submission Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">Destination URL</label>
            <input
              type="text"
              placeholder="e.g., https://www.wikipedia.org"
              value={longUrl}
              onChange={(e) => setLongUrl(e.target.value)}
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 text-slate-800 transition-all"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">Custom Alias <span className="text-xs text-slate-400 font-normal">(Optional)</span></label>
            <input
              type="text"
              placeholder="e.g., wiki-facts"
              value={customAlias}
              onChange={(e) => setCustomAlias(e.target.value)}
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 text-slate-800 transition-all"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-slate-900 hover:bg-slate-800 text-white font-medium py-3 px-4 rounded-xl shadow-lg transition-all flex items-center justify-center gap-2 disabled:bg-slate-300"
          >
            {loading ? 'Processing System Locks...' : 'Shorten Link'}
            {!loading && <ArrowRight size={18} />}
          </button>
        </form>

        {/* Notification Feedback Zones */}
        {error && (
          <div className="mt-6 flex items-start gap-3 bg-rose-50 border border-rose-100 p-4 rounded-xl text-rose-700 text-sm animate-fade-in">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <p className="font-medium">{error}</p>
          </div>
        )}

        {shortUrl && (
          <div className="mt-6 bg-emerald-50/50 border border-emerald-100 p-5 rounded-xl animate-fade-in">
            <label className="block text-xs font-semibold text-emerald-800 uppercase tracking-wider mb-2">Your Shortened Link</label>
            <div className="flex gap-2">
              <input
                type="text"
                readOnly
                value={shortUrl}
                className="w-full bg-white px-4 py-2.5 rounded-lg border border-emerald-200 text-emerald-700 font-medium text-sm focus:outline-none"
              />
              <button
                onClick={copyToClipboard}
                className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 rounded-lg flex items-center justify-center transition-all shrink-0 shadow-md shadow-emerald-100"
              >
                {copied ? <CheckCircle2 size={18} /> : <Copy size={18} />}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;