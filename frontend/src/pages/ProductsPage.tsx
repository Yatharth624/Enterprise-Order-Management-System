import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Product, User } from '../types';
import { Search, Plus, ShoppingCart, CheckCircle, Package } from 'lucide-react';

interface ProductsPageProps {
  user: User | null;
}

export const ProductsPage: React.FC<ProductsPageProps> = ({ user }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [message, setMessage] = useState('');

  // Add product form state
  const [sku, setSku] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [initialStock, setInitialStock] = useState('10');

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const res = await api.getProducts(search, 0, 20);
      setProducts(res.content);
    } catch (err) {
      console.error('Failed to fetch products', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [search]);

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.createProduct({
        sku,
        name,
        description,
        price: parseFloat(price),
        initialStock: parseInt(initialStock),
      });
      setShowModal(false);
      setMessage('Product created successfully!');
      setTimeout(() => setMessage(''), 3000);
      fetchProducts();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || 'Failed to create product');
    }
  };

  const handleQuickOrder = async (product: Product) => {
    try {
      const order = await api.createOrder('100 Technology Plaza, San Francisco CA', [
        { productId: product.id, quantity: 1 },
      ]);
      setMessage(`Order #${order.orderNumber} created successfully! Processing payment...`);
      
      // Auto process simulated payment
      await api.processPayment(order.id, order.totalAmount, 'CREDIT_CARD', 'FORCE_SUCCESS');
      
      setTimeout(() => setMessage(''), 4000);
      fetchProducts();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || 'Order creation failed');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Product Catalog</h2>
          <p className="text-sm text-slate-500">Browse enterprise items and trigger instant order placement</p>
        </div>

        {user?.role === 'ADMIN' && (
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center space-x-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2.5 rounded-xl font-semibold text-sm shadow transition"
          >
            <Plus size={18} />
            <span>Add New Product</span>
          </button>
        )}
      </div>

      {message && (
        <div className="bg-emerald-50 border border-emerald-300 text-emerald-800 text-sm p-4 rounded-xl flex items-center space-x-2">
          <CheckCircle size={18} className="text-emerald-600" />
          <span>{message}</span>
        </div>
      )}

      {/* Search Input */}
      <div className="relative max-w-md">
        <Search className="absolute left-3.5 top-3 text-slate-400" size={18} />
        <input
          type="text"
          placeholder="Search products by SKU or Name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full bg-white border border-slate-200 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-800 focus:outline-none focus:border-indigo-500 shadow-sm"
        />
      </div>

      {/* Products Grid */}
      {loading ? (
        <div className="text-center py-12 text-slate-500 font-medium">Loading products...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product) => (
            <div key={product.id} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 flex flex-col justify-between hover:shadow-md transition">
              <div>
                <div className="flex items-start justify-between mb-2">
                  <span className="text-[11px] font-mono bg-slate-100 text-slate-600 px-2 py-0.5 rounded font-semibold">
                    {product.sku}
                  </span>
                  {product.category && (
                    <span className="text-[11px] font-semibold bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-full">
                      {product.category.name}
                    </span>
                  )}
                </div>
                <h3 className="font-bold text-slate-900 text-base mb-1">{product.name}</h3>
                <p className="text-xs text-slate-500 line-clamp-2 mb-4">{product.description}</p>
              </div>

              <div className="border-t border-slate-100 pt-4 flex items-center justify-between">
                <div>
                  <span className="text-xs text-slate-400 block">Unit Price</span>
                  <span className="text-xl font-extrabold text-slate-900">${product.price.toFixed(2)}</span>
                </div>

                <button
                  onClick={() => handleQuickOrder(product)}
                  className="flex items-center space-x-1.5 bg-slate-900 hover:bg-slate-800 text-white px-3.5 py-2 rounded-xl text-xs font-semibold shadow transition"
                >
                  <ShoppingCart size={14} />
                  <span>Instant Checkout</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Product Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-slate-900">Add New Product (Admin)</h3>
            <form onSubmit={handleCreateProduct} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-600">SKU Code</label>
                <input
                  type="text"
                  required
                  placeholder="SKU-XXX-01"
                  value={sku}
                  onChange={(e) => setSku(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg p-2 text-sm mt-1"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-slate-600">Product Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg p-2 text-sm mt-1"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-slate-600">Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg p-2 text-sm mt-1"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-xs font-semibold text-slate-600">Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    className="w-full border border-slate-300 rounded-lg p-2 text-sm mt-1"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600">Initial Stock</label>
                  <input
                    type="number"
                    required
                    value={initialStock}
                    onChange={(e) => setInitialStock(e.target.value)}
                    className="w-full border border-slate-300 rounded-lg p-2 text-sm mt-1"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-xs font-semibold bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                >
                  Save Product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
