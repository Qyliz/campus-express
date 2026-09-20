import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    //自动导入 Element Plus API，并补齐对应样式
    AutoImport({
      imports: [],
      resolvers: [ElementPlusResolver()],
      //类型声明放在 tsconfig 的扫描范围内
      dts: 'src/types/auto-imports.d.ts',
    }),
    //按需导入模板中的 Element Plus 组件
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
  build: {
    //Element Plus 独立分包后的体积在预期范围内
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        //稳定依赖单独分包，避免业务更新时重复下载
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          //@element-plus/icons-vue 也会进入这个分包
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('/vue/') || id.includes('@vue/') || id.includes('vue-router') || id.includes('pinia')) {
            return 'vue-vendor'
          }
          //其余第三方依赖
          return 'vendor'
        },
      },
    },
  },
  server: {
    port: 5173,
    //固定开发端口
    strictPort: true,
    proxy: {
      //保持接口同源，让浏览器自动携带会话 Cookie
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      //上传文件也通过开发代理访问
      '/upload': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
