<template>
  <div>
    <h2>执行引擎管理</h2>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="engineName" label="名称" />
      <el-table-column prop="engineCode" label="编码" />
      <el-table-column prop="baseUrl" label="地址" />
      <el-table-column prop="status" label="状态">
        <template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'">{{row.status}}</el-tag></template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
const list = ref([]); const loading = ref(false)
onMounted(async () => { loading.value = true; const r = await axios.get('/api/admin/execution-engines'); list.value = r.data.data.records; loading.value = false })
</script>
