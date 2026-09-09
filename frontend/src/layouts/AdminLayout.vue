<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, DocumentChecked, Lock, User } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import { imageUrl } from '@/utils/image'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const initial = computed(() => auth.username.charAt(0) || 'A')

async function onCommand(command: string) {
  if (command === 'home') {
    router.push({ name: 'home' })
  } else if (command === 'profile') {
    router.push({ name: 'profile' })
  } else if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已登出')
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="220px" class="aside">
      <div class="brand" @click="router.push({ name: 'home' })">校园快递 · 管理后台</div>

      <!-- :router="true" + index 等于路由路径 ⇒ default-active 自动跟随 URL，不用写点击处理 -->
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#b7bcc7"
        active-text-color="#ffffff"
        class="menu"
      >
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/audits">
          <el-icon><DocumentChecked /></el-icon>
          <span>审核管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/bans">
          <el-icon><Lock /></el-icon>
          <span>封禁记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="spacer" />

        <el-dropdown @command="onCommand">
          <span class="user-chip">
            <el-image :src="imageUrl(auth.profile?.avatar)" fit="cover" class="avatar">
              <!-- /upload/** 也在登录拦截器后面，会话过期时图片会 401，必须有兜底 -->
              <template #error>
                <span class="avatar-fallback">{{ initial }}</span>
              </template>
            </el-image>
            <span class="name">{{ auth.username }}</span>
            <el-tag type="danger" size="small" effect="plain">管理员</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="home">返回前台</el-dropdown-item>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>登出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell {
  height: 100%;
}

.aside {
  background: #001529;
}

.brand {
  height: 60px;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
  line-height: 60px;
  color: #fff;
  cursor: pointer;
  background: #002140;
}

.menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
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
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f0f2f5;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 13px;
  color: #909399;
}

.name {
  font-size: 14px;
  color: #303133;
}

.main {
  padding: 20px;
  background: #f5f7fa;
}
</style>
