<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { createOrder, type CreateOrder } from '@/api/order'
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<CreateOrder>({
  pickupAddress: '',
  pickupName: '',
  pickupPhone: '',
  deliveryAddress: '',
  deliveryName: '',
  deliveryPhone: '',
  itemDescription: '',
  remark: '',
  fee: 5,
})
const rules: FormRules = {}
for (const key of [
  'pickupAddress',
  'pickupName',
  'deliveryAddress',
  'deliveryName',
  'itemDescription',
]) {
  rules[key] = [{ required: true, whitespace: true, message: '请填写此项', trigger: 'blur' }]
}
for (const key of ['pickupPhone', 'deliveryPhone']) {
  rules[key] = [
    { required: true, pattern: /^1[3-9]\d{9}$/, message: '请填写正确的手机号', trigger: 'blur' },
  ]
}
rules.fee = [
  {
    required: true,
    type: 'number',
    min: 0.01,
    max: 9999.99,
    message: '配送费须为0.01～9999.99元',
    trigger: 'change',
  },
]
async function submit() {
  if (submitting.value || !(await formRef.value?.validate().catch(() => false))) return
  submitting.value = true
  try {
    const id = await createOrder({ ...form, deliveryPhone: form.deliveryPhone.trim() })
    ElMessage.success('订单已发布，请完成模拟支付')
    await router.push({ name: 'order-detail', params: { id } })
  } catch {
    /* 统一接口提示 */
  } finally {
    submitting.value = false
  }
}
</script>
<template>
  <el-card class="create-order" shadow="never">
    <template #header><b>发布配送订单</b></template>
    <el-alert
      title="发布后需完成模拟支付，配送员才可接单。本项目不发生真实扣款。"
      type="info"
      :closable="false"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <h3>取件信息</h3>
          <el-form-item label="取件地址" prop="pickupAddress"
            ><el-input
              v-model="form.pickupAddress"
              maxlength="255"
              placeholder="例如：校园快递站A区"
          /></el-form-item>
          <el-form-item label="取件联系人" prop="pickupName"
            ><el-input v-model="form.pickupName" maxlength="50"
          /></el-form-item>
          <el-form-item label="取件联系电话" prop="pickupPhone"
            ><el-input v-model="form.pickupPhone" maxlength="11"
          /></el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <h3>送达信息</h3>
          <el-form-item label="送达地址" prop="deliveryAddress"
            ><el-input
              v-model="form.deliveryAddress"
              maxlength="255"
              placeholder="例如：学生宿舍3栋一楼"
          /></el-form-item>
          <el-form-item label="收件联系人" prop="deliveryName"
            ><el-input v-model="form.deliveryName" maxlength="50"
          /></el-form-item>
          <el-form-item label="收件联系电话" prop="deliveryPhone"
            ><el-input v-model="form.deliveryPhone" maxlength="11"
          /></el-form-item>
          <p class="muted">收件人无需注册也可下单；绑定该手机号的收寄件人可查看订单并确认取件。</p>
        </el-col>
      </el-row>
      <el-form-item label="物品说明" prop="itemDescription"
        ><el-input
          v-model="form.itemDescription"
          maxlength="255"
          show-word-limit
          placeholder="请说明物品种类、数量及大小"
      /></el-form-item>
      <el-form-item label="备注"
        ><el-input v-model="form.remark" type="textarea" maxlength="255" show-word-limit
      /></el-form-item>
      <el-form-item label="配送费（元）" prop="fee"
        ><el-input-number v-model="form.fee" :min="0.01" :max="9999.99" :precision="2" :step="1"
      /></el-form-item>
      <el-button type="primary" :loading="submitting" @click="submit">发布订单</el-button>
      <el-button @click="router.push({ name: 'orders-mine' })">返回我的订单</el-button>
    </el-form>
  </el-card>
</template>
<style scoped>
.create-order {
  max-width: 900px;
  margin: auto;
}
.form {
  margin-top: 20px;
}
h3 {
  font-size: 16px;
}
</style>
