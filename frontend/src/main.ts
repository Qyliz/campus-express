import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/main.css'

// 这里刻意没有 app.use(ElementPlus)，也没有 import 'element-plus/dist/index.css'。
// 组件与样式都由 vite.config.ts 里的两个 unplugin 按需注入，
// 组件样式会自动带上 element-plus/theme-chalk/base.css，所以不需要单独引基础样式。
const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
