<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">项目列表</h2>
      <el-button type="primary" @click="$router.push('/multicli/projects/create')" class="!bg-zinc-900 !border-none hover:!bg-zinc-800 transition-colors">
        <el-icon class="mr-1"><Plus /></el-icon> 创建项目
      </el-button>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <el-table :data="projects" style="width: 100%" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#52525b', fontWeight: '500', borderBottom: '1px solid #e4e4e7' }">
        <el-table-column prop="projectNo" label="编号" width="180" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <span :class="row.status === 'ARCHIVED' ? 'bg-zinc-100 text-zinc-600' : 'bg-green-100 text-green-700'" class="px-2.5 py-0.5 rounded text-xs font-medium border border-transparent">
              {{ row.status === 'ARCHIVED' ? '已归档' : '活跃' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="$router.push(`/multicli/projects/${row.id}`)">详情</el-button>
            <el-button size="small" plain @click="toggleArchive(row)">{{ row.status === 'ARCHIVED' ? '恢复' : '归档' }}</el-button>
            <el-button size="small" type="danger" plain @click="del(row.id)">删除</el-button>
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
const projects = ref([]); const loading = ref(false)
onMounted(() => fetchProjects())
async function fetchProjects() { loading.value = true; try { const r = await api.get('/projects'); projects.value = r.data.records; } catch (e) { console.error(e) } loading.value = false }
async function del(id) { await api.delete(`/projects/${id}`); ElMessage.success('已删除'); fetchProjects() }
async function toggleArchive(row) {
  const url = row.status === 'ARCHIVED' ? `/projects/${row.id}/restore` : `/projects/${row.id}/archive`
  await api.post(url); ElMessage.success('操作成功'); fetchProjects()
}
</script>
