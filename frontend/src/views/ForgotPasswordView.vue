<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'

import { ApiError } from '@/api/request'
import { checkVerifyCode, resetPassword } from '@/api/user'
import { useVerifyCode } from '@/composables/useVerifyCode'
import { accountRule, codeRules, isAccount, PASSWORD_MAX, PASSWORD_MIN } from '@/utils/patterns'

const router = useRouter()

function returnToLogin() {
  router.push({ name: 'home' })
}

const step = ref(0)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const checking = ref(false)

const form = reactive({
  account: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules<typeof form> = {
  account: [accountRule],
  code: codeRules,
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      min: PASSWORD_MIN,
      max: PASSWORD_MAX,
      message: `密码长度必须在 ${PASSWORD_MIN}-${PASSWORD_MAX} 之间`,
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const { mockCode, sending, disabled, buttonText, send, reset } = useVerifyCode(
  'FORGOT_PASSWORD',
  () => form.account,
  () => (isAccount(form.account) ? null : '请先填写正确的邮箱或手机号'),
)

watch(
  () => form.account,
  () => {
    form.code = ''
    step.value = 0
    reset()
  },
)

async function copyCode() {
  try {
    await navigator.clipboard.writeText(mockCode.value)
    ElMessage.success('验证码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动输入')
  }
}

async function nextStep() {
  const ok = await formRef.value?.validateField(['account', 'code']).catch(() => false)
  if (ok === false) return
  checking.value = true
  try {
    await checkVerifyCode({
      account: form.account,
      scene: 'FORGOT_PASSWORD',
      code: form.code,
    })
    step.value = 1
  } catch {
    // 后端会区分验证码错误与验证码过期，统一拦截器已经提示。
  } finally {
    checking.value = false
  }
}

async function onSubmit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    await resetPassword({
      account: form.account,
      code: form.code,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码已重置，请用新密码登录')
    returnToLogin()
  } catch (e) {
    // 2011 验证码错误 → 留在原步骤让用户重填；
    // 2012 验证码已过期或失效 → 这个码已经废了，退回第一步重新获取
    if (e instanceof ApiError && e.code === 2012) {
      reset()
      form.code = ''
      step.value = 0
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="auth-card auth-card--backdrop" style="max-width: 520px">
    <template #header>
      <span class="title">忘记密码</span>
    </template>

    <el-steps :active="step" align-center finish-status="success" class="steps">
      <el-step title="获取验证码" />
      <el-step title="重置密码" />
    </el-steps>

    <el-alert
      v-if="mockCode && step === 0"
      type="success"
      :closable="false"
      show-icon
      class="mock-code"
    >
      <template #title>模拟短信 / 邮件（忘记密码）</template>
      <p class="code-line">
        您的验证码是 <b class="code">{{ mockCode }}</b>
        <el-button link type="primary" size="small" @click="copyCode">复制</el-button>
      </p>
      <p class="sub">5 分钟内有效，只能使用一次，且不能用于换绑手机 / 邮箱。</p>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
      <template v-if="step === 0">
        <el-form-item label="注册时使用的邮箱或手机号" prop="account">
          <el-input v-model="form.account" placeholder="邮箱或手机号" clearable />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model.trim="form.code" maxlength="6" placeholder="6 位数字验证码" />
            <el-button
              type="primary"
              class="code-button"
              :loading="sending"
              :disabled="disabled"
              @click="send"
            >
              {{ buttonText }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit" :loading="checking" @click="nextStep">
            验证并进入下一步
          </el-button>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="账号">
          <el-input :model-value="form.account" disabled />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="6-20 位密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再输入一次新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit" :loading="submitting" @click="onSubmit">
            重置密码
          </el-button>
          <el-button link class="back" @click="step = 0">返回上一步</el-button>
        </el-form-item>
      </template>
    </el-form>

    <div class="links">
      <el-link type="primary" underline="never" @click="returnToLogin"> 返回登录 </el-link>
    </div>
  </el-card>
</template>

<style scoped>
.title {
  font-size: 17px;
  font-weight: 600;
}

.steps {
  margin-bottom: 24px;
}

.mock-code {
  margin-bottom: 20px;
  font-size: 13px;
}

.mock-code p {
  margin: 0;
}

.code-line {
  display: flex;
  align-items: center;
  gap: 4px;
}

.code {
  font-size: 18px;
  letter-spacing: 2px;
}

.mock-code .sub {
  margin-top: 4px;
  color: #67c23a;
  opacity: 0.85;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.submit {
  width: 100%;
}

.back {
  width: 100%;
  margin: 8px 0 0;
}

.links {
  margin-top: 8px;
  font-size: 13px;
  text-align: center;
}
</style>
