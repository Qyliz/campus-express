import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    // 自动导入 Element Plus 的命令式 API（ElMessage / ElMessageBox / ElLoading）。
    // 关键作用其实是「顺带注入这些组件的 CSS」：命令式 API 在模板里没有对应标签，
    // unplugin-vue-components 看不见它们，样式就永远不会被引入，
    // 提示会以无样式的裸文本出现在页面角落。
    // imports: [] 表示不自动导入 ref / computed 等 Vue API —— 那些仍然手写 import，
    // 每个文件只省一行，却会让人看不出符号从哪来，不划算。
    AutoImport({
      imports: [],
      resolvers: [ElementPlusResolver()],
      // 必须放在 src/ 下：tsconfig.app.json 的 include 只覆盖 env.d.ts 和 src/**/*，
      // 放到项目根目录 vue-tsc 看不见，每个 ElMessage 都会报 Cannot find name。
      dts: 'src/types/auto-imports.d.ts',
    }),
    // 模板里的 <el-button> 之类自动按需导入，不用在每个 SFC 里写 import
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 5173 被占用时直接报错退出，而不是悄悄漂到 5174 ——
    // 端口一漂移，README 里写的地址和你在浏览器里开的页面就对不上了
    strictPort: true,
    proxy: {
      // 用相对路径而不是 http://localhost:8080：浏览器眼中一切都是同源(5173)，
      // satoken cookie 会自动随每个请求发送 —— 不需要 withCredentials，
      // 后端也不需要任何 CORS 配置。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 头像 / 审核材料图片。后端 WebConfig 把 /upload/** 映射到 file:upload/，
      // 而这个路径不在 Sa-Token 拦截器的排除名单里 —— 图片本身也需要登录态。
      // 少了这条代理，<img src="/upload/avatar/x.png"> 会向 5173 要文件而 404。
      '/upload': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
