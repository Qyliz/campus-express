// ============ 通用信封 ============

/** 对应 cn.njust.campusexpress.common.Result。字段名是 message，不是 msg。 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/**
 * 对应 common.PageResult。size 由服务端写死为 10，请求里只有 currentPage，没有 pageSize。
 *
 * 这四个数字是 Java 的原始类型 long，而后端 JacksonConfig 只把包装类型 Long 序列化成字符串
 * （雪花 ID 会超出 JS 的安全整数范围），所以它们仍然是 number，可以直接喂给 el-pagination。
 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// ============ 枚举：线上传输的是常量名字符串，不是数字 code ============

export type RoleEnum = 'ADMIN' | 'CUSTOMER' | 'COURIER'
export type GenderEnum = 'UNKNOWN' | 'MALE' | 'FEMALE'
export type StatusEnum = 'DISABLED' | 'NORMAL' | 'REVIEWING' | 'REJECTED'
export type AuditStatusEnum = 'REVIEWING' | 'NORMAL' | 'REJECTED'
export type VerifySceneEnum = 'REGISTER' | 'FORGOT_PASSWORD' | 'CHANGE_PHONE' | 'CHANGE_EMAIL'
export type SortEnum =
  'CREATE_TIME_ASC' | 'CREATE_TIME_DESC' | 'UPDATE_TIME_ASC' | 'UPDATE_TIME_DESC'

/**
 * 后端主键是 MyBatis-Plus 雪花 ID（19 位 Long，约 1.9e18），超出 JS 的
 * Number.MAX_SAFE_INTEGER（约 9.0e15），直接当数字接收会被 JSON.parse 舍位。
 * 后端 JacksonConfig 已把它们序列化成字符串，所以前端一律用 string 承载，
 * 并且只原样回传、从不做算术（Jackson 能把 JSON 字符串反序列化回 Long）。
 */
export type EntityId = string

/**
 * 后端的时间字段（java.util.Date）。
 *
 * 实测下发的是 ISO-8601 字符串，例如 "2026-09-09T16:35:30.000Z"（UTC）——
 * 因为 Jackson 3 默认关掉了 WRITE_DATES_AS_TIMESTAMPS，这和 Jackson 2
 * 「默认输出 epoch 毫秒数字」的行为正好相反，很容易按旧经验想当然。
 * formatDateTime() 两种形状都能吃，所以后端万一改了配置也不会白屏。
 */
export type ServerDate = string

// ============ 响应 VO ============

export interface UserProfileVO {
  username: string
  role: RoleEnum
  gender: GenderEnum
  phone: string | null
  email: string | null
  /** 形如 "/upload/avatar/uuid.png"，直接当同源相对路径用即可 */
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
  /** java.util.Date → ISO-8601 字符串，见 ServerDate */
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
  /** 形如 "/upload/audit/uuid.jpg" */
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

// ============ 请求 DTO ============

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
  /** 必须是邮箱或手机号，否则后端返回 code 1「请输入正确的邮箱或手机号」 */
  account: string
  password: string
  role: RoleEnum
}

export interface SendCodeDTO {
  /** FORGOT_PASSWORD 用登录账号；CHANGE_PHONE / CHANGE_EMAIL 用「新」的手机号 / 邮箱 */
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
  /** DTO 上没有 @NotBlank，但留空会被 controller 判为 code 2，所以前端必须必填 */
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

/** status 只接受 NORMAL(通过) / REJECTED(驳回)，传别的会被后端判为 code 1 */
export interface UserAuditDTO {
  userAuditRecordId: EntityId
  status: 'NORMAL' | 'REJECTED'
  reason?: string
}

export interface UserBanDTO {
  userId: EntityId
  /** 必须和 userId 一起用：一个人可以有多个角色，封的是这一个角色账户 */
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

// ============ 查询 DTO（GET，绑定自 query string） ============

/**
 * 分页查询的公共部分。
 * 可选字段用 undefined 表示「不传」，千万不要用 null ——
 * axios 会省略 undefined 参数，但会把 null 发成空字符串，Spring 绑不上 Boolean。
 */
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

/** 与 UserAuditQueryDTO 唯一的区别：状态字段叫 userStatus，不是 auditStatus */
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
