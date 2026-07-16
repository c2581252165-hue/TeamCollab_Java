<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">Worker 管理</h2>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <el-table :data="list" style="width: 100%" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#52525b', fontWeight: '500', borderBottom: '1px solid #e4e4e7' }">
        <el-table-column prop="workerCode" label="编码" />
        <el-table-column prop="workerName" label="名称" />
        <el-table-column prop="agentRole" label="角色" />
        <el-table-column prop="status" label="状态">
          <template #default="{row}">
            <span class="flex items-center space-x-1.5">
              <span class="w-1.5 h-1.5 rounded-full" :class="row.status==='ACTIVE'?'bg-green-500':row.status==='INACTIVE'?'bg-zinc-400':'bg-yellow-500'"></span>
              <span class="text-xs font-medium text-zinc-700">{{ row.status }}</span>
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../../utils/api'
const list = ref([]); const loading = ref(false)
onMounted(async () => { loading.value = true; try { const r = await api.get('/admin/workers'); list.value = r.data.records; } catch(e) { console.error(e) } loading.value = false })
</script>
