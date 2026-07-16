<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">创建项目</h2>
      <el-button plain @click="$router.push('/multicli/projects')">返回列表</el-button>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden max-w-3xl">
      <div class="px-6 py-4 border-b border-zinc-200 bg-zinc-50/50">
        <h3 class="text-base font-semibold text-zinc-900">项目基本信息</h3>
      </div>
      <div class="p-6">
        <el-form @submit.prevent="create" :model="form" label-width="120px" class="max-w-2xl">
          <el-form-item label="名称" required>
            <el-input v-model="form.name" placeholder="请输入项目名称" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="项目简介..." />
          </el-form-item>
          <el-form-item label="本地路径">
            <el-input v-model="form.localPath" placeholder="例如: D:/projects/my-project" />
            <div class="text-xs text-zinc-500 mt-1">本地引擎工作时的关联目录</div>
          </el-form-item>
          <div class="pt-4 border-t border-zinc-100 flex justify-end">
            <el-button plain @click="$router.push('/multicli/projects')" class="mr-2">取消</el-button>
            <el-button type="primary" native-type="submit" :loading="loading" class="!bg-zinc-900 !border-none hover:!bg-zinc-800">
              立即创建
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../../utils/api'
import { ElMessage } from 'element-plus'
const router = useRouter(); const loading = ref(false)
const form = reactive({ name: '', description: '', localPath: 'D:/projects/demo' })
async function create() {
  loading.value = true
  try { await api.post('/projects', form); ElMessage.success('项目已创建'); router.push('/multicli/projects') }
  catch (e) { ElMessage.error(e.response?.data?.message || '创建失败') }
  finally { loading.value = false }
}
</script>
