import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'

import type { Result } from '@/types'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

/**
 * 给 axios 的请求配置加一个自定义开关。
 * InternalAxiosRequestConfig extends AxiosRequestConfig，
 * 所以在错误拦截器里也能通过 error.config.silent 读到它。
 */
declare module 'axios' {
  export interface AxiosRequestConfig {
    /** true = 出错时拦截器只 reject，不弹 ElMessage、不跳登录页，完全交给调用方 */
    silent?: boolean
  }
}

/** 所有失败都收敛成这个类型：HTTP 200 但 code!==0、401、403、500、网络不通 */
export class ApiError extends Error {
  constructor(
    readonly code: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

const instance: AxiosInstance = axios.create({
  // 关键：相对路径。浏览器只看到 localhost:5173，/api 与 /upload 都由 Vite 代理到 8080，
  // 所以 satoken cookie 是同源的，会自动随每个请求发送 —— 不需要 withCredentials，
  // 后端也不需要任何 CORS 配置。
  baseURL: '',
  timeout: 10000,
})

/** 统一失败出口：弹提示（除非 silent）并 reject 一个 ApiError */
function fail(config: AxiosRequestConfig | undefined, code: number, message: string) {
  if (!config?.silent) ElMessage.error(message)
  return Promise.reject(new ApiError(code, message))
}

/** 会话失效后跳登录页，并把当前地址带上，登录成功后可以回来 */
function goLogin() {
  const current = router.currentRoute.value
  if (current.name === 'login') return
  // 守卫可能再次重定向，router.replace 会 reject，这里必须吞掉，否则控制台冒未处理的 rejection
  router.replace({ name: 'login', query: { redirect: current.fullPath } }).catch(() => {})
}

instance.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    const body = response.data

    // 业务错误和参数校验错误都是 HTTP 200，只能靠 code 判断，不能靠状态码
    if (body !== null && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        // 解包：把 data 直接当成响应值。类型由下面的 http 包装器修正。
        return body.data as never
      }
      // code 1 的 message 形如 "username: 用户名格式不正确; password: 密码长度必须在6-20之间"，
      // 后端已经把字段明细拼进一个字符串了，直接展示即可
      return fail(response.config, body.code, body.message)
    }

    // 不是 Result 信封（本项目理论上不会出现），原样透传
    return response as never
  },

  (error: AxiosError<Result<unknown>>) => {
    const config = error.config
    const status = error.response?.status
    const body = error.response?.data

    // 请求根本没到后端：服务没启动、端口不对、代理写错、断网
    if (!error.response) {
      return fail(config, -1, '无法连接后端服务，请确认 localhost:8080 已经启动')
    }

    // 401 = NotLoginException，body.code 为 1001/1002/1003。
    // message 已经是正确的中文，直接用后端文案，前端不再维护一份映射表（避免两边不同步）。
    if (status === 401) {
      const code = body?.code ?? 1001
      const message = body?.message || '未登录或登录已过期'
      // 服务端已经把这个会话判为失效了，这里只清本地状态；
      // cookie 本身不用管（下次登录会写入新的 satoken，登出接口也会清掉它）。
      useAuthStore().clear()
      if (!config?.silent) {
        ElMessage.error(message)
        goLogin()
      }
      return Promise.reject(new ApiError(code, message))
    }

    // 403 = NotRoleException，code 1004。路由守卫本来会拦住非管理员，
    // 走到这里通常是「管理页开着，会话角色变了」，提示后送回首页。
    if (status === 403) {
      const code = body?.code ?? 1004
      const message = body?.message || '没有操作权限'
      if (!config?.silent) {
        ElMessage.error(message)
        if (router.currentRoute.value.path.startsWith('/admin')) {
          router.replace({ name: 'home' }).catch(() => {})
        }
      }
      return Promise.reject(new ApiError(code, message))
    }

    // 后端没启动时，Vite 代理会自己回一个 502/504（而不是让请求直接失败），
    // 所以 error.response 是存在的。这几个状态要当成「后端不可达」，
    // 否则会掉到下面的兜底分支，提示成没有指导意义的「系统繁忙」。
    if (status === 502 || status === 503 || status === 504) {
      return fail(config, -1, '无法连接后端服务，请确认 localhost:8080 已经启动')
    }

    // 500 → code 9999；文件相关是 HTTP 200 + 3001~3004，走的是上面的成功分支
    return fail(config, body?.code ?? 9999, body?.message || '系统繁忙，请稍候再试')
  },
)

/**
 * axios 的 get<T, R> 第二个泛型 R 就是 Promise 的解析类型。
 * 因为拦截器已经把 Result 解包了，所以 R = T 而不是 AxiosResponse<T>。
 * 在这里统一写一次，21 个接口函数就不用各自 as 了。
 */
export const http = {
  get<T>(url: string, params?: object, config?: AxiosRequestConfig) {
    return instance.get<unknown, T>(url, { ...config, params })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.post<unknown, T>(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.put<unknown, T>(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return instance.delete<unknown, T>(url, config)
  },
  /**
   * multipart/form-data 上传。
   * 千万不要手写 headers: { 'Content-Type': 'multipart/form-data' } ——
   * 那样会缺少 boundary，后端根本解析不出 part。
   * axios 检测到 body 是 FormData 会自动带上正确的 Content-Type。
   */
  upload<T>(url: string, form: FormData, config?: AxiosRequestConfig) {
    return instance.post<unknown, T>(url, form, config)
  },
}
