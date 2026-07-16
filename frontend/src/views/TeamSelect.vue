<template>
  <div class="w-full min-h-screen bg-zinc-50 flex items-center justify-center p-4 sm:p-8 font-sans">
    
    <div class="w-full max-w-4xl bg-white border border-zinc-200 shadow-sm rounded-lg p-8 sm:p-12 relative">
      
      <div class="mb-10 flex justify-between items-start">
        <div>
          <h2 class="text-2xl font-semibold text-zinc-900 tracking-tight mb-2">项目空间</h2>
          <p class="text-sm text-zinc-500">
            请选择您要进入的工作空间，或建立一个新的协作节点。
          </p>
        </div>
        <div class="flex items-center space-x-3">
          <el-button v-if="authStore.user?.current_project_id" link class="!text-zinc-500 hover:!text-zinc-900 text-sm" @click="router.push({name: 'Dashboard'})">
            返回工作台
          </el-button>
          <el-button 
            plain 
            size="small"
            @click="showWelcomeDialog = true" 
            class="!text-zinc-600 !border-zinc-200 hover:!bg-zinc-100"
          >
            <el-icon class="mr-1"><MagicStick /></el-icon> 新手向导
          </el-button>
        </div>
      </div>

      <div v-if="loading" class="text-center py-16 text-zinc-500 text-sm">
        <el-icon class="is-loading text-xl mb-2"><Loading /></el-icon>
        <p>正在加载项目列表...</p>
      </div>

      <div v-else class="space-y-10">
        
        <!-- 个人中心 (Personal Center) -->
        <div class="bg-gradient-to-r from-zinc-50 to-white rounded-xl p-6 border border-zinc-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-0 overflow-hidden">
          <div class="flex items-center space-x-4">
            <div class="w-14 h-14 rounded-full bg-gradient-to-tr from-zinc-800 to-zinc-600 text-white flex items-center justify-center text-xl font-bold shadow-md shrink-0">
              {{ authStore.user?.username?.charAt(0).toUpperCase() }}
            </div>
            <div>
              <h3 class="text-lg font-bold text-zinc-900 flex items-center">
                {{ authStore.user?.username }}
                <span class="ml-2 text-[10px] font-medium px-2 py-0.5 rounded-full bg-zinc-800 text-white">
                  {{ authStore.user?.role === 'teacher' ? '指导教师' : '开发者' }}
                </span>
              </h3>
              <p class="text-xs text-zinc-500 mt-1">
                欢迎回来，您目前参与了 <span class="font-semibold text-zinc-700">{{ totalProjects }}</span> 个工作空间
              </p>
            </div>
          </div>
          
          <div class="flex items-center space-x-8 md:justify-end">
            <div class="text-center min-w-[4rem]">
              <div class="text-xs text-zinc-500 mb-1 flex items-center justify-center"><el-icon class="mr-1 text-yellow-500"><Trophy /></el-icon> 资产积分</div>
              <div class="text-2xl font-bold text-zinc-800">{{ authStore.user?.points || 0 }}</div>
            </div>
            <div class="w-px h-10 bg-zinc-200 hidden md:block"></div>
            <div class="text-center min-w-[4rem]">
              <div class="text-xs text-zinc-500 mb-1 flex items-center justify-center"><el-icon class="mr-1 text-blue-500"><Timer /></el-icon> 专注时长</div>
              <div class="text-2xl font-bold text-zinc-800">{{ totalFocusMinutes }} <span class="text-xs font-normal text-zinc-500">分钟</span></div>
            </div>
          </div>
        </div>

        <div v-if="memberships.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4 relative z-10">
          <div 
            v-for="mem in memberships" :key="mem.id"
            @click="mem.is_approved ? switchProject(mem.project_id) : null"
            :class="[
              'group p-5 rounded-md border transition-all duration-200 relative',
              mem.is_approved 
                ? 'bg-white border-zinc-200 cursor-pointer hover:border-zinc-400 hover:shadow-sm' 
                : 'bg-zinc-50 border-zinc-200 opacity-60 cursor-not-allowed'
            ]"
          >
            <div class="flex justify-between items-start mb-2">
              <h3 class="text-base font-semibold text-zinc-900 truncate pr-4">{{ mem.project.name }}</h3>
              <span v-if="!mem.is_approved" class="text-[10px] font-medium px-2 py-0.5 rounded bg-zinc-200 text-zinc-600 shrink-0">待审核</span>
              <span v-else-if="mem.project_id === authStore.user?.current_project_id" class="text-[10px] font-medium px-2 py-0.5 rounded bg-green-100 text-green-700 shrink-0">当前所在</span>
            </div>
            <p class="text-xs text-zinc-500 mb-5 line-clamp-2 leading-relaxed">{{ mem.project.description || '暂无项目描述信息' }}</p>
            
            <div v-if="mem.is_approved" class="flex justify-between items-center text-xs">
              <span class="text-zinc-400">
                邀请码: <code class="text-zinc-600 bg-zinc-100 px-1.5 py-0.5 rounded ml-1">{{ mem.project.invite_code }}</code>
              </span>
              <span class="text-zinc-900 font-medium flex items-center group-hover:underline">
                进入空间 <el-icon class="ml-1"><Right /></el-icon>
              </span>
            </div>
          </div>
        </div>

        <div v-else class="text-center py-12 text-zinc-500 bg-zinc-50/50 rounded-md border border-zinc-200 border-dashed">
          <el-icon class="text-3xl mb-2 text-zinc-400"><Box /></el-icon>
          <p class="text-sm">您还没有加入任何项目，请先创建或加入。</p>
        </div>

        <div class="border-t border-zinc-200 pt-8">
          <h3 class="text-sm font-semibold text-zinc-900 mb-4">快捷操作</h3>
          
          <div class="flex flex-col sm:flex-row gap-4">
            <!-- Join Team -->
            <div class="flex-1 bg-zinc-50/50 p-5 rounded-md border border-zinc-200">
              <h4 class="text-sm font-medium text-zinc-900 mb-3 flex items-center"><el-icon class="mr-2"><Connection /></el-icon>加入已有项目</h4>
              <div class="flex space-x-2">
                <el-input v-model="inviteCode" placeholder="请输入邀请码" class="flex-1"></el-input>
                <el-button type="primary" @click="joinTeam">加入</el-button>
              </div>
            </div>

            <!-- Create Team -->
            <div class="flex-1 bg-zinc-50/50 p-5 rounded-md border border-zinc-200">
              <h4 class="text-sm font-medium text-zinc-900 mb-3 flex items-center"><el-icon class="mr-2"><Plus /></el-icon>创建新项目</h4>
              <div class="flex space-x-2">
                <el-input v-model="newTeamName" placeholder="给项目起个响亮的名字" class="flex-1"></el-input>
                <el-button @click="createTeam" plain>创建</el-button>
              </div>
            </div>
          </div>
        </div>

      </div>
      

    </div>

    <!-- Minimal Welcome Dialog -->
    <el-dialog
      v-model="showWelcomeDialog"
      width="600px"
      :show-close="false"
      align-center
    >
      <div class="p-2">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">CloudOffice 核心特性</h2>
          <el-button circle plain @click="closeWelcomeDialog" class="border-0 bg-transparent hover:bg-zinc-100 text-zinc-500">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        
        <div class="space-y-6">
          <div class="flex">
            <div class="w-10 h-10 rounded bg-zinc-100 flex items-center justify-center text-xl shrink-0 mr-4">💻</div>
            <div>
              <h3 class="text-sm font-semibold text-zinc-900 mb-1">沉浸式云端文档编辑</h3>
              <p class="text-sm text-zinc-500">内嵌高仿 Word 沉浸文档编辑器，每个项目独立空间。支持一键保存及打包下载。</p>
            </div>
          </div>
          <div class="flex">
            <div class="w-10 h-10 rounded bg-zinc-100 flex items-center justify-center text-xl shrink-0 mr-4">🎙️</div>
            <div>
              <h3 class="text-sm font-semibold text-zinc-900 mb-1">AI 智能语音站会</h3>
              <p class="text-sm text-zinc-500">无需手写日报。通过录音，AI会自动将其拆解为标准的结构化日报内容填入表单。</p>
            </div>
          </div>
          <div class="flex">
            <div class="w-10 h-10 rounded bg-zinc-100 flex items-center justify-center text-xl shrink-0 mr-4">🏆</div>
            <div>
              <h3 class="text-sm font-semibold text-zinc-900 mb-1">贡献度激励系统</h3>
              <p class="text-sm text-zinc-500">在看板完成研发任务，自动下发贡献度积分，可用于在激励商城中兑换专属奖励。</p>
            </div>
          </div>
          <div class="flex">
            <div class="w-10 h-10 rounded bg-zinc-100 flex items-center justify-center text-xl shrink-0 mr-4">🧠</div>
            <div>
              <h3 class="text-sm font-semibold text-zinc-900 mb-1">AI 智能任务拆解</h3>
              <p class="text-sm text-zinc-500">在新建任务时输入一句话目标，点击智能拆解，AI将自动为您填入细化后的子任务。</p>
            </div>
          </div>
          <div class="flex">
            <div class="w-10 h-10 rounded bg-zinc-100 flex items-center justify-center text-xl shrink-0 mr-4">👑</div>
            <div>
              <h3 class="text-sm font-semibold text-zinc-900 mb-1">管理者专属能力</h3>
              <p class="text-sm text-zinc-500">基于研发进度自动生成项目风险预测报告，支持发布强制阅读的系统弹窗公告。</p>
            </div>
          </div>
        </div>
        
        <div class="mt-8 flex items-center justify-between border-t border-zinc-100 pt-4">
          <el-checkbox v-model="disableAutoShow" class="!text-zinc-500 font-medium text-sm">不再自动弹出</el-checkbox>
          <el-button type="primary" @click="closeWelcomeDialog">
            开始体验
          </el-button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Connection, Plus, MagicStick, Close, Right, Box, Loading, Trophy, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const memberships = ref([])
const inviteCode = ref('')
const newTeamName = ref('')

const totalProjects = computed(() => memberships.value.length)
const totalFocusMinutes = computed(() => {
  return memberships.value.reduce((acc, mem) => acc + (mem.focus_minutes || 0), 0)
})

const fetchTeams = async () => {
  loading.value = true
  try {
    const res = await api.get('/projects/my')
    memberships.value = res.data
  } catch (error) {
    ElMessage.error('获取项目列表失败')
  } finally {
    loading.value = false
  }
}

const switchProject = async (projectId) => {
  try {
    await api.post('/projects/switch', { project_id: projectId })
    const meRes = await api.get('/auth/me')
    authStore.user = meRes.data
    ElMessage.success('已切换至该项目')
    router.push({ name: 'Dashboard' })
  } catch (error) {
    ElMessage.error('切换失败')
  }
}

const joinTeam = async () => {
  if (!inviteCode.value.trim()) return
  try {
    await api.post('/projects/join', { invite_code: inviteCode.value.trim() })
    ElMessage.success('申请已发送，等待组长审核！')
    inviteCode.value = ''
    await fetchTeams()
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || '申请失败')
  }
}

const createTeam = async () => {
  if (!newTeamName.value.trim()) return
  try {
    await api.post('/projects/', { name: newTeamName.value.trim() })
    ElMessage.success('项目创建成功！')
    
    const meRes = await api.get('/auth/me')
    authStore.user = meRes.data
    
    router.push({ name: 'Dashboard' })
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const showWelcomeDialog = ref(false)
const disableAutoShow = ref(localStorage.getItem('cloudoffice_welcome_disabled') === 'true')

const closeWelcomeDialog = () => {
  showWelcomeDialog.value = false
  if (disableAutoShow.value) {
    localStorage.setItem('cloudoffice_welcome_disabled', 'true')
  } else {
    localStorage.removeItem('cloudoffice_welcome_disabled')
  }
}

onMounted(() => {
  fetchTeams()
  
  if (localStorage.getItem('cloudoffice_welcome_disabled') !== 'true') {
    setTimeout(() => {
      showWelcomeDialog.value = true
    }, 500)
  }
})
</script>
