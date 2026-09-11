<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

import { pageBanRecords, unbanUser } from '@/api/admin'
import { allSortOptions, genderLabel, PAGE_SIZE, roleLabel, roleTagType } from '@/constants'
import { formatDateTime } from '@/utils/date'
import { orUndefined, type All } from '@/utils/query'
import type { GenderEnum, RoleEnum, SortEnum, UserBanRecordVO } from '@/types'

/** 筛选值一律用 All<T>，'' 表示「全部」；发请求前统一走 orUndefined()，见 @/utils/query。 */
const filters = reactive({
  username: '',
  phone: '',
  email: '',
  /** 布尔三态直接用真布尔承载：'' 不传 / false 封禁中 / true 已解封 */
  unbanned: '' as All<boolean>,
  deleted: '' as All<boolean>,
  sort: '' as All<SortEnum>,
})

const rows = ref<UserBanRecordVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await pageBanRecords({
      currentPage: currentPage.value,
      username: orUndefined(filters.username),
      phone: orUndefined(filters.phone),
      email: orUndefined(filters.email),
      unbanned: orUndefined(filters.unbanned),
      deleted: orUndefined(filters.deleted),
      sort: orUndefined(filters.sort),
    })
    rows.value = page.records
    total.value = page.total
  } catch {
  } finally {
    loading.value = false
  }
}

function search() {
  currentPage.value = 1
  load()
}

function resetFilters() {
  Object.assign(filters, {
    username: '',
    phone: '',
    email: '',
    unbanned: '',
    deleted: '',
    sort: '',
  })
  search()
}

onMounted(load)

async function onUnban(row: UserBanRecordVO) {
  // confirm 单独一个 try：点「取消」时它 reject，不能和接口错误混在一个 catch 里
  try {
    await ElMessageBox.confirm(
      `确定解封「${row.username}」的${roleLabel[row.role]}账号吗？解封后他可以重新登录。`,
      '解封账号',
      { type: 'warning', confirmButtonText: '确认解封', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    // 解封和封禁一样要同时发 userId + role：针对的是这一个角色账户
    await unbanUser({ userId: row.userId, role: row.role })
    ElMessage.success('已解封')
    load()
  } catch {
    // 列表数据过期时后端会返回 2009 账号未被封禁，拦截器已提示
  }
}
</script>

<template>
  <div>
    <el-card shadow="never" class="page-card">
      <el-form inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="filters.username"
            placeholder="模糊搜索"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="filters.phone" placeholder="模糊搜索" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="filters.email" placeholder="模糊搜索" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="封禁状态">
          <!-- 三态查询参数是 Boolean：'' 不传 / false 封禁中 / true 已解封。布尔值必须用 :value 绑定。 -->
          <el-select v-model="filters.unbanned" placeholder="全部" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="封禁中" :value="false" />
            <el-option label="已解封" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="删除状态">
          <el-select v-model="filters.deleted" placeholder="全部" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="未删除" :value="false" />
            <el-option label="已删除" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filters.sort" placeholder="默认" style="width: 150px">
            <el-option label="默认" value="" />
            <el-option
              v-for="o in allSortOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="page-card">
      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="(row: UserBanRecordVO) => row.userBanRecordId"
        border
        stripe
      >
        <el-table-column label="记录ID" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.userBanRecordId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="userId" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.userId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />

        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType[row.role as RoleEnum]" effect="plain" size="small">
              {{ roleLabel[row.role as RoleEnum] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ genderLabel[row.gender as GenderEnum] }}</template>
        </el-table-column>

        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            <span class="mono">{{ row.phone || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>

        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.unbanned ? 'success' : 'danger'" size="small">
              {{ row.unbanned ? '已解封' : '封禁中' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="reason" label="封禁原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>

        <el-table-column label="封禁时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.unbanned"
              @click="onUnban(row as UserBanRecordVO)"
            >
              解封
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        :page-size="PAGE_SIZE"
        :total="total"
        layout="total, prev, pager, next, jumper"
        background
        class="pager"
        @current-change="load"
      />
    </el-card>
  </div>
</template>
