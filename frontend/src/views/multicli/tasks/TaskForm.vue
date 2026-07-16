<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">创建任务</h2>
      <el-button plain @click="$router.push('/multicli/tasks')">返回列表</el-button>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden max-w-3xl">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
        <h3 class="text-base font-semibold text-zinc-900">任务基本信息</h3>
      </div>
      <div class="p-6">
        <el-form @submit.prevent="create" :model="form" label-width="100px" class="max-w-2xl">
          <el-form-item label="项目" required>
            <el-select v-model="form.projectId" placeholder="选择关联的项目" class="w-full">
              <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="form.title" placeholder="例如: 爬取某网站数据" />
          </el-form-item>
          <el-form-item label="目标" required>
            <el-input v-model="form.objective" type="textarea" :rows="4" placeholder="详细描述任务要达成的目标和执行逻辑..." />
          </el-form-item>
          <div class="pt-4 border-t border-zinc-100 flex justify-end">
            <el-button plain @click="$router.push('/multicli/tasks')" class="mr-2">取消</el-button>
            <el-button type="primary" native-type="submit" :loading="loading" class="!bg-zinc-900 !border-none hover:!bg-zinc-800">
              创建并提交
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../../utils/api'
import { ElMessage } from 'element-plus'
const router = useRouter(); const loading = ref(false); const projects = ref([])
const form = reactive({ projectId: null, title: '', objective: '' })
onMounted(async () => { try { const r = await api.get('/projects'); projects.value = r.data.records } catch(e) { console.error(e) } })
async function create() {
  loading.value = true
  try {
    const r = await api.post('/tasks', form)
    await api.post(`/tasks/${r.data.id}/submit`)
    ElMessage.success('任务已创建并提交'); router.push(`/multicli/tasks/${r.data.id}`)
  } catch (e) { ElMessage.error(e.response?.data?.message || '创建失败') }
  finally { loading.value = false }
}
</script>
