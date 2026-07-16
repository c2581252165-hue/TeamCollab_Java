<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">MultiCLI Hub 控制台</h2>
    </div>
    
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm font-medium text-zinc-500">项目数</span>
          <el-icon class="text-zinc-400"><Folder /></el-icon>
        </div>
        <div class="text-3xl font-semibold text-zinc-900">{{ stats.projects }}</div>
      </div>
      <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm font-medium text-zinc-500">任务数</span>
          <el-icon class="text-zinc-400"><List /></el-icon>
        </div>
        <div class="text-3xl font-semibold text-zinc-900">{{ stats.tasks }}</div>
      </div>
      <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm font-medium text-zinc-500">待审批</span>
          <el-icon class="text-zinc-400"><Check /></el-icon>
        </div>
        <div class="text-3xl font-semibold text-zinc-900">{{ stats.approvals }}</div>
      </div>
      <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm font-medium text-zinc-500">活跃 Worker</span>
          <el-icon class="text-zinc-400"><User /></el-icon>
        </div>
        <div class="text-3xl font-semibold text-zinc-900">{{ stats.workers }}</div>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
        <h3 class="text-base font-semibold text-zinc-900">系统信息</h3>
      </div>
      <div class="p-6 space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-center py-3 border-b border-zinc-100 last:border-0">
          <span class="text-sm font-medium text-zinc-500 w-32">主导智能体</span>
          <span class="text-sm text-zinc-900">Antigravity Supervisor</span>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center py-3 border-b border-zinc-100 last:border-0">
          <span class="text-sm font-medium text-zinc-500 w-32">可用 Worker</span>
          <div class="flex items-center space-x-2">
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Claude Code (ACTIVE)</span>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">OpenCode (UNVERIFIED)</span>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-zinc-100 text-zinc-800">Gemini (INACTIVE)</span>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Mock (ACTIVE)</span>
          </div>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center py-3 border-b border-zinc-100 last:border-0">
          <span class="text-sm font-medium text-zinc-500 w-32">执行引擎</span>
          <span class="text-sm text-zinc-900">默认Python引擎 (8766)</span>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center py-3 border-b border-zinc-100 last:border-0">
          <span class="text-sm font-medium text-zinc-500 w-32">Codex</span>
          <span class="text-sm text-zinc-500">不接入本项目</span>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { reactive, onMounted } from 'vue'
import api from '../../utils/api'
const stats = reactive({ projects: 0, tasks: 0, approvals: 0, workers: 4 })
onMounted(async () => {
  try { const p = await api.get('/projects?size=1'); stats.projects = p.data.total } catch (e) { console.error(e) }
  try { const t = await api.get('/tasks?size=1'); stats.tasks = t.data.total } catch (e) { console.error(e) }
  try { const a = await api.get('/approvals?size=1'); stats.approvals = a.data.total } catch (e) { console.error(e) }
})
</script>
