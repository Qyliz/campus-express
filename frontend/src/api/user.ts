import type { AxiosRequestConfig } from 'axios'

import { http } from './request'
import type {
  ChangeEmailDTO,
  ChangePhoneDTO,
  ResetPasswordDTO,
  SendCodeDTO,
  UserGenderDTO,
  UserLoginDTO,
  UserPasswordDTO,
  UserProfileVO,
  UserRegisterDTO,
  UserUsernameDTO,
} from '@/types'

/** 1. POST /api/user/register — multipart/form-data，part 名是 material */
export function register(form: UserRegisterDTO, material?: File) {
  const fd = new FormData()
  fd.append('username', form.username)
  fd.append('password', form.password)
  fd.append('role', form.role)
  fd.append('gender', form.gender)
  // 可选字段为空时不要 append：FormData 会把 undefined 变成字符串 "undefined" 发出去
  if (form.phone) fd.append('phone', form.phone)
  if (form.email) fd.append('email', form.email)
  if (material) fd.append('material', material)
  return http.upload<void>('/api/user/register', fd)
}

/** 2. POST /api/user/login — 响应体是 void，token 在 Set-Cookie 里 */
export function login(data: UserLoginDTO) {
  return http.post<void>('/api/user/login', data)
}

/** 3. POST /api/user/logout */
export function logout() {
  return http.post<void>('/api/user/logout')
}

/** 4. GET /api/user/profile — 登录态探测就靠它；silent 用于启动时匿名访客的合法 401 */
export function getProfile(config?: AxiosRequestConfig) {
  return http.get<UserProfileVO>('/api/user/profile', undefined, config)
}

/** 5. PUT /api/user/username — 返回更新后的资料，可直接喂给 store */
export function updateUsername(data: UserUsernameDTO) {
  return http.put<UserProfileVO>('/api/user/username', data)
}

/** 6. PUT /api/user/gender */
export function updateGender(data: UserGenderDTO) {
  return http.put<UserProfileVO>('/api/user/gender', data)
}

/** 7. POST /api/user/avatar — multipart/form-data，part 名固定是 file */
export function uploadAvatar(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return http.upload<UserProfileVO>('/api/user/avatar', fd)
}

/** 8. PUT /api/user/password — 成功后服务端会顺手登出当前会话 */
export function updatePassword(data: UserPasswordDTO) {
  return http.put<void>('/api/user/password', data)
}

/** 9. DELETE /api/user/account — 只逻辑删除「当前角色」那一行，user 主表与其他角色保留 */
export function deleteAccount() {
  return http.delete<void>('/api/user/account')
}

/** 10. POST /api/user/verify-code — data 就是那 6 位验证码本身（没有真实短信/邮件通道） */
export function sendVerifyCode(data: SendCodeDTO) {
  return http.post<string>('/api/user/verify-code', data)
}

/** 11. POST /api/user/reset-password */
export function resetPassword(data: ResetPasswordDTO) {
  return http.post<void>('/api/user/reset-password', data)
}

/** 12. PUT /api/user/phone — code 校验的是 newPhone，所以验证码也要发给新手机号 */
export function updatePhone(data: ChangePhoneDTO) {
  return http.put<void>('/api/user/phone', data)
}

/** 13. PUT /api/user/email — code 校验的是 newEmail */
export function updateEmail(data: ChangeEmailDTO) {
  return http.put<void>('/api/user/email', data)
}
