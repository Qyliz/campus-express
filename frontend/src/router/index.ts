import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import type { OrderScope } from '@/api/order'
import type { RoleEnum } from '@/types'

/** 让 to.meta.xxx 有类型，而不是 unknown */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
    role?: RoleEnum
    orderScope?: OrderScope
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: 'order/create',
        name: 'order-create',
        component: () => import('@/views/order/CreateOrderView.vue'),
        meta: { title: '发布订单', requiresAuth: true, role: 'CUSTOMER' },
      },
      {
        path: 'order/mine',
        name: 'orders-mine',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: '我的订单', requiresAuth: true, role: 'CUSTOMER', orderScope: 'mine' },
      },
      {
        path: 'order/available',
        name: 'orders-available',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: '接单大厅', requiresAuth: true, role: 'COURIER', orderScope: 'available' },
      },
      {
        path: 'order/assigned',
        name: 'orders-assigned',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: '我的配送', requiresAuth: true, role: 'COURIER', orderScope: 'assigned' },
      },
      {
        path: 'order/:id',
        name: 'order-detail',
        component: () => import('@/views/order/OrderDetailView.vue'),
        meta: { title: '订单详情', requiresAuth: true },
      },
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/LoginView.vue'),
        meta: { title: '登录', guestOnly: true },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/RegisterView.vue'),
        meta: { title: '注册', guestOnly: true },
      },
      {
        path: 'forgot-password',
        name: 'forgot-password',
        component: () => import('@/views/ForgotPasswordView.vue'),
        meta: { title: '忘记密码', guestOnly: true },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
    ],
  },

  // ===== 管理端 =====
  // meta 写在父记录上即可：to.meta 是所有匹配记录 meta 的合并结果，子路由自动继承。
  // 页面组件全部 () => import() 懒加载：Element Plus 按需打包后单 bundle 仍超过 600KB，
  // 按路由分包后首屏只下载登录/首页用到的部分，管理端的大表格页面留在各自 chunk 里按需取。
  // 两个 Layout 保持静态导入：它们小且每次导航必现，拆出去只多一次请求。
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'profile',
        name: 'admin-profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'review-appeals',
        name: 'admin-review-appeals',
        component: () => import('@/views/admin/ReviewAppealsView.vue'),
        meta: { title: '评价申诉' },
      },
      {
        path: 'exceptions',
        name: 'admin-exceptions',
        component: () => import('@/views/admin/ExceptionsView.vue'),
        meta: { title: '异常管理' },
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: '订单管理', orderScope: 'admin' },
      },
      {
        path: 'orders/:id',
        name: 'admin-order-detail',
        component: () => import('@/views/order/OrderDetailView.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { title: '账号管理' },
      },
      {
        path: 'audits',
        name: 'admin-audits',
        component: () => import('@/views/admin/AuditsView.vue'),
        meta: { title: '审核管理' },
      },
      {
        path: 'bans',
        name: 'admin-bans',
        component: () => import('@/views/admin/BansView.vue'),
        meta: { title: '封禁记录' },
      },
    ],
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
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

  // 首次导航查询 GET /api/user/session，匿名访问返回空数据，之后复用同一个 Promise。
  // 守卫是 async 的 → 首屏在探测结束前不渲染任何路由组件，因此不会闪一下未登录的样子。
  await auth.init()

  // 管理员的个人中心留在后台布局中，避免切换页面后丢失侧栏。
  if (auth.isAdmin && to.name === 'profile') {
    return { name: 'admin-profile' }
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'home' }
  }

  // 后端 @SaCheckRole("ADMIN") 本来也会回 403，这里只是提前拦住，体验更好
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.warning('该功能仅管理员可用')
    return { name: 'home' }
  }

  if (to.meta.role && auth.role !== to.meta.role) {
    ElMessage.warning('当前身份不能访问此页面')
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
