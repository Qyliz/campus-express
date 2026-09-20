//筛选项用空串表示全部，提交前再转为 undefined
export type All<T> = '' | T

//不能用 v || undefined，否则布尔值 false 会丢失
export function orUndefined<T>(v: All<T>): T | undefined {
  return v === '' ? undefined : v
}
