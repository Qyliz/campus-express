<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { exceptionTypes, listExceptions, type DeliveryException } from '@/api/order'
import { formatDateTime } from '@/utils/date'
const router = useRouter()
const rows = ref<DeliveryException[]>([])
const page = ref(1)
const total = ref(0)
const status = ref<number | undefined>(0)
const orderId = ref('')
const loading = ref(false)
const error = ref(false)
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  error.value = false
  try {
    const result = await listExceptions({ currentPage: page.value, status: status.value, orderId: orderId.value.trim() || undefined })
    if (current === sequence) { rows.value = result.records; total.value = result.total }
  } catch {
    if (current === sequence) { rows.value = []; total.value = 0; error.value = true }
  } finally { if (current === sequence) loading.value = false }
}
function search() { page.value = 1; void load() }
onMounted(load)
</script>
<template>
  <el-card shadow="never">
    <template #header><b>异常管理</b></template>
    <el-space wrap class="filters">
      <el-select v-model="status" clearable placeholder="全部状态" style="width: 150px" @change="search"><el-option label="待处理" :value="0" /><el-option label="已处理" :value="1" /></el-select>
      <el-input v-model="orderId" clearable placeholder="完整订单编号" style="width: 240px" @keyup.enter="search" />
      <el-button :loading="loading" @click="search">查询 / 刷新</el-button>
    </el-space>
    <el-alert v-if="error" title="加载失败，请刷新重试" type="error" :closable="false" />
    <el-table v-loading="loading" :data="rows" empty-text="暂无异常记录">
      <el-table-column prop="orderId" label="订单编号" min-width="190" />
      <el-table-column label="异常类型" min-width="140"><template #default="{ row }">{{ exceptionTypes[row.type] }}</template></el-table-column>
      <el-table-column prop="description" label="异常说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'warning' : 'success'">{{ row.status === 0 ? '待处理' : '已处理' }}</el-tag></template></el-table-column>
      <el-table-column label="上报时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="router.push({ name: 'admin-order-detail', params: { id: row.orderId } })">{{ row.status === 0 ? '查看并处理' : '查看结果' }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next, total" @current-change="load" class="pagination" />
  </el-card>
</template>
<style scoped>
.filters { margin-bottom: 16px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
