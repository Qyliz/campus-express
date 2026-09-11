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

// 这个文件里的接口全部在后端标了 @SaCheckRole("ADMIN")，非管理员调用会拿到 HTTP 403 / code 1004。
// api/ 目录就是按这条授权边界切的：user.ts = 登录者可调，admin.ts = 管理员专属。

/** 14. GET /api/user/audit — 查询条件走 query string，不是 body */
export function pageAuditRecords(params: UserAuditQueryDTO) {
  return http.get<PageResult<UserAuditRecordVO>>('/api/user/audit', params)
}

/** 15. PUT /api/user/audit/{recordId} — status 只接受 'NORMAL'(通过) | 'REJECTED'(驳回) */
export function auditUser(data: UserAuditDTO) {
  return http.put<void>('/api/user/audit/' + data.userAuditRecordId, {
    status: data.status,
    reason: data.reason,
  })
}

/** 16. GET /api/user/all-users — 注意状态筛选字段叫 userStatus，审核页那个叫 auditStatus */
export function pageUsers(params: UserQueryDTO) {
  return http.get<PageResult<UserProfileAdminVO>>('/api/user/all-users', params)
}

/** 17. POST /api/user/{userId}/roles/{role}/ban — 后端同时会把该用户踢下线 */
export function banUser(data: UserBanDTO) {
  return http.post<void>('/api/user/' + data.userId + '/roles/' + data.role + '/ban', {
    reason: data.reason,
  })
}

/** 18. GET /api/user/ban */
export function pageBanRecords(params: UserBanQueryDTO) {
  return http.get<PageResult<UserBanRecordVO>>('/api/user/ban', params)
}

/** 19. POST /api/user/{userId}/roles/{role}/unban */
export function unbanUser(data: UserUnbanDTO) {
  return http.post<void>('/api/user/' + data.userId + '/roles/' + data.role + '/unban')
}

/** 20. POST /api/user/{userId}/kickout — 只要 userId，因为会话是按 user 而不是按角色 */
export function kickoutUser(data: UserKickoutDTO) {
  return http.post<void>('/api/user/' + data.userId + '/kickout')
}

/** 21. POST /api/user/{userId}/reset-password — 同样只要 userId：密码在 user 主表上，多角色共用 */
export function adminResetPassword(data: AdminResetPasswordDTO) {
  return http.post<void>('/api/user/' + data.userId + '/reset-password', {
    newPassword: data.newPassword,
  })
}
