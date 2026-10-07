import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Package, ShoppingBag, Layers, Warehouse } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const links = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/products', label: 'Products Catalog', icon: Package },
    { to: '/orders', label: 'Orders & Tracking', icon: ShoppingBag },
    { to: '/inventory', label: 'Inventory Management', icon: Warehouse },
  ];

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 min-h-screen p-4 border-r border-slate-800">
      <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-3">
        System Navigation
      </div>
      <nav className="space-y-1">
        {links.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                  isActive
                    ? 'bg-indigo-600 text-white font-semibold shadow'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-100'
                }`
              }
            >
              <Icon size={18} />
              <span>{link.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="mt-10 p-3 bg-slate-800/60 rounded-xl border border-slate-700/50">
        <div className="flex items-center space-x-2 text-indigo-400 font-semibold text-xs mb-1">
          <Layers size={14} />
          <span>System Architecture</span>
        </div>
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Driven by Spring Boot 3, Redis Locks, Kafka Async Events, JWT Security & Idempotency.
        </p>
      </div>
    </aside>
  );
};
