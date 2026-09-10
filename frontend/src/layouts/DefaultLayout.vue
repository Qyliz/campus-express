<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import { roleLabel, roleTagType } from '@/constants'
import { imageUrl } from '@/utils/image'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

/** 这条说明可以关掉，关掉后本次浏览期间不再出现 */
const showRoleTip = ref(true)

const roleText = computed(() => (auth.role ? roleLabel[auth.role] : ''))
const roleType = computed(() => (auth.role ? roleTagType[auth.role] : 'info'))
const initial = computed(() => auth.username.charAt(0) || 'U')

/** 头像菜单提供个人中心、管理后台和登出入口。 */
async function onCommand(command: string) {
  if (command === 'profile') {
    router.push({ name: 'profile' })
  } else if (command === 'admin') {
    router.push({ name: 'admin-users' })
  } else if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已登出')
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <el-container class="shell">
    <el-header class="header">
      <div class="brand" @click="router.push({ name: 'home' })">校园快递</div>

      <el-menu
        mode="horizontal"
        :router="true"
        :default-active="route.path"
        class="nav"
        :ellipsis="false"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item v-if="auth.isCustomer" index="/order/mine">我的订单</el-menu-item>
        <el-menu-item v-if="auth.isCustomer" index="/order/create">发布订单</el-menu-item>
        <el-menu-item v-if="auth.isCourier" index="/order/available">接单大厅</el-menu-item>
        <el-menu-item v-if="auth.isCourier" index="/order/assigned">我的配送</el-menu-item>
        <el-menu-item v-if="auth.isLoggedIn" index="/profile">个人中心</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/admin/users">管理后台</el-menu-item>
      </el-menu>

      <div class="spacer" />

      <el-dropdown v-if="auth.isLoggedIn" @command="onCommand">
        <span class="user-chip">
          <el-image :src="imageUrl(auth.profile?.avatar)" fit="cover" class="avatar">
            <!-- /upload/** 在 Sa-Token 拦截器后面，会话过期时图片会 401，
                 所以必须有兜底，否则顶栏会出现碎图标 -->
            <template #error>
              <span class="avatar-fallback">{{ initial }}</span>
            </template>
          </el-image>
          <span class="name">{{ auth.username }}</span>
          <el-tag :type="roleType" size="small" effect="plain">{{ roleText }}</el-tag>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item v-if="auth.isAdmin" command="admin">管理后台</el-dropdown-item>
            <el-dropdown-item command="logout" divided>登出</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <template v-else>
        <el-button text @click="router.push({ name: 'login' })">登录</el-button>
        <el-button type="primary" @click="router.push({ name: 'register' })">注册</el-button>
      </template>
    </el-header>

    <el-main class="main">
      <el-alert
        v-if="auth.isLoggedIn && showRoleTip"
        type="info"
        show-icon
        closable
        class="role-tip"
        @close="showRoleTip = false"
      >
        <template #title>
          当前身份：<b>{{ roleText }}</b
          >。一个账号可以同时拥有多个角色，但同一时刻只能有一个在线会话。要切换身份，请先登出，再用另一个角色登录。
        </template>
      </el-alert>

      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.shell {
  min-height: 100%;
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.brand {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  cursor: pointer;
  white-space: nowrap;
}

.nav {
  border-bottom: none;
}

.spacer {
  flex: 1;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f0f2f5;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 14px;
  color: #909399;
}

.name {
  font-size: 14px;
  color: #303133;
}

.main {
  padding: 24px;
}

.role-tip {
  margin-bottom: 16px;
}
</style>
