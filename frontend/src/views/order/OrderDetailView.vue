<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import ExceptionPanel from './ExceptionPanel.vue'
import ReviewPanel from './ReviewPanel.vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  getOrder,
  actOnOrder,
  orderStatusLabels,
  paymentLabels,
  type OrderDetail,
  type OrderAction,
} from '@/api/order'
import { formatDateTime } from '@/utils/date'
import { roleLabel } from '@/constants'
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const data = ref<OrderDetail>()
const loading = ref(false)
const submitting = ref(false)
const order = computed(() => data.value?.order)
const labels: Record<OrderAction, string> = {
  pay: '模拟支付',
  accept: '接单',
  pickup: '确认揽收',
  deliver: '确认送达',
  complete: '确认取件',
  cancel: '取消订单',
  'admin-cancel': '管理员取消',
}
const actions = computed<OrderAction[]>(() => data.value?.allowedActions ?? [])
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  try {
    const result = await getOrder(String(route.params.id))
    if (current === sequence) data.value = result
  } catch {
    if (current === sequence) data.value = undefined
  } finally {
    if (current === sequence) loading.value = false
  }
}
watch(
  () => route.params.id,
  () => {
    data.value = undefined
    void load()
  },
  { immediate: true },
)
function back() {
  router.push({
    name: auth.isAdmin ? 'admin-orders' : auth.isCourier ? 'orders-assigned' : 'orders-mine',
  })
}
async function act(action: OrderAction) {
  if (submitting.value || !order.value) return
  let reason: string | undefined
  try {
    if (action.includes('cancel')) {
      const result = await ElMessageBox.prompt(
        '请输入取消原因（已支付订单会模拟退款）',
        labels[action],
        {
          inputType: 'textarea',
          inputValidator: (value: string) =>
            (!!value?.trim() && value.length <= 255) || '请填写255字以内的取消原因',
        },
      )
      reason = result.value.trim()
    } else {
      await ElMessageBox.confirm(
        action === 'pay'
          ? '确认模拟支付 ¥' + Number(order.value.fee).toFixed(2) + '？不会真实扣款。'
          : '确定执行“' + labels[action] + '”？',
        labels[action],
        { type: 'warning' },
      )
    }
  } catch {
    return
  }
  submitting.value = true
  try {
    await actOnOrder(order.value.id, action, reason)
    ElMessage.success(labels[action] + '成功')
  } catch {
    /* 统一提示后刷新最新状态 */
  } finally {
    await load()
    submitting.value = false
  }
}
</script>
<template>
  <el-card v-loading="loading" shadow="never">
    <template #header
      ><el-space
        ><el-button @click="back">返回列表</el-button><b>订单详情</b
        ><el-button :loading="loading" @click="load">刷新</el-button></el-space
      ></template
    >
    <template v-if="order">
      <el-space wrap class="actions">
        <el-tag size="large">{{ orderStatusLabels[order.orderStatus] }}</el-tag>
        <el-button
          v-for="action in actions"
          :key="action"
          :type="action.includes('cancel') ? 'danger' : 'primary'"
          :loading="submitting"
          @click="act(action)"
          >{{ labels[action] }}</el-button
        >
      </el-space>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单编号">{{ order.id }}</el-descriptions-item>
        <el-descriptions-item label="取件地址">{{ order.pickupAddress }}</el-descriptions-item>
        <el-descriptions-item label="取件联系人"
          >{{ order.pickupName }} · {{ order.pickupPhone }}</el-descriptions-item
        >
        <el-descriptions-item label="送达地址">{{ order.deliveryAddress }}</el-descriptions-item>
        <el-descriptions-item label="收件联系人"
          >{{ order.deliveryName }} · {{ order.deliveryPhone || '—' }}</el-descriptions-item
        >
        <el-descriptions-item label="配送员">{{
          order.courierId
            ? `${order.courierName ?? '—'} · ${order.courierPhone ?? '—'}`
            : '尚未接单'
        }}</el-descriptions-item>
        <el-descriptions-item label="物品说明">{{ order.itemDescription }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="配送费"
          >¥{{ Number(order.fee).toFixed(2) }} ·
          {{ paymentLabels[order.paymentStatus] }}</el-descriptions-item
        >
        <el-descriptions-item label="发布时间">{{
          formatDateTime(order.createTime)
        }}</el-descriptions-item>
      </el-descriptions>
      <ExceptionPanel :key="order.id" :order-id="order.id" :exceptions="data?.exceptions ?? []" :can-report="data?.canReportException ?? false" :is-admin="auth.isAdmin" @refresh="load" />
      <ReviewPanel :key="order.id + ':' + order.orderStatus" :order-id="order.id" />
      <h3>订单进度</h3>
      <el-timeline>
        <el-timeline-item
          v-for="record in data?.records"
          :key="record.id"
          :timestamp="formatDateTime(record.createTime)"
          placement="top"
        >
          <b>{{ orderStatusLabels[record.toStatus] }}</b> · {{ record.description }}
          <div class="muted">操作身份：{{ roleLabel[record.operatorRole] }}</div>
        </el-timeline-item>
      </el-timeline>
    </template>
    <el-empty v-else-if="!loading" description="无法加载订单，请刷新重试或返回列表" />
  </el-card>
</template>
<style scoped>
.actions {
  margin-bottom: 20px;
}
h3 {
  margin: 24px 0 20px;
  font-size: 16px;
}
</style>
