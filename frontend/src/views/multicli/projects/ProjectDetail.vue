<template>
  <div class="h-full space-y-6 animate-in pb-8" v-loading="loading">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">{{ project?.name || '项目详情' }}</h2>
      <el-button plain @click="$router.push('/multicli/projects')">返回列表</el-button>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden" v-if="project">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
        <h3 class="text-base font-semibold text-zinc-900">基本信息</h3>
      </div>
      <div class="p-6">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编号">{{ project.projectNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ project.status }}</el-descriptions-item>
          <el-descriptions-item label="本地路径">{{ project.localPath }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ project.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
        <h3 class="text-base font-semibold text-zinc-900">任务列表</h3>
      </div>
      <el-table :data="tasks" style="width: 100%" :header-cell-style="{ background: '#fafafa', color: '#52525b', fontWeight: '500', borderBottom: '1px solid #e4e4e7' }">
        <el-table-column prop="taskNo" label="编号" width="180" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <span class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-zinc-100 text-zinc-800 border border-zinc-200">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="right">
          <template #default="{row}">
            <el-button size="small" plain @click="$router.push(`/multicli/tasks/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../../utils/api'
const route = useRoute(); const project = ref(null); const loading = ref(true); const tasks = ref([])
onMounted(async () => {
  try { const r = await api.get(`/projects/${route.params.id}`); project.value = r.data } catch(e) { console.error(e) }
  try { const r = await api.get(`/tasks?projectId=${route.params.id}`); tasks.value = r.data.records } catch(e) { console.error(e) }
  loading.value = false
})
</script>
