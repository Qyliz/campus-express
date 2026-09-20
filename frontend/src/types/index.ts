//通用响应

export interface Result<T> {
  code: number
  message: string
  data: T
}

//分页大小由服务端固定为 10
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

//接口枚举使用常量名字符串

export type RoleEnum = 'ADMIN' | 'CUSTOMER' | 'COURIER'
export type GenderEnum = 'UNKNOWN' | 'MALE' | 'FEMALE'
export type StatusEnum = 'DISABLED' | 'NORMAL' | 'REVIEWING' | 'REJECTED'
export type AuditStatusEnum = 'REVIEWING' | 'NORMAL' | 'REJECTED'
export type VerifySceneEnum = 'REGISTER' | 'FORGOT_PASSWORD' | 'CHANGE_PHONE' | 'CHANGE_EMAIL'
export type SortEnum =
  'CREATE_TIME_ASC' | 'CREATE_TIME_DESC' | 'UPDATE_TIME_ASC' | 'UPDATE_TIME_DESC'

//雪花 ID 超出安全整数范围，前端始终按字符串处理
export type EntityId = string

//兼容 ISO 时间和 epoch 毫秒
export type ServerDate = string

//响应模型

export interface UserProfileVO {
  username: string
  role: RoleEnum
  gender: GenderEnum
  phone: string | null
  email: string | null
  //同源上传路径
  avatar: string | null
}

export interface UserProfileAdminVO {
  userId: EntityId
  username: string
  role: RoleEnum
  gender: GenderEnum
  phone: string | null
  email: string | null
  avatar: string | null
  status: StatusEnum
  createTime: ServerDate
}

export interface UserAuditRecordVO {
  userAuditRecordId: EntityId
  username: string
  gender: GenderEnum
  phone: string | null
  email: string | null
  status: AuditStatusEnum
  reason: string | null
  material: string | null
  createTime: ServerDate
  updateTime: ServerDate
}

export interface UserBanRecordVO {
  userBanRecordId: EntityId
  userId: EntityId
  role: RoleEnum
  username: string
  gender: GenderEnum
  phone: string | null
  email: string | null
  unbanned: boolean
  reason: string | null
  createTime: ServerDate
}

//请求模型

export interface UserRegisterDTO {
  username: string
  password: string
  role: RoleEnum
  gender: GenderEnum
  phone?: string
  email?: string
  phoneCode?: string
  emailCode?: string
}

export interface UserLoginDTO {
  //邮箱或手机号
  account: string
  password: string
  role: RoleEnum
}

export interface SendCodeDTO {
  //找回密码传登录账号，换绑传新联系方式
  account: string
  scene: VerifySceneEnum
}

export interface VerifyCodeDTO extends SendCodeDTO {
  code: string
}

export interface ResetPasswordDTO {
  account: string
  code: string
  newPassword: string
}

export interface UserUsernameDTO {
  username: string
}

export interface UserGenderDTO {
  gender: GenderEnum
}

export interface UserPasswordDTO {
  oldPassword: string
  newPassword: string
}

export interface ChangePhoneDTO {
  newPhone: string
  code: string
}

export interface ChangeEmailDTO {
  newEmail: string
  code: string
}

//审核结果只接受通过或驳回
export interface UserAuditDTO {
  userAuditRecordId: EntityId
  status: 'NORMAL' | 'REJECTED'
  reason?: string
}

export interface UserBanDTO {
  userId: EntityId
  //封禁用户的指定角色
  role: RoleEnum
  reason?: string
}

export interface UserUnbanDTO {
  userId: EntityId
  role: RoleEnum
}

export interface UserKickoutDTO {
  userId: EntityId
}

export interface AdminResetPasswordDTO {
  userId: EntityId
  newPassword: string
}

//查询参数

//空筛选项转为 undefined 后再提交
export interface PageQuery {
  currentPage?: number
  sort?: SortEnum
}

export interface UserAuditQueryDTO extends PageQuery {
  username?: string
  phone?: string
  email?: string
  auditStatus?: AuditStatusEnum
  deleted?: boolean
}

export interface UserQueryDTO extends PageQuery {
  username?: string
  phone?: string
  email?: string
  userStatus?: StatusEnum
  deleted?: boolean
}

export interface UserBanQueryDTO extends PageQuery {
  username?: string
  phone?: string
  email?: string
  unbanned?: boolean
  deleted?: boolean
}
