<script setup lang="ts">
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { roleLabel } from '@/constants'

const auth = useAuthStore()
const router = useRouter()
</script>

<template>
  <div>
    <el-card shadow="never" class="hero">
      <h1>校园快递管理系统</h1>
      <p class="muted">
        面向校园场景的快递代收代寄平台：收寄件人下单、配送员取送、管理员审核与账号治理。
      </p>

      <el-space wrap :size="12" class="entries">
        <template v-if="!auth.isLoggedIn">
          <el-button type="primary" size="large" @click="router.push({ name: 'login' })">
            登录
          </el-button>
          <el-button size="large" @click="router.push({ name: 'register' })">注册新账号</el-button>
        </template>
        <template v-else>
          <el-button
            v-if="auth.isCustomer"
            type="primary"
            size="large"
            @click="router.push({ name: 'order-create' })"
            >发布订单</el-button
          >
          <el-button
            v-if="auth.isCourier"
            type="primary"
            size="large"
            @click="router.push({ name: 'orders-available' })"
            >进入接单大厅</el-button
          >
          <el-button type="primary" size="large" @click="router.push({ name: 'profile' })">
            个人中心
          </el-button>
          <el-button v-if="auth.isAdmin" size="large" @click="router.push({ name: 'admin-users' })">
            进入管理后台
          </el-button>
        </template>
      </el-space>

      <p v-if="auth.isLoggedIn" class="muted current">
        当前登录身份：<b>{{ auth.role ? roleLabel[auth.role] : '' }}</b
        >（{{ auth.username }}）
      </p>
    </el-card>

    <el-row :gutter="16" class="rules">
      <el-col :xs="24" :sm="8">
        <el-card shadow="never">
          <template #header>一人可有多个角色</template>
          <p class="muted">
            同一个手机号 / 邮箱可以同时是收寄件人、配送员甚至管理员。用<b>相同的账号 + 相同的密码</b
            >再注册一次，就能给已有账号追加一个新角色，资料（用户名、头像、性别）全账号共用一份。
          </p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never">
          <template #header>同时只有一个在线会话</template>
          <p class="muted">
            登录时必须选择本次使用的身份。切换身份要先登出再重新登录；在别处登录同一账号，会把当前会话顶下线（提示「您的账号在其他设备登录」）。
          </p>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never">
          <template #header>配送员需要审核</template>
          <p class="muted">
            注册配送员时必须上传身份证明材料，提交后账号进入「审核中」，<b>此时无法登录</b>。管理员审核通过后才能正常使用；被驳回则会看到驳回原因。
          </p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #303133;
}

.hero p {
  margin: 0 0 16px;
  line-height: 1.7;
}

.entries {
  margin-bottom: 8px;
}

.current {
  margin-top: 12px;
  margin-bottom: 0;
  font-size: 13px;
}

.rules {
  margin-top: 16px;
}

.rules p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
}
</style>
