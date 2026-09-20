import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import type { OrderScope } from '@/api/order'
import type { RoleEnum } from '@/types'

//路由元信息类型
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
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' },
      },
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

  //管理端子路由继承父级权限配置
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
  //生产环境需将未知路径回退到 index.html
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  //首次导航先确认服务端会话
  await auth.init()

  //管理员个人中心保留后台布局
  if (auth.isAdmin && to.name === 'profile') {
    return { name: 'admin-profile' }
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'home' }
  }

  //前端提前拦截无权限导航
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.warning('该功能仅管理员可用')
    return { name: 'home' }
  }

  if (to.meta.role && auth.role !== to.meta.role) {
    ElMessage.warning('当前身份不能访问此页面')
    return { name: 'home' }
  }

  //登录后不再进入游客页面
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 校园快递` : '校园快递'
})

export default router
