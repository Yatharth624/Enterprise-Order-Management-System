export type Role = 'CUSTOMER' | 'ADMIN' | 'WAREHOUSE_MANAGER' | 'SUPPORT_AGENT';

export type OrderStatus =
  | 'CREATED'
  | 'INVENTORY_RESERVED'
  | 'PAYMENT_PENDING'
  | 'PAID'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'PAYMENT_FAILED'
  | 'CANCELLED'
  | 'INVENTORY_FAILED';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface Category {
  id: string;
  name: string;
  description: string;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string;
  price: number;
  category?: Category;
  availableStock?: number;
}

export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  unitPrice: number;
  quantity: number;
  itemTotal: number;
}

export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  totalAmount: number;
}

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface OrderTimelineEvent {
  id: string;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  reason: string;
  timestamp: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  customerEmail: string;
  customerName: string;
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: string;
  items: OrderItem[];
  timeline: OrderTimelineEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface Inventory {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  warehouseId: string;
  warehouseName: string;
  quantityAvailable: number;
  quantityReserved: number;
}

export interface DashboardSummary {
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  failedPayments: number;
  lowStockProducts: Inventory[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  error?: {
    code: string;
    message: string;
  };
  timestamp: string;
  requestId: string;
}
