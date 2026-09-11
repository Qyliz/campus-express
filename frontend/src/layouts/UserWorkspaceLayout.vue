<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Collection,
  HomeFilled,
  Plus,
  SwitchButton,
  Tickets,
  User,
  Van,
} from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import { imageUrl } from '@/utils/image'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const isCustomer = computed(() => auth.isCustomer)
const roleName = computed(() => (isCustomer.value ? '收寄件人' : '配送员'))
const activeMenu = computed(() => {
  if (route.name === 'home') return 'home'
  if (route.name === 'profile') return 'profile'
  if (route.name === 'order-create') return 'order-create'
  if (route.name === 'orders-available') return 'orders-available'
  if (route.name === 'orders-assigned') return 'orders-assigned'
  if (route.name === 'orders-mine' || route.name === 'order-detail') {
    return isCustomer.value ? 'orders-mine' : 'orders-assigned'
  }
  return ''
})

async function logout() {
  await auth.logout()
  await router.replace({ name: 'home' })
  ElMessage.success('已登出')
}
</script>

<template>
  <div class="workbench-shell">
    <aside class="workspace-sidebar">
      <button type="button" class="brand-mark" @click="router.push({ name: 'home' })">
        <span class="brand-icon"><Van /></span>
        <span><b>校园配送</b><small>Campus Express</small></span>
      </button>

      <nav class="side-nav" :aria-label="`${roleName}功能`">
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeMenu === 'home' }"
          @click="router.push({ name: 'home' })"
        >
          <el-icon><HomeFilled /></el-icon><span>工作台</span>
        </button>

        <template v-if="isCustomer">
          <button
            type="button"
            class="nav-item"
            :class="{ active: activeMenu === 'orders-mine' }"
            @click="router.push({ name: 'orders-mine' })"
          >
            <el-icon><Tickets /></el-icon><span>我的订单</span>
          </button>
          <button
            type="button"
            class="nav-item"
            :class="{ active: activeMenu === 'order-create' }"
            @click="router.push({ name: 'order-create' })"
          >
            <el-icon><Plus /></el-icon><span>发布订单</span>
          </button>
        </template>

        <template v-else>
          <button
            type="button"
            class="nav-item"
            :class="{ active: activeMenu === 'orders-available' }"
            @click="router.push({ name: 'orders-available' })"
          >
            <el-icon><Collection /></el-icon><span>接单大厅</span>
          </button>
          <button
            type="button"
            class="nav-item"
            :class="{ active: activeMenu === 'orders-assigned' }"
            @click="router.push({ name: 'orders-assigned' })"
          >
            <el-icon><Tickets /></el-icon><span>我的配送</span>
          </button>
        </template>

        <button
          type="button"
          class="nav-item"
          :class="{ active: activeMenu === 'profile' }"
          @click="router.push({ name: 'profile' })"
        >
          <el-icon><User /></el-icon><span>个人中心</span>
        </button>
      </nav>

      <div class="account">
        <el-image
          :key="auth.profile?.avatar || 'avatar-empty'"
          :src="imageUrl(auth.profile?.avatar)"
          fit="cover"
          class="avatar"
        >
          <template #error>
            <span class="avatar-fallback">{{ auth.username.charAt(0) || '用' }}</span>
          </template>
        </el-image>
        <span class="account-text">
          <b>{{ auth.username }}</b>
          <small>{{ roleName }}</small>
        </span>
        <el-button text circle aria-label="退出登录" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>

    <main class="workspace-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.workbench-shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  min-height: 100vh;
  background: #f3f6fa;
}

.workspace-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 28px 18px 20px;
  color: #d9e7f4;
  background: linear-gradient(180deg, #071a2d 0%, #0b2943 100%);
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px 28px;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.brand-mark > span:last-child,
.account-text {
  display: flex;
  flex-direction: column;
}

.brand-mark b {
  color: #fff;
  font-size: 17px;
}

.brand-mark small,
.account-text small {
  margin-top: 3px;
  color: #7896af;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.brand-icon,
.avatar {
  display: grid;
  flex: 0 0 auto;
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

.side-nav {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  color: #9db3c7;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 10px;
  transition: 0.2s ease;
}

.nav-item:hover,
.nav-item.active {
  color: #fff;
  background: rgba(64, 158, 255, 0.18);
}

.account {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.avatar {
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
  min-width: 0;
}

.account-text b {
  overflow: hidden;
  color: #fff;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account .el-button {
  margin-left: auto;
  color: #8ea9bf;
}

.workspace-content {
  min-width: 0;
  padding: clamp(26px, 4vw, 52px);
}

@media (max-width: 720px) {
  .workbench-shell {
    grid-template-columns: 1fr;
  }

  .workspace-sidebar {
    position: static;
    height: auto;
    padding: 18px;
  }

  .brand-mark {
    padding-bottom: 16px;
  }

  .side-nav {
    flex-direction: row;
    overflow-x: auto;
  }

  .nav-item {
    justify-content: center;
    min-width: 116px;
  }

  .account {
    display: none;
  }

  .workspace-content {
    padding: 24px 16px;
  }
}
</style>
