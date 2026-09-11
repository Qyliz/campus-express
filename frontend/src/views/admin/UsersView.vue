<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Picture, Search } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import { adminResetPassword, banUser, kickoutUser, pageUsers } from '@/api/admin'
import {
  createTimeSortOptions,
  genderLabel,
  PAGE_SIZE,
  roleLabel,
  roleTagType,
  statusLabel,
  statusOptions,
  statusTagType,
} from '@/constants'
import { formatDateTime } from '@/utils/date'
import { imageUrl } from '@/utils/image'
import { orUndefined, type All } from '@/utils/query'
import { PASSWORD_MAX, PASSWORD_MIN } from '@/utils/patterns'
import type { RoleEnum, SortEnum, StatusEnum, UserProfileAdminVO } from '@/types'

/** 筛选值一律用 All<T>，'' 表示「全部」；发请求前统一走 orUndefined()，见 @/utils/query。 */
const filters = reactive({
  username: '',
  phone: '',
  email: '',
  /** 注意字段名是 userStatus —— 审核页那个查询 DTO 里叫 auditStatus */
  userStatus: '' as All<StatusEnum>,
  /** 布尔三态直接用真布尔承载：'' 不传 / false 未删除 / true 已删除 */
  deleted: '' as All<boolean>,
  /** 账号列表产品上只允许按创建时间排序，所以选项用 createTimeSortOptions 而不是全部四种 */
  sort: '' as All<SortEnum>,
})

const rows = ref<UserProfileAdminVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await pageUsers({
      currentPage: currentPage.value,
      username: orUndefined(filters.username),
      phone: orUndefined(filters.phone),
      email: orUndefined(filters.email),
      userStatus: orUndefined(filters.userStatus),
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
    userStatus: '',
    deleted: '',
    sort: '',
  })
  search()
}

onMounted(load)

// ===== 封禁 =====

const banDialog = reactive({
  open: false,
  submitting: false,
  reason: '',
  row: null as UserProfileAdminVO | null,
})

function openBan(row: UserProfileAdminVO) {
  Object.assign(banDialog, { open: true, submitting: false, reason: '', row })
}

async function submitBan() {
  const row = banDialog.row
  if (!row) return
  banDialog.submitting = true
  try {
    // 封禁要同时发 userId + role：一个人可以有多个角色，封的是这一个角色账户，
    // 同一个人的其他角色不受影响。（对比：踢人和重置密码只发 userId。）
    await banUser({ userId: row.userId, role: row.role, reason: banDialog.reason || undefined })
    banDialog.open = false
    ElMessage.success('已封禁，该用户已被强制下线')
    load()
  } catch {
  } finally {
    banDialog.submitting = false
  }
}

// ===== 强制下线 =====

async function onKickout(row: UserProfileAdminVO) {
  // confirm 单独一个 try：点「取消」时它 reject，不能和接口错误混在一个 catch 里
  try {
    await ElMessageBox.confirm(
      `确定把「${row.username}」强制下线吗？该用户的所有在线会话都会立即失效。`,
      '强制下线',
      { type: 'warning', confirmButtonText: '强制下线', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await kickoutUser({ userId: row.userId })
    ElMessage.success('已强制下线')
  } catch {}
}

// ===== 重置密码 =====

const resetFormRef = ref<FormInstance>()
const resetDialog = reactive({
  open: false,
  submitting: false,
  newPassword: '',
  row: null as UserProfileAdminVO | null,
})

function openReset(row: UserProfileAdminVO) {
  Object.assign(resetDialog, { open: true, submitting: false, newPassword: '', row })
}

async function submitReset() {
  const ok = await resetFormRef.value?.validate().catch(() => false)
  if (!ok) return
  const row = resetDialog.row
  if (!row) return

  resetDialog.submitting = true
  try {
    await adminResetPassword({ userId: row.userId, newPassword: resetDialog.newPassword })
    resetDialog.open = false
    ElMessage.success('密码已重置')
  } catch {
  } finally {
    resetDialog.submitting = false
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
        <el-form-item label="状态">
          <el-select v-model="filters.userStatus" placeholder="全部" style="width: 130px">
            <el-option label="全部" value="" />
            <el-option
              v-for="o in statusOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="删除状态">
          <!-- 三态：'' 不传 / false 未删除 / true 已删除。布尔值必须用 :value 绑定，否则会变成字符串。 -->
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
              v-for="o in createTimeSortOptions"
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
      <!-- 一个人拥有多个角色时，列表里会出现多行相同的 userId，
           所以 row-key 必须用 userId + role 组合，否则行复用/选中会串行 -->
      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="(row: UserProfileAdminVO) => `${row.userId}-${row.role}`"
        border
        stripe
      >
        <el-table-column label="userId" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.userId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="头像" width="70" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.avatar"
              :src="imageUrl(row.avatar)"
              :preview-src-list="[imageUrl(row.avatar)]"
              preview-teleported
              fit="cover"
              class="thumb"
            >
              <template #error>
                <div class="thumb-fallback">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <span v-else class="muted">—</span>
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
          <template #default="{ row }">{{
            genderLabel[row.gender as keyof typeof genderLabel]
          }}</template>
        </el-table-column>

        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            <span class="mono">{{ row.phone || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status as StatusEnum]" size="small">
              {{ statusLabel[row.status as StatusEnum] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <!-- el-table 把插槽里的 row 标成自己的 DefaultRow（松散类型），
                 传给强类型函数时要在调用点收窄一次 -->
            <el-button
              link
              type="danger"
              :disabled="row.status === 'DISABLED'"
              @click="openBan(row as UserProfileAdminVO)"
            >
              封禁
            </el-button>
            <el-button link type="warning" @click="onKickout(row as UserProfileAdminVO)">
              强制下线
            </el-button>
            <el-button link type="primary" @click="openReset(row as UserProfileAdminVO)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 服务端把每页条数写死为 10，所以这里不提供 size 选择器 -->
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

    <el-dialog v-model="banDialog.open" title="封禁账号" width="460px">
      <p v-if="banDialog.row" class="dialog-target">
        {{ banDialog.row.username }}
        <el-tag :type="roleTagType[banDialog.row.role]" effect="plain" size="small">
          {{ roleLabel[banDialog.row.role] }}
        </el-tag>
      </p>
      <el-input
        v-model="banDialog.reason"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="封禁原因（可选）"
      />
      <el-alert type="warning" :closable="false" show-icon class="dialog-tip">
        <template #title>只封禁这一个角色</template>
        该用户的其他角色不受影响；封禁后他会立即被强制下线，且无法再用此角色登录。
      </el-alert>
      <template #footer>
        <el-button @click="banDialog.open = false">取消</el-button>
        <el-button type="danger" :loading="banDialog.submitting" @click="submitBan">
          确认封禁
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialog.open" title="重置密码" width="440px">
      <el-form ref="resetFormRef" :model="resetDialog" label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input :model-value="resetDialog.row?.username ?? ''" disabled />
        </el-form-item>
        <el-form-item
          label="新密码"
          prop="newPassword"
          :rules="[
            { required: true, message: '请输入新密码', trigger: 'blur' },
            {
              min: PASSWORD_MIN,
              max: PASSWORD_MAX,
              message: `密码长度必须在 ${PASSWORD_MIN}-${PASSWORD_MAX} 之间`,
              trigger: 'blur',
            },
          ]"
        >
          <el-input
            v-model="resetDialog.newPassword"
            type="password"
            placeholder="6-20 位"
            show-password
          />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon class="dialog-tip">
        <template #title>会影响这个人的所有角色</template>
        密码挂在主账号上，重置后他的<b>全部角色</b>都要改用新密码登录。
      </el-alert>
      <template #footer>
        <el-button @click="resetDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="resetDialog.submitting" @click="submitReset">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.thumb-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #c0c4cc;
  background: #f5f7fa;
}

.dialog-target {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.dialog-tip {
  margin-top: 12px;
  font-size: 13px;
}
</style>
