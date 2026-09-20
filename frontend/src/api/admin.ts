import { http } from './request'
import type {
  AdminResetPasswordDTO,
  PageResult,
  UserAuditDTO,
  UserAuditQueryDTO,
  UserAuditRecordVO,
  UserBanDTO,
  UserBanQueryDTO,
  UserBanRecordVO,
  UserKickoutDTO,
  UserProfileAdminVO,
  UserQueryDTO,
  UserUnbanDTO,
} from '@/types'

//管理员接口

export function pageAuditRecords(params: UserAuditQueryDTO) {
  return http.get<PageResult<UserAuditRecordVO>>('/api/user/audit', params)
}

//审核结果只接受通过或驳回
export function auditUser(data: UserAuditDTO) {
  return http.put<void>('/api/user/audit/' + data.userAuditRecordId, {
    status: data.status,
    reason: data.reason,
  })
}

export function pageUsers(params: UserQueryDTO) {
  return http.get<PageResult<UserProfileAdminVO>>('/api/user/all-users', params)
}

//封禁指定角色并结束该用户的会话
export function banUser(data: UserBanDTO) {
  return http.post<void>('/api/user/' + data.userId + '/roles/' + data.role + '/ban', {
    reason: data.reason,
  })
}

export function pageBanRecords(params: UserBanQueryDTO) {
  return http.get<PageResult<UserBanRecordVO>>('/api/user/ban', params)
}

export function unbanUser(data: UserUnbanDTO) {
  return http.post<void>('/api/user/' + data.userId + '/roles/' + data.role + '/unban')
}

//会话属于用户，不区分角色
export function kickoutUser(data: UserKickoutDTO) {
  return http.post<void>('/api/user/' + data.userId + '/kickout')
}

//多角色共用同一登录密码
export function adminResetPassword(data: AdminResetPasswordDTO) {
  return http.post<void>('/api/user/' + data.userId + '/reset-password', {
    newPassword: data.newPassword,
  })
}
