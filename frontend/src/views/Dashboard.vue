<template>
  <div class="h-full">
    <div class="space-y-6 animate-in pb-8" v-if="authStore.user?.current_project_id">
      
      <!-- Top Stats Grid -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- Total Tasks -->
        <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm font-medium text-zinc-500">任务总数</span>
            <el-icon class="text-zinc-400"><List /></el-icon>
          </div>
          <div class="text-3xl font-semibold text-zinc-900 mb-1">{{ stats.total_tasks }}</div>
          <div class="text-xs text-zinc-500 flex items-center">
            包含进行中与已完成
          </div>
        </div>
        
        <!-- Completion Rate -->
        <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm font-medium text-zinc-500">整体完成率</span>
            <el-icon class="text-zinc-400"><DataBoard /></el-icon>
          </div>
          <div class="flex items-baseline mb-2 space-x-2">
            <span class="text-3xl font-semibold text-zinc-900">{{ completionRate }}%</span>
            <span class="text-sm text-zinc-500 font-medium">{{ stats.done_tasks }} / {{ stats.total_tasks }}</span>
          </div>
          <div class="mt-auto">
            <el-progress :percentage="completionRate" :show-text="false" color="#18181b" class="w-full" :stroke-width="6" />
          </div>
        </div>

        <!-- Active Members -->
        <div class="bg-white rounded-lg p-6 shadow-sm border border-zinc-200 flex flex-col hover:border-zinc-300 transition-colors">
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm font-medium text-zinc-500">项目成员</span>
            <el-icon class="text-zinc-400"><User /></el-icon>
          </div>
          <div class="text-3xl font-semibold text-zinc-900 mb-1">{{ stats.workload.length }}</div>
          <div class="text-xs text-zinc-500 flex items-center">
            当前活跃人数
          </div>
        </div>
      </div>

      <!-- Main Content Area -->
      <div class="bg-white rounded-lg shadow-sm border border-zinc-200 overflow-hidden">
        
        <div class="px-6 py-4 border-b border-zinc-200 flex justify-between items-center bg-zinc-50/50">
          <h2 class="text-base font-semibold text-zinc-900 tracking-tight">团队贡献榜</h2>
          <div class="flex items-center space-x-2">
            <el-button size="small" plain @click="generateDailyRisk" :loading="isGeneratingRisk">
              <el-icon class="mr-1"><Warning /></el-icon> 风险预测
            </el-button>
            <el-button size="small" type="primary" @click="generateGradePrediction" :loading="isGeneratingGrade">
              <el-icon class="mr-1"><MagicStick /></el-icon> 智能评估
            </el-button>
            
            <el-tooltip v-if="(authStore.user?.role === 'leader' || authStore.user?.role === 'teacher') && authStore.user?.current_project?.invite_code" content="点击复制邀请码" placement="top">
              <div 
                class="ml-2 bg-zinc-100 text-zinc-700 px-3 py-1.5 rounded text-xs font-medium border border-zinc-200 cursor-pointer hover:bg-zinc-200 transition-colors flex items-center"
                @click="copyInviteCode(authStore.user.current_project.invite_code)"
              >
                <el-icon class="mr-1"><CopyDocument /></el-icon>
                {{ authStore.user.current_project.invite_code }}
              </div>
            </el-tooltip>
          </div>
        </div>
        
        <div class="p-6">
          <div v-if="sortedUsers.length === 0" class="text-center text-zinc-400 py-12 text-sm font-medium">
            暂无团队数据
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div v-for="(user, idx) in sortedUsers" :key="user.username" 
                 class="group p-4 rounded-md transition-all duration-200 bg-white border border-zinc-200 hover:border-zinc-400 cursor-pointer flex flex-col"
                 @click="isLeader ? openManageMember(user) : null">
              
              <div class="flex items-start justify-between mb-4">
                <div class="flex items-center space-x-3">
                  <div class="relative">
                    <div class="w-10 h-10 rounded bg-zinc-100 flex items-center justify-center font-medium text-zinc-700 text-sm border border-zinc-200">
                      {{ user.username.charAt(0).toUpperCase() }}
                    </div>
                    <div v-if="idx === 0" class="absolute -top-1.5 -right-1.5 w-5 h-5 bg-zinc-900 rounded-full border border-white flex items-center justify-center shadow-sm" title="榜首">
                      <span class="text-[8px] text-white">1</span>
                    </div>
                  </div>
                  <div>
                    <div class="flex items-center space-x-2">
                      <div class="font-medium text-zinc-900 text-sm">{{ user.username }}</div>
                      <div class="flex space-x-0.5" v-if="user.badges && user.badges.length > 0">
                        <span v-for="b in user.badges" :key="b" class="text-[10px]" :title="getBadgeName(b)">{{ getBadgeEmoji(b) }}</span>
                      </div>
                    </div>
                    <div class="text-xs text-zinc-500 mt-1 flex items-center space-x-2">
                      <span>{{ user.role === 'leader' ? '组长' : (user.role === 'teacher' ? '导师' : '组员') }}</span>
                      <span class="bg-zinc-100 text-zinc-600 px-1 rounded">{{ user.in_progress }} 项进行中</span>
                    </div>
                  </div>
                </div>
                <div class="text-right">
                  <span class="font-semibold text-lg text-zinc-900">{{ user.points }}</span>
                  <span class="text-xs text-zinc-500 ml-1">分</span>
                </div>
              </div>
              <div class="mt-auto pt-2">
                <el-progress 
                  :percentage="Math.min((user.points / maxPoints) * 100, 100)" 
                  :show-text="false" 
                  :stroke-width="4"
                  color="#18181b"
                  class="w-full opacity-60 group-hover:opacity-100 transition-opacity light-progress" 
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Empty State -->
    <div v-else class="text-center py-24 animate-in flex flex-col items-center justify-center h-full">
      <div class="w-16 h-16 rounded border border-zinc-200 flex items-center justify-center mb-4 text-zinc-400 bg-zinc-50">
        <el-icon class="text-2xl"><Lock /></el-icon>
      </div>
      <h2 class="text-base font-semibold text-zinc-900 mb-1">未连接项目</h2>
      <p class="text-zinc-500 text-sm mb-6">请先选择或创建一个项目空间</p>
      <el-button type="primary" size="small" @click="$router.push('/teams')">前往项目列表</el-button>
    </div>

    <!-- Grade Prediction Drawer -->
    <el-drawer v-model="gradeDrawerVisible" title="AI 智能评估报告" size="650px" class="minimal-drawer">
      <template #header>
        <div class="font-semibold text-zinc-900 flex items-center text-base"><el-icon class="mr-2"><MagicStick /></el-icon> AI 智能评估报告</div>
      </template>
      <div v-if="gradeReport" class="markdown-body minimal-markdown p-6 bg-zinc-50 rounded-md border border-zinc-200" v-html="gradeReportHtml"></div>
      <div v-else class="text-center text-zinc-500 py-32 text-sm flex flex-col items-center">
        <el-icon class="is-loading text-2xl mb-4"><Loading /></el-icon>
        正在生成分析报告...
      </div>
    </el-drawer>

    <!-- Risk Radar Drawer -->
    <el-drawer v-model="riskDrawerVisible" title="项目风险预测" size="650px" class="minimal-drawer">
      <template #header>
        <div class="font-semibold text-zinc-900 flex items-center text-base"><el-icon class="mr-2"><Warning /></el-icon> 项目风险预测</div>
      </template>
      <div v-if="riskReport" class="markdown-body minimal-markdown p-6 bg-zinc-50 rounded-md border border-zinc-200" v-html="riskReportHtml"></div>
      <div v-else class="text-center text-zinc-500 py-32 text-sm flex flex-col items-center">
        <el-icon class="is-loading text-2xl mb-4"><Loading /></el-icon>
        正在扫描潜在风险...
      </div>
    </el-drawer>

    <!-- Manage Member Dialog -->
    <el-dialog
      v-model="manageMemberDialog"
      width="400px"
      :show-close="false"
      align-center
    >
      <div class="p-2">
        <div class="flex items-center justify-between mb-5">
          <h3 class="text-base font-semibold text-zinc-900 tracking-tight">成员管理: {{ selectedMember?.username }}</h3>
          <el-button circle plain @click="manageMemberDialog = false" class="border-0 bg-transparent hover:bg-zinc-100 text-zinc-500">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        
        <div v-if="selectedMember" class="space-y-5">
          <div>
            <label class="block text-xs font-medium text-zinc-700 mb-2">积分调整 (奖惩)</label>
            <div class="flex items-center space-x-3">
              <el-input-number v-model="pointForm.amount" :step="5" class="!w-32" />
              <span class="text-xs text-zinc-500 bg-zinc-100 px-2 py-1 rounded">正数奖励，负数扣除</span>
            </div>
          </div>
          <div>
            <label class="block text-xs font-medium text-zinc-700 mb-2">调整原因</label>
            <el-input v-model="pointForm.reason" placeholder="请输入调整原因..." />
          </div>
          
          <div class="pt-5 mt-5 border-t border-zinc-100 flex justify-between items-center">
            <el-button plain @click="handleKickMember" size="small" class="!text-red-600 hover:!bg-red-50 hover:!border-red-200">
              移出团队
            </el-button>
            <div class="space-x-2">
              <el-button plain @click="manageMemberDialog = false" size="small">取消</el-button>
              <el-button type="primary" @click="submitPointUpdate" :disabled="pointForm.amount === 0" size="small">确认执行</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { TopRight, User, MagicStick, CopyDocument, Lock, Warning, Close, List, DataBoard, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import { useAuthStore } from '../stores/auth'
import { marked } from 'marked'

const authStore = useAuthStore()

const copyInviteCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('邀请码已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const stats = ref({
  total_tasks: 0,
  done_tasks: 0,
  workload: []
})

const fetchDashboardStats = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get(`/projects/${authStore.user.current_project_id}/dashboard`)
    stats.value = res.data
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

const completionRate = computed(() => {
  if (stats.value.total_tasks === 0) return 0
  return Math.round((stats.value.done_tasks / stats.value.total_tasks) * 100)
})

const sortedUsers = computed(() => [...stats.value.workload].sort((a, b) => b.points - a.points))
const maxPoints = computed(() => Math.max(...stats.value.workload.map(u => u.points), 1))

const getBadgeEmoji = (badge) => {
  const map = {
    'night_owl': '🦉',
    'bug_hunter': '🐛',
    'focus_master': '🔥'
  }
  return map[badge] || '🌟'
}

const getBadgeName = (badge) => {
  const map = {
    'night_owl': '夜猫子',
    'bug_hunter': '修虫达人',
    'focus_master': '专注大师'
  }
  return map[badge] || badge
}

const isLeader = computed(() => {
  if (!authStore.user) return false
  if (authStore.user.role === 'teacher') return true
  const me = stats.value.workload.find(u => u.username === authStore.user.username)
  return me?.role === 'leader'
})

const manageMemberDialog = ref(false)
const selectedMember = ref(null)
const pointForm = ref({ amount: 0, reason: '组长手动调整' })

const openManageMember = (user) => {
  if (!isLeader.value) return
  if (user.username === authStore.user.username) {
    ElMessage.warning('无法修改自己的积分')
    return
  }
  if (authStore.user.role !== 'teacher' && user.role === 'teacher') {
    ElMessage.error('权限不足：组长无法操作导师')
    return
  }
  selectedMember.value = user
  pointForm.value = { amount: 0, reason: '组长评估' }
  manageMemberDialog.value = true
}

const submitPointUpdate = async () => {
  if (pointForm.value.amount === 0) return
  try {
    await api.put(`/projects/${authStore.user.current_project_id}/members/${selectedMember.value.id}/points`, pointForm.value)
    ElMessage.success('积分修改成功')
    manageMemberDialog.value = false
    fetchDashboardStats()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '修改失败')
  }
}

const handleKickMember = () => {
  ElMessageBox.confirm(
    `确定要将成员 ${selectedMember.value.username} 移出当前团队吗？此操作不可逆。`,
    '移出确认',
    {
      confirmButtonText: '确认移出',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await api.delete(`/projects/${authStore.user.current_project_id}/members/${selectedMember.value.id}`)
      ElMessage.success('成员已移出')
      manageMemberDialog.value = false
      fetchDashboardStats()
    } catch (err) {
      ElMessage.error(err.response?.data?.detail || '操作失败')
    }
  }).catch(() => {})
}

// Grade Prediction Logic
const isGeneratingGrade = ref(false)
const gradeDrawerVisible = ref(false)
const gradeReport = ref('')

const gradeReportHtml = computed(() => {
  return marked(gradeReport.value)
})

const generateGradePrediction = async () => {
  isGeneratingGrade.value = true
  gradeDrawerVisible.value = true
  gradeReport.value = ''
  try {
    const res = await api.post('/ai/grade-prediction', {
      project_id: authStore.user.current_project_id
    })
    gradeReport.value = res.data.report
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || '生成失败')
    gradeDrawerVisible.value = false
  } finally {
    isGeneratingGrade.value = false
  }
}

// Daily Risk Logic
const isGeneratingRisk = ref(false)
const riskDrawerVisible = ref(false)
const riskReport = ref('')

const riskReportHtml = computed(() => {
  return marked(riskReport.value)
})

const generateDailyRisk = async () => {
  isGeneratingRisk.value = true
  riskDrawerVisible.value = true
  riskReport.value = ''
  try {
    const res = await api.post('/ai/daily-risk', {
      project_id: authStore.user.current_project_id
    })
    riskReport.value = res.data.report
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || '生成失败')
    riskDrawerVisible.value = false
  } finally {
    isGeneratingRisk.value = false
  }
}

onMounted(() => {
  fetchDashboardStats()
})
</script>

<style>
.animate-in {
  animation: fadeSlideIn 0.3s ease-out;
}
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.light-progress .el-progress-bar__outer {
  background-color: #f4f4f5 !important;
}

/* Minimal Drawer */
.minimal-drawer {
  background: #ffffff !important;
  border-left: 1px solid #e4e4e7 !important;
}
.minimal-drawer .el-drawer__header {
  border-bottom: 1px solid #f4f4f5;
  margin-bottom: 0;
  padding: 20px 24px;
}
.minimal-drawer .el-drawer__body {
  padding: 24px;
}

/* Minimal Markdown */
.minimal-markdown {
  color: #3f3f46;
  font-size: 14px;
}
.minimal-markdown h1, .minimal-markdown h2, .minimal-markdown h3 {
  color: #09090b;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.minimal-markdown h1 { font-size: 1.25rem; margin-bottom: 1rem; }
.minimal-markdown h2 { font-size: 1.125rem; margin-top: 1.5rem; margin-bottom: 0.75rem; border-bottom: 1px solid #f4f4f5; padding-bottom: 0.5rem; }
.minimal-markdown p { line-height: 1.6; margin-bottom: 1rem; }
.minimal-markdown ul { padding-left: 1.25rem; list-style-type: disc; margin-bottom: 1rem; }
.minimal-markdown li { margin-bottom: 0.25rem; }
.minimal-markdown strong { color: #09090b; font-weight: 600; }
</style>
