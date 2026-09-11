<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import {
  exceptionStatusOptions, exceptionStatusLabels, exceptionStatusTagType, exceptionTypeLabels,
  listExceptions, type DeliveryException, type ExceptionStatusEnum, type ExceptionTypeEnum,
} from '@/api/order'
import { orUndefined, type All } from '@/utils/query'
import { formatDateTime } from '@/utils/date'
const router = useRouter()
const filters = reactive({
  status: '' as All<ExceptionStatusEnum>,
  orderId: '',
})
const rows = ref<DeliveryException[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref(false)
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  error.value = false
  try {
    const result = await listExceptions({
      currentPage: page.value,
      status: orUndefined(filters.status),
      orderId: orUndefined(filters.orderId.trim()),
    })
    if (current === sequence) { rows.value = result.records; total.value = result.total }
  } catch {
    if (current === sequence) { rows.value = []; total.value = 0; error.value = true }
  } finally { if (current === sequence) loading.value = false }
}
function search() { page.value = 1; void load() }
function resetFilters() {
  Object.assign(filters, { status: '', orderId: '' })
  search()
}
onMounted(load)
</script>
<template>
  <el-card shadow="never" class="page-card">
    <el-form inline @submit.prevent>
      <el-form-item label="状态">
        <!-- 默认「全部」：空串哨兵 + 显式的全部选项，不加 clearable -->
        <el-select v-model="filters.status" placeholder="全部" style="width: 130px">
          <el-option label="全部" value="" />
          <el-option v-for="o in exceptionStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单编号">
        <el-input v-model="filters.orderId" placeholder="完整订单编号" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
  <el-card shadow="never">
    <el-alert v-if="error" title="加载失败，请刷新重试" type="error" :closable="false" />
    <el-table v-loading="loading" :data="rows" empty-text="暂无异常记录">
      <el-table-column prop="orderId" label="订单编号" min-width="190" />
      <el-table-column label="异常类型" min-width="140"><template #default="{ row }">{{ exceptionTypeLabels[row.type as ExceptionTypeEnum] }}</template></el-table-column>
      <el-table-column prop="description" label="异常说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="exceptionStatusTagType[row.status as ExceptionStatusEnum]">{{ exceptionStatusLabels[row.status as ExceptionStatusEnum] }}</el-tag></template></el-table-column>
      <el-table-column label="上报时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="router.push({ name: 'admin-order-detail', params: { id: row.orderId } })">{{ row.status === 'PENDING' ? '查看并处理' : '查看结果' }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next, total" @current-change="load" class="pagination" />
  </el-card>
</template>
<style scoped>
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
