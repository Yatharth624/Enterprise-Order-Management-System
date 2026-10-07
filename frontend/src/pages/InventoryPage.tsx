import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Inventory, User } from '../types';
import { Warehouse as WarehouseIcon, AlertTriangle, Edit2, CheckCircle2 } from 'lucide-react';

interface InventoryPageProps {
  user: User | null;
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ user }) => {
  const [inventoryList, setInventoryList] = useState<Inventory[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingItem, setEditingItem] = useState<Inventory | null>(null);
  const [newStock, setNewStock] = useState<number>(0);
  const [message, setMessage] = useState('');

  const fetchInventory = async () => {
    setLoading(true);
    try {
      const data = await api.getInventory();
      setInventoryList(data);
    } catch (err) {
      console.error('Failed to load inventory', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, []);

  const handleUpdateStock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingItem) return;

    try {
      await api.updateStock(editingItem.productId, editingItem.warehouseId, newStock);
      setMessage(`Stock for ${editingItem.productName} updated to ${newStock} units`);
      setTimeout(() => setMessage(''), 3000);
      setEditingItem(null);
      fetchInventory();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || 'Failed to update stock');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Inventory & Warehouse Management</h2>
          <p className="text-sm text-slate-500">Monitor warehouse stock levels, optimistic locks & stock reservations</p>
        </div>
      </div>

      {message && (
        <div className="bg-emerald-50 border border-emerald-300 text-emerald-800 text-sm p-4 rounded-xl flex items-center space-x-2">
          <CheckCircle2 size={18} className="text-emerald-600" />
          <span>{message}</span>
        </div>
      )}

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500 font-medium">Loading inventory levels...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 text-slate-500 uppercase text-xs font-semibold">
                  <th className="py-3.5 px-5">Product Name</th>
                  <th className="py-3.5 px-5">SKU</th>
                  <th className="py-3.5 px-5">Warehouse</th>
                  <th className="py-3.5 px-5 text-right">Available Stock</th>
                  <th className="py-3.5 px-5 text-right">Reserved Stock</th>
                  <th className="py-3.5 px-5 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {inventoryList.map((item) => {
                  const isLow = item.quantityAvailable < 10;
                  return (
                    <tr key={item.id} className="hover:bg-slate-50/80 transition">
                      <td className="py-4 px-5 font-semibold text-slate-800">
                        {item.productName}
                        {isLow && (
                          <span className="ml-2 inline-flex items-center space-x-1 bg-amber-100 text-amber-800 text-[10px] px-2 py-0.5 rounded-full font-bold">
                            <AlertTriangle size={10} /> LOW STOCK
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-5 font-mono text-xs text-slate-500">{item.productSku}</td>
                      <td className="py-4 px-5 text-slate-600 flex items-center space-x-1.5 mt-2">
                        <WarehouseIcon size={14} className="text-indigo-500" />
                        <span>{item.warehouseName}</span>
                      </td>
                      <td className={`py-4 px-5 text-right font-extrabold ${isLow ? 'text-rose-600' : 'text-slate-900'}`}>
                        {item.quantityAvailable} units
                      </td>
                      <td className="py-4 px-5 text-right text-slate-500 font-semibold">{item.quantityReserved} units</td>
                      <td className="py-4 px-5 text-center">
                        {['ADMIN', 'WAREHOUSE_MANAGER'].includes(user?.role || '') && (
                          <button
                            onClick={() => {
                              setEditingItem(item);
                              setNewStock(item.quantityAvailable);
                            }}
                            className="inline-flex items-center space-x-1 text-xs font-semibold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 px-3 py-1.5 rounded-lg transition"
                          >
                            <Edit2 size={12} />
                            <span>Adjust Stock</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Edit Stock Modal */}
      {editingItem && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-slate-900">Adjust Warehouse Stock</h3>
            <p className="text-xs text-slate-500">
              Product: <span className="font-semibold text-slate-800">{editingItem.productName}</span> ({editingItem.warehouseName})
            </p>

            <form onSubmit={handleUpdateStock} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-slate-600">New Available Quantity</label>
                <input
                  type="number"
                  min="0"
                  required
                  value={newStock}
                  onChange={(e) => setNewStock(parseInt(e.target.value))}
                  className="w-full border border-slate-300 rounded-lg p-2.5 text-sm mt-1 focus:border-indigo-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setEditingItem(null)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-xs font-semibold bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                >
                  Update Stock
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
