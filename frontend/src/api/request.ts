import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'

import type { Result } from '@/types'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

//扩展 Axios 配置，允许调用方静默处理错误
declare module 'axios' {
  export interface AxiosRequestConfig {
    //静默模式下不提示也不跳转
    silent?: boolean
  }
}

//统一接口错误类型
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
  //相对路径由开发代理转发，并保持 Cookie 同源
  baseURL: '',
  timeout: 10000,
})

//统一提示并抛出接口错误
function fail(config: AxiosRequestConfig | undefined, code: number, message: string) {
  if (!config?.silent) ElMessage.error(message)
  return Promise.reject(new ApiError(code, message))
}

//会话失效后回到首页
function goHome() {
  const current = router.currentRoute.value
  if (current.name === 'home') return
  //忽略路由守卫产生的重复跳转
  router.replace({ name: 'home' }).catch(() => {})
}

instance.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    const body = response.data

    //业务错误通过响应体 code 判断
    if (body !== null && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        //成功时直接返回 data
        return body.data as never
      }
      //后端 message 已包含字段校验明细
      return fail(response.config, body.code, body.message)
    }

    //非标准响应原样返回
    return response as never
  },

  (error: AxiosError<Result<unknown>>) => {
    const config = error.config
    const status = error.response?.status
    const body = error.response?.data

    //请求未到达服务端
    if (!error.response) {
      return fail(config, -1, '无法连接后端服务，请确认 localhost:8080 已经启动')
    }

    //登录失效时清理本地状态并使用后端提示
    if (status === 401) {
      const code = body?.code ?? 1001
      const message = body?.message || '未登录或登录已过期'
      useAuthStore().clear()
      if (!config?.silent) {
        ElMessage.error(message)
        goHome()
      }
      return Promise.reject(new ApiError(code, message))
    }

    //权限变化后退回首页
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

    //开发代理的 502/503/504 也表示服务不可用
    if (status === 502 || status === 503 || status === 504) {
      return fail(config, -1, '无法连接后端服务，请确认 localhost:8080 已经启动')
    }

    //其余服务端错误统一兜底
    return fail(config, body?.code ?? 9999, body?.message || '系统繁忙，请稍候再试')
  },
)

//包装拦截器解包后的响应类型
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
  //让 Axios 自动生成 multipart boundary
  upload<T>(url: string, form: FormData, config?: AxiosRequestConfig) {
    return instance.post<unknown, T>(url, form, config)
  },
}
