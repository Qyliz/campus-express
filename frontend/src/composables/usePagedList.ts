import { ref, shallowRef } from 'vue'

import type { PageResult } from '@/types'

/**
 * 分页列表的通用状态与加载流程。
 * sequence 保证连续筛选或切页时，较慢的旧请求不会覆盖最新结果。
 */
export function usePagedList<T>(request: (currentPage: number) => Promise<PageResult<T>>) {
  const rows = shallowRef<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const loading = ref(false)
  const error = ref(false)
  let sequence = 0

  async function load() {
    const current = ++sequence
    loading.value = true
    error.value = false
    try {
      const result = await request(page.value)
      if (current !== sequence) return
      rows.value = result.records
      total.value = result.total
    } catch {
      if (current !== sequence) return
      rows.value = []
      total.value = 0
      error.value = true
    } finally {
      if (current === sequence) loading.value = false
    }
  }

  function search() {
    page.value = 1
    void load()
  }

  /** 供同一组件切换列表范围时立即清掉旧页面数据。 */
  function clear() {
    sequence++
    rows.value = []
    total.value = 0
    loading.value = false
    error.value = false
  }

  return { rows, total, page, loading, error, load, search, clear }
}
