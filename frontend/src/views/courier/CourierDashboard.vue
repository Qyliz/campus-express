<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, Collection, Position, Tickets } from '@element-plus/icons-vue'

import { listOrders, orderStatusLabels, type ExpressOrder, type OrderStatusEnum } from '@/api/order'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/date'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const error = ref(false)
const recentOrders = ref<ExpressOrder[]>([])
const counts = reactive({ available: 0, awaitingPickup: 0, delivering: 0, completed: 0 })
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

async function load() {
  loading.value = true
  error.value = false
  try {
    const [available, assigned, awaitingPickup, delivering, completed] = await Promise.all([
      listOrders('available', { currentPage: 1 }),
      listOrders('assigned', { currentPage: 1 }),
      listOrders('assigned', { currentPage: 1, orderStatus: 'AWAITING_PICKUP' }),
      listOrders('assigned', { currentPage: 1, orderStatus: 'DELIVERING' }),
      listOrders('assigned', { currentPage: 1, orderStatus: 'COMPLETED' }),
    ])
    recentOrders.value = assigned.records.slice(0, 5)
    counts.available = available.total
    counts.awaitingPickup = awaitingPickup.total
    counts.delivering = delivering.total
    counts.completed = completed.total
  } catch {
    recentOrders.value = []
    Object.assign(counts, { available: 0, awaitingPickup: 0, delivering: 0, completed: 0 })
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
        <span>新的配送任务已经准备好，出发前请确认取送信息。</span>
      </div>
      <el-button
        type="primary"
        size="large"
        :icon="Collection"
        @click="router.push({ name: 'orders-available' })"
      >
        前往接单大厅
      </el-button>
    </header>

    <section v-loading="loading" class="stats" aria-label="配送概览">
      <StatCard
        label="可接订单"
        :value="counts.available"
        tone="blue"
        @click="router.push({ name: 'orders-available' })"
      >
        <Collection />
      </StatCard>
      <StatCard
        label="待揽收"
        :value="counts.awaitingPickup"
        tone="amber"
        @click="router.push({ name: 'orders-assigned' })"
      >
        <Tickets />
      </StatCard>
      <StatCard
        label="配送中"
        :value="counts.delivering"
        tone="violet"
        @click="router.push({ name: 'orders-assigned' })"
      >
        <Position />
      </StatCard>
      <StatCard
        label="已完成"
        :value="counts.completed"
        tone="green"
        @click="router.push({ name: 'orders-assigned' })"
      >
        <CircleCheck />
      </StatCard>
    </section>

    <el-alert
      v-if="error"
      title="配送数据加载失败，请稍后重试"
      type="error"
      :closable="false"
      class="dashboard-alert"
    />

    <el-card shadow="never" class="recent-card">
      <template #header>
        <div class="section-heading">
          <div>
            <h2>最近配送</h2>
            <p>查看最近接取及正在处理的配送订单</p>
          </div>
          <el-button text type="primary" @click="router.push({ name: 'orders-assigned' })">
            查看全部
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="recentOrders"
        empty-text="暂无配送订单，可前往接单大厅看看"
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
        <el-table-column label="接单时间" width="175">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
