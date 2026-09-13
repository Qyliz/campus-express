<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import {
  getAdminAppeals, getReviews, resolveAppeal,
  appealStatusLabels, appealStatusOptions, appealStatusTagType,
  reviewStatusLabels, reviewStatusTagType,
  type AppealStatusEnum, type ReviewAppeal, type ReviewItem,
} from '@/api/review'
import { orUndefined, type All } from '@/utils/query'
import { formatDateTime } from '@/utils/date'

const filters = reactive({
  status: '' as All<AppealStatusEnum>,
  orderId: '',
})
const page = ref(1)
const total = ref(0)
const rows = ref<ReviewAppeal[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogOpen = ref(false)
const selected = ref<ReviewAppeal>()
const original = ref<ReviewItem>()
const detailLoading = ref(false)
const resolution = ref<AppealStatusEnum>('UPHELD')
const reason = ref('')
let sequence = 0
let detailSequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  try {
    const result = await getAdminAppeals({
      currentPage: page.value,
      status: orUndefined(filters.status),
      orderId: orUndefined(filters.orderId.trim()),
    })
    if (current === sequence) { rows.value = result.records; total.value = result.total }
  } catch { /* 统一提示 */ }
  finally { if (current === sequence) loading.value = false }
}
function search() { page.value = 1; void load() }
function resetFilters() {
  Object.assign(filters, { status: '', orderId: '' })
  search()
}
async function open(row: ReviewAppeal) {
  selected.value = row
  reason.value = ''
  resolution.value = 'UPHELD'
  original.value = undefined
  dialogOpen.value = true
  detailLoading.value = true
  const current = ++detailSequence
  try {
    const result = await getReviews(row.orderId)
    if (current === detailSequence) {
      original.value = result.reviews.find(item => item.review.id === row.reviewId)
      if (original.value?.appeal) selected.value = original.value.appeal
    }
  } catch { /* 显示失败状态，禁止处理 */ }
  finally { if (current === detailSequence) detailLoading.value = false }
}
async function submit() {
  if (submitting.value || !selected.value || !original.value) return
  if (!reason.value.trim() || reason.value.trim().length > 500) {
    ElMessage.warning('请填写1～500字处理理由')
    return
  }
  submitting.value = true
  try {
    await resolveAppeal(selected.value.id, resolution.value, reason.value.trim())
    dialogOpen.value = false
    ElMessage.success('申诉已处理')
    await load()
  } catch { /* 保留处理理由 */ }
  finally { submitting.value = false }
}
onMounted(load)
</script>
<template>
  <el-card shadow="never" class="page-card">
    <el-form inline @submit.prevent>
      <el-form-item label="申诉状态">
        <el-select v-model="filters.status" placeholder="全部" style="width: 130px">
          <el-option label="全部" value="" />
          <el-option v-for="o in appealStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
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
    <el-table v-loading="loading" :data="rows" row-key="id" border stripe>
      <el-table-column prop="id" label="申诉编号" min-width="180" />
      <el-table-column label="订单编号" min-width="180">
        <template #default="{ row }"><router-link :to="{ name: 'admin-order-detail', params: { id: row.orderId } }">{{ row.orderId }}</router-link></template>
      </el-table-column>
      <!-- 管理端按裸 ID 定位账户：courierId 是 courier 表主键、adminId 是 admin 表主键，换成都可能重名且会变的用户名反而不好核对 -->
      <el-table-column prop="courierId" label="配送员账户" min-width="180" />
      <el-table-column prop="reason" label="申诉理由" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="appealStatusTagType[row.status as AppealStatusEnum]">{{ appealStatusLabels[row.status as AppealStatusEnum] }}</el-tag></template></el-table-column>
      <el-table-column label="提交时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
      <el-table-column label="操作" width="110"><template #default="{ row }"><el-button link type="primary" @click="open(row as ReviewAppeal)">{{ row.status === 'PENDING' ? '查看并处理' : '查看结果' }}</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" class="pager" @current-change="load" />
  </el-card>
  <el-dialog v-model="dialogOpen" title="评价申诉" width="min(600px, 90vw)" :close-on-click-modal="!submitting" :show-close="!submitting" :close-on-press-escape="!submitting">
    <div v-loading="detailLoading">
      <template v-if="original && selected">
        <el-rate :model-value="original.review.rating" disabled />
        <el-tag :type="reviewStatusTagType[original.review.status]">{{ reviewStatusLabels[original.review.status] }}</el-tag>
        <p class="text">原评价：{{ original.review.content }}</p>
        <p class="text">申诉理由：{{ selected.reason }}</p>
        <el-tag :type="appealStatusTagType[selected.status]">{{ appealStatusLabels[selected.status] }}</el-tag>
        <el-form v-if="selected.status === 'PENDING'" label-position="top">
          <el-form-item for="" label="处理结果">
            <el-radio-group v-model="resolution" :disabled="submitting">
              <el-radio value="UPHELD">申诉成立，作废评价</el-radio>
              <el-radio value="REJECTED">驳回，保留评价</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理理由（必填）"><el-input v-model="reason" type="textarea" :rows="4" maxlength="500" show-word-limit :disabled="submitting" /></el-form-item>
        </el-form>
        <template v-else>
          <p class="text">处理理由：{{ selected.resolutionReason }}</p>
          <p>处理管理员：{{ selected.adminId }}</p>
          <p>处理时间：{{ formatDateTime(selected.resolvedTime!) }}</p>
        </template>
      </template>
      <el-empty v-else-if="!detailLoading" description="原评价加载失败，请关闭后重试" />
    </div>
    <template #footer>
      <el-button :disabled="submitting" @click="dialogOpen = false">关闭</el-button>
      <el-button v-if="original && selected?.status === 'PENDING'" type="primary" :loading="submitting" @click="submit">确认处理</el-button>
    </template>
  </el-dialog>
</template>
<style scoped>
/* 分页底边距与右对齐复用全局 .pager（assets/main.css） */
.text { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
