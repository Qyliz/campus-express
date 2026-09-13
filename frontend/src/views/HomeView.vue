<script setup lang="ts">
import { defineAsyncComponent } from 'vue'

import { useAuthStore } from '@/stores/auth'

// 三个组件都不小且游客首屏只需要登录区：异步引用让它们留在各自的路由 chunk 里，
// 首页真正渲染到哪块才下载哪块（登录成功后才会渲染 Dashboard）。
const LoginView = defineAsyncComponent(() => import('@/views/LoginView.vue'))
const CustomerDashboard = defineAsyncComponent(() => import('@/views/customer/CustomerDashboard.vue'))
const CourierDashboard = defineAsyncComponent(() => import('@/views/courier/CourierDashboard.vue'))

const auth = useAuthStore()
</script>

<template>
  <section v-if="!auth.isLoggedIn" class="landing">
    <div class="login-zone">
      <LoginView embedded />
    </div>

    <div class="introduction">
      <p class="eyebrow">Campus Express</p>
      <h1>校园配送平台</h1>
      <p class="summary">
        连接校园里的每一次托付，让取件、寄件与配送更简单。在线发布需求、实时掌握订单进度，安全高效地送达校园每个角落。
      </p>
      <div class="features" aria-label="平台特点">
        <span>校内直达</span>
        <span>进度透明</span>
        <span>服务可靠</span>
      </div>
    </div>
  </section>

  <CustomerDashboard v-else-if="auth.isCustomer" />
  <CourierDashboard v-else-if="auth.isCourier" />
</template>

<style scoped>
.landing {
  position: relative;
  display: grid;
  grid-template-columns: minmax(390px, 42%) 1fr;
  min-height: 100vh;
  overflow: hidden;
  background-position: center;
  background-size: cover;
  isolation: isolate;
}

.landing::after {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(5, 17, 33, 0.62) 0%, rgba(5, 17, 33, 0.18) 50%, transparent 75%),
    linear-gradient(0deg, rgba(5, 17, 33, 0.24), transparent 55%);
  content: '';
}

.login-zone {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 48px clamp(28px, 4vw, 72px);
  background: linear-gradient(90deg, rgba(5, 16, 31, 0.78), rgba(5, 16, 31, 0.38));
  border-right: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(2px);
}

.introduction {
  align-self: center;
  max-width: 720px;
  margin: 0 clamp(40px, 7vw, 112px);
  padding: clamp(28px, 4vw, 52px);
  color: #fff;
  background: linear-gradient(135deg, rgba(4, 22, 41, 0.78), rgba(4, 22, 41, 0.38));
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 28px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(10px);
}

.eyebrow {
  margin: 0 0 14px;
  color: #a9d8ff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.introduction h1 {
  margin: 0;
  font-size: clamp(42px, 5vw, 72px);
  line-height: 1.12;
  letter-spacing: -0.04em;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.25);
}

.summary {
  max-width: 620px;
  margin: 24px 0 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: clamp(16px, 1.5vw, 20px);
  line-height: 1.9;
}

.features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 30px;
}

.features span {
  padding: 8px 14px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
}

@media (max-width: 900px) {
  .landing {
    grid-template-areas:
      'intro'
      'login';
    grid-template-columns: 1fr;
    padding: 34px 20px;
  }

  .login-zone {
    grid-area: login;
    min-height: auto;
    padding: 22px 0 0;
    background: transparent;
    border: 0;
    backdrop-filter: none;
  }

  .introduction {
    grid-area: intro;
    margin: 0 auto;
    padding: 26px;
  }
}

@media (max-width: 520px) {
  .landing {
    padding: 20px 14px 28px;
  }

  .introduction h1 {
    font-size: 38px;
  }

  .summary {
    margin-top: 16px;
    font-size: 15px;
    line-height: 1.75;
  }

  .features {
    margin-top: 20px;
  }
}
</style>
