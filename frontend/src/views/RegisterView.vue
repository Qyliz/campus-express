<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus'

import { register } from '@/api/user'
import { genderOptions, roleOptions } from '@/constants'
import { ACCEPT_ATTR, validateImageFile } from '@/utils/image'
import { PASSWORD_MAX, PASSWORD_MIN, PHONE_RE, usernameRules } from '@/utils/patterns'
import type { GenderEnum, RoleEnum } from '@/types'

const router = useRouter()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const fileList = ref<UploadUserFile[]>([])
const material = ref<File | null>(null)

const form = reactive({
  role: 'CUSTOMER' as RoleEnum,
  username: '',
  password: '',
  confirmPassword: '',
  gender: 'UNKNOWN' as GenderEnum,
  phone: '',
  email: '',
})

const rules: FormRules<typeof form> = {
  role: [{ required: true, message: '请选择注册身份', trigger: 'change' }],
  username: usernameRules,
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      min: PASSWORD_MIN,
      max: PASSWORD_MAX,
      message: `密码长度必须在 ${PASSWORD_MIN}-${PASSWORD_MAX} 之间`,
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur',
    },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  // 手机号与邮箱都是可选的：只写 pattern / type 而不写 required，空值才能通过校验
  phone: [{ pattern: PHONE_RE, message: '手机号格式不正确', trigger: 'blur' }],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 254, message: '邮箱长度不能超过 254 个字符', trigger: 'blur' },
  ],
}

// 切换身份时清掉已选的材料：从配送员切走后，这个文件不应该再被提交
watch(
  () => form.role,
  (role) => {
    if (role !== 'COURIER') {
      fileList.value = []
      material.value = null
    }
  },
)

/**
 * 上传前预校验类型和大小，省掉一次必然失败(3002/3003)的往返。
 * 材料没有走 el-form 的 rules：async-validator 不理解 File 对象，
 * 硬塞进 form model 需要一个假字段再手动 validateField，不如在提交时判三行来得直白。
 */
function onMaterialChange(file: UploadFile) {
  const problem = validateImageFile(file.raw)
  if (problem) {
    ElMessage.error(problem)
    fileList.value = []
    material.value = null
    return
  }
  material.value = file.raw ?? null
}

function onMaterialRemove() {
  material.value = null
}

function onExceed() {
  ElMessage.warning('只能上传一张材料，请先删除已选的文件')
}

async function onSubmit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  if (form.role === 'COURIER' && !material.value) {
    ElMessage.error('配送员注册必须上传身份证明材料')
    return
  }

  submitting.value = true
  try {
    await register(
      {
        username: form.username,
        password: form.password,
        role: form.role,
        gender: form.gender,
        // 空串要转成 undefined，否则 FormData 会把 "" 发过去，后端的 @Pattern/@Email 会判失败
        phone: form.phone || undefined,
        email: form.email || undefined,
      },
      // material 是 File | null，而 register 收 File | undefined；
      // 上面已经拦过「配送员必须有材料」，这里只是把 null 收敛成 undefined
      form.role === 'COURIER' ? (material.value ?? undefined) : undefined,
    )
    ElMessage.success(
      form.role === 'COURIER'
        ? '注册成功！配送员账号需管理员审核通过后才能登录'
        : '注册成功，请登录',
    )
    router.push({ name: 'login' })
  } catch {
    // 拦截器已提示：2003 该手机号已被注册 / 2004 该邮箱已被注册 /
    // 2013 该角色已注册，请直接登录 / 3001-3004 文件相关
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="auth-card" style="max-width: 560px">
    <template #header>
      <span class="title">注册</span>
    </template>

    <el-alert type="info" :closable="false" show-icon class="append-tip">
      <template #title>已经有账号了？</template>
      <p>
        用<b>相同的邮箱 / 手机号 + 相同的密码</b
        >再注册一次，就能给这个账号追加一个新角色。此时下面填写的用户名和性别会被忽略，系统沿用已有资料。
      </p>
      <p class="sub">注册不需要验证码。同一时刻只能有一个在线会话，追加角色前请先登出。</p>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
      <el-form-item label="注册身份" prop="role">
        <el-radio-group v-model="form.role">
          <el-radio-button v-for="o in roleOptions" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          maxlength="10"
          placeholder="1-10 位中文、字母、数字、下划线或短横线"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="6-20 位密码" show-password />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="再输入一次密码"
          show-password
        />
      </el-form-item>

      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio v-for="o in genderOptions" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="手机号（选填）" prop="phone">
        <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
      </el-form-item>

      <el-form-item label="邮箱（选填）" prop="email">
        <el-input v-model="form.email" maxlength="254" placeholder="用于登录和找回密码" />
      </el-form-item>

      <p class="field-hint muted">
        手机号和邮箱至少填一个，否则将无法登录。如果这里提示「已被注册」而你确实注册过，请检查密码是否与原来一致
        —— 追加角色要求密码完全相同。
      </p>

      <el-form-item v-if="form.role === 'COURIER'" label="身份证明材料" required>
        <el-upload
          v-model:file-list="fileList"
          drag
          :auto-upload="false"
          :limit="1"
          :accept="ACCEPT_ATTR"
          list-type="picture"
          :on-change="onMaterialChange"
          :on-remove="onMaterialRemove"
          :on-exceed="onExceed"
          class="uploader"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">把材料拖到这里，或<em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 jpg / png / webp，不超过 5MB，只能上传一张</div>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" class="submit" :loading="submitting" @click="onSubmit">
          注册
        </el-button>
      </el-form-item>
    </el-form>

    <div class="links">
      <el-link type="primary" :underline="false" @click="router.push({ name: 'login' })">
        已有账号？去登录
      </el-link>
    </div>
  </el-card>
</template>

<style scoped>
.title {
  font-size: 17px;
  font-weight: 600;
}

.append-tip {
  margin-bottom: 20px;
  font-size: 13px;
  line-height: 1.7;
}

.append-tip p {
  margin: 0;
}

.append-tip .sub {
  margin-top: 4px;
  color: #909399;
}

.field-hint {
  margin: -8px 0 18px;
  font-size: 12px;
  line-height: 1.7;
}

.uploader {
  width: 100%;
}

.submit {
  width: 100%;
}

.links {
  margin-top: 8px;
  font-size: 13px;
  text-align: center;
}
</style>
