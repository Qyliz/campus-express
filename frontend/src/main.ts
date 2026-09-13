import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
// 全局样式按职责拆分，这里的引入顺序即优先级：
// tokens（变量）→ main（reset + 工具类）→ 认证页主题 → 工作台共享 → 布局外壳共享。
import './assets/styles/tokens.css'
import './assets/main.css'
import './assets/styles/auth-theme.css'
import './assets/styles/dashboard.css'
import './assets/styles/workspace-shell.css'

// 这里刻意没有 app.use(ElementPlus)，也没有 import 'element-plus/dist/index.css'。
// 组件与样式都由 vite.config.ts 里的两个 unplugin 按需注入，
// 组件样式会自动带上 element-plus/theme-chalk/base.css，所以不需要单独引基础样式。
const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
