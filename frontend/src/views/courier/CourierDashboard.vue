<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, Collection, Position, Tickets } from '@element-plus/icons-vue'

import { listOrders, orderStatusLabels, type ExpressOrder, type OrderStatusEnum } from '@/api/order'
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
  <div class="dashboard">
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
      <article class="stat-card blue" @click="router.push({ name: 'orders-available' })">
        <span class="stat-icon"><Collection /></span>
        <div>
          <small>可接订单</small><strong>{{ counts.available }}</strong>
        </div>
      </article>
      <article class="stat-card amber" @click="router.push({ name: 'orders-assigned' })">
        <span class="stat-icon"><Tickets /></span>
        <div>
          <small>待揽收</small><strong>{{ counts.awaitingPickup }}</strong>
        </div>
      </article>
      <article class="stat-card violet" @click="router.push({ name: 'orders-assigned' })">
        <span class="stat-icon"><Position /></span>
        <div>
          <small>配送中</small><strong>{{ counts.delivering }}</strong>
        </div>
      </article>
      <article class="stat-card green" @click="router.push({ name: 'orders-assigned' })">
        <span class="stat-icon"><CircleCheck /></span>
        <div>
          <small>已完成</small><strong>{{ counts.completed }}</strong>
        </div>
      </article>
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

<style scoped>
.dashboard {
  width: 100%;
}

.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 30px;
}

.welcome p,
.welcome h1,
.welcome span {
  margin: 0;
}

.welcome p {
  margin-bottom: 7px;
  color: #7d8b99;
  font-size: 13px;
}

.welcome h1 {
  color: #172b3d;
  font-size: clamp(26px, 3vw, 36px);
}

.welcome span {
  display: block;
  margin-top: 8px;
  color: #788896;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 18px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 118px;
  padding: 22px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e7edf3;
  border-radius: 16px;
  box-shadow: 0 8px 28px rgba(34, 61, 83, 0.06);
  transition: 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(34, 61, 83, 0.1);
}

.stat-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
}

.stat-icon svg {
  width: 23px;
}

.stat-card.blue .stat-icon {
  color: #287dcc;
  background: #e9f4ff;
}
.stat-card.amber .stat-icon {
  color: #d48816;
  background: #fff4dd;
}
.stat-card.violet .stat-icon {
  color: #7356c9;
  background: #f0ecff;
}
.stat-card.green .stat-icon {
  color: #32966a;
  background: #e7f8f0;
}

.stat-card div:last-child {
  display: flex;
  flex-direction: column;
}

.stat-card small {
  color: #7d8b99;
  font-size: 13px;
}
.stat-card strong {
  margin-top: 6px;
  color: #172b3d;
  font-size: 28px;
}
.dashboard-alert {
  margin-top: 20px;
}

.recent-card {
  margin-top: 24px;
  border: 1px solid #e7edf3;
  border-radius: 16px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-heading h2,
.section-heading p {
  margin: 0;
}
.section-heading h2 {
  color: #172b3d;
  font-size: 18px;
}
.section-heading p {
  margin-top: 5px;
  color: #8a98a6;
  font-size: 12px;
}
.order-id {
  color: #287dcc;
  font-weight: 600;
}
.route-text {
  color: #596a79;
}
:deep(.el-table__row) {
  cursor: pointer;
}

@media (max-width: 1050px) {
  .stats {
    grid-template-columns: repeat(2, minmax(150px, 1fr));
  }
}

@media (max-width: 720px) {
  .welcome {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .stats {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .stat-card {
    min-height: 98px;
    padding: 14px;
  }
  .stat-icon {
    width: 40px;
    height: 40px;
  }
}
</style>
