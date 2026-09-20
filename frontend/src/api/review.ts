import { http } from './request'
import { toOptions, type TagType } from '@/constants'
import type { PageResult } from '@/types'

//接口传枚举名，数据库存枚举编码
export type ReviewStatusEnum = 'VALID' | 'VOID'
export type AppealStatusEnum = 'PENDING' | 'UPHELD' | 'REJECTED'
export const reviewStatusLabels: Record<ReviewStatusEnum, string> = {
  VALID: '有效',
  VOID: '已作废',
}
export const reviewStatusTagType: Record<ReviewStatusEnum, TagType> = {
  VALID: 'success',
  VOID: 'info',
}
export const appealStatusLabels: Record<AppealStatusEnum, string> = {
  PENDING: '待处理',
  UPHELD: '申诉成立',
  REJECTED: '已驳回',
}
export const appealStatusTagType: Record<AppealStatusEnum, TagType> = {
  PENDING: 'warning',
  UPHELD: 'danger',
  REJECTED: 'success',
}
export const appealStatusOptions = toOptions(appealStatusLabels)

export interface ServiceReview {
  id: string
  orderId: string
  userId: string
  courierId: string
  rating: number
  content: string
  status: ReviewStatusEnum
  createTime: string
}
export interface ReviewAppeal {
  id: string
  reviewId: string
  orderId: string
  courierId: string
  reason: string
  status: AppealStatusEnum
  adminId?: string
  resolutionReason?: string
  createTime: string
  resolvedTime?: string
}
//用户端显示评价人名称，管理端仍用账户 ID 定位
export interface ReviewItem {
  review: ServiceReview
  appeal?: ReviewAppeal
  canAppeal: boolean
  username?: string | null
}
export interface OrderReviews {
  reviews: ReviewItem[]
  canReview: boolean
}
export const getReviews = (id: string) => http.get<OrderReviews>('/api/order/' + id + '/reviews')
export const createReview = (id: string, rating: number, content: string) =>
  http.post<string>('/api/order/' + id + '/reviews', { rating, content })
export const createAppeal = (id: string, reason: string) =>
  http.post<string>('/api/review/' + id + '/appeals', { reason })
export const getAdminAppeals = (params: {
  currentPage: number
  status?: AppealStatusEnum
  orderId?: string
}) => http.get<PageResult<ReviewAppeal>>('/api/review/appeals/admin', params)
export const resolveAppeal = (id: string, status: AppealStatusEnum, reason: string) =>
  http.post<void>('/api/review/appeals/' + id + '/resolve', { status, reason })
