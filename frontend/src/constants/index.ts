import type {
  AuditStatusEnum,
  GenderEnum,
  RoleEnum,
  SortEnum,
  StatusEnum,
  VerifySceneEnum,
} from '@/types'

/**
 * 一律用 Record<字面量联合, string>，不要写 { [k: string]: string }。
 * tsconfig.app.json 开了 noUncheckedIndexedAccess，索引签名会让取值变成 string | undefined，
 * 每次都要 ?? 兜底；而字面量键的 Record 用枚举值去索引，结果仍然是 string。
 */
export const roleLabel: Record<RoleEnum, string> = {
  CUSTOMER: '收寄件人',
  COURIER: '配送员',
  ADMIN: '管理员',
}

export const genderLabel: Record<GenderEnum, string> = {
  UNKNOWN: '未知',
  MALE: '男',
  FEMALE: '女',
}

export const statusLabel: Record<StatusEnum, string> = {
  DISABLED: '禁用',
  NORMAL: '正常',
  REVIEWING: '审核中',
  REJECTED: '审核驳回',
}

export const sortLabel: Record<SortEnum, string> = {
  CREATE_TIME_ASC: '创建时间升序',
  CREATE_TIME_DESC: '创建时间降序',
  UPDATE_TIME_ASC: '更新时间升序',
  UPDATE_TIME_DESC: '更新时间降序',
}

export const verifySceneLabel: Record<VerifySceneEnum, string> = {
  REGISTER: '注册',
  FORGOT_PASSWORD: '忘记密码',
  CHANGE_PHONE: '换绑手机号',
  CHANGE_EMAIL: '换绑邮箱',
}

export type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

export const statusTagType: Record<StatusEnum, TagType> = {
  DISABLED: 'danger',
  NORMAL: 'success',
  REVIEWING: 'warning',
  REJECTED: 'info',
}

export const roleTagType: Record<RoleEnum, TagType> = {
  CUSTOMER: 'primary',
  COURIER: 'success',
  ADMIN: 'danger',
}

/**
 * 把「枚举名 → 中文」的标签表转成 el-option 需要的 { value, label } 数组。
 * 顺序来自 Object.keys，所以想调下拉框里的顺序就改标签表的键序。
 * value 带上枚举的精确类型，筛选栏的 modelValue 才能直接用 All<T> 接住。
 */
export function toOptions<T extends string>(map: Record<T, string>) {
  return (Object.keys(map) as T[]).map((value) => ({ value, label: map[value] }))
}

export const auditStatusLabel: Record<AuditStatusEnum, string> = {
  REVIEWING: '审核中',
  NORMAL: '已通过',
  REJECTED: '已驳回',
}
export const auditStatusTagType: Record<AuditStatusEnum, TagType> = {
  REVIEWING: 'warning',
  NORMAL: 'success',
  REJECTED: 'danger',
}
export const auditStatusOptions = toOptions(auditStatusLabel)
export const roleOptions = toOptions(roleLabel)
export const registerRoleOptions = roleOptions.filter((option) => option.value !== 'ADMIN')
export const genderOptions = toOptions(genderLabel)
export const statusOptions = toOptions(statusLabel)
export const verifySceneOptions = toOptions(verifySceneLabel)

/** 审核列表、封禁记录列表：四种排序都可以 */
export const allSortOptions = toOptions(sortLabel)

/** 账号管理列表：产品上只允许按创建时间排序 */
export const createTimeSortOptions = allSortOptions.filter(
  (o) => o.value === 'CREATE_TIME_ASC' || o.value === 'CREATE_TIME_DESC',
)

/** 服务端 PageResult.size 写死为 10，前端不提供「每页条数」选择器 */
export const PAGE_SIZE = 10
