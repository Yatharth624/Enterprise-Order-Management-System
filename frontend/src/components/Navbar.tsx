import React from 'react';
import { LogOut, User, ShieldCheck } from 'lucide-react';
import { User as UserType } from '../types';

interface NavbarProps {
  user: UserType | null;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onLogout }) => {
  return (
    <header className="bg-slate-900 text-white shadow-md px-6 py-4 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center space-x-3">
        <div className="bg-indigo-600 p-2 rounded-lg font-bold text-lg text-white">OMS</div>
        <div>
          <h1 className="font-bold text-lg leading-tight text-slate-100">Enterprise Order Management</h1>
          <p className="text-xs text-slate-400">Production Microservices & EDA System</p>
        </div>
      </div>

      {user && (
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2 bg-slate-800 px-3 py-1.5 rounded-full text-sm">
            <User size={16} className="text-indigo-400" />
            <span className="font-medium text-slate-200">{user.fullName}</span>
            <span className="bg-indigo-950 text-indigo-300 text-xs px-2 py-0.5 rounded-full font-semibold flex items-center gap-1">
              <ShieldCheck size={12} />
              {user.role}
            </span>
          </div>

          <button
            onClick={onLogout}
            className="flex items-center space-x-1 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold px-3 py-1.5 rounded-md transition"
          >
            <LogOut size={14} />
            <span>Logout</span>
          </button>
        </div>
      )}
    </header>
  );
};
