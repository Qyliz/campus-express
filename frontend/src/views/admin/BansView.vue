<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

import { pageBanRecords, unbanUser } from '@/api/admin'
import { allSortOptions, genderLabel, PAGE_SIZE, roleLabel, roleTagType } from '@/constants'
import { formatDateTime } from '@/utils/date'
import type { GenderEnum, RoleEnum, SortEnum, UserBanRecordVO } from '@/types'

/**
 * 本项目最容易踩的一个坑：同一个词 unbanned 在两边类型不同。
 *   查询参数 UserBanQueryDTO.unbanned : Boolean | null → 用 true / false / undefined
 *   表格数据 UserBanRecordVO.unbanned : Integer        → 是 0(封禁中) / 1(已解封)
 * 所以渲染侧绝不能写 row.unbanned === true（恒为 false，所有行都会显示「封禁中」）。
 * Integer → 布尔语义的转换只集中在下面这一个函数里。
 */
const isUnbanned = (row: UserBanRecordVO) => row.unbanned === 1

/**
 * 三态/枚举筛选一律用字符串承载，'' 表示「全部」。
 * 原因有两层：el-option 的 value 属性在类型上不接受 undefined / null；
 * 而接口真正需要的是「这个参数干脆不传」，所以在发请求那一刻才转换。
 */
const filters = reactive({
  username: '',
  phone: '',
  email: '',
  unbanned: '' as '' | 'false' | 'true',
  deleted: '' as '' | 'false' | 'true',
  sort: '' as SortEnum | '',
})

/** '' → undefined（axios 会省略值为 undefined 的查询参数），'false'/'true' → boolean */
function toBool(v: '' | 'false' | 'true'): boolean | undefined {
  return v === '' ? undefined : v === 'true'
}

const rows = ref<UserBanRecordVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await pageBanRecords({
      currentPage: currentPage.value,
      // 空串要转成 undefined：axios 会省略 undefined 参数，
      // 但会把 '' 原样发过去，变成无意义的 like '%%'
      username: filters.username || undefined,
      phone: filters.phone || undefined,
      email: filters.email || undefined,
      unbanned: toBool(filters.unbanned),
      deleted: toBool(filters.deleted),
      sort: filters.sort || undefined,
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
          <!-- 三态查询参数是 Boolean：'' 不传 / 'false' 封禁中 / 'true' 已解封 -->
          <el-select v-model="filters.unbanned" placeholder="全部" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="封禁中" value="false" />
            <el-option label="已解封" value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="删除状态">
          <el-select v-model="filters.deleted" placeholder="全部" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="未删除" value="false" />
            <el-option label="已删除" value="true" />
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
            <!-- el-table 把插槽里的 row 标成自己的 DefaultRow（松散类型），
                 传给强类型的辅助函数时要在调用点收窄一次 -->
            <el-tag :type="isUnbanned(row as UserBanRecordVO) ? 'success' : 'danger'" size="small">
              {{ isUnbanned(row as UserBanRecordVO) ? '已解封' : '封禁中' }}
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
              :disabled="isUnbanned(row as UserBanRecordVO)"
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
