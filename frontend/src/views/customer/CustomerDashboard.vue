<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, Plus, Tickets, Van } from '@element-plus/icons-vue'

import { listOrders, orderStatusLabels, type ExpressOrder, type OrderStatusEnum } from '@/api/order'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/date'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const error = ref(false)
const recentOrders = ref<ExpressOrder[]>([])
const counts = reactive({ all: 0, unpaid: 0, inProgress: 0, completed: 0 })
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

async function load() {
  loading.value = true
  error.value = false
  try {
    const [all, unpaid, completed, cancelled] = await Promise.all([
      listOrders('mine', { currentPage: 1 }),
      listOrders('mine', { currentPage: 1, orderStatus: 'UNPAID' }),
      listOrders('mine', { currentPage: 1, orderStatus: 'COMPLETED' }),
      listOrders('mine', { currentPage: 1, orderStatus: 'CANCELLED' }),
    ])
    recentOrders.value = all.records.slice(0, 5)
    counts.all = all.total
    counts.unpaid = unpaid.total
    counts.completed = completed.total
    counts.inProgress = Math.max(0, all.total - unpaid.total - completed.total - cancelled.total)
  } catch {
    recentOrders.value = []
    Object.assign(counts, { all: 0, unpaid: 0, inProgress: 0, completed: 0 })
    error.value = true
  } finally {
    loading.value = false
  }
}

function openOrder(id: string) {
  router.push({ name: 'order-detail', params: { id } })
}

onMounted(load)
</script>

<template>
  <div class="dashboard-page">
    <header class="welcome">
      <div>
        <p>{{ today }}</p>
        <h1>你好，{{ auth.username }}</h1>
        <span>今天也让每一份托付准时抵达。</span>
      </div>
      <el-button
        type="primary"
        size="large"
        :icon="Plus"
        @click="router.push({ name: 'order-create' })"
      >
        发布新订单
      </el-button>
    </header>

    <section v-loading="loading" class="stats" aria-label="订单概览">
      <StatCard
        label="全部订单"
        :value="counts.all"
        tone="blue"
        @click="router.push({ name: 'orders-mine' })"
      >
        <Tickets />
      </StatCard>
      <StatCard
        label="待支付"
        :value="counts.unpaid"
        tone="amber"
        @click="router.push({ name: 'orders-mine' })"
      >
        <span class="currency">¥</span>
      </StatCard>
      <StatCard
        label="进行中"
        :value="counts.inProgress"
        tone="violet"
        @click="router.push({ name: 'orders-mine' })"
      >
        <Van />
      </StatCard>
      <StatCard
        label="已完成"
        :value="counts.completed"
        tone="green"
        @click="router.push({ name: 'orders-mine' })"
      >
        <CircleCheck />
      </StatCard>
    </section>

    <el-alert
      v-if="error"
      title="工作台数据加载失败，请稍后重试"
      type="error"
      :closable="false"
      class="dashboard-alert"
    />

    <el-card shadow="never" class="recent-card">
      <template #header>
        <div class="section-heading">
          <div>
            <h2>最近订单</h2>
            <p>查看最新发布和接收的配送订单</p>
          </div>
          <el-button text type="primary" @click="router.push({ name: 'orders-mine' })">
            查看全部
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="recentOrders"
        empty-text="暂无订单，发布第一笔配送需求吧"
        @row-click="(row: ExpressOrder) => openOrder(row.id)"
      >
        <el-table-column prop="id" label="订单编号" min-width="180">
          <template #default="{ row }"
            ><span class="order-id">#{{ row.id }}</span></template
          >
        </el-table-column>
        <el-table-column
          prop="itemDescription"
          label="物品"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column label="配送路线" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="route-text">{{ row.pickupAddress }} → {{ row.deliveryAddress }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag effect="light">{{
              orderStatusLabels[row.orderStatus as OrderStatusEnum]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配送费" width="100">
          <template #default="{ row }">¥{{ Number(row.fee).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="175">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
/* 插槽内容在父组件作用域编译，这里的样式能作用到 StatCard 图标区里的文字符号 */
.currency {
  font-size: 23px;
  font-weight: 700;
}
</style>
