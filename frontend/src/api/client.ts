import axios from 'axios';
import { ApiResponse, AuthResponse, Cart, DashboardSummary, Inventory, Order, Product, User } from '../types';

const API_BASE = '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const api = {
  // Auth
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    return res.data.data;
  },
  register: async (email: string, password: string, fullName: string, role = 'CUSTOMER'): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', { email, password, fullName, role });
    return res.data.data;
  },
  getCurrentUser: async (): Promise<User> => {
    const res = await apiClient.get<ApiResponse<User>>('/users/me');
    return res.data.data;
  },

  // Dashboard
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const res = await apiClient.get<ApiResponse<DashboardSummary>>('/dashboard/summary');
    return res.data.data;
  },

  // Products
  getProducts: async (query = '', page = 0, size = 10): Promise<{ content: Product[]; totalPages: number }> => {
    const res = await apiClient.get<ApiResponse<{ content: Product[]; totalPages: number }>>('/products', {
      params: { query, page, size },
    });
    return res.data.data;
  },
  getProductById: async (id: string): Promise<Product> => {
    const res = await apiClient.get<ApiResponse<Product>>(`/products/${id}`);
    return res.data.data;
  },
  createProduct: async (product: Partial<Product> & { initialStock?: number }): Promise<Product> => {
    const res = await apiClient.post<ApiResponse<Product>>('/products', product);
    return res.data.data;
  },

  // Orders
  getOrders: async (page = 0, size = 10): Promise<{ content: Order[]; totalPages: number }> => {
    const res = await apiClient.get<ApiResponse<{ content: Order[]; totalPages: number }>>('/orders', {
      params: { page, size },
    });
    return res.data.data;
  },
  getOrderById: async (id: string): Promise<Order> => {
    const res = await apiClient.get<ApiResponse<Order>>(`/orders/${id}`);
    return res.data.data;
  },
  createOrder: async (shippingAddress: string, items: { productId: string; quantity: number }[], idempotencyKey?: string): Promise<Order> => {
    const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {};
    const res = await apiClient.post<ApiResponse<Order>>('/orders', { shippingAddress, items }, { headers });
    return res.data.data;
  },
  cancelOrder: async (id: string, reason?: string): Promise<Order> => {
    const res = await apiClient.post<ApiResponse<Order>>(`/orders/${id}/cancel`, { reason });
    return res.data.data;
  },

  // Payments
  processPayment: async (orderId: string, amount: number, paymentMethod: string, simulateMode?: string, idempotencyKey?: string): Promise<any> => {
    const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {};
    const res = await apiClient.post<ApiResponse<any>>('/payments', { orderId, amount, paymentMethod, simulateMode }, { headers });
    return res.data.data;
  },

  // Inventory
  getInventory: async (): Promise<Inventory[]> => {
    const res = await apiClient.get<ApiResponse<Inventory[]>>('/inventory');
    return res.data.data;
  },
  updateStock: async (productId: string, warehouseId: string, quantityAvailable: number): Promise<Inventory> => {
    const res = await apiClient.post<ApiResponse<Inventory>>('/inventory/update-stock', { productId, warehouseId, quantityAvailable });
    return res.data.data;
  },

  // Cart
  getCart: async (): Promise<Cart> => {
    const res = await apiClient.get<ApiResponse<Cart>>('/cart');
    return res.data.data;
  },
  addToCart: async (productId: string, quantity: number): Promise<Cart> => {
    const res = await apiClient.post<ApiResponse<Cart>>('/cart/items', { productId, quantity });
    return res.data.data;
  },
};
