<template>
  <div class="h-full flex flex-col animate-fade-in max-w-5xl mx-auto" v-if="authStore.user?.current_project_id">
    <div class="mb-8">
      <h2 class="text-3xl font-extrabold text-slate-800 tracking-tight">回收站</h2>
      <p class="text-slate-500 text-sm mt-1">查看和恢复已被删除的任务。</p>
    </div>

    <div class="bg-white p-6 rounded-2xl shadow-[0_2px_12px_rgba(0,0,0,0.04)] border border-slate-100 flex-1 flex flex-col">
      <el-table :data="deletedTasks" class="w-full custom-table" v-loading="loading">
        <el-table-column prop="title" label="任务名称" min-width="200">
          <template #default="{ row }">
            <div class="font-bold text-slate-700">{{ row.title }}</div>
            <div class="text-xs text-slate-400 truncate">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="phase" label="所属阶段" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ getPhaseName(row.phase) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分配给" width="120">
          <template #default="{ row }">
            <span class="text-slate-600">{{ row.assignee?.username || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" plain @click="restoreTask(row.id)">
              <el-icon class="mr-1"><RefreshLeft /></el-icon> 恢复
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="回收站为空"></el-empty>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const authStore = useAuthStore()
const deletedTasks = ref([])
const loading = ref(false)

const getPhaseName = (phase) => {
  const map = { 'requirement': '需求分析', 'design': '系统设计', 'coding': '编码实现', 'test': '测试验收', 'none': '无阶段' }
  return map[phase] || '无阶段'
}

const loadDeletedTasks = async () => {
  if (!authStore.user?.current_project_id) return
  loading.value = true
  try {
    const res = await api.get(`/tasks/deleted?project_id=${authStore.user.current_project_id}`)
    deletedTasks.value = res.data
  } catch (error) {
    ElMessage.error('加载回收站数据失败')
  } finally {
    loading.value = false
  }
}

const restoreTask = async (id) => {
  try {
    await api.post(`/tasks/${id}/restore`)
    ElMessage.success('任务已成功恢复！')
    loadDeletedTasks()
  } catch (error) {
    ElMessage.error('恢复失败')
  }
}

onMounted(() => {
  loadDeletedTasks()
})
</script>

<style scoped>
.custom-table :deep(th.el-table__cell) {
  background-color: #f8fafc;
  color: #64748b;
  font-weight: 600;
}
</style>
