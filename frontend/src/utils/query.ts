/**
 * 管理端筛选栏的统一约定，六个页面全部照此办理：
 *
 * 1. 每个筛选字段的类型都是 All<T> = '' | T，'' 表示「全部」。
 *    不用 undefined 当哨兵，是因为 el-option 的 value 属性在类型上不接受 undefined/null。
 * 2. 发请求前一律用 orUndefined() 把 '' 换成 undefined —— axios 会省略 undefined 参数，
 *    但会把 '' 原样发过去，变成后端无意义的 like '%%'。
 * 3. ⚠️ 绝不用 `x || undefined`。布尔 false 是 falsy，会被静默吞成「全部」：
 *    用户选了「未删除」，表格返回全部记录，页面不报错、select 上还原样显示「未删除」。
 *    数字 0 同理（状态字段本次已统一改成枚举名，所以筛选栏里不会再出现数字）。
 *    orUndefined 是唯一入口，它只做 === '' 判断，对字符串枚举和布尔一视同仁。
 * 4. select 一律不加 clearable（清空会塞进 All<T> 之外的值），靠显式的「全部」选项 + 重置按钮。
 */

/** '' = 全部，T = 后端要的真实类型（字符串枚举 / 布尔） */
export type All<T> = '' | T

/** 空串哨兵 → undefined，其余原样传出。后端需要的是「这个参数干脆不传」。 */
export function orUndefined<T>(v: All<T>): T | undefined {
  return v === '' ? undefined : v
}
