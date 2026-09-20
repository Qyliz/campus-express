<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { Picture, Search } from '@element-plus/icons-vue'

import { auditUser, pageAuditRecords } from '@/api/admin'
import { usePagedList } from '@/composables/usePagedList'
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

//空串表示不限制该筛选项
const filters = reactive({
  username: '',
  phone: '',
  email: '',
  //审核查询使用 auditStatus
  auditStatus: '' as All<AuditStatusEnum>,
  //空串表示全部，布尔值表示删除状态
  deleted: '' as All<boolean>,
  sort: '' as All<SortEnum>,
})

const {
  rows,
  total,
  page: currentPage,
  loading,
  error,
  load,
  search,
} = usePagedList<UserAuditRecordVO>((currentPage) =>
  pageAuditRecords({
    currentPage,
    username: orUndefined(filters.username),
    phone: orUndefined(filters.phone),
    email: orUndefined(filters.email),
    auditStatus: orUndefined(filters.auditStatus),
    deleted: orUndefined(filters.deleted),
    sort: orUndefined(filters.sort),
  }),
)

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

//审核

const dialog = reactive({
  open: false,
  submitting: false,
  row: null as UserAuditRecordVO | null,
  //审核结果只接受通过或驳回
  status: 'NORMAL' as 'NORMAL' | 'REJECTED',
  reason: '',
})

function openAudit(row: UserAuditRecordVO, status: 'NORMAL' | 'REJECTED') {
  Object.assign(dialog, { open: true, submitting: false, row, status, reason: '' })
}

async function submitAudit() {
  const row = dialog.row
  if (!row) return
  //前端要求驳回时填写原因
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
      <el-form inline class="filter-form" @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="filters.username"
            placeholder="模糊搜索"
            clearable
            class="filter-control"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="filters.phone"
            placeholder="模糊搜索"
            clearable
            class="filter-control"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="filters.email"
            placeholder="模糊搜索"
            clearable
            class="filter-control"
          />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="filters.auditStatus" placeholder="全部" class="filter-control">
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
          <!-- 布尔值使用 :value，空串表示全部 -->
          <el-select v-model="filters.deleted" placeholder="全部" class="filter-control">
            <el-option label="全部" value="" />
            <el-option label="未删除" :value="false" />
            <el-option label="已删除" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filters.sort" placeholder="默认" class="filter-control">
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
      <el-alert v-if="error" title="审核列表加载失败，请刷新重试" type="error" :closable="false" />
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
            <!-- 只有审核中的申请可以处理 -->
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
        :pager-count="5"
        layout="prev, pager, next"
        background
        class="pager"
        @current-change="load"
      />
    </el-card>

    <el-dialog
      v-model="dialog.open"
      :title="dialog.status === 'NORMAL' ? '通过审核' : '驳回申请'"
      width="min(460px, calc(100vw - 24px))"
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

      <!-- 驳回时必须填写原因 -->
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
/* 审核页差异样式，公共弹窗样式见 main.css */
.thumb-fallback {
  font-size: 12px;
}

.dialog-target {
  /* 联系方式按文字基线对齐 */
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
