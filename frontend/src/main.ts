import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
//样式引入顺序也是覆盖顺序
import './assets/styles/tokens.css'
import './assets/main.css'
import './assets/styles/auth-theme.css'
import './assets/styles/dashboard.css'
import './assets/styles/workspace-shell.css'

//Element Plus 组件和样式由 Vite 插件按需引入
const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
