<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'

import { ApiError } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { roleOptions } from '@/constants'
import { accountRule, passwordRules } from '@/utils/patterns'
import type { RoleEnum } from '@/types'

const auth = useAuthStore()
const router = useRouter()

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })

const formRef = ref<FormInstance>()
const submitting = ref(false)
/** 审核中 / 审核驳回时额外显示的常驻提示，toast 一闪而过看不清 */
const reviewTip = ref('')

const form = reactive({
  role: 'CUSTOMER' as RoleEnum,
  account: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  role: [{ required: true, message: '请选择登录身份', trigger: 'change' }],
  account: [accountRule],
  password: passwordRules,
}

async function onSubmit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return

  reviewTip.value = ''
  submitting.value = true
  try {
    await auth.login({ role: form.role, account: form.account, password: form.password })
    ElMessage.success('登录成功')
    router.replace(auth.isAdmin ? { name: 'admin-dashboard' } : { name: 'home' })
  } catch (e) {
    // 拦截器已经弹过 toast 了，这里只针对审核相关的两个码补一条常驻说明
    if (e instanceof ApiError && e.code === 2005) {
      reviewTip.value = '该配送员账号还在审核中，需要管理员审核通过后才能登录。'
    } else if (e instanceof ApiError && e.code === 2006) {
      reviewTip.value = e.message
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card
    shadow="never"
    class="auth-card auth-card--backdrop"
    :class="{ 'auth-card--embedded': embedded }"
    style="max-width: 420px"
  >
    <template #header>
      <span class="title">登录</span>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
      <el-form-item label="邮箱或手机号" prop="account">
        <el-input v-model="form.account" placeholder="注册时使用的邮箱或手机号" clearable />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="6-20 位密码"
          show-password
          @keyup.enter="onSubmit"
        />
      </el-form-item>

      <!-- radio 组没有可标注的输入控件：置空 for 让 label 按 div 渲染，避免 Chrome 的 label[for] 无效引用警告 -->
      <el-form-item for="" label="登录身份" prop="role">
        <el-radio-group v-model="form.role">
          <el-radio-button v-for="o in roleOptions" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="submit"
          :loading="submitting || auth.loading"
          @click="onSubmit"
        >
          登录
        </el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="reviewTip" type="warning" :closable="false" show-icon class="tip">
      <template #title>无法登录</template>
      {{ reviewTip }}
    </el-alert>

    <div class="links">
      <el-link type="primary" underline="never" @click="router.push({ name: 'register' })">
        还没有账号？去注册
      </el-link>
      <el-link type="info" underline="never" @click="router.push({ name: 'forgot-password' })">
        忘记密码？
      </el-link>
    </div>
  </el-card>
</template>

<style scoped>
/* .title / .submit / .links 的字号在 styles/auth-theme.css 里统一，这里只留本页布局 */
.tip {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.7;
}

.tip + .tip {
  margin-top: 8px;
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}
</style>
