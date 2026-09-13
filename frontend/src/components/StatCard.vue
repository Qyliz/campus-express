<script setup lang="ts">
// 三个工作台（Admin / Customer / Courier）共用的统计卡片。
// 图标走默认插槽（Element Plus 图标组件或任意元素），点击等事件透传到根元素。
withDefaults(
  defineProps<{
    label: string
    value: number | string
    /** 图标区的配色，对应各工作台原来的 blue/amber/violet/red/green 变体 */
    tone?: 'blue' | 'amber' | 'violet' | 'red' | 'green'
  }>(),
  { tone: 'blue' },
)
</script>

<template>
  <article class="stat-card" :class="tone">
    <span class="stat-icon"><slot /></span>
    <div>
      <small>{{ label }}</small>
      <strong>{{ value }}</strong>
    </div>
  </article>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 118px;
  padding: 22px;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--ce-border);
  border-radius: 16px;
  box-shadow: var(--ce-shadow-card);
  transition: 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ce-shadow-card-hover);
}

/* 图标尺寸走 font-size：el-icon 内部是 1em 的 svg，文字图标（如 ¥）也按同一字号对齐 */
.stat-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 48px;
  height: 48px;
  font-size: 23px;
  border-radius: 14px;
}

.stat-card.blue .stat-icon {
  color: #287dcc;
  background: #e9f4ff;
}
.stat-card.amber .stat-icon {
  color: #d48816;
  background: #fff4dd;
}
.stat-card.violet .stat-icon {
  color: #7356c9;
  background: #f0ecff;
}
.stat-card.red .stat-icon {
  color: #d95656;
  background: #ffeded;
}
.stat-card.green .stat-icon {
  color: #32966a;
  background: #e7f8f0;
}

.stat-card div:last-child {
  display: flex;
  flex-direction: column;
}

.stat-card small {
  color: var(--ce-text-secondary);
  font-size: 13px;
}
.stat-card strong {
  margin-top: 6px;
  color: var(--ce-text-strong);
  font-size: 28px;
}

@media (max-width: 480px) {
  .stat-card {
    min-height: 98px;
    padding: 14px;
  }
  .stat-icon {
    width: 40px;
    height: 40px;
  }
}
</style>
