<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'

import {
  deleteAccount,
  updateEmail,
  updateGender,
  updatePassword,
  updatePhone,
  updateUsername,
  uploadAvatar,
} from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useVerifyCode } from '@/composables/useVerifyCode'
import { genderLabel, genderOptions, roleLabel, roleTagType } from '@/constants'
import { ACCEPT_ATTR, imageUrl, validateImageFile } from '@/utils/image'
import {
  codeRules,
  EMAIL_RE,
  PASSWORD_MAX,
  PASSWORD_MIN,
  PHONE_RE,
  usernameRules,
} from '@/utils/patterns'
import type { GenderEnum } from '@/types'

const auth = useAuthStore()
const router = useRouter()

// 路由守卫已经 await 过 auth.init()，进到这一页 profile 必然非空
const p = computed(() => auth.profile!)
const roleText = computed(() => roleLabel[p.value.role])
const initial = computed(() => p.value.username.charAt(0) || 'U')

// ===== 卡 1：基本资料 =====

const uploadingAvatar = ref(false)
const usernameFormRef = ref<FormInstance>()
const savingUsername = ref(false)
const usernameForm = reactive({ username: p.value.username })
const usernameFormRules: FormRules<typeof usernameForm> = { username: usernameRules }

const savingGender = ref(false)
const genderValue = ref<GenderEnum>(p.value.gender)

/**
 * el-upload 一律 :auto-upload="false"，由我们自己调 axios ——
 * 用 action= 会绕过 axios 实例（丢掉 Result 解包、错误提示、401 处理），
 * 而且它的 on-success 在 HTTP 200 时就触发，即使 code !== 0，被拒的上传看起来像成功。
 */
async function onAvatarChange(file: UploadFile) {
  const problem = validateImageFile(file.raw)
  if (problem) {
    ElMessage.error(problem)
    return
  }
  if (!file.raw) return

  uploadingAvatar.value = true
  try {
    // 返回的就是更新后的 UserProfileVO，直接喂给 store，不用再查一次
    auth.applyProfile(await uploadAvatar(file.raw))
    ElMessage.success('头像已更新')
  } catch {
  } finally {
    uploadingAvatar.value = false
  }
}

async function saveUsername() {
  const ok = await usernameFormRef.value?.validate().catch(() => false)
  if (!ok) return
  savingUsername.value = true
  try {
    auth.applyProfile(await updateUsername({ username: usernameForm.username }))
    ElMessage.success('用户名已更新')
  } catch {
  } finally {
    savingUsername.value = false
  }
}

async function saveGender() {
  savingGender.value = true
  try {
    auth.applyProfile(await updateGender({ gender: genderValue.value }))
    ElMessage.success('性别已更新')
  } catch {
  } finally {
    savingGender.value = false
  }
}

// ===== 卡 2：换绑手机号 / 邮箱 =====

const phoneFormRef = ref<FormInstance>()
const phoneDialog = reactive({ open: false, submitting: false, newPhone: '', code: '' })
const phoneRules: FormRules<typeof phoneDialog> = {
  newPhone: [
    { required: true, message: '请输入新手机号', trigger: 'blur' },
    { pattern: PHONE_RE, message: '手机号格式不正确', trigger: 'blur' },
  ],
  code: codeRules,
}
/**
 * 关键：验证码是发给「新手机号」的 —— 后端校验的是 verify(dto.getNewPhone(), CHANGE_PHONE, code)，
 * 所以这里的 account 取表单里的 newPhone，而不是当前已绑定的手机号。
 *
 * 解构成顶层绑定：模板只会自动展开「顶层」的 ref，
 * 如果留着 useVerifyCode() 返回的那个对象，模板里就得写 phoneCode.sending.value。
 */
const {
  mockCode: phoneMockCode,
  sending: phoneSending,
  disabled: phoneDisabled,
  buttonText: phoneButtonText,
  send: sendPhoneCode,
  reset: resetPhoneCode,
} = useVerifyCode(
  'CHANGE_PHONE',
  () => phoneDialog.newPhone,
  () => (PHONE_RE.test(phoneDialog.newPhone) ? null : '请先填写正确的新手机号'),
)

function openPhoneDialog() {
  Object.assign(phoneDialog, { open: true, submitting: false, newPhone: '', code: '' })
  resetPhoneCode()
}

async function submitPhone() {
  const ok = await phoneFormRef.value?.validate().catch(() => false)
  if (!ok) return
  phoneDialog.submitting = true
  try {
    await updatePhone({ newPhone: phoneDialog.newPhone, code: phoneDialog.code })
    phoneDialog.open = false
    ElMessage.success('手机号已换绑')
    await auth.refresh() // 该接口返回 void，必须重新拉一次资料
  } catch {
  } finally {
    phoneDialog.submitting = false
  }
}

const emailFormRef = ref<FormInstance>()
const emailDialog = reactive({ open: false, submitting: false, newEmail: '', code: '' })
const emailRules: FormRules<typeof emailDialog> = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { pattern: EMAIL_RE, message: '邮箱格式不正确', trigger: 'blur' },
    { max: 254, message: '邮箱长度不能超过 254 个字符', trigger: 'blur' },
  ],
  code: codeRules,
}
const {
  mockCode: emailMockCode,
  sending: emailSending,
  disabled: emailDisabled,
  buttonText: emailButtonText,
  send: sendEmailCode,
  reset: resetEmailCode,
} = useVerifyCode(
  'CHANGE_EMAIL',
  () => emailDialog.newEmail,
  () => (EMAIL_RE.test(emailDialog.newEmail) ? null : '请先填写正确的新邮箱'),
)

function openEmailDialog() {
  Object.assign(emailDialog, { open: true, submitting: false, newEmail: '', code: '' })
  resetEmailCode()
}

async function submitEmail() {
  const ok = await emailFormRef.value?.validate().catch(() => false)
  if (!ok) return
  emailDialog.submitting = true
  try {
    await updateEmail({ newEmail: emailDialog.newEmail, code: emailDialog.code })
    emailDialog.open = false
    ElMessage.success('邮箱已换绑')
    await auth.refresh()
  } catch {
  } finally {
    emailDialog.submitting = false
  }
}

// ===== 卡 3：修改密码 =====

const pwdFormRef = ref<FormInstance>()
const savingPwd = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules: FormRules<typeof pwdForm> = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
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
        if (value !== pwdForm.newPassword) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submitPassword() {
  const ok = await pwdFormRef.value?.validate().catch(() => false)
  if (!ok) return
  savingPwd.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    // 后端改完密码会顺手登出当前会话，所以本地状态必须清掉并回登录页
    auth.clear()
    // showClose / closeOnPressEscape 都关掉：alert 被 X 或 ESC 关掉时会 reject，
    // 那样就会跳过下面的跳转，把用户留在一个会话已死的页面上。
    await ElMessageBox.alert('密码修改成功，当前会话已失效，请重新登录。', '提示', {
      type: 'success',
      confirmButtonText: '去登录',
      showClose: false,
      closeOnPressEscape: false,
    }).catch(() => {})
    router.replace({ name: 'login' })
  } catch {
    // 2010 旧密码不正确，拦截器已提示
  } finally {
    savingPwd.value = false
  }
}

// ===== 卡 4：危险操作（登出 vs 注销，两件事） =====

async function onLogout() {
  await auth.logout()
  ElMessage.success('已登出')
  router.replace({ name: 'login' })
}

async function onDeleteAccount() {
  const roleName = roleText.value
  // confirm 单独一个 try：用户点「再想想」时它会 reject，
  // 如果和下面的 API 调用共用一个 catch，取消就会被当成接口失败处理。
  try {
    await ElMessageBox.confirm(
      `<p>此操作将<b>永久注销你的「${roleName}」角色账户</b>。</p>
       <ul style="padding-left:18px;line-height:1.9;margin:8px 0">
         <li>只会删除<b>当前这一个角色</b>；如果你还有别的角色，那些角色仍然可以正常登录。</li>
         <li>用户名、头像、性别等资料挂在主账号上，不会因为注销某个角色而丢失。</li>
         <li>注销后需要重新注册才能再次拥有「${roleName}」角色（配送员还要重新审核）。</li>
         <li>此操作<b>不可恢复</b>。</li>
       </ul>
       <p style="margin:0">如果只是想退出登录，请使用「登出」。</p>`,
      `注销「${roleName}」角色`,
      {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: `我确认注销「${roleName}」`,
        cancelButtonText: '再想想',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }

  try {
    await deleteAccount()
    // 顺手把会话也结束掉，避免留下一个指向已删除角色的 cookie
    await auth.logout()
    ElMessage.success('已注销当前角色')
    router.replace({ name: 'login' })
  } catch {}
}
</script>

<template>
  <div>
    <el-card shadow="never" class="page-card">
      <div class="head">
        <el-upload
          :show-file-list="false"
          :auto-upload="false"
          :accept="ACCEPT_ATTR"
          :on-change="onAvatarChange"
          class="avatar-upload"
        >
          <div class="avatar-box" v-loading="uploadingAvatar">
            <el-image :src="imageUrl(p.avatar)" fit="cover" class="avatar-img">
              <template #error>
                <span class="avatar-fallback">{{ initial }}</span>
              </template>
            </el-image>
            <div class="avatar-mask">点击更换</div>
          </div>
        </el-upload>

        <div class="head-info">
          <div class="head-title">
            <span class="head-name">{{ p.username }}</span>
            <el-tag :type="roleTagType[p.role]" effect="plain">{{ roleText }}</el-tag>
          </div>
          <el-descriptions :column="2" border size="small" class="head-desc">
            <el-descriptions-item label="性别">{{ genderLabel[p.gender] }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ p.phone || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ p.email || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="头像">
              <span class="muted mono">{{ p.avatar || '未设置' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>基本资料</template>

      <el-form
        ref="usernameFormRef"
        :model="usernameForm"
        :rules="usernameFormRules"
        label-width="72px"
        class="inline-form"
        @submit.prevent
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="usernameForm.username" maxlength="10" class="inline-input" />
          <el-button type="primary" :loading="savingUsername" @click="saveUsername">保存</el-button>
        </el-form-item>
      </el-form>

      <el-form label-width="72px" class="inline-form" @submit.prevent>
        <el-form-item label="性别">
          <el-radio-group v-model="genderValue">
            <el-radio v-for="o in genderOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </el-radio>
          </el-radio-group>
          <el-button
            type="primary"
            :loading="savingGender"
            :disabled="genderValue === p.gender"
            @click="saveGender"
          >
            保存
          </el-button>
        </el-form-item>
      </el-form>

      <p class="muted note">
        用户名和性别各有一个保存按钮：后端就是一个字段一个接口，这里 1:1
        对应，不做假的「全部保存」。
      </p>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>账号与安全</template>

      <div class="bind-row">
        <div>
          <div class="bind-label">手机号</div>
          <div class="bind-value mono">{{ p.phone || '未绑定' }}</div>
        </div>
        <el-button @click="openPhoneDialog">换绑</el-button>
      </div>

      <div class="bind-row">
        <div>
          <div class="bind-label">邮箱</div>
          <div class="bind-value mono">{{ p.email || '未绑定' }}</div>
        </div>
        <el-button @click="openEmailDialog">换绑</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>修改密码</template>

      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="90px"
        class="narrow-form"
        @submit.prevent
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="6-20 位"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPwd" @click="submitPassword"
            >修改密码</el-button
          >
        </el-form-item>
      </el-form>

      <el-alert type="warning" :closable="false" show-icon class="note-alert">
        <template #title>修改成功后当前会话会立即失效</template>
        密码挂在主账号上，这个人的<b>所有角色</b>都会改用新密码；改完需要重新登录。
      </el-alert>
    </el-card>

    <el-card shadow="never" class="page-card danger" id="danger">
      <template #header>危险操作</template>

      <div class="danger-row">
        <div>
          <div class="bind-label">登出</div>
          <div class="muted note-inline">只结束当前会话，账号和资料都保留，随时可以重新登录。</div>
        </div>
        <el-button @click="onLogout">登出</el-button>
      </div>

      <div class="danger-row">
        <div>
          <div class="bind-label">注销当前角色</div>
          <div class="muted note-inline">
            永久删除「{{ roleText }}」这个角色账户，<b>不可恢复</b>。其他角色和主账号资料保留。
          </div>
        </div>
        <el-button type="danger" @click="onDeleteAccount">注销当前角色</el-button>
      </div>
    </el-card>

    <!-- 换绑手机号 -->
    <el-dialog v-model="phoneDialog.open" title="换绑手机号" width="440px">
      <el-form
        ref="phoneFormRef"
        :model="phoneDialog"
        :rules="phoneRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="新手机号" prop="newPhone">
          <el-input v-model="phoneDialog.newPhone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item>
          <el-button :loading="phoneSending" :disabled="phoneDisabled" @click="sendPhoneCode">
            {{ phoneButtonText }}
          </el-button>
          <span class="muted send-hint">验证码会发给上面这个<b>新</b>手机号</span>
        </el-form-item>
        <el-alert v-if="phoneMockCode" type="success" :closable="false" show-icon class="mock">
          <template #title>模拟短信（换绑手机号）</template>
          您的验证码是 <b class="code">{{ phoneMockCode }}</b>
          <span class="sub">5 分钟内有效，只能使用一次。</span>
        </el-alert>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="phoneDialog.code" maxlength="6" placeholder="6 位数字验证码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="phoneDialog.submitting" @click="submitPhone"
          >确定</el-button
        >
      </template>
    </el-dialog>

    <!-- 换绑邮箱 -->
    <el-dialog v-model="emailDialog.open" title="换绑邮箱" width="440px">
      <el-form
        ref="emailFormRef"
        :model="emailDialog"
        :rules="emailRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="新邮箱" prop="newEmail">
          <el-input v-model="emailDialog.newEmail" maxlength="254" placeholder="新邮箱地址" />
        </el-form-item>
        <el-form-item>
          <el-button :loading="emailSending" :disabled="emailDisabled" @click="sendEmailCode">
            {{ emailButtonText }}
          </el-button>
          <span class="muted send-hint">验证码会发给上面这个<b>新</b>邮箱</span>
        </el-form-item>
        <el-alert v-if="emailMockCode" type="success" :closable="false" show-icon class="mock">
          <template #title>模拟邮件（换绑邮箱）</template>
          您的验证码是 <b class="code">{{ emailMockCode }}</b>
          <span class="sub">5 分钟内有效，只能使用一次。</span>
        </el-alert>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="emailDialog.code" maxlength="6" placeholder="6 位数字验证码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="emailDialog.submitting" @click="submitEmail"
          >确定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  gap: 20px;
  align-items: center;
}

.avatar-upload {
  flex: none;
}

.avatar-box {
  position: relative;
  width: 88px;
  height: 88px;
  overflow: hidden;
  cursor: pointer;
  border-radius: 8px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  background: #f0f2f5;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 32px;
  color: #909399;
}

.avatar-mask {
  position: absolute;
  inset: auto 0 0;
  padding: 4px 0;
  font-size: 12px;
  color: #fff;
  text-align: center;
  background: rgb(0 0 0 / 50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-box:hover .avatar-mask {
  opacity: 1;
}

.head-info {
  flex: 1;
  min-width: 0;
}

.head-title {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.head-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.inline-form {
  margin-bottom: 4px;
}

.inline-input {
  max-width: 260px;
  margin-right: 12px;
}

.narrow-form {
  max-width: 460px;
}

.note {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
}

.note-alert {
  margin-top: 8px;
  font-size: 13px;
}

.note-inline {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.6;
}

.bind-row,
.danger-row {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.bind-row + .bind-row,
.danger-row + .danger-row {
  border-top: 1px solid #ebeef5;
}

.bind-label {
  font-size: 14px;
  color: #303133;
}

.bind-value {
  margin-top: 2px;
  font-size: 13px;
  color: #606266;
}

.danger {
  border-left: 3px solid #f56c6c;
}

.send-hint {
  margin-left: 12px;
  font-size: 12px;
}

.mock {
  margin-bottom: 18px;
  font-size: 13px;
}

.mock .code {
  font-size: 17px;
  letter-spacing: 2px;
}

.mock .sub {
  margin-left: 8px;
  font-size: 12px;
  opacity: 0.85;
}
</style>
