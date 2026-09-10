<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  listOrders,
  actOnOrder,
  orderStatusLabels,
  type ExpressOrder,
  type OrderScope,
} from '@/api/order'
import { formatDateTime } from '@/utils/date'
const route = useRoute()
const router = useRouter()
const scope = computed(() => route.meta.orderScope as OrderScope)
const rows = ref<ExpressOrder[]>([])
const total = ref(0)
const page = ref(1)
const orderStatus = ref<number>()
const orderId = ref('')
const relation = ref('all')
const loading = ref(false)
const acting = ref('')
const error = ref(false)
let requestSequence = 0
async function load() {
  const sequence = ++requestSequence
  loading.value = true
  error.value = false
  try {
    const result = await listOrders(scope.value, {
      currentPage: page.value,
      relation: scope.value === 'mine' ? relation.value : undefined,
      orderStatus: orderStatus.value,
      orderId: scope.value === 'admin' && orderId.value.trim() ? orderId.value.trim() : undefined,
    })
    if (sequence !== requestSequence) return
    rows.value = result.records
    total.value = result.total
  } catch {
    if (sequence === requestSequence) {
      rows.value = []
      total.value = 0
      error.value = true
    }
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}
function search() {
  page.value = 1
  void load()
}
watch(
  scope,
  () => {
    page.value = 1
    orderStatus.value = undefined
    orderId.value = ''
    relation.value = 'all'
    rows.value = []
    void load()
  },
  { immediate: true },
)
function detail(id: string) {
  router.push({
    name: scope.value === 'admin' ? 'admin-order-detail' : 'order-detail',
    params: { id },
  })
}
async function accept(id: string) {
  if (acting.value) return
  try {
    await ElMessageBox.confirm('确认接下此订单并负责配送？', '确认接单', { type: 'warning' })
  } catch {
    return
  }
  acting.value = id
  try {
    await actOnOrder(id, 'accept')
    ElMessage.success('接单成功')
    detail(id)
  } catch {
    await load()
  } finally {
    acting.value = ''
  }
}
</script>
<template>
  <el-card shadow="never">
    <template #header
      ><b>{{ route.meta.title }}</b></template
    >
    <el-space wrap class="filters">
      <el-select v-if="scope === 'mine'" v-model="relation" style="width: 140px" @change="search">
        <el-option label="全部订单" value="all" />
        <el-option label="我下的" value="created" />
        <el-option label="我收到的" value="received" />
      </el-select>
      <el-select
        v-if="scope !== 'available'"
        v-model="orderStatus"
        clearable
        placeholder="全部状态"
        style="width: 160px"
        @change="search"
      >
        <el-option
          v-for="(label, index) in orderStatusLabels"
          :key="index"
          :label="label"
          :value="index"
        />
      </el-select>
      <el-input
        v-if="scope === 'admin'"
        v-model="orderId"
        placeholder="输入完整订单编号"
        clearable
        style="width: 240px"
        @keyup.enter="search"
      />
      <el-button :loading="loading" @click="search">查询 / 刷新</el-button>
      <el-button
        v-if="scope === 'mine'"
        type="primary"
        @click="router.push({ name: 'order-create' })"
        >发布订单</el-button
      >
    </el-space>
    <el-alert
      v-if="scope === 'available'"
      title="接单后可在订单详情查看联系人、电话及备注。"
      :closable="false"
      type="info"
      class="filters"
    />
    <el-alert v-if="error" title="订单加载失败，请点击刷新重试。" type="error" :closable="false" />
    <el-table v-loading="loading" :data="rows" empty-text="暂无订单" style="width: 100%">
      <el-table-column prop="id" label="订单编号" min-width="190" />
      <el-table-column v-if="scope === 'mine'" label="与我关系" min-width="130">
        <template #default="{ row }"><el-tag v-if="row.createdByMe">我下的</el-tag> <el-tag v-if="row.receivedByMe" type="success">我收到的</el-tag></template>
      </el-table-column>
      <el-table-column v-if="scope !== 'available'" label="异常" width="130">
        <template #default="{ row }"><el-tag v-if="row.pendingException" type="danger">异常待处理</el-tag><span v-else>—</span></template>
      </el-table-column>
      <el-table-column
        prop="pickupAddress"
        label="取件地址"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        prop="deliveryAddress"
        label="送达地址"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        prop="itemDescription"
        label="物品说明"
        min-width="150"
        show-overflow-tooltip
      />
      <el-table-column label="配送费" width="100"
        ><template #default="{ row }">¥{{ Number(row.fee).toFixed(2) }}</template></el-table-column
      >
      <el-table-column label="状态" width="100"
        ><template #default="{ row }"
          ><el-tag>{{ orderStatusLabels[row.orderStatus] }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="发布时间" min-width="170"
        ><template #default="{ row }">{{
          formatDateTime(row.createTime)
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="scope === 'available'"
            type="primary"
            :loading="acting === row.id"
            :disabled="!!acting && acting !== row.id"
            @click="accept(row.id)"
            >接单</el-button
          >
          <el-button v-else link type="primary" @click="detail(row.id)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :page-size="10"
      :total="total"
      layout="prev, pager, next, total"
      class="pagination"
      @current-change="load"
    />
  </el-card>
</template>
<style scoped>
.filters {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
