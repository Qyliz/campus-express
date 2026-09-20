import type { FormItemRule } from 'element-plus'

export const PHONE_RE = /^1[3-9]\d{9}$/
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
//与后端用户名规则一致
export const USERNAME_RE = /^[a-zA-Z0-9_\-\u4e00-\u9fa5]{1,10}$/
export const CODE_RE = /^\d{6}$/
export const PASSWORD_MIN = 6
export const PASSWORD_MAX = 20

//登录账号只接受邮箱或手机号
export function isAccount(value: string): boolean {
  return EMAIL_RE.test(value) || PHONE_RE.test(value)
}

export const accountRule: FormItemRule = {
  validator: (_rule, value: string, callback) => {
    if (!value) return callback(new Error('请输入邮箱或手机号'))
    if (!isAccount(value)) return callback(new Error('请输入正确的邮箱或手机号'))
    callback()
  },
  trigger: 'blur',
}

export const usernameRules: FormItemRule[] = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  {
    pattern: USERNAME_RE,
    message: '用户名只能是 1-10 位中文、字母、数字、下划线或短横线',
    trigger: 'blur',
  },
]

export const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  {
    min: PASSWORD_MIN,
    max: PASSWORD_MAX,
    message: `密码长度必须在 ${PASSWORD_MIN}-${PASSWORD_MAX} 之间`,
    trigger: 'blur',
  },
]

export const newPasswordRules: FormItemRule[] = [
  { required: true, message: '请输入新密码', trigger: 'blur' },
  {
    min: PASSWORD_MIN,
    max: PASSWORD_MAX,
    message: `密码长度必须在 ${PASSWORD_MIN}-${PASSWORD_MAX} 之间`,
    trigger: 'blur',
  },
]

//通过取值函数读取最新密码
export function confirmPasswordRules(
  password: () => string,
  requiredMessage = '请再次输入新密码',
): FormItemRule[] {
  return [
    { required: true, message: requiredMessage, trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== password()) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur',
    },
  ]
}

export const codeRules: FormItemRule[] = [
  { required: true, message: '请输入验证码', trigger: 'blur' },
  { pattern: CODE_RE, message: '验证码是 6 位数字', trigger: 'blur' },
]
