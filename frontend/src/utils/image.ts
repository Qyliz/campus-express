/** 与后端 common/util/FileUtil.java 里 switch 的 Content-Type 完全一致 */
export const ACCEPTED_IMAGE_TYPES: string[] = ['image/jpeg', 'image/png', 'image/webp']

/** 与后端 spring.servlet.multipart.max-file-size 一致 */
export const MAX_IMAGE_SIZE = 5 * 1024 * 1024

/** 直接喂给 <el-upload accept="..."> */
export const ACCEPT_ATTR = ACCEPTED_IMAGE_TYPES.join(',')

/**
 * 后端返回的 avatar / material 已经是 "/upload/avatar/uuid.png" 这种以 / 开头的路径。
 * 交给 <img src> 后浏览器会拼成 http://localhost:5173/upload/avatar/uuid.png，
 * 由 Vite 代理转发到 8080，并自动带上 satoken cookie（/upload/** 也在登录拦截器后面）。
 * 所以这里什么都不用拼 —— 尤其不要去拼 http://localhost:8080，那就变成跨域了。
 */
export function imageUrl(path?: string | null): string {
  return path || ''
}

/** 上传前预校验，省掉一次必然失败(3001/3002/3003)的往返。返回 null 表示通过 */
export function validateImageFile(file?: File | null): string | null {
  if (!file) return '请选择文件'
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) return '只支持 jpg / png / webp 格式的图片'
  if (file.size > MAX_IMAGE_SIZE) return '图片不能超过 5MB'
  return null
}
