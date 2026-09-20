<script setup lang="ts">
//工作台共用的统计卡片
withDefaults(
  defineProps<{
    label: string
    value: number | string
    //图标区配色
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
  gap: clamp(10px, 2vw, 16px);
  min-height: clamp(98px, 10vw, 118px);
  padding: clamp(14px, 2.2vw, 22px);
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

/* 图标和文字符号共用字号 */
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
  min-width: 0;
  flex-direction: column;
}

.stat-card small {
  color: var(--ce-text-secondary);
  font-size: 13px;
}
.stat-card strong {
  margin-top: 6px;
  color: var(--ce-text-strong);
  font-size: clamp(22px, 3vw, 28px);
  overflow-wrap: anywhere;
}
</style>
