import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getProfile, login as loginApi, logout as logoutApi } from '@/api/user'
import type { RoleEnum, UserLoginDTO, UserProfileVO } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  /**
   * 当前登录者的资料，null 表示未登录。
   * 这里没有任何 token：登录态只存在于 satoken cookie 里。
   * 实测这个 cookie 并没有 HttpOnly（document.cookie 读得到），但我们刻意不去读它 ——
   * 「浏览器里有个 token」既不能说明它还有效，也不告诉你是哪个角色在登录，
   * 判断登录态唯一可靠的办法就是问一次 /profile。
   */
  const profile = ref<UserProfileVO | null>(null)
  /** 首次探测是否已完成。App 首屏不闪「未登录」就是靠守卫 await 了它 */
  const initialized = ref(false)
  /** 探测 / 登录进行中，喂给按钮的 loading */
  const loading = ref(false)

  /** 让并发调用共享同一次探测请求（守卫在启动时可能被多次触发） */
  let initPromise: Promise<void> | null = null

  const isLoggedIn = computed(() => profile.value !== null)
  const role = computed<RoleEnum | null>(() => profile.value?.role ?? null)
  const isAdmin = computed(() => profile.value?.role === 'ADMIN')
  const isCourier = computed(() => profile.value?.role === 'COURIER')
  const isCustomer = computed(() => profile.value?.role === 'CUSTOMER')
  const username = computed(() => profile.value?.username ?? '')

  /** 用后端返回的 VO 直接刷新本地资料，省掉一次额外查询（改用户名/性别/头像都返回 VO） */
  function applyProfile(vo: UserProfileVO) {
    profile.value = vo
    initialized.value = true
  }

  /** 静默探测。匿名访客会拿到 401，这是预期行为，所以 silent，不能弹提示也不能跳登录 */
  async function fetchProfile() {
    loading.value = true
    try {
      applyProfile(await getProfile({ silent: true }))
    } catch {
      profile.value = null
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  /** 整个页面生命周期内只真正探测一次；之后的导航复用同一个 Promise */
  function init(): Promise<void> {
    initPromise ??= fetchProfile()
    return initPromise
  }

  /** 换绑手机/邮箱这类返回 void 的接口成功后，用它把资料拉回来（非 silent，出错要提示） */
  async function refresh() {
    applyProfile(await getProfile())
  }

  async function login(form: UserLoginDTO) {
    loading.value = true
    try {
      await loginApi(form) // 响应头 Set-Cookie: satoken=... 浏览器已经自动存好
      applyProfile(await getProfile()) // 紧接着的这次请求就会带上 cookie
    } catch (e) {
      clear()
      throw e
    } finally {
      loading.value = false
    }
  }

  /** 登出只是结束会话，账号还在。跳转交给页面做，store 只管状态 */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 会话可能早就没了（比如被顶下线），忽略
    }
    clear()
  }

  /** 清空本地状态。cookie 由后端/浏览器负责，我们碰不到 */
  function clear() {
    profile.value = null
    initialized.value = true
  }

  return {
    profile,
    initialized,
    loading,
    isLoggedIn,
    role,
    isAdmin,
    isCourier,
    isCustomer,
    username,
    applyProfile,
    fetchProfile,
    init,
    refresh,
    login,
    logout,
    clear,
  }
})
