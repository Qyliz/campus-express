import { computed, onUnmounted, ref } from 'vue'

import { sendVerifyCode } from '@/api/user'
import type { VerifySceneEnum } from '@/types'

//验证码发送和倒计时；getAccount 读取最新联系方式，canSend 返回文本时终止发送
export function useVerifyCode(
  scene: VerifySceneEnum,
  getAccount: () => string,
  canSend?: () => string | null,
) {
  const mockCode = ref('')
  const sending = ref(false)
  const countdown = ref(0)
  let timer: number | undefined
  let requestVersion = 0

  const disabled = computed(() => sending.value || countdown.value > 0)
  const buttonText = computed(() =>
    countdown.value > 0 ? `${countdown.value} 秒后重发` : '获取验证码',
  )

  function startCountdown() {
    countdown.value = 60
    window.clearInterval(timer)
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) window.clearInterval(timer)
    }, 1000)
  }

  async function send() {
    if (disabled.value) return
    const problem = canSend?.() ?? null
    if (problem) {
      ElMessage.warning(problem)
      return
    }
    const account = getAccount()
    const version = ++requestVersion
    sending.value = true
    try {
      const code = await sendVerifyCode({ account, scene })
      if (version !== requestVersion || account !== getAccount()) return
      mockCode.value = code
      startCountdown()
    } catch {
      //错误由请求拦截器提示
    } finally {
      if (version === requestVersion) sending.value = false
    }
  }

  //清除旧验证码和倒计时
  function reset() {
    requestVersion += 1
    sending.value = false
    mockCode.value = ''
    window.clearInterval(timer)
    countdown.value = 0
  }

  onUnmounted(() => window.clearInterval(timer))

  return { mockCode, sending, countdown, disabled, buttonText, send, reset }
}
