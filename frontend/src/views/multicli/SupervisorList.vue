<template>
  <div>
    <h2>Supervisor 管理</h2>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="supervisorCode" label="编码" />
      <el-table-column prop="supervisorName" label="名称" />
      <el-table-column prop="agentRole" label="角色" />
      <el-table-column prop="status" label="状态">
        <template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':row.status==='INACTIVE'?'info':'warning'">{{row.status}}</el-tag></template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
const list = ref([]); const loading = ref(false)
onMounted(async () => { loading.value = true; const r = await axios.get('/api/admin/supervisors'); list.value = r.data.data.records; loading.value = false })
</script>
