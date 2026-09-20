import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getProfile, getSession, login as loginApi, logout as logoutApi } from '@/api/user'
import type { RoleEnum, UserLoginDTO, UserProfileVO } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  //登录状态以服务端会话为准
  const profile = ref<UserProfileVO | null>(null)
  //是否完成首次会话探测
  const initialized = ref(false)
  //探测或登录中的加载状态
  const loading = ref(false)

  //并发导航共用同一次会话探测
  let initPromise: Promise<void> | null = null

  const isLoggedIn = computed(() => profile.value !== null)
  const role = computed<RoleEnum | null>(() => profile.value?.role ?? null)
  const isAdmin = computed(() => profile.value?.role === 'ADMIN')
  const isCourier = computed(() => profile.value?.role === 'COURIER')
  const isCustomer = computed(() => profile.value?.role === 'CUSTOMER')
  const username = computed(() => profile.value?.username ?? '')

  function applyProfile(vo: UserProfileVO) {
    profile.value = vo
    initialized.value = true
  }

  //匿名访问和探测失败都不弹提示
  async function fetchProfile() {
    loading.value = true
    try {
      profile.value = await getSession({ silent: true })
    } catch {
      profile.value = null
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  //页面生命周期内只探测一次
  function init(): Promise<void> {
    initPromise ??= fetchProfile()
    return initPromise
  }

  //重新拉取用户资料
  async function refresh() {
    applyProfile(await getProfile())
  }

  async function login(form: UserLoginDTO) {
    loading.value = true
    try {
      await loginApi(form)
      applyProfile(await getProfile())
    } catch (e) {
      clear()
      throw e
    } finally {
      loading.value = false
    }
  }

  //只处理会话状态，页面负责跳转
  async function logout() {
    try {
      await logoutApi()
    } catch {
      //会话可能已经失效
    }
    clear()
  }

  //清空本地登录状态
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
