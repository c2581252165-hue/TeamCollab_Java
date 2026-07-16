<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">审批中心</h2>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
      <el-table :data="approvals" style="width: 100%" v-loading="loading" :header-cell-style="{ background: '#fafafa', color: '#52525b', fontWeight: '500', borderBottom: '1px solid #e4e4e7' }">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="riskLevel" label="风险" width="100">
          <template #default="{row}">
            <span class="px-2.5 py-0.5 rounded-full text-xs font-medium border border-transparent" :class="{
              'bg-red-100 text-red-700': row.riskLevel === 'HIGH',
              'bg-orange-100 text-orange-700': row.riskLevel === 'MEDIUM',
              'bg-zinc-100 text-zinc-600': row.riskLevel === 'LOW'
            }">
              {{ row.riskLevel }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{row}">
            <span class="px-2.5 py-0.5 rounded-full text-[10px] font-medium border border-zinc-200 bg-white text-zinc-700">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="right">
          <template #default="{row}">
            <el-button v-if="row.status==='PENDING'" size="small" type="success" plain @click="approve(row.id)">批准</el-button>
            <el-button v-if="row.status==='PENDING'" size="small" type="danger" plain @click="reject(row.id)">拒绝</el-button>
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
const approvals = ref([]); const loading = ref(false)
onMounted(() => fetchApprovals())
async function fetchApprovals() { loading.value = true; try { const r = await api.get('/approvals'); approvals.value = r.data.records; } catch(e){ console.error(e) } loading.value = false }
async function approve(id) { await api.post(`/approvals/${id}/approve`, { comment: '批准' }); ElMessage.success('已批准'); fetchApprovals() }
async function reject(id) { await api.post(`/approvals/${id}/reject`, { reason: '已拒绝' }); ElMessage.success('已拒绝'); fetchApprovals() }
</script>
