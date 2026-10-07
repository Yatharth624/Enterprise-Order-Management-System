import React, { useState } from 'react';
import { api } from '../api/client';
import { AuthResponse } from '../types';
import { Shield, Lock, Mail, KeyRound } from 'lucide-react';

interface LoginPageProps {
  onLoginSuccess: (authData: AuthResponse) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('admin@enterprise.com');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const res = await api.login(email, password);
      localStorage.setItem('token', res.token);
      onLoginSuccess(res);
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const setPreset = (presetEmail: string, presetPass: string) => {
    setEmail(presetEmail);
    setPassword(presetPass);
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-slate-800 border border-slate-700 rounded-2xl shadow-2xl p-8">
        <div className="flex flex-col items-center mb-6">
          <div className="bg-indigo-600 p-3 rounded-2xl shadow-lg text-white mb-3">
            <Shield size={32} />
          </div>
          <h2 className="text-2xl font-bold text-slate-100">Enterprise OMS Login</h2>
          <p className="text-sm text-slate-400 text-center mt-1">
            Production-style Order Management System with Role-Based Access
          </p>
        </div>

        {error && (
          <div className="mb-4 bg-rose-950/80 border border-rose-700 text-rose-300 text-xs p-3 rounded-lg">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Email Address</label>
            <div className="relative">
              <Mail className="absolute left-3 top-3 text-slate-500" size={18} />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full bg-slate-900 border border-slate-700 rounded-lg pl-10 pr-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-3 text-slate-500" size={18} />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full bg-slate-900 border border-slate-700 rounded-lg pl-10 pr-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 px-4 rounded-xl shadow-lg transition duration-200 disabled:opacity-50"
          >
            {loading ? 'Authenticating...' : 'Sign In to Dashboard'}
          </button>
        </form>

        <div className="mt-8 border-t border-slate-700/60 pt-4">
          <div className="flex items-center space-x-1 text-xs font-semibold text-slate-400 mb-2">
            <KeyRound size={14} />
            <span>Preset Accounts for Evaluation</span>
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <button
              onClick={() => setPreset('admin@enterprise.com', 'admin123')}
              className="bg-slate-700/60 hover:bg-slate-700 text-slate-300 p-2 rounded-lg text-left"
            >
              <span className="font-semibold text-indigo-400 block">ADMIN</span>
              <span className="text-[11px] text-slate-400">admin@enterprise.com</span>
            </button>
            <button
              onClick={() => setPreset('customer@enterprise.com', 'customer123')}
              className="bg-slate-700/60 hover:bg-slate-700 text-slate-300 p-2 rounded-lg text-left"
            >
              <span className="font-semibold text-emerald-400 block">CUSTOMER</span>
              <span className="text-[11px] text-slate-400">customer@enterprise.com</span>
            </button>
            <button
              onClick={() => setPreset('warehouse@enterprise.com', 'manager123')}
              className="bg-slate-700/60 hover:bg-slate-700 text-slate-300 p-2 rounded-lg text-left"
            >
              <span className="font-semibold text-amber-400 block">WAREHOUSE</span>
              <span className="text-[11px] text-slate-400">warehouse@enterprise.com</span>
            </button>
            <button
              onClick={() => setPreset('support@enterprise.com', 'support123')}
              className="bg-slate-700/60 hover:bg-slate-700 text-slate-300 p-2 rounded-lg text-left"
            >
              <span className="font-semibold text-purple-400 block">SUPPORT</span>
              <span className="text-[11px] text-slate-400">support@enterprise.com</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
