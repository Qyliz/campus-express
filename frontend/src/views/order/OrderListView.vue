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
import { orUndefined, type All } from '@/utils/query'
import { formatDateTime } from '@/utils/date'
const route = useRoute()
const router = useRouter()
const scope = computed(() => route.meta.orderScope as OrderScope)
const rows = ref<ExpressOrder[]>([])
const total = ref(0)
const page = ref(1)
const filters = reactive({
  // '' = 全部订单；省略该参数时后端 OrderQueryDTO.relation 的字段初始值就是 ALL。
  relation: '' as All<Exclude<OrderRelationEnum, 'ALL'>>,
  orderStatus: '' as All<OrderStatusEnum>,
  orderId: '',
})
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
      relation: scope.value === 'mine' ? orUndefined(filters.relation) : undefined,
      orderStatus: orUndefined(filters.orderStatus),
      orderId: scope.value === 'admin' ? orUndefined(filters.orderId.trim()) : undefined,
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
function resetFilters() {
  Object.assign(filters, { relation: '', orderStatus: '', orderId: '' })
  search()
}
// immediate: true 让这一段同时充当首屏加载；切 scope 时清空筛选而不只是清列表。
watch(
  scope,
  () => {
    page.value = 1
    Object.assign(filters, { relation: '', orderStatus: '', orderId: '' })
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
  <el-card shadow="never" class="page-card">
    <!-- 三个筛选条件与各 scope 的可见性都保持原样，只是换成账号管理的样式与「点查询才查」的逻辑 -->
    <el-form inline @submit.prevent>
      <el-form-item v-if="scope === 'mine'" label="与我关系">
        <el-select v-model="filters.relation" placeholder="全部" style="width: 130px">
          <el-option label="全部订单" value="" />
          <el-option label="我下的" value="CREATED" />
          <el-option label="我收到的" value="RECEIVED" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="scope !== 'available'" label="状态">
        <el-select v-model="filters.orderStatus" placeholder="全部" style="width: 130px">
          <el-option label="全部" value="" />
          <el-option
            v-for="o in orderStatusOptions"
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
          style="width: 180px"
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
    <el-table v-loading="loading" :data="rows" empty-text="暂无订单" style="width: 100%">
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
      <!-- 待接单订单还没有骑手，骑手信息对用户端也是空白，所以大厅不显示这一列 -->
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
      :page-size="10"
      :total="total"
      layout="prev, pager, next, total"
      class="pager"
      @current-change="load"
    />
  </el-card>
</template>
<style scoped>
/* 分页底边距与右对齐复用全局 .pager（assets/main.css） */
.notice {
  margin-bottom: 16px;
}
</style>
