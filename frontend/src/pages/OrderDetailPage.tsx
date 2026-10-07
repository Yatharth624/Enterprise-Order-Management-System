import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Order, OrderStatus } from '../types';
import { ArrowLeft, CheckCircle2, Circle, Clock, CreditCard, XCircle, AlertCircle, RefreshCw } from 'lucide-react';

export const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchOrder = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await api.getOrderById(id);
      setOrder(data);
    } catch (err) {
      console.error('Failed to load order details', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const handlePayNow = async (simulateMode = 'FORCE_SUCCESS') => {
    if (!order) return;
    setActionLoading(true);
    try {
      await api.processPayment(order.id, order.totalAmount, 'CREDIT_CARD', simulateMode);
      await fetchOrder();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || 'Payment processing failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelOrder = async () => {
    if (!order) return;
    if (!confirm('Are you sure you want to cancel this order?')) return;
    setActionLoading(true);
    try {
      await api.cancelOrder(order.id, 'User requested cancellation');
      await fetchOrder();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || 'Order cancellation failed');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-slate-500 font-medium">
        <RefreshCw className="animate-spin mr-2" size={20} /> Loading Order Lifecycle Details...
      </div>
    );
  }

  if (!order) {
    return <div className="text-center py-12 text-slate-500">Order not found.</div>;
  }

  // Lifecycle steps for state machine visualizer
  const lifecycleSteps: { label: string; status: OrderStatus }[] = [
    { label: 'Created', status: 'CREATED' },
    { label: 'Inventory Reserved', status: 'INVENTORY_RESERVED' },
    { label: 'Payment', status: 'PAID' },
    { label: 'Confirmed', status: 'CONFIRMED' },
    { label: 'Shipped', status: 'SHIPPED' },
    { label: 'Out for Delivery', status: 'OUT_FOR_DELIVERY' },
    { label: 'Delivered', status: 'DELIVERED' },
  ];

  const getStepIndex = (status: OrderStatus) => {
    switch (status) {
      case 'CREATED': return 0;
      case 'INVENTORY_RESERVED': return 1;
      case 'PAYMENT_PENDING': return 1;
      case 'PAID': return 2;
      case 'CONFIRMED': return 3;
      case 'SHIPPED': return 4;
      case 'OUT_FOR_DELIVERY': return 5;
      case 'DELIVERED': return 6;
      default: return -1;
    }
  };

  const currentStep = getStepIndex(order.status);
  const isFailed = ['PAYMENT_FAILED', 'INVENTORY_FAILED', 'CANCELLED'].includes(order.status);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/orders')}
          className="flex items-center space-x-1 text-slate-600 hover:text-slate-900 text-sm font-semibold transition"
        >
          <ArrowLeft size={16} />
          <span>Back to Orders</span>
        </button>

        <div className="flex items-center space-x-2">
          {['CREATED', 'INVENTORY_RESERVED', 'PAYMENT_PENDING', 'PAYMENT_FAILED'].includes(order.status) && (
            <>
              <button
                onClick={() => handlePayNow('FORCE_SUCCESS')}
                disabled={actionLoading}
                className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow flex items-center space-x-1.5 transition disabled:opacity-50"
              >
                <CreditCard size={14} />
                <span>Simulate Payment (SUCCESS)</span>
              </button>

              <button
                onClick={() => handlePayNow('FORCE_FAIL')}
                disabled={actionLoading}
                className="bg-amber-600 hover:bg-amber-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow flex items-center space-x-1.5 transition disabled:opacity-50"
              >
                <AlertCircle size={14} />
                <span>Simulate Payment (FAIL)</span>
              </button>
            </>
          )}

          {!['DELIVERED', 'CANCELLED'].includes(order.status) && (
            <button
              onClick={handleCancelOrder}
              disabled={actionLoading}
              className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-xl text-xs font-bold shadow transition disabled:opacity-50"
            >
              Cancel Order
            </button>
          )}
        </div>
      </div>

      {/* Order Header Card */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">Order ID</span>
          <h2 className="text-2xl font-extrabold text-indigo-600 font-mono">#{order.orderNumber}</h2>
          <p className="text-xs text-slate-500 mt-1">Customer: {order.customerName} ({order.customerEmail})</p>
        </div>

        <div className="text-right">
          <span className="text-xs text-slate-400 block">Total Amount</span>
          <span className="text-3xl font-extrabold text-slate-900">${order.totalAmount.toFixed(2)}</span>
        </div>
      </div>

      {/* Visual State Machine Lifecycle Bar */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h3 className="font-bold text-slate-800 text-base mb-6">Order Lifecycle State Machine Visualizer</h3>

        {isFailed ? (
          <div className="bg-rose-50 border border-rose-200 text-rose-800 p-4 rounded-xl flex items-center space-x-3">
            <XCircle size={24} className="text-rose-600" />
            <div>
              <div className="font-bold text-sm">Order Stopped in Failure State: {order.status}</div>
              <p className="text-xs text-rose-700 mt-0.5">State machine halted progression safely and released stock reservation.</p>
            </div>
          </div>
        ) : (
          <div className="relative flex items-center justify-between">
            <div className="absolute top-1/2 left-0 right-0 h-1 bg-slate-200 -translate-y-1/2 z-0" />
            <div
              className="absolute top-1/2 left-0 h-1 bg-indigo-600 -translate-y-1/2 z-0 transition-all duration-500"
              style={{ width: `${(currentStep / (lifecycleSteps.length - 1)) * 100}%` }}
            />

            {lifecycleSteps.map((step, idx) => {
              const isPassed = currentStep >= idx;
              const isCurrent = currentStep === idx;

              return (
                <div key={step.status} className="relative z-10 flex flex-col items-center">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs transition ${
                      isCurrent
                        ? 'bg-indigo-600 text-white ring-4 ring-indigo-100 shadow-md'
                        : isPassed
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-200 text-slate-500'
                    }`}
                  >
                    {isPassed ? <CheckCircle2 size={16} /> : idx + 1}
                  </div>
                  <span
                    className={`mt-2 text-[11px] font-semibold text-center max-w-[80px] ${
                      isCurrent ? 'text-indigo-600 font-bold' : isPassed ? 'text-slate-800' : 'text-slate-400'
                    }`}
                  >
                    {step.label}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Audit Timeline Log */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h3 className="font-bold text-slate-800 text-base mb-4">State Transition Audit Logs</h3>
        <div className="space-y-4 border-l-2 border-slate-100 pl-4">
          {order.timeline.map((event) => (
            <div key={event.id} className="relative">
              <div className="absolute -left-[21px] top-1.5 w-2.5 h-2.5 rounded-full bg-indigo-600 ring-4 ring-white" />
              <div className="text-sm font-semibold text-slate-800">
                {event.fromStatus ? `${event.fromStatus} ➔ ` : ''}
                <span className="text-indigo-600 font-bold">{event.toStatus}</span>
              </div>
              <p className="text-xs text-slate-500">{event.reason}</p>
              <span className="text-[11px] text-slate-400">{new Date(event.timestamp).toLocaleString()}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
