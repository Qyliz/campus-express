function pad(n: number): string {
  return String(n).padStart(2, '0')
}

//兼容 ISO 时间、epoch 数字和数字字符串
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
