<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import {
  getReviews,
  createReview,
  createAppeal,
  appealStatusLabels,
  appealStatusTagType,
  reviewStatusLabels,
  reviewStatusTagType,
  type OrderReviews,
  type AppealStatusEnum,
} from '@/api/review'
import { formatDateTime } from '@/utils/date'
const props = defineProps<{ orderId: string }>()
const data = ref<OrderReviews>()
const loading = ref(false)
const submitting = ref(false)
const rating = ref(0)
const content = ref('')
const appealId = ref('')
const reason = ref('')
const dialogOpen = ref(false)
let sequence = 0
onBeforeUnmount(() => {
  sequence++
})
async function load() {
  const current = ++sequence
  loading.value = true
  try {
    const result = await getReviews(props.orderId)
    if (current === sequence) data.value = result
  } catch {
    /* 统一错误提示 */
  } finally {
    if (current === sequence) loading.value = false
  }
}
watch(
  () => props.orderId,
  () => {
    data.value = undefined
    rating.value = 0
    content.value = ''
    dialogOpen.value = false
    void load()
  },
  { immediate: true },
)
async function submitReview() {
  if (submitting.value) return
  if (!rating.value || !content.value.trim() || content.value.trim().length > 500) {
    ElMessage.warning('请选择星级并填写1～500字评价')
    return
  }
  submitting.value = true
  try {
    await createReview(props.orderId, rating.value, content.value.trim())
    content.value = ''
    rating.value = 0
    ElMessage.success('评价成功')
    await load()
  } catch {
    /* 保留输入 */
  } finally {
    submitting.value = false
  }
}
function openAppeal(id: string) {
  appealId.value = id
  reason.value = ''
  dialogOpen.value = true
}
async function submitAppeal() {
  if (submitting.value) return
  if (!reason.value.trim() || reason.value.trim().length > 500) {
    ElMessage.warning('请填写1～500字申诉理由')
    return
  }
  submitting.value = true
  try {
    await createAppeal(appealId.value, reason.value.trim())
    dialogOpen.value = false
    ElMessage.success('申诉已提交')
    await load()
  } catch {
    /* 保留输入 */
  } finally {
    submitting.value = false
  }
}
</script>
<template>
  <section v-loading="loading" class="reviews">
    <h3>服务评价</h3>
    <template v-if="data">
      <el-form v-if="data.canReview" label-position="top">
        <el-form-item label="服务评分"
          ><el-rate v-model="rating" :disabled="submitting"
        /></el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            :disabled="submitting"
            placeholder="请描述配送员的服务，提交后不可修改"
          />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submitReview">发表评价</el-button>
      </el-form>
      <el-empty v-if="!data.reviews.length" description="暂无评价" :image-size="60" />
      <el-card v-for="item in data.reviews" :key="item.review.id" shadow="never" class="review">
        <el-space wrap>
          <span>评价用户：{{ item.username || item.review.userId }}</span>
          <el-rate :model-value="item.review.rating" disabled />
          <el-tag :type="reviewStatusTagType[item.review.status]">{{
            reviewStatusLabels[item.review.status]
          }}</el-tag>
          <span>{{ formatDateTime(item.review.createTime) }}</span>
        </el-space>
        <p class="text">{{ item.review.content }}</p>
        <el-button v-if="item.canAppeal" @click="openAppeal(item.review.id)">申诉评价</el-button>
        <div v-if="item.appeal" class="appeal">
          <el-tag :type="appealStatusTagType[item.appeal.status as AppealStatusEnum]">{{
            appealStatusLabels[item.appeal.status as AppealStatusEnum]
          }}</el-tag>
          <p class="text">申诉理由：{{ item.appeal.reason }}</p>
          <p>申诉时间：{{ formatDateTime(item.appeal.createTime) }}</p>
          <template v-if="item.appeal.status !== 'PENDING'">
            <p class="text">处理理由：{{ item.appeal.resolutionReason }}</p>
            <p>处理时间：{{ formatDateTime(item.appeal.resolvedTime!) }}</p>
          </template>
        </div>
      </el-card>
    </template>
    <el-empty v-else-if="!loading" description="评价加载失败，请刷新重试" :image-size="60" />
    <el-dialog
      v-model="dialogOpen"
      title="申诉评价"
      width="min(500px, 90vw)"
      :close-on-click-modal="!submitting"
      :show-close="!submitting"
      :close-on-press-escape="!submitting"
    >
      <p>每条评价只能申诉一次，请完整说明理由。</p>
      <el-input
        v-model="reason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        :disabled="submitting"
      />
      <template #footer>
        <el-button :disabled="submitting" @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </section>
</template>
<style scoped>
.reviews {
  margin-top: 24px;
}
.review {
  margin-top: 16px;
}
.text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.appeal {
  padding: 12px;
  background: var(--el-fill-color-light);
  margin-top: 12px;
}
</style>
