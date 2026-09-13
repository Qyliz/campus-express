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
  <div class="workbench-shell app-shell">
    <aside class="shell-aside workspace-sidebar">
      <button type="button" class="brand" @click="router.push({ name: 'home' })">
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
        <button
          type="button"
          class="account-link"
          title="进入个人中心"
          @click="router.push({ name: 'profile' })"
        >
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
        </button>
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
/* 品牌区 / 账户区 / 头像的共性样式在 styles/workspace-shell.css（.app-shell 命名空间），
   这里只保留用户工作台自己的部分：布局网格、导航按钮、内容区。 */
.workbench-shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  min-height: 100vh;
  background: #f3f6fa;
}

.workspace-sidebar {
  padding: 28px 18px 20px;
  color: #d9e7f4;
}

.brand {
  padding: 0 8px 28px;
  color: inherit;
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
  /* 导航是 li/button 之外也禁止选中：触屏长按会拉出浅色选区 */
  -webkit-user-select: none;
  user-select: none;
  /* 触屏点按默认的半透明高亮块在深色导航上像一块色斑 */
  -webkit-tap-highlight-color: transparent;
}

.nav-item:hover,
.nav-item.active {
  color: #fff;
  background: rgba(64, 158, 255, 0.18);
}

.account .el-button {
  margin-left: auto;
}

.workspace-content {
  min-width: 0;
  padding: clamp(26px, 4vw, 52px);
}

@media (max-width: 720px) {
  .workbench-shell {
    /* 1fr 的隐式最小宽度是 auto，会被宽表格撑大整页；0 才能把宽度锁在视口内 */
    grid-template-columns: minmax(0, 1fr);
  }

  .workspace-sidebar {
    position: static;
    height: auto;
    padding: 18px;
  }

  .brand {
    padding-bottom: 16px;
  }

  .side-nav {
    /* 移动端不需要撑满侧栏，固定内容高度，杜绝任何瞬态拉伸 */
    flex: 0 0 auto;
    flex-direction: row;
    align-items: center;
    overflow-x: auto;
    overflow-y: hidden;
    /* 触屏上横滑即可，露出的滚动条在深色底上像一块色斑 */
    scrollbar-width: none;
  }

  .side-nav::-webkit-scrollbar {
    display: none;
  }

  .nav-item {
    justify-content: center;
    width: auto;
    min-width: 116px;
    height: 45px;
    /* 压缩后文字空间不足会折行，把导航行撑高 */
    white-space: nowrap;
  }

  .account {
    padding: 14px 8px 0;
  }

  .workspace-content {
    padding: 24px 16px;
    /* 宽表格在内容区内部横向滚动，不允许撑宽整页连带侧栏重排 */
    overflow-x: auto;
  }
}
</style>
