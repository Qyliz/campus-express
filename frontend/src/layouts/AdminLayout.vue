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
  <el-container v-if="auth.isLoggedIn" class="admin-shell">
    <el-aside width="236px" class="aside">
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
        <div class="account-profile">
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
        </div>
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

.aside {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #071a2d 0%, #0b2943 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 28px 26px;
  color: #fff;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.brand > span:last-child {
  display: flex;
  flex-direction: column;
}

.brand b {
  font-size: 17px;
}

.brand small {
  margin-top: 3px;
  color: #7896af;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.brand-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  color: #fff;
  background: #409eff;
  border-radius: 12px;
}

.brand-icon svg {
  width: 21px;
}

.menu {
  flex: 1;
  border-right: none;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 18px;
  border-radius: 10px;
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
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 18px 20px;
  padding: 16px 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.account-profile {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 10px;
  min-width: 0;
  text-align: left;
}

.avatar {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  overflow: hidden;
  background: rgba(64, 158, 255, 0.2);
  border-radius: 50%;
}

.avatar-fallback {
  display: grid;
  width: 100%;
  height: 100%;
  color: #fff;
  place-items: center;
}

.account-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.account-text b {
  overflow: hidden;
  color: #fff;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-text small {
  margin-top: 3px;
  color: #7896af;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.account > .el-button {
  color: #8ea9bf;
}

.main {
  padding: clamp(26px, 4vw, 48px);
  background: #f3f6fa;
}

@media (max-width: 720px) {
  .admin-shell {
    flex-direction: column;
  }

  .aside {
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
  }

  .account {
    display: none;
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
  }
}
</style>
