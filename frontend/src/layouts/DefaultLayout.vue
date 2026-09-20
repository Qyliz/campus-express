<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'

//游客首页背景图
import heroImage from '@/assets/images/campus-delivery-hero.webp'
import { useAuthStore } from '@/stores/auth'

//用户工作台异步加载，避免进入游客首页时下载
const UserWorkspaceLayout = defineAsyncComponent(() => import('@/layouts/UserWorkspaceLayout.vue'))
const HomeView = defineAsyncComponent(() => import('@/views/HomeView.vue'))

const auth = useAuthStore()
const route = useRoute()

const guestBackdrop = computed(
  () =>
    !auth.isLoggedIn &&
    (route.meta.requiresAuth ||
      route.name === 'home' ||
      route.name === 'login' ||
      route.name === 'register' ||
      route.name === 'forgot-password'),
)
const backdropStyle = computed(() =>
  guestBackdrop.value
    ? {
        backgroundImage: `linear-gradient(90deg, rgba(4, 17, 32, 0.48), rgba(4, 17, 32, 0.12)), url(${heroImage})`,
      }
    : undefined,
)
</script>

<template>
  <UserWorkspaceLayout v-if="auth.isLoggedIn && !auth.isAdmin" />

  <el-container v-else class="shell">
    <el-main
      class="main"
      :class="{
        'main--landing':
          !auth.isLoggedIn && (route.name === 'home' || Boolean(route.meta.requiresAuth)),
        'main--auth':
          route.name === 'login' || route.name === 'register' || route.name === 'forgot-password',
      }"
      :style="backdropStyle"
    >
      <HomeView v-if="!auth.isLoggedIn && route.meta.requiresAuth" />
      <RouterView v-else />
    </el-main>
  </el-container>
</template>

<style scoped>
.shell {
  min-height: 100%;
}

.main {
  min-width: 0;
  padding: clamp(14px, 3vw, 24px);
}

.main--landing {
  padding: 0;
  background-position: center;
  background-size: cover;
}

.main--auth {
  min-height: 100vh;
  padding: clamp(14px, 4vw, 36px) clamp(12px, 3vw, 20px);
  overflow: auto;
  background-attachment: fixed;
  background-position: center;
  background-size: cover;
}
</style>
