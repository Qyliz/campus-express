<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  CircleCheck,
  HomeFilled,
  Lock,
  SwitchButton,
  Tickets,
  User,
  UserFilled,
  Van,
} from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import { imageUrl } from '@/utils/image'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const initial = computed(() => auth.username.charAt(0) || 'A')
const activeMenu = computed(() => {
  if (route.name === 'admin-order-detail') return '/admin/orders'
  return route.path
})

async function logout() {
  await auth.logout()
  await router.replace({ name: 'home' })
  ElMessage.success('已登出')
}
</script>

<template>
  <el-container v-if="auth.isLoggedIn" class="admin-shell app-shell">
    <el-aside width="236px" class="shell-aside">
      <button type="button" class="brand" @click="router.push({ name: 'admin-dashboard' })">
        <span class="brand-icon"><Van /></span>
        <span><b>校园配送</b><small>管理后台</small></span>
      </button>

      <!-- :router="true" + index 等于路由路径 ⇒ default-active 自动跟随 URL，不用写点击处理 -->
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#9db3c7"
        active-text-color="#ffffff"
        class="menu"
      >
        <el-menu-item index="/admin">
          <el-icon><HomeFilled /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/audits">
          <el-icon><CircleCheck /></el-icon>
          <span>审核管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/bans">
          <el-icon><Lock /></el-icon>
          <span>封禁记录</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/exceptions"
          ><el-icon><Bell /></el-icon><span>异常管理</span></el-menu-item
        >
        <el-menu-item index="/admin/review-appeals"
          ><el-icon><CircleCheck /></el-icon><span>评价申诉</span></el-menu-item
        >
        <el-menu-item index="/admin/profile">
          <el-icon><UserFilled /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>

      <div class="account">
        <button
          type="button"
          class="account-link"
          title="进入个人中心"
          @click="router.push({ name: 'admin-profile' })"
        >
          <el-image
            :key="auth.profile?.avatar || 'avatar-empty'"
            :src="imageUrl(auth.profile?.avatar)"
            fit="cover"
            class="avatar"
          >
            <template #error>
              <span class="avatar-fallback">{{ initial }}</span>
            </template>
          </el-image>
          <span class="account-text">
            <b>{{ auth.username }}</b>
            <small>管理员</small>
          </span>
        </button>
        <el-button text circle aria-label="退出登录" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
      </el-header>

      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
  <div v-else class="session-exit">正在返回首页…</div>
</template>

<style scoped>
/* 品牌区 / 账户区 / 头像的共性样式在 styles/workspace-shell.css（.app-shell 命名空间），
   这里只保留管理端自己的部分：侧栏内边距、菜单、面包屑头、内容区。 */
.admin-shell {
  min-height: 100vh;
}

.session-exit {
  display: grid;
  min-height: 100vh;
  color: #9db3c7;
  background: #071a2d;
  place-items: center;
}

.brand {
  padding: 28px 26px;
  color: #fff;
}

.menu {
  flex: 1;
  border-right: none;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 18px;
  border-radius: 10px;
  /* el-menu-item 是 li：触屏长按会拉出浅色文字选区 */
  -webkit-user-select: none;
  user-select: none;
  /* 触屏点按默认的半透明高亮块在深色导航上像一块色斑 */
  -webkit-tap-highlight-color: transparent;
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.18);
}

.header {
  display: flex;
  align-items: center;
  height: 68px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.account {
  margin: 0 18px 20px;
}

.main {
  padding: clamp(26px, 4vw, 48px);
  background: #f3f6fa;
}

@media (max-width: 720px) {
  .admin-shell {
    flex-direction: column;
  }

  .shell-aside {
    position: static;
    height: auto;
    width: 100% !important;
  }

  .brand {
    padding: 18px 26px 12px;
  }

  .menu {
    display: flex;
    overflow-x: auto;
    padding: 0 10px 14px;
    /* 触屏上横滑即可，露出的滚动条在深色底上像一块色斑 */
    scrollbar-width: none;
  }

  .menu::-webkit-scrollbar {
    display: none;
  }

  .account {
    margin: 0 26px 20px;
  }

  .menu :deep(.el-menu-item) {
    flex: 0 0 auto;
    margin: 4px;
  }

  .header {
    padding: 0 16px;
  }

  .header :deep(.el-breadcrumb) {
    display: none;
  }

  .main {
    padding: 24px 16px;
    /* 宽表格在内容区内部横向滚动，不允许撑宽整页连带侧栏重排 */
    overflow-x: auto;
  }
}
</style>
