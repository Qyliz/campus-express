<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import {
  listOrders,
  actOnOrder,
  orderStatusLabels,
  orderStatusOptions,
  type ExpressOrder,
  type OrderRelationEnum,
  type OrderScope,
  type OrderStatusEnum,
} from '@/api/order'
import { usePagedList } from '@/composables/usePagedList'
import { PAGE_SIZE } from '@/constants'
import { orUndefined, type All } from '@/utils/query'
import { formatDateTime } from '@/utils/date'
const route = useRoute()
const router = useRouter()
const scope = computed(() => route.meta.orderScope as OrderScope)
const filters = reactive({
  //空串沿用后端默认的全部关系
  relation: '' as All<Exclude<OrderRelationEnum, 'ALL'>>,
  orderStatus: '' as All<OrderStatusEnum>,
  orderId: '',
})
const acting = ref('')

const assignedStatusOptions = orderStatusOptions.filter(
  ({ value }) => value !== 'UNPAID' && value !== 'AVAILABLE',
)
const visibleStatusOptions = computed(() =>
  scope.value === 'assigned' ? assignedStatusOptions : orderStatusOptions,
)

const { rows, total, page, loading, error, load, search, clear } = usePagedList<ExpressOrder>(
  (currentPage) =>
    listOrders(scope.value, {
      currentPage,
      relation: scope.value === 'mine' ? orUndefined(filters.relation) : undefined,
      orderStatus: scope.value === 'available' ? undefined : orUndefined(filters.orderStatus),
      orderId: scope.value === 'admin' ? orUndefined(filters.orderId.trim()) : undefined,
    }),
)
function resetFilters() {
  Object.assign(filters, { relation: '', orderStatus: '', orderId: '' })
  search()
}
//切换范围时重置筛选并重新加载
watch(
  scope,
  () => {
    page.value = 1
    Object.assign(filters, { relation: '', orderStatus: '', orderId: '' })
    clear()
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
  <el-card v-if="scope !== 'available'" shadow="never" class="page-card">
    <el-form inline class="filter-form" @submit.prevent>
      <el-form-item v-if="scope === 'mine'" label="与我关系">
        <el-select v-model="filters.relation" placeholder="全部" class="filter-control">
          <el-option label="全部订单" value="" />
          <el-option label="我下的" value="CREATED" />
          <el-option label="我收到的" value="RECEIVED" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.orderStatus" placeholder="全部" class="filter-control">
          <el-option label="全部" value="" />
          <el-option
            v-for="o in visibleStatusOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="scope === 'admin'" label="订单编号">
        <el-input
          v-model="filters.orderId"
          placeholder="完整订单编号"
          clearable
          class="filter-control"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
  <el-card shadow="never">
    <template #header
      ><b>{{ route.meta.title }}</b></template
    >
    <el-alert
      v-if="scope === 'available'"
      title="接单后可在订单详情查看联系人、电话及备注。"
      :closable="false"
      type="info"
      class="notice"
    />
    <el-alert
      v-if="error"
      title="订单加载失败，请刷新重试。"
      type="error"
      :closable="false"
      class="notice"
    />
    <el-table v-loading="loading" :data="rows" empty-text="暂无订单">
      <el-table-column prop="id" label="订单编号" min-width="190" />
      <el-table-column v-if="scope === 'mine'" label="与我关系" min-width="130">
        <template #default="{ row }"
          ><el-tag v-if="row.createdByMe">我寄送的</el-tag>
          <el-tag v-if="row.receivedByMe" type="success">我收到的</el-tag></template
        >
      </el-table-column>
      <el-table-column v-if="scope !== 'available'" label="异常" width="130">
        <template #default="{ row }"
          ><el-tag v-if="row.pendingException" type="danger">异常待处理</el-tag
          ><span v-else>—</span></template
        >
      </el-table-column>
      <!-- 接单大厅不显示骑手列 -->
      <el-table-column
        v-if="scope !== 'available'"
        label="配送员"
        min-width="170"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{
          row.courierName ? `${row.courierName} · ${row.courierPhone ?? '—'}` : '尚未接单'
        }}</template>
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
          ><el-tag>{{ orderStatusLabels[(row as ExpressOrder).orderStatus] }}</el-tag></template
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
      :page-size="PAGE_SIZE"
      :total="total"
      :pager-count="5"
      layout="prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </el-card>
</template>
<style scoped>
/* 分页使用全局 pager 样式 */
.notice {
  margin-bottom: 16px;
}
</style>
