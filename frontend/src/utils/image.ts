//与后端允许的图片类型保持一致
export const ACCEPTED_IMAGE_TYPES: string[] = ['image/jpeg', 'image/png', 'image/webp']

//与后端上传大小限制保持一致
export const MAX_IMAGE_SIZE = 5 * 1024 * 1024

export const ACCEPT_ATTR = ACCEPTED_IMAGE_TYPES.join(',')

//上传路径保持同源，开发环境由 Vite 代理
export function imageUrl(path?: string | null): string {
  return path || ''
}

//返回 null 表示文件有效
export function validateImageFile(file?: File | null): string | null {
  if (!file) return '请选择文件'
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) return '只支持 jpg / png / webp 格式的图片'
  if (file.size > MAX_IMAGE_SIZE) return '图片不能超过 5MB'
  return null
}
