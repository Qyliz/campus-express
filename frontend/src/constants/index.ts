import type { AuditStatusEnum, GenderEnum, RoleEnum, SortEnum, StatusEnum } from '@/types'

//标签表使用精确枚举键，避免索引结果带 undefined
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

//将枚举标签转为下拉选项
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

//完整排序选项
export const allSortOptions = toOptions(sortLabel)

//账号列表只按创建时间排序
export const createTimeSortOptions = allSortOptions.filter(
  (o) => o.value === 'CREATE_TIME_ASC' || o.value === 'CREATE_TIME_DESC',
)

//服务端固定分页大小
export const PAGE_SIZE = 10
