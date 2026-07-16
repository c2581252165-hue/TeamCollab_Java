<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">任务列表</h2>
      <el-button type="primary" @click="$router.push('/multicli/tasks/create')" class="!bg-zinc-900 !border-none hover:!bg-zinc-800 transition-colors">
        <el-icon class="mr-1"><Plus /></el-icon> 创建任务
      </el-button>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <el-table :data="tasks" style="width: 100%" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#52525b', fontWeight: '500', borderBottom: '1px solid #e4e4e7' }">
        <el-table-column prop="taskNo" label="编号" width="180" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <span class="px-2.5 py-0.5 rounded-full text-xs font-medium border border-transparent" :class="{
              'bg-zinc-100 text-zinc-600': row.status === 'PENDING' || row.status === 'CANCELLED',
              'bg-blue-100 text-blue-700': row.status === 'RUNNING' || row.status === 'SUBMITTING',
              'bg-green-100 text-green-700': row.status === 'COMPLETED',
              'bg-red-100 text-red-700': row.status === 'FAILED'
            }">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="$router.push(`/multicli/tasks/${row.id}`)">详情</el-button>
            <el-button size="small" type="success" plain v-if="row.status==='PENDING'" @click="submit(row.id)">提交</el-button>
            <el-button size="small" type="danger" plain v-if="['RUNNING','SUBMITTING'].includes(row.status)" @click="cancel(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../../utils/api'
import { ElMessage } from 'element-plus'
const tasks = ref([]); const loading = ref(false)
onMounted(() => fetchTasks())
async function fetchTasks() { loading.value = true; try { const r = await api.get('/tasks'); tasks.value = r.data.records; } catch (e) { console.error(e) } loading.value = false }
async function submit(id) { await api.post(`/tasks/${id}/submit`); ElMessage.success('已提交'); fetchTasks() }
async function cancel(id) { await api.post(`/tasks/${id}/cancel`); ElMessage.success('已取消'); fetchTasks() }
function statusType(s) { return { COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'info', PENDING: 'warning', RUNNING: 'primary' }[s] || 'info' }
</script>
