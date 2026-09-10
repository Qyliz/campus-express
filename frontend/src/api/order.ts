import { http } from './request'
import type { PageResult, RoleEnum } from '@/types'

export const orderStatusLabels = [
  '待支付',
  '待接单',
  '待揽收',
  '配送中',
  '待取件',
  '已完成',
  '已取消',
]
export const paymentLabels = ['未支付', '已支付（模拟）', '已退款（模拟）']
export type OrderScope = 'mine' | 'available' | 'assigned' | 'admin'
export type OrderAction =
  'pay' | 'accept' | 'pickup' | 'deliver' | 'complete' | 'cancel' | 'admin-cancel'
export interface CreateOrder {
  pickupAddress: string
  pickupName: string
  pickupPhone: string
  deliveryAddress: string
  deliveryName: string
  deliveryPhone: string
  deliveryEmail?: string | null
  itemDescription: string
  remark: string
  fee: number
}
export interface ExpressOrder extends Omit<
  CreateOrder,
  'pickupName' | 'pickupPhone' | 'deliveryName' | 'deliveryPhone' | 'remark'
> {
  id: string
  customerId: string | null
  courierId: string | null
  pickupName: string | null
  pickupPhone: string | null
  deliveryName: string | null
  deliveryPhone: string | null
  remark: string | null
  orderStatus: number
  paymentStatus: number
  createTime: string
  updateTime: string
  pendingException: boolean
  createdByMe: boolean
  receivedByMe: boolean
}
export interface OrderRecord {
  id: string
  fromStatus: number | null
  toStatus: number
  operatorId: string
  operatorRole: RoleEnum
  description: string | null
  createTime: string
}
export interface OrderDetail {
  order: ExpressOrder
  records: OrderRecord[]
  exceptions: DeliveryException[]
  allowedActions: OrderAction[]
  canReportException: boolean
}
export const createOrder = (data: CreateOrder) => http.post<string>('/api/order', data)
export const listOrders = (
  scope: OrderScope,
  params: { currentPage: number; orderStatus?: number; orderId?: string; relation?: string },
) => http.get<PageResult<ExpressOrder>>('/api/order/' + scope, params)
export const getOrder = (id: string) => http.get<OrderDetail>('/api/order/' + id)
export const actOnOrder = (id: string, action: OrderAction, reason?: string) =>
  http.post<void>('/api/order/' + id + '/' + action, reason === undefined ? undefined : { reason })

export const exceptionTypes: Record<string, string> = {
  CONTACT: '联系不上', ADDRESS: '地址问题', ITEM: '物品问题', COURIER: '配送员突发情况', OTHER: '其他',
}
export interface DeliveryException {
  id: string
  orderId: string
  courierId: string
  type: string
  description: string
  status: number
  adminId: string | null
  resolution: 'RESUME' | 'CANCEL' | null
  resolutionDescription: string | null
  createTime: string
  resolvedTime: string | null
}
export const reportException = (id: string, data: { type: string; description: string }) =>
  http.post<string>('/api/order/' + id + '/exceptions', data)
export const listExceptions = (params: { currentPage: number; status?: number; orderId?: string }) =>
  http.get<PageResult<DeliveryException>>('/api/exception/admin', params)
export const resolveException = (id: string, data: { resolution: 'RESUME' | 'CANCEL'; description: string }) =>
  http.post<void>('/api/exception/' + id + '/resolve', data)
