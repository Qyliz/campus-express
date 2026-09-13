import { http } from './request'
import { toOptions, type TagType } from '@/constants'
import type { PageResult, RoleEnum } from '@/types'

export type OrderStatusEnum =
  | 'UNPAID'
  | 'AVAILABLE'
  | 'AWAITING_PICKUP'
  | 'DELIVERING'
  | 'AWAITING_COLLECTION'
  | 'COMPLETED'
  | 'CANCELLED'
export type PaymentStatusEnum = 'UNPAID' | 'PAID' | 'REFUNDED'
export type OrderRelationEnum = 'ALL' | 'CREATED' | 'RECEIVED'
export const orderStatusLabels: Record<OrderStatusEnum, string> = {
  UNPAID: '待支付',
  AVAILABLE: '待接单',
  AWAITING_PICKUP: '待揽收',
  DELIVERING: '配送中',
  AWAITING_COLLECTION: '待取件',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
export const paymentLabels: Record<PaymentStatusEnum, string> = {
  UNPAID: '未支付',
  PAID: '已支付',
  REFUNDED: '已退款',
}
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
  orderStatus: OrderStatusEnum
  paymentStatus: PaymentStatusEnum
  createTime: string
  updateTime: string
  pendingException: boolean
  createdByMe: boolean
  receivedByMe: boolean
  /** 接单骑手的展示信息，由后端 courierId → courier.user_id → user 两跳批量回填；未接单或骑手已注销时为空。 */
  courierName: string | null
  courierPhone: string | null
}
export interface OrderRecord {
  id: string
  fromStatus: OrderStatusEnum | null
  toStatus: OrderStatusEnum
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
  params: {
    currentPage: number
    orderStatus?: OrderStatusEnum
    orderId?: string
    relation?: OrderRelationEnum
  },
) => http.get<PageResult<ExpressOrder>>('/api/order/' + scope, params)
export const getOrder = (id: string) => http.get<OrderDetail>('/api/order/' + id)
export const actOnOrder = (id: string, action: OrderAction, reason?: string) =>
  http.post<void>('/api/order/' + id + '/' + action, reason === undefined ? undefined : { reason })

// 配送异常。status/type/resolution 后端都是 @EnumValue 枚举：库里存 code，接口传枚举名。
export type ExceptionStatusEnum = 'PENDING' | 'RESOLVED'
export type ExceptionTypeEnum = 'CONTACT' | 'ADDRESS' | 'ITEM' | 'COURIER' | 'OTHER'
export type ExceptionResolutionEnum = 'RESUME' | 'CANCEL'
export const exceptionStatusLabels: Record<ExceptionStatusEnum, string> = {
  PENDING: '待处理',
  RESOLVED: '已处理',
}
export const exceptionStatusTagType: Record<ExceptionStatusEnum, TagType> = {
  PENDING: 'warning',
  RESOLVED: 'success',
}
export const exceptionTypeLabels: Record<ExceptionTypeEnum, string> = {
  CONTACT: '联系不上',
  ADDRESS: '地址问题',
  ITEM: '物品问题',
  COURIER: '配送员突发情况',
  OTHER: '其他',
}
export const exceptionResolutionLabels: Record<ExceptionResolutionEnum, string> = {
  RESUME: '恢复配送',
  CANCEL: '取消订单（已支付则退款）',
}
export const orderStatusOptions = toOptions(orderStatusLabels)
export const exceptionStatusOptions = toOptions(exceptionStatusLabels)
export const exceptionTypeOptions = toOptions(exceptionTypeLabels)
export interface DeliveryException {
  id: string
  orderId: string
  courierId: string
  type: ExceptionTypeEnum
  description: string
  status: ExceptionStatusEnum
  adminId: string | null
  resolution: ExceptionResolutionEnum | null
  resolutionDescription: string | null
  createTime: string
  resolvedTime: string | null
}
export const reportException = (
  id: string,
  data: { type: ExceptionTypeEnum; description: string },
) => http.post<string>('/api/order/' + id + '/exceptions', data)
export const listExceptions = (params: {
  currentPage: number
  status?: ExceptionStatusEnum
  orderId?: string
}) => http.get<PageResult<DeliveryException>>('/api/exception/admin', params)
export const resolveException = (
  id: string,
  data: { resolution: ExceptionResolutionEnum; description: string },
) => http.post<void>('/api/exception/' + id + '/resolve', data)
