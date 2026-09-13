<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Picture, Search } from '@element-plus/icons-vue'

import { auditUser, pageAuditRecords } from '@/api/admin'
import {
  allSortOptions,
  genderLabel,
  PAGE_SIZE,
  auditStatusLabel,
  auditStatusOptions,
  auditStatusTagType,
} from '@/constants'
import { formatDateTime } from '@/utils/date'
import { imageUrl } from '@/utils/image'
import { orUndefined, type All } from '@/utils/query'
import type { GenderEnum, SortEnum, AuditStatusEnum, UserAuditRecordVO } from '@/types'

/** 筛选值一律用 All<T>，'' 表示「全部」；发请求前统一走 orUndefined()，见 @/utils/query。 */
const filters = reactive({
  username: '',
  phone: '',
  email: '',
  /** 这个查询 DTO 里的字段叫 auditStatus（账号管理页那个叫 userStatus） */
  auditStatus: '' as All<AuditStatusEnum>,
  /** 布尔三态直接用真布尔承载：'' 不传 / false 未删除 / true 已删除 */
  deleted: '' as All<boolean>,
  sort: '' as All<SortEnum>,
})

const rows = ref<UserAuditRecordVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await pageAuditRecords({
      currentPage: currentPage.value,
      username: orUndefined(filters.username),
      phone: orUndefined(filters.phone),
      email: orUndefined(filters.email),
      auditStatus: orUndefined(filters.auditStatus),
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
    auditStatus: '',
    deleted: '',
    sort: '',
  })
  search()
}

onMounted(load)

// ===== 审核（通过 / 驳回共用一个对话框） =====

const dialog = reactive({
  open: false,
  submitting: false,
  row: null as UserAuditRecordVO | null,
  // 后端只接受 NORMAL(通过) 和 REJECTED(驳回)，传别的会被判为参数校验失败
  status: 'NORMAL' as 'NORMAL' | 'REJECTED',
  reason: '',
})

function openAudit(row: UserAuditRecordVO, status: 'NORMAL' | 'REJECTED') {
  Object.assign(dialog, { open: true, submitting: false, row, status, reason: '' })
}

async function submitAudit() {
  const row = dialog.row
  if (!row) return
  // 驳回原因是服务端可选的，但没有原因的驳回对申请人毫无帮助，所以前端强制要求
  if (dialog.status === 'REJECTED' && !dialog.reason.trim()) {
    ElMessage.warning('驳回必须填写原因')
    return
  }

  dialog.submitting = true
  try {
    await auditUser({
      userAuditRecordId: row.userAuditRecordId,
      status: dialog.status,
      reason: dialog.reason || undefined,
    })
    dialog.open = false
    ElMessage.success(
      dialog.status === 'NORMAL' ? '已通过审核，该配送员现在可以登录了' : '已驳回该申请',
    )
    load()
  } catch {
  } finally {
    dialog.submitting = false
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
        <el-form-item label="审核状态">
          <el-select v-model="filters.auditStatus" placeholder="全部" style="width: 130px">
            <el-option label="全部" value="" />
            <el-option
              v-for="o in auditStatusOptions"
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
      <!-- 这张表一条审核记录一行（不是「一角色一行」），所以 row-key 用记录主键就够了 -->
      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="(row: UserAuditRecordVO) => row.userAuditRecordId"
        border
        stripe
      >
        <el-table-column label="记录ID" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.userAuditRecordId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />

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

        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="auditStatusTagType[row.status as AuditStatusEnum]" size="small">
              {{ auditStatusLabel[row.status as AuditStatusEnum] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="证明材料" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.material"
              :src="imageUrl(row.material)"
              :preview-src-list="[imageUrl(row.material)]"
              preview-teleported
              fit="cover"
              lazy
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

        <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>

        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <!-- 只有「审核中」的申请才能操作；已经审完的行按钮置灰 -->
            <!-- el-table 把插槽里的 row 标成自己的 DefaultRow（松散类型），
                 传给强类型函数时要在调用点收窄一次 -->
            <el-button
              link
              type="success"
              :disabled="row.status !== 'REVIEWING'"
              @click="openAudit(row as UserAuditRecordVO, 'NORMAL')"
            >
              通过
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.status !== 'REVIEWING'"
              @click="openAudit(row as UserAuditRecordVO, 'REJECTED')"
            >
              驳回
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

    <el-dialog
      v-model="dialog.open"
      :title="dialog.status === 'NORMAL' ? '通过审核' : '驳回申请'"
      width="460px"
    >
      <p v-if="dialog.row" class="dialog-target">
        {{ dialog.row.username }}
        <span class="muted mono">{{ dialog.row.phone || dialog.row.email || '' }}</span>
      </p>

      <el-image
        v-if="dialog.row?.material"
        :src="imageUrl(dialog.row.material)"
        :preview-src-list="[imageUrl(dialog.row.material)]"
        preview-teleported
        fit="contain"
        class="dialog-material"
      >
        <template #error>
          <div class="thumb-fallback">材料加载失败（可能登录态已过期）</div>
        </template>
      </el-image>

      <!-- 通过时不需要原因；驳回时必填（下面 submitAudit 里拦） -->
      <el-input
        v-if="dialog.status === 'REJECTED'"
        v-model="dialog.reason"
        type="textarea"
        :rows="3"
        maxlength="100"
        show-word-limit
        placeholder="驳回原因（必填，会展示给申请人）"
      />
      <el-input
        v-else
        v-model="dialog.reason"
        type="textarea"
        :rows="2"
        maxlength="100"
        show-word-limit
        placeholder="备注（可选）"
      />

      <el-alert
        :type="dialog.status === 'NORMAL' ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="dialog-tip"
      >
        <template #title>
          {{ dialog.status === 'NORMAL' ? '通过后该配送员即可登录' : '驳回后该账号无法登录' }}
        </template>
        {{
          dialog.status === 'NORMAL'
            ? '审核结果不可撤销，通过后申请人会以「配送员」身份正常使用系统。'
            : '申请人登录时会看到「账号申请被驳回，请联系管理员」，并可以看到你填写的原因。'
        }}
      </el-alert>

      <template #footer>
        <el-button @click="dialog.open = false">取消</el-button>
        <el-button
          :type="dialog.status === 'NORMAL' ? 'success' : 'danger'"
          :loading="dialog.submitting"
          @click="submitAudit"
        >
          {{ dialog.status === 'NORMAL' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* .thumb / .thumb-fallback / .dialog-target / .dialog-tip 的共性在 assets/main.css，
  这里只留审核页自己的差异与材料预览 */
.thumb-fallback {
  font-size: 12px;
}

.dialog-target {
  /* 用户名后面跟的是联系方式文本，基线对齐比居中更自然 */
  align-items: baseline;
}

.dialog-material {
  width: 100%;
  height: 180px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
