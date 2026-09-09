<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<template>
  <!-- 按需导入模式下没有 app.use(ElementPlus)，所以中文语言包只能通过 el-config-provider 注入，
       否则组件内置文案会是英文：分页器显示 "Total / Go to"、空表格显示 "No Data"。 -->
  <!-- 所有页面外壳都在 layout 组件里，而 layout 是路由组件；
       路由守卫 await 了登录态探测，所以首屏渲染时状态已就绪，不会闪一下「未登录」。 -->
  <el-config-provider :locale="zhCn">
    <RouterView />
  </el-config-provider>
</template>
