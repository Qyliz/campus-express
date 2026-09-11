<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import heroImage from '@/assets/images/campus-delivery-hero.png'
import { useAuthStore } from '@/stores/auth'
import UserWorkspaceLayout from '@/layouts/UserWorkspaceLayout.vue'
import HomeView from '@/views/HomeView.vue'

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
  padding: 24px;
}

.main--landing {
  padding: 0;
  background-position: center;
  background-size: cover;
}

.main--auth {
  min-height: 100vh;
  padding: 36px 20px;
  overflow: auto;
  background-attachment: fixed;
  background-position: center;
  background-size: cover;
}
</style>
