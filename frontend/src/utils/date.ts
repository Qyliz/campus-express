function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/**
 * 后端 java.util.Date 没有配置 Jackson 日期格式，默认序列化成 epoch 毫秒「数字」。
 * 这里同时兼容数字和数字字符串：万一以后后端加了日期格式配置，页面也不会白屏。
 * 不引入 dayjs —— 十几行代码就够了，也不要去依赖某个库的间接依赖。
 */
export function formatDateTime(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') return '—'
  const input = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value
  const d = new Date(input)
  if (Number.isNaN(d.getTime())) return '—'
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}
