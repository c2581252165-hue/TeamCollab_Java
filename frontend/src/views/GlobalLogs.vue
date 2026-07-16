<template>
  <div class="h-full flex flex-col animate-fade-in w-full px-6 mx-auto" v-if="authStore.user?.current_project_id">
    <div class="mb-8">
      <h2 class="text-3xl font-extrabold text-slate-800 tracking-tight">团队动态与操作日志</h2>
      <p class="text-slate-500 text-sm mt-1">全局时间轴，精准追溯全员协作轨迹、增删改查及积分流转动态。</p>
    </div>

    <div class="bg-white p-8 rounded-2xl shadow-[0_2px_12px_rgba(0,0,0,0.04)] border border-slate-100 flex-1 flex flex-col overflow-auto custom-scrollbar">
      <el-timeline v-if="logs.length" class="mt-4">
        <el-timeline-item
          v-for="log in logs"
          :key="log.id"
          :type="getLogType(log.action)"
          :icon="getLogIcon(log.action)"
          :color="getLogColor(log.action)"
          size="large"
          :timestamp="formatDate(log.timestamp)"
          placement="top"
        >
          <div class="bg-slate-50 border border-slate-100 p-4 rounded-xl relative overflow-hidden group hover:border-blue-200 transition-colors">
            <div class="absolute left-0 top-0 bottom-0 w-1" :style="{ backgroundColor: getLogColor(log.action) || '#409EFF' }"></div>
            <div class="flex items-center text-sm">
              <strong class="text-slate-800 mr-2">{{ log.username }}</strong>
              <span class="text-slate-600">{{ log.details }}</span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无全局日志记录"></el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../utils/api'
import dayjs from 'dayjs'
import { Plus, Edit, Delete, Check, ChatLineRound, StarFilled, DocumentAdd, RefreshLeft } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const logs = ref([])

const loadLogs = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get(`/projects/${authStore.user.current_project_id}/logs`)
    logs.value = res.data
  } catch (error) {}
}

const formatDate = (dateStr) => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

const getLogType = (action) => {
  const map = {
    'create': 'success',
    'delete': 'danger',
    'points': 'warning',
    'complete': 'success',
    'edit': 'primary',
    'standup': 'info',
    'restore': 'warning'
  }
  return map[action] || 'primary'
}

const getLogColor = (action) => {
  const map = {
    'points': '#9333ea', // purple
    'delete': '#ef4444', // red
    'complete': '#10b981', // green
    'create': '#3b82f6', // blue
    'standup': '#8b5cf6', // violet
    'restore': '#f59e0b', // orange
    'update_status': '#06b6d4' // cyan
  }
  return map[action]
}

const getLogIcon = (action) => {
  const map = {
    'create': DocumentAdd,
    'edit': Edit,
    'update_status': Edit,
    'delete': Delete,
    'complete': Check,
    'points': StarFilled,
    'standup': ChatLineRound,
    'restore': RefreshLeft
  }
  return map[action] || Plus
}

onMounted(() => {
  loadLogs()
})
</script>
