<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, DocumentChecked, Tickets, User } from '@element-plus/icons-vue'

import { pageAuditRecords, pageUsers } from '@/api/admin'
import {
  listExceptions,
  listOrders,
  orderStatusLabels,
  type ExpressOrder,
  type OrderStatusEnum,
} from '@/api/order'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/date'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const error = ref(false)
const recentOrders = ref<ExpressOrder[]>([])
const counts = reactive({ users: 0, audits: 0, orders: 0, exceptions: 0 })

async function load() {
  loading.value = true
  error.value = false
  try {
    const [users, audits, orders, exceptions] = await Promise.all([
      pageUsers({ currentPage: 1 }),
      pageAuditRecords({ currentPage: 1, auditStatus: 'REVIEWING' }),
      listOrders('admin', { currentPage: 1 }),
      listExceptions({ currentPage: 1, status: 'PENDING' }),
    ])
    counts.users = users.total
    counts.audits = audits.total
    counts.orders = orders.total
    counts.exceptions = exceptions.total
    recentOrders.value = orders.records.slice(0, 6)
  } catch {
    recentOrders.value = []
    Object.assign(counts, { users: 0, audits: 0, orders: 0, exceptions: 0 })
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="dashboard-page">
    <header class="welcome">
      <div>
        <p>后台总览</p>
        <h1>你好，{{ auth.username }}</h1>
        <span>在这里掌握平台运行状态并处理待办事项。</span>
      </div>
    </header>

    <section v-loading="loading" class="stats" aria-label="平台概览">
      <StatCard
        label="平台账号"
        :value="counts.users"
        tone="blue"
        @click="router.push({ name: 'admin-users' })"
      >
        <User />
      </StatCard>
      <StatCard
        label="待审核"
        :value="counts.audits"
        tone="amber"
        @click="router.push({ name: 'admin-audits' })"
      >
        <DocumentChecked />
      </StatCard>
      <StatCard
        label="全部订单"
        :value="counts.orders"
        tone="violet"
        @click="router.push({ name: 'admin-orders' })"
      >
        <Tickets />
      </StatCard>
      <StatCard
        label="待处理异常"
        :value="counts.exceptions"
        tone="red"
        @click="router.push({ name: 'admin-exceptions' })"
      >
        <Bell />
      </StatCard>
    </section>

    <el-alert
      v-if="error"
      title="后台数据加载失败，请稍后重试"
      type="error"
      :closable="false"
      class="dashboard-alert"
    />

    <el-card shadow="never" class="recent-card">
      <template #header>
        <div class="section-heading">
          <div>
            <h2>最近订单</h2>
            <p>快速查看平台最新发布的配送订单</p>
          </div>
          <el-button text type="primary" @click="router.push({ name: 'admin-orders' })"
            >查看全部</el-button
          >
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="recentOrders"
        empty-text="暂无订单"
        @row-click="
          (row: ExpressOrder) => router.push({ name: 'admin-order-detail', params: { id: row.id } })
        "
      >
        <el-table-column prop="id" label="订单编号" min-width="190">
          <template #default="{ row }"
            ><span class="order-id">#{{ row.id }}</span></template
          >
        </el-table-column>
        <el-table-column
          prop="itemDescription"
          label="物品"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column label="配送路线" min-width="280" show-overflow-tooltip>
          <template #default="{ row }"
            >{{ row.pickupAddress }} → {{ row.deliveryAddress }}</template
          >
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag effect="light">{{
              orderStatusLabels[row.orderStatus as OrderStatusEnum]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="175">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
