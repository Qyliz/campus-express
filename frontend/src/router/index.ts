import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AdminUsersView from '@/views/admin/UsersView.vue'
import AdminAuditsView from '@/views/admin/AuditsView.vue'
import AdminBansView from '@/views/admin/BansView.vue'

/** 让 to.meta.xxx 有类型，而不是 unknown */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: '', name: 'home', component: HomeView, meta: { title: '首页' } },
      {
        path: 'login',
        name: 'login',
        component: LoginView,
        meta: { title: '登录', guestOnly: true },
      },
      {
        path: 'register',
        name: 'register',
        component: RegisterView,
        meta: { title: '注册', guestOnly: true },
      },
      {
        path: 'forgot-password',
        name: 'forgot-password',
        component: ForgotPasswordView,
        meta: { title: '忘记密码' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: ProfileView,
        meta: { title: '个人中心', requiresAuth: true },
      },
    ],
  },

  // ===== 管理端 =====
  // meta 写在父记录上即可：to.meta 是所有匹配记录 meta 的合并结果，子路由自动继承。
  // 用静态 import 而不是 () => import()：懒加载在这么小的项目里只增加网络瀑布概念而无收益，
  // 静态导入还能让 type-check 一次性覆盖到管理端页面。
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: { name: 'admin-users' } },
      {
        path: 'users',
        name: 'admin-users',
        component: AdminUsersView,
        meta: { title: '账号管理' },
      },
      {
        path: 'audits',
        name: 'admin-audits',
        component: AdminAuditsView,
        meta: { title: '审核管理' },
      },
      {
        path: 'bans',
        name: 'admin-bans',
        component: AdminBansView,
        meta: { title: '封禁记录' },
      },
    ],
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  // createWebHistory 需要服务端把所有未知路径回退到 index.html。
  // 开发期 Vite dev server 自动做了这件事；vite preview 也做了。
  // 真正部署到 nginx 时要自己加 try_files $uri $uri/ /index.html;
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 首次导航真正发一次 GET /api/user/profile，之后复用同一个 Promise。
  // 守卫是 async 的 → 首屏在探测结束前不渲染任何路由组件，因此不会闪一下未登录的样子。
  await auth.init()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 后端 @SaCheckRole("ADMIN") 本来也会回 403，这里只是提前拦住，体验更好
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.warning('该功能仅管理员可用')
    return { name: 'home' }
  }

  // 已登录的人还去看登录/注册页没有意义。
  // 注意：追加角色也必须先登出 —— 后端 is-concurrent: false，一个用户同时只能有一个在线会话。
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 校园快递` : '校园快递'
})

export default router
