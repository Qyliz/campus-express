import type { AxiosRequestConfig } from 'axios'

import { http } from './request'
import type {
  ChangeEmailDTO,
  ChangePhoneDTO,
  ResetPasswordDTO,
  SendCodeDTO,
  VerifyCodeDTO,
  UserGenderDTO,
  UserLoginDTO,
  UserPasswordDTO,
  UserProfileVO,
  UserRegisterDTO,
  UserUsernameDTO,
} from '@/types'

//注册材料使用 multipart 的 material 字段
export function register(form: UserRegisterDTO, material?: File) {
  const fd = new FormData()
  fd.append('username', form.username)
  fd.append('password', form.password)
  fd.append('role', form.role)
  fd.append('gender', form.gender)
  //不提交空的可选字段
  if (form.phone) fd.append('phone', form.phone)
  if (form.email) fd.append('email', form.email)
  if (form.phoneCode) fd.append('phoneCode', form.phoneCode)
  if (form.emailCode) fd.append('emailCode', form.emailCode)
  if (material) fd.append('material', material)
  return http.upload<void>('/api/user/register', fd)
}

export function login(data: UserLoginDTO) {
  return http.post<void>('/api/user/login', data)
}

export function logout() {
  return http.post<void>('/api/user/logout')
}

//未登录时返回 null，不作为异常处理
export function getSession(config?: AxiosRequestConfig) {
  return http.get<UserProfileVO | null>('/api/user/session', undefined, config)
}

export function getProfile(config?: AxiosRequestConfig) {
  return http.get<UserProfileVO>('/api/user/profile', undefined, config)
}

export function updateUsername(data: UserUsernameDTO) {
  return http.put<UserProfileVO>('/api/user/username', data)
}

export function updateGender(data: UserGenderDTO) {
  return http.put<UserProfileVO>('/api/user/gender', data)
}

export function uploadAvatar(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return http.upload<UserProfileVO>('/api/user/avatar', fd)
}

//修改成功后服务端会结束当前会话
export function updatePassword(data: UserPasswordDTO) {
  return http.put<void>('/api/user/password', data)
}

//只注销当前角色，保留用户及其他角色
export function deleteAccount() {
  return http.delete<void>('/api/user/account')
}

//开发环境直接返回验证码
export function sendVerifyCode(data: SendCodeDTO) {
  return http.post<string>('/api/user/verify-code', data)
}

//预校验不消费验证码
export function checkVerifyCode(data: VerifyCodeDTO) {
  return http.post<void>('/api/user/verify-code/check', data)
}

export function resetPassword(data: ResetPasswordDTO) {
  return http.post<void>('/api/user/reset-password', data)
}

//验证码发送到新手机号
export function updatePhone(data: ChangePhoneDTO) {
  return http.put<void>('/api/user/phone', data)
}

//验证码发送到新邮箱
export function updateEmail(data: ChangeEmailDTO) {
  return http.put<void>('/api/user/email', data)
}
