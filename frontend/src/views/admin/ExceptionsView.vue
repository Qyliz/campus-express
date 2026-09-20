<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import {
  exceptionStatusOptions,
  exceptionStatusLabels,
  exceptionStatusTagType,
  exceptionTypeLabels,
  listExceptions,
  type DeliveryException,
  type ExceptionStatusEnum,
  type ExceptionTypeEnum,
} from '@/api/order'
import { usePagedList } from '@/composables/usePagedList'
import { PAGE_SIZE } from '@/constants'
import { orUndefined, type All } from '@/utils/query'
import { formatDateTime } from '@/utils/date'
const router = useRouter()
const filters = reactive({
  status: '' as All<ExceptionStatusEnum>,
  orderId: '',
})
const { rows, page, total, loading, error, load, search } = usePagedList<DeliveryException>(
  (currentPage) =>
    listExceptions({
      currentPage,
      status: orUndefined(filters.status),
      orderId: orUndefined(filters.orderId.trim()),
    }),
)
function resetFilters() {
  Object.assign(filters, { status: '', orderId: '' })
  search()
}
onMounted(load)
</script>
<template>
  <el-card shadow="never" class="page-card">
    <el-form inline class="filter-form" @submit.prevent>
      <el-form-item label="状态">
        <!-- 空串表示全部 -->
        <el-select v-model="filters.status" placeholder="全部" class="filter-control">
          <el-option label="全部" value="" />
          <el-option
            v-for="o in exceptionStatusOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单编号">
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
    <el-alert v-if="error" title="加载失败，请刷新重试" type="error" :closable="false" />
    <el-table v-loading="loading" :data="rows" empty-text="暂无异常记录">
      <el-table-column prop="orderId" label="订单编号" min-width="190" />
      <el-table-column label="异常类型" min-width="140"
        ><template #default="{ row }">{{
          exceptionTypeLabels[row.type as ExceptionTypeEnum]
        }}</template></el-table-column
      >
      <el-table-column prop="description" label="异常说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100"
        ><template #default="{ row }"
          ><el-tag :type="exceptionStatusTagType[row.status as ExceptionStatusEnum]">{{
            exceptionStatusLabels[row.status as ExceptionStatusEnum]
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="上报时间" min-width="170"
        ><template #default="{ row }">{{
          formatDateTime(row.createTime)
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="130"
        ><template #default="{ row }"
          ><el-button
            link
            type="primary"
            @click="router.push({ name: 'admin-order-detail', params: { id: row.orderId } })"
            >{{ row.status === 'PENDING' ? '查看并处理' : '查看结果' }}</el-button
          ></template
        ></el-table-column
      >
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
