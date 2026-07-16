<template>
  <div class="h-full flex flex-col animate-fade-in max-w-6xl mx-auto" v-if="authStore.user?.current_project_id">
    <div class="mb-8">
      <h2 class="text-3xl font-extrabold text-slate-800 tracking-tight">项目甘特图</h2>
      <p class="text-slate-500 text-sm mt-1">全局视图查看项目所有任务的时间跨度与进度安排。</p>
    </div>

    <div class="bg-white p-6 rounded-2xl shadow-[0_2px_12px_rgba(0,0,0,0.04)] border border-slate-100 flex-1 flex flex-col overflow-hidden">
      <!-- Simple CSS Gantt Chart -->
      <div v-if="tasks.length" class="flex-1 overflow-auto custom-scrollbar relative">
        <div class="min-w-[800px]">
          <!-- Header -->
          <div class="flex border-b border-slate-200 pb-2 mb-4 sticky top-0 bg-white z-10">
            <div class="w-48 font-bold text-slate-500 text-sm flex-shrink-0">任务名称</div>
            <div class="flex-1 flex justify-between px-2">
              <span class="text-xs font-bold text-slate-400">项目起点 ({{ projectStart }})</span>
              <span class="text-xs font-bold text-slate-400">目前阶段/最晚截止 ({{ projectEnd }})</span>
            </div>
          </div>

          <!-- Rows -->
          <div class="space-y-4">
            <div v-for="task in sortedTasks" :key="task.id" class="flex items-center group">
              <div class="w-48 pr-4 flex-shrink-0">
                <div class="text-sm font-bold text-slate-700 truncate group-hover:text-blue-600" :title="task.title">{{ task.title }}</div>
                <div class="text-xs text-slate-400 mt-1 flex items-center">
                  <span :class="task.status === 'done' ? 'text-emerald-500' : 'text-blue-500'">●</span>
                  <span class="ml-1">{{ getStatusName(task.status) }}</span>
                </div>
              </div>
              <div class="flex-1 bg-slate-50 h-8 rounded-lg relative overflow-hidden flex items-center">
                <!-- Grid lines -->
                <div class="absolute inset-0 flex justify-between opacity-10 pointer-events-none">
                  <div class="border-l border-slate-900 h-full"></div>
                  <div class="border-l border-slate-900 h-full"></div>
                  <div class="border-l border-slate-900 h-full"></div>
                  <div class="border-l border-slate-900 h-full"></div>
                </div>
                
                <!-- Bar -->
                <el-tooltip :content="`创建: ${formatDate(task.created_at)} | 截止: ${formatDate(task.deadline)}`" placement="top">
                  <div 
                    :class="['h-6 rounded-md shadow-sm transition-all duration-300 flex items-center justify-center text-[10px] text-white font-bold px-2 cursor-pointer', task.status === 'done' ? 'bg-emerald-400' : (isOverdue(task.deadline) ? 'bg-red-400' : 'bg-blue-400')]"
                    :style="{ 
                      left: getLeftPercent(task) + '%', 
                      width: Math.max(getWidthPercent(task), 5) + '%',
                      position: 'absolute'
                    }"
                  >
                    <span v-if="Math.max(getWidthPercent(task), 5) > 10" class="truncate">{{ formatDate(task.deadline, 'MM-DD') }}</span>
                  </div>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无任务，快去看板创建一些带截止日期的任务吧！"></el-empty>
    </div>
  </div>
  
  <div v-else class="text-center py-20 animate-fade-in">
    <h2 class="text-2xl font-bold text-slate-700">您尚未加入任何团队</h2>
    <p class="text-slate-500 mt-2">请先在团队切换界面加入或创建一个团队。</p>
    <el-button type="primary" class="mt-6" @click="$router.push('/teams')">去切换/加入团队</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../utils/api'
import dayjs from 'dayjs'

const authStore = useAuthStore()
const tasks = ref([])

const loadTasks = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get(`/tasks/?project_id=${authStore.user.current_project_id}`)
    // only show tasks that have a deadline or we fallback to +7 days
    tasks.value = res.data
  } catch (error) {}
}

onMounted(() => {
  loadTasks()
})

const getStatusName = (status) => {
  const map = { 'todo': '待办', 'in_progress': '进行中', 'review': '待审核', 'done': '已完成' }
  return map[status] || status
}

const isOverdue = (dateStr) => {
  if (!dateStr) return false
  return dayjs().isAfter(dayjs(dateStr))
}

const formatDate = (dateStr, fmt = 'YYYY-MM-DD') => {
  if (!dateStr) return '未设置'
  return dayjs(dateStr).format(fmt)
}

// Compute global start and end for the project view
const globalStart = computed(() => {
  if (!tasks.value.length) return dayjs()
  const dates = tasks.value.map(t => dayjs(t.created_at).valueOf())
  return dayjs(Math.min(...dates)).subtract(1, 'day')
})

const globalEnd = computed(() => {
  if (!tasks.value.length) return dayjs().add(7, 'day')
  const dates = tasks.value.map(t => t.deadline ? dayjs(t.deadline).valueOf() : dayjs(t.created_at).add(7, 'day').valueOf())
  const maxD = dayjs(Math.max(...dates))
  return dayjs().isAfter(maxD) ? dayjs().add(1, 'day') : maxD.add(1, 'day')
})

const totalDuration = computed(() => Math.max(1, globalEnd.value.diff(globalStart.value, 'day')))

const projectStart = computed(() => globalStart.value.format('YYYY-MM-DD'))
const projectEnd = computed(() => globalEnd.value.format('YYYY-MM-DD'))

const sortedTasks = computed(() => {
  return [...tasks.value].sort((a, b) => dayjs(a.created_at).valueOf() - dayjs(b.created_at).valueOf())
})

const getLeftPercent = (task) => {
  const start = dayjs(task.created_at)
  const diff = start.diff(globalStart.value, 'day')
  return Math.max(0, (diff / totalDuration.value) * 100)
}

const getWidthPercent = (task) => {
  const start = dayjs(task.created_at)
  const end = task.deadline ? dayjs(task.deadline) : start.add(7, 'day')
  const dur = end.diff(start, 'day')
  return Math.min(100, (dur / totalDuration.value) * 100)
}
</script>
