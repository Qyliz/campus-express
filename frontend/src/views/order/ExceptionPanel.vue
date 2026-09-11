<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  exceptionTypeLabels, exceptionTypeOptions, exceptionStatusLabels, exceptionStatusTagType,
  exceptionResolutionLabels, reportException, resolveException,
  type DeliveryException, type ExceptionResolutionEnum, type ExceptionStatusEnum, type ExceptionTypeEnum,
} from '@/api/order'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ orderId: string; exceptions: DeliveryException[]; canReport: boolean; isAdmin: boolean }>()
const emit = defineEmits<{ refresh: [] }>()
const visible = ref(false)
const submitting = ref(false)
const target = ref<string>()
const form = reactive({
  type: 'CONTACT' as ExceptionTypeEnum,
  resolution: 'RESUME' as ExceptionResolutionEnum,
  description: '',
})
function open(id?: string) {
  target.value = id
  form.type = 'CONTACT'
  form.resolution = 'RESUME'
  form.description = ''
  visible.value = true
}
async function submit() {
  if (submitting.value) return
  const description = form.description.trim()
  if (!description || description.length > 255) {
    ElMessage.warning('请填写255字以内的说明')
    return
  }
  submitting.value = true
  try {
    if (target.value) await resolveException(target.value, { resolution: form.resolution, description })
    else await reportException(props.orderId, { type: form.type, description })
    ElMessage.success(target.value ? '异常处理成功' : '异常已上报，配送暂停')
    visible.value = false
  } catch {
    visible.value = false
  } finally {
    submitting.value = false
    emit('refresh')
  }
}
</script>
<template>
  <section>
    <h3>配送异常</h3>
    <el-button v-if="canReport" type="warning" @click="open()">上报异常</el-button>
    <el-alert v-if="exceptions.some(e => e.status === 'PENDING')" title="异常待处理，配送已暂停。管理员处理后可继续配送或取消订单。" type="warning" :closable="false" class="notice" />
    <p v-if="!exceptions.length" class="muted">暂无异常记录</p>
    <el-timeline v-else class="history">
      <el-timeline-item v-for="item in exceptions" :key="item.id" :timestamp="formatDateTime(item.createTime)" placement="top" :type="exceptionStatusTagType[item.status as ExceptionStatusEnum]">
        <el-space wrap><b>{{ exceptionTypeLabels[item.type as ExceptionTypeEnum] }}</b><el-tag :type="exceptionStatusTagType[item.status as ExceptionStatusEnum]">{{ exceptionStatusLabels[item.status as ExceptionStatusEnum] }}</el-tag>
          <el-button v-if="isAdmin && item.status === 'PENDING'" type="primary" size="small" @click="open(item.id)">处理异常</el-button>
        </el-space>
        <p class="description">上报说明：{{ item.description }}</p>
        <template v-if="item.status === 'RESOLVED'">
          <p>处理结果：{{ item.resolution ? exceptionResolutionLabels[item.resolution] : '—' }}</p>
          <p class="description">处理说明：{{ item.resolutionDescription }}</p>
          <p class="muted">处理时间：{{ item.resolvedTime ? formatDateTime(item.resolvedTime) : '—' }}</p>
        </template>
      </el-timeline-item>
    </el-timeline>
    <el-dialog v-model="visible" :title="target ? '处理异常' : '上报异常'" width="min(520px, 94vw)" :close-on-click-modal="!submitting" :close-on-press-escape="!submitting" :show-close="!submitting">
      <el-form label-position="top">
        <el-form-item v-if="!target" label="异常类型" required><el-select v-model="form.type"><el-option v-for="o in exceptionTypeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item v-else label="处理结果" required><el-radio-group v-model="form.resolution"><el-radio value="RESUME">恢复配送</el-radio><el-radio value="CANCEL">取消订单</el-radio></el-radio-group></el-form-item>
        <el-alert v-if="target && form.resolution === 'CANCEL'" title="提交后订单将取消，已支付订单同步模拟退款。" type="warning" :closable="false" />
        <el-form-item :label="target ? '处理说明' : '异常说明'" required><el-input v-model="form.description" type="textarea" :rows="4" maxlength="255" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="submitting" @click="visible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">提交</el-button></template>
    </el-dialog>
  </section>
</template>
<style scoped>
.notice, .history { margin-top: 20px; }
.description { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
