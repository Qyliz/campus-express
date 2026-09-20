<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<template>
  <!-- 按需引入 Element Plus 时通过 ConfigProvider 注入中文语言包 -->
  <el-config-provider :locale="zhCn">
    <RouterView />
  </el-config-provider>
</template>
