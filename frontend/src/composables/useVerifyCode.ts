import { computed, onUnmounted, ref } from 'vue'

import { sendVerifyCode } from '@/api/user'
import type { VerifySceneEnum } from '@/types'

/**
 * 发送验证码 + 60 秒倒计时 + 展示「短信/邮件」。
 *
 * 后端没有真实短信/邮件通道：POST /api/user/verify-code 把 6 位验证码直接放在 data 里返回，
 * 所以必须把它显示出来，否则流程根本走不下去。
 *
 * 验证码在后端的缓存 key 是 vc:{scene}:{account}，一次性、5 分钟过期、跨场景不通用。
 *
 * @param scene      场景。换绑手机/邮箱时，后端校验的是「新」手机号/「新」邮箱
 *                   （UserServiceImpl 里是 verify(dto.getNewPhone(), CHANGE_PHONE, ...)），
 *                   所以 getAccount 必须返回表单里的新值，而不是当前已绑定的值。
 * @param getAccount 发送目标。用函数传是为了每次点击都取到最新输入。
 * @param canSend    额外前置校验，返回字符串表示不能发送，并把该字符串作为提示文案
 */
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
      // 拦截器已经提示过了
    } finally {
      if (version === requestVersion) sending.value = false
    }
  }

  /** 验证码过期(2012)时由页面调用，清掉旧码让用户重新获取 */
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
