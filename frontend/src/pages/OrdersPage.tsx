import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Order, OrderStatus } from '../types';
import { Eye, Clock, CheckCircle2, AlertOctagon, Truck, XCircle, Box } from 'lucide-react';

export const OrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const res = await api.getOrders(0, 20);
      setOrders(res.content);
    } catch (err) {
      console.error('Failed to fetch orders', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'DELIVERED':
        return <span className="bg-emerald-100 text-emerald-800 border border-emerald-300 text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"><CheckCircle2 size={12}/> DELIVERED</span>;
      case 'CONFIRMED':
      case 'PAID':
        return <span className="bg-blue-100 text-blue-800 border border-blue-300 text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"><CheckCircle2 size={12}/> {status}</span>;
      case 'SHIPPED':
      case 'OUT_FOR_DELIVERY':
        return <span className="bg-purple-100 text-purple-800 border border-purple-300 text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"><Truck size={12}/> {status}</span>;
      case 'CREATED':
      case 'INVENTORY_RESERVED':
      case 'PAYMENT_PENDING':
        return <span className="bg-amber-100 text-amber-800 border border-amber-300 text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"><Clock size={12}/> {status}</span>;
      case 'PAYMENT_FAILED':
      case 'INVENTORY_FAILED':
      case 'CANCELLED':
        return <span className="bg-rose-100 text-rose-800 border border-rose-300 text-xs px-2.5 py-1 rounded-full font-semibold flex items-center gap-1"><XCircle size={12}/> {status}</span>;
      default:
        return <span className="bg-slate-100 text-slate-800 text-xs px-2.5 py-1 rounded-full">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Orders & Lifecycle Management</h2>
          <p className="text-sm text-slate-500">Track order statuses, payment events, and state machine audit timelines</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500 font-medium">Loading orders...</div>
        ) : orders.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 text-slate-500 uppercase text-xs font-semibold">
                  <th className="py-3.5 px-5">Order Number</th>
                  <th className="py-3.5 px-5">Customer</th>
                  <th className="py-3.5 px-5">Status</th>
                  <th className="py-3.5 px-5 text-right">Amount</th>
                  <th className="py-3.5 px-5">Date Created</th>
                  <th className="py-3.5 px-5 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-4 px-5 font-mono font-bold text-indigo-600">#{order.orderNumber}</td>
                    <td className="py-4 px-5">
                      <div className="font-semibold text-slate-800">{order.customerName}</div>
                      <div className="text-xs text-slate-400">{order.customerEmail}</div>
                    </td>
                    <td className="py-4 px-5">{getStatusBadge(order.status)}</td>
                    <td className="py-4 px-5 text-right font-extrabold text-slate-900">${order.totalAmount.toFixed(2)}</td>
                    <td className="py-4 px-5 text-xs text-slate-500">{new Date(order.createdAt).toLocaleString()}</td>
                    <td className="py-4 px-5 text-center">
                      <button
                        onClick={() => navigate(`/orders/${order.id}`)}
                        className="inline-flex items-center space-x-1 text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 px-3 py-1.5 rounded-lg transition"
                      >
                        <Eye size={14} />
                        <span>Timeline</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center text-slate-500">
            <Box size={36} className="mx-auto text-slate-300 mb-2" />
            No orders found. Place an order from the Product Catalog to get started.
          </div>
        )}
      </div>
    </div>
  );
};
