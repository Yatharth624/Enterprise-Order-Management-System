import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { DashboardSummary } from '../types';
import { ShoppingBag, DollarSign, Clock, AlertTriangle, AlertCircle, RefreshCw } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchSummary = async () => {
    setLoading(true);
    try {
      const data = await api.getDashboardSummary();
      setSummary(data);
    } catch (err) {
      console.error('Failed to load dashboard summary', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-slate-500 font-medium">
        <RefreshCw className="animate-spin mr-2" size={20} /> Loading Enterprise Analytics...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Enterprise Dashboard Overview</h2>
          <p className="text-sm text-slate-500">Real-time system health, revenue & inventory analytics</p>
        </div>
        <button
          onClick={fetchSummary}
          className="flex items-center space-x-2 bg-white border border-slate-300 hover:bg-slate-50 px-3.5 py-2 rounded-lg text-sm font-semibold text-slate-700 shadow-sm transition"
        >
          <RefreshCw size={16} />
          <span>Refresh Analytics</span>
        </button>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Orders</span>
            <div className="text-3xl font-extrabold text-slate-900 mt-1">{summary?.totalOrders ?? 0}</div>
          </div>
          <div className="bg-blue-50 text-blue-600 p-3 rounded-xl">
            <ShoppingBag size={24} />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Revenue</span>
            <div className="text-3xl font-extrabold text-slate-900 mt-1">
              ${summary?.totalRevenue != null ? Number(summary.totalRevenue).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>
          <div className="bg-emerald-50 text-emerald-600 p-3 rounded-xl">
            <DollarSign size={24} />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending Orders</span>
            <div className="text-3xl font-extrabold text-amber-600 mt-1">{summary?.pendingOrders ?? 0}</div>
          </div>
          <div className="bg-amber-50 text-amber-600 p-3 rounded-xl">
            <Clock size={24} />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Failed Payments</span>
            <div className="text-3xl font-extrabold text-rose-600 mt-1">{summary?.failedPayments ?? 0}</div>
          </div>
          <div className="bg-rose-50 text-rose-600 p-3 rounded-xl">
            <AlertCircle size={24} />
          </div>
        </div>
      </div>

      {/* Low Stock Warning Section */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="text-amber-500" size={20} />
            <h3 className="font-bold text-slate-800">Low Stock Inventory Alerts</h3>
          </div>
          <span className="text-xs bg-amber-100 text-amber-800 px-2.5 py-1 rounded-full font-semibold">
            Threshold &lt; 10 units
          </span>
        </div>

        {summary?.lowStockProducts && summary.lowStockProducts.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 text-slate-500 uppercase text-xs font-semibold">
                  <th className="py-3 px-5">Product Name</th>
                  <th className="py-3 px-5">SKU</th>
                  <th className="py-3 px-5">Warehouse</th>
                  <th className="py-3 px-5 text-right">Available Stock</th>
                  <th className="py-3 px-5 text-right">Reserved Stock</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {summary.lowStockProducts.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3.5 px-5 font-semibold text-slate-800">{item.productName}</td>
                    <td className="py-3.5 px-5 text-slate-500 font-mono text-xs">{item.productSku}</td>
                    <td className="py-3.5 px-5 text-slate-600">{item.warehouseName}</td>
                    <td className="py-3.5 px-5 text-right font-bold text-rose-600">
                      {item.quantityAvailable} units
                    </td>
                    <td className="py-3.5 px-5 text-right text-slate-500">{item.quantityReserved} units</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-8 text-center text-slate-500 text-sm">
            All product inventory levels are healthy across warehouses.
          </div>
        )}
      </div>
    </div>
  );
};
