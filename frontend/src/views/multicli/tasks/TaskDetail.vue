<template>
  <div class="h-full space-y-6 animate-in pb-8" v-loading="loading">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">任务详情</h2>
      <el-button plain @click="$router.push('/multicli/tasks')">返回列表</el-button>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden" v-if="task">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50 flex justify-between items-center">
        <h3 class="text-base font-semibold text-zinc-900">基本信息</h3>
        <div class="space-x-2">
          <el-button size="small" type="success" plain v-if="task?.status==='PENDING'" @click="submit">提交执行</el-button>
          <el-button size="small" type="warning" plain v-if="['RUNNING','SUBMITTING'].includes(task?.status)" @click="sync">同步事件</el-button>
          <el-button size="small" type="danger" plain v-if="['RUNNING','SUBMITTING'].includes(task?.status)" @click="cancel">取消</el-button>
        </div>
      </div>
      <div class="p-6">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编号">{{ task.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="px-2.5 py-0.5 rounded-full text-xs font-medium border border-transparent" :class="{
              'bg-zinc-100 text-zinc-600': task.status === 'PENDING' || task.status === 'CANCELLED',
              'bg-blue-100 text-blue-700': task.status === 'RUNNING' || task.status === 'SUBMITTING',
              'bg-green-100 text-green-700': task.status === 'COMPLETED',
              'bg-red-100 text-red-700': task.status === 'FAILED'
            }">
              {{ task.status }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="标题">{{ task.title }}</el-descriptions-item>
          <el-descriptions-item label="主导智能体">Antigravity Supervisor</el-descriptions-item>
          <el-descriptions-item label="目标" :span="2">{{ task.objective }}</el-descriptions-item>
          <el-descriptions-item label="Python执行ID">{{ task.pythonExecutionId || '未提交' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <!-- SSE Log -->
    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden" v-if="connecting || logs.length">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50 flex items-center justify-between">
        <h3 class="text-base font-semibold text-zinc-900">实时执行日志</h3>
        <div v-if="connecting" class="flex items-center space-x-2 text-xs text-blue-600 font-medium bg-blue-50 px-2 py-1 rounded">
          <span class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></span>
          <span>连接中...</span>
        </div>
      </div>
      <div class="p-0">
        <div class="log-container m-6" ref="logContainer">
          <div v-for="(log, i) in logs" :key="i" class="log-line flex items-start">
            <span class="log-seq shrink-0">[{{ log.sequence_no }}]</span>
            <span class="px-1.5 py-0.5 rounded text-[10px] font-bold mx-2 shrink-0 border" :class="{
              'bg-green-900 text-green-300 border-green-700': log.event_type?.includes('COMPLETED')||log.event_type?.includes('SUCCESS'),
              'bg-red-900 text-red-300 border-red-700': log.event_type?.includes('FAIL'),
              'bg-zinc-800 text-zinc-300 border-zinc-600': !log.event_type?.includes('COMPLETED')&&!log.event_type?.includes('SUCCESS')&&!log.event_type?.includes('FAIL')
            }">{{ log.event_type }}</span>
            <span class="log-msg text-zinc-300">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Submitter -->
    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
        <h3 class="text-base font-semibold text-zinc-900">执行历史</h3>
      </div>
      <el-table :data="executions" v-loading="execLoading" size="small" :header-cell-style="{ background: '#fafafa', color: '#52525b', fontWeight: '500', borderBottom: '1px solid #e4e4e7' }">
        <el-table-column prop="executionNo" label="编号" width="200" />
        <el-table-column prop="pythonExecutionId" label="Python ID" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="px-2 py-0.5 rounded-full text-[10px] font-medium bg-zinc-100 text-zinc-700 border border-zinc-200">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="attemptNumber" label="尝试次数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" />
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../../utils/api'
import { ElMessage } from 'element-plus'
const route = useRoute(); const task = ref(null); const loading = ref(true)
const logs = ref([]); const connecting = ref(false); const logContainer = ref(null)
const executions = ref([]); const execLoading = ref(false)

let eventSource = null; let lastSeq = -1

onMounted(async () => {
  await fetchTask()
  if (['RUNNING','SUBMITTING','PLANNING'].includes(task.value?.status)) startSSE()
})

async function fetchTask() { loading.value = true; try { const r = await api.get(`/tasks/${route.params.id}`); task.value = r.data; } catch(e){ console.error(e) } loading.value = false }
async function fetchExecutions() { execLoading.value = true; try { const r = await api.get(`/tasks/${route.params.id}/executions`); executions.value = r.data; } catch(e){ console.error(e) } execLoading.value = false }

function startSSE() {
  connecting.value = true
  const token = localStorage.getItem('token') // Use token instead of accessToken
  // Use fetch-based SSE for auth header support
  fetch(`http://localhost:8000/api/tasks/${route.params.id}/stream?afterSequence=${lastSeq}`, {
    headers: { Authorization: `Bearer ${token}` }
  }).then(response => {
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    function read() {
      reader.read().then(({ done, value }) => {
        if (done) { connecting.value = false; fetchTask(); fetchExecutions(); return }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const data = JSON.parse(line.substring(5).trim())
              if (data.sequence_no !== undefined) lastSeq = data.sequence_no
              logs.value.push(data)
              nextTick(() => { if (logContainer.value) logContainer.value.scrollTop = logContainer.value.scrollHeight })
            } catch (e) {}
          }
        }
        read()
      })
    }
    read()
  }).catch(() => { connecting.value = false })
}

async function submit() { await api.post(`/tasks/${route.params.id}/submit`); ElMessage.success('已提交'); fetchTask(); fetchExecutions(); startSSE() }
async function sync() { await api.post(`/tasks/${route.params.id}/sync`); ElMessage.success('已同步'); fetchTask() }
async function cancel() { await api.post(`/tasks/${route.params.id}/cancel`); ElMessage.success('已取消'); fetchTask() }
function statusType(s) { return { COMPLETED:'success', FAILED:'danger', CANCELLED:'info', PENDING:'warning', RUNNING:'primary', SUBMITTING:'primary' }[s]||'info' }
</script>
<style scoped>
.log-container { max-height:400px; overflow-y:auto; background:#1e1e1e; color:#d4d4d4; padding:12px; border-radius:4px; font-family:monospace; font-size:13px; }
.log-line { padding:2px 0; }
.log-seq { color:#888; margin-right:8px; }
.log-msg { margin-left:8px; }
</style>
