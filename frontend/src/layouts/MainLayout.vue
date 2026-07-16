<template>
  <div class="light-bg"></div>

  <FakeWord v-if="isIdeMode" @close="isIdeMode = false" />
  
  <div v-show="!isIdeMode" class="flex flex-row h-screen bg-white text-zinc-900 font-sans overflow-hidden">
    
    <!-- Sidebar -->
    <aside class="w-64 border-r border-zinc-200 bg-zinc-50/50 flex flex-col shrink-0 transition-all duration-300">
      <div class="h-14 flex items-center px-4 border-b border-zinc-200">
        <div class="w-7 h-7 bg-zinc-900 text-white rounded flex items-center justify-center font-bold mr-3 text-sm">C</div>
        <span class="font-semibold text-sm tracking-tight">CloudOffice</span>
      </div>
      
      <div class="flex-1 overflow-y-auto py-4 px-3 space-y-1 custom-scrollbar">
        <!-- Navigation Links -->
        <router-link to="/" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><DataBoard /></el-icon> 工作台
          </div>
        </router-link>
        <router-link :to="`/projects/${authStore.user?.current_project_id || '1'}/kanban`" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><List /></el-icon> 任务看板
          </div>
        </router-link>
        <router-link to="/standups" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><ChatLineSquare /></el-icon> 每日站会
          </div>
        </router-link>
        <router-link to="/mall" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><ShoppingCart /></el-icon> 激励商城
          </div>
        </router-link>
        
        <div class="pt-4 pb-2">
          <div class="px-3 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider mb-2">更多模块</div>
          <router-link to="/gantt" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><Calendar /></el-icon> 甘特图
            </div>
          </router-link>
          <router-link to="/logs" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><Document /></el-icon> 操作日志
            </div>
          </router-link>
          <router-link to="/recycle-bin" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><Delete /></el-icon> 回收站
            </div>
          </router-link>
        </div>
      </div>
      
      <!-- User Profile Bottom -->
      <div class="p-4 border-t border-zinc-200">
        <el-popover v-model:visible="isUserPopoverVisible" placement="top-end" :width="260" trigger="click" :show-arrow="false" :offset="12">
          <template #reference>
            <div class="flex items-center space-x-3 cursor-pointer hover:bg-zinc-200/50 p-2 -mx-2 rounded-md transition-colors">
              <el-avatar :size="28" class="bg-zinc-200 text-zinc-700 font-semibold text-xs border border-zinc-300">
                {{ authStore.user?.username?.charAt(0).toUpperCase() || 'U' }}
              </el-avatar>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-semibold text-zinc-900 truncate leading-tight">{{ authStore.user?.username || '未登录' }}</p>
                <p class="text-xs text-zinc-500 truncate mt-0.5">{{ authStore.user?.current_project?.name || '选择项目' }}</p>
              </div>
              <el-icon class="text-zinc-400"><MoreFilled /></el-icon>
            </div>
          </template>
          
          <div class="flex flex-col">
            <div class="px-2 py-1 mb-2 border-b border-zinc-100 pb-2">
              <p class="text-sm font-semibold text-zinc-900">{{ authStore.user?.username }}</p>
              <p class="text-xs text-zinc-500">Lv.{{ authStore.rpgLevel }} {{ userDisplayRole }}</p>
            </div>
            
            <div class="space-y-1">
              <div class="flex justify-between items-center px-2 py-1.5 hover:bg-zinc-100 rounded cursor-pointer text-sm text-zinc-700" @click="goToTeams">
                <span class="flex items-center"><el-icon class="mr-2"><Switch /></el-icon>切换项目</span>
              </div>
              <div v-if="isLeader" class="flex justify-between items-center px-2 py-1.5 hover:bg-zinc-100 rounded cursor-pointer text-sm text-zinc-700" @click="openAnnouncementDialog">
                <span class="flex items-center"><el-icon class="mr-2"><Notification /></el-icon>发布全员公告</span>
              </div>
              <div v-else class="flex justify-between items-center px-2 py-1.5 hover:bg-zinc-100 rounded cursor-pointer text-sm text-zinc-700" @click="openAnnouncementDialog">
                <span class="flex items-center"><el-icon class="mr-2"><Notification /></el-icon>全员公告</span>
              </div>
              <div class="flex justify-between items-center px-2 py-1.5 hover:bg-zinc-100 rounded text-sm text-zinc-700">
                <span class="flex items-center"><el-icon class="mr-2"><Document /></el-icon>沉浸文档</span>
                <el-switch v-model="isIdeMode" size="small" />
              </div>

              <div class="h-px bg-zinc-100 my-1"></div>
              <div class="flex justify-between items-center px-2 py-1.5 hover:bg-red-50 rounded cursor-pointer text-sm text-red-600 font-medium" @click="handleLogout">
                <span class="flex items-center"><el-icon class="mr-2"><SwitchButton /></el-icon>退出登录</span>
              </div>
            </div>
          </div>
        </el-popover>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-w-0 bg-white relative">
      <!-- Top Header -->
      <header class="h-14 border-b border-zinc-200 flex items-center justify-between px-6 shrink-0 bg-white">
        <!-- Breadcrumb / Title -->
        <div class="flex items-center text-sm text-zinc-600 font-medium">
          <span>{{ authStore.user?.current_project?.name || 'CloudOffice' }}</span>
          
          <div v-if="authStore.user?.current_project?.invite_code" 
               @click="copyInviteCode(authStore.user.current_project.invite_code)"
               class="ml-3 flex items-center px-2 py-0.5 bg-zinc-100 text-zinc-500 rounded border border-zinc-200 cursor-pointer hover:bg-zinc-200 hover:text-zinc-700 transition-colors group"
               title="点击复制邀请码">
            <span class="text-xs mr-1 font-mono">ID: {{ authStore.user.current_project.invite_code }}</span>
            <el-icon class="text-zinc-400 group-hover:text-zinc-600"><CopyDocument /></el-icon>
          </div>

          <span class="text-zinc-300 mx-2">/</span>
          <span class="font-semibold text-zinc-900">{{ currentRouteName }}</span>
        </div>
        
        <!-- Right actions -->
        <div class="flex items-center space-x-4">
          <!-- Notification Bell -->
          <el-popover placement="bottom-end" :width="320" trigger="click" :show-arrow="false" offset="12">
            <template #reference>
              <div class="relative cursor-pointer text-zinc-500 hover:text-zinc-900 transition-colors flex items-center justify-center w-8 h-8 rounded-md hover:bg-zinc-100">
                <el-icon class="text-lg"><Bell /></el-icon>
                <div v-if="applications.length > 0" class="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border border-white"></div>
              </div>
            </template>
            <div class="p-1">
              <h4 class="font-semibold text-sm mb-2 px-1 text-zinc-900">通知中心</h4>
              <div v-if="applications.length === 0" class="text-center py-6 text-zinc-400 text-sm">暂无新通知</div>
              <div v-else class="space-y-1 max-h-60 overflow-y-auto">
                <div v-for="app in applications" :key="app.id" class="p-2 border border-zinc-100 bg-zinc-50 rounded-md flex items-center justify-between">
                  <span class="text-sm font-medium text-zinc-700">{{ app.user?.username }} 申请加入</span>
                  <div class="flex space-x-1">
                    <el-button type="primary" size="small" class="!px-2 !py-1 !h-7 !rounded" @click.stop="handleApprove(app.user_id)">同意</el-button>
                    <el-button size="small" class="!px-2 !py-1 !h-7 !rounded" @click.stop="handleReject(app.user_id)">拒绝</el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
        </div>
      </header>
      
      <!-- Router View Container -->
      <main class="flex-1 overflow-auto bg-zinc-50/30">
        <div class="w-full h-full p-6 mx-auto">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
    
    <!-- AI Floating Assistant -->
    <AIAssistant />

    <!-- Minimal Announcement Dialog -->
    <el-dialog
      v-model="showAnnouncementDialog"
      width="480px"
      :show-close="false"
      align-center
    >
      <div class="p-2">
        <div class="flex items-center justify-between mb-5">
          <h3 class="text-lg font-semibold text-zinc-900 tracking-tight">系统公告</h3>
          <el-button circle plain @click="showAnnouncementDialog = false" class="border-0 bg-transparent hover:bg-zinc-100 text-zinc-500">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <div v-if="!isEditingAnnouncement">
          <div v-if="announcementContent" class="bg-zinc-50 border border-zinc-200 p-5 rounded-md text-zinc-700 text-sm leading-relaxed whitespace-pre-wrap">
            {{ announcementContent }}
          </div>
          <div v-else class="text-center py-10 bg-zinc-50 border border-zinc-200 rounded-md">
            <p class="text-zinc-500 text-sm font-medium">当前没有任何公告</p>
          </div>
        </div>

        <div v-else class="rounded-md">
          <el-input
            v-model="announcementDraft"
            type="textarea"
            :rows="6"
            placeholder="请输入要发布给全员的公告内容..."
          />
        </div>

        <div class="mt-6 flex items-center justify-between">
          <div>
            <el-checkbox v-if="!isEditingAnnouncement && announcementContent" v-model="hideAnnouncementToday" class="!text-zinc-500 font-medium text-sm">今日不再弹出</el-checkbox>
          </div>
          <div class="flex space-x-2">
            <template v-if="!isEditingAnnouncement">
              <el-button v-if="!isLeader && announcementContent && !hasAcknowledgedCurrent" type="primary" @click="acknowledgeAnnouncement">
                我已收到
              </el-button>
              <el-button v-if="isLeader" @click="startEditingAnnouncement">编辑公告</el-button>
            </template>
            <template v-else>
              <el-button plain @click="isEditingAnnouncement = false">取消</el-button>
              <el-button type="primary" @click="saveAnnouncement" :loading="isSavingAnnouncement">发布公告</el-button>
            </template>
          </div>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { DataBoard, List, ChatLineSquare, Bell, Monitor, SwitchButton, Switch, Delete, Document, Calendar, ShoppingCart, Notification, Close, MoreFilled, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import AIAssistant from '../components/AIAssistant.vue'
import FakeWord from '../components/FakeWord.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentRouteName = computed(() => {
  const map = {
    '/': '工作台',
    '/standups': '每日站会',
    '/mall': '激励商城',
    '/gantt': '项目甘特图',
    '/logs': '操作日志',
    '/recycle-bin': '回收站',
    '/teams': '团队选择'
  }
  if (route.path.includes('/kanban')) return '任务看板'
  return map[route.path] || ''
})

const copyInviteCode = (code) => {
  if (code) {
    navigator.clipboard.writeText(code)
    ElMessage.success(`邀请码 ${code} 已复制，快发给伙伴吧！`)
  }
}

const userDisplayRole = computed(() => {
  if (!authStore.user) return '访客'
  if (authStore.user.role === 'teacher') return '导师'
  
  const currentProjectId = authStore.user.current_project_id
  if (currentProjectId && authStore.user.project_memberships) {
    const membership = authStore.user.project_memberships.find(m => m.project_id === currentProjectId)
    if (membership && membership.role === 'leader') {
      return '组长'
    }
  }
  return '组员'
})

const handleLogout = () => {
  closeUserPopover()
  setTimeout(() => {
    authStore.logout()
    router.push('/login')
  }, 100)
}

const isUserPopoverVisible = ref(false)

const closeUserPopover = () => {
  isUserPopoverVisible.value = false
}

const goToTeams = () => {
  closeUserPopover()
  setTimeout(() => {
    router.push('/teams')
  }, 100)
}

const isIdeMode = ref(false)
const applications = ref([])

const isLeader = computed(() => {
  if (authStore.user?.role === 'teacher') return true;
  const membership = authStore.user?.project_memberships?.find(m => m.project_id === authStore.user?.current_project_id);
  return membership?.role === 'leader';
})

const fetchNotifications = async () => {
  if (!isLeader.value || !authStore.user?.current_project_id) {
    applications.value = []
    return
  }
  try {
    const res = await api.get(`/projects/${authStore.user.current_project_id}/applications`)
    applications.value = res.data
  } catch (e) {
    console.error("Failed to fetch applications", e)
  }
}

const handleApprove = async (userId) => {
  try {
    await api.post(`/projects/${authStore.user.current_project_id}/approve/${userId}`)
    ElMessage.success('已同意加入')
    await fetchNotifications()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '操作失败')
  }
}

const handleReject = async (userId) => {
  try {
    await api.post(`/projects/${authStore.user.current_project_id}/reject/${userId}`)
    ElMessage.success('已拒绝加入')
    await fetchNotifications()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '操作失败')
  }
}

// Announcement logic
const showAnnouncementDialog = ref(false)
const announcementContent = ref('')
const announcementDraft = ref('')
const isEditingAnnouncement = ref(false)
const isSavingAnnouncement = ref(false)
const hideAnnouncementToday = ref(false)
const hasAcknowledgedCurrent = ref(false)

const getTodayDateString = () => {
  const d = new Date()
  return `${d.getFullYear()}-${d.getMonth()+1}-${d.getDate()}`
}

const checkAcknowledged = () => {
  if (!authStore.user?.current_project_id || !announcementContent.value) {
    hasAcknowledgedCurrent.value = false
    return
  }
  const storageKey = `ack_announcement_${authStore.user.current_project_id}`
  const ackedContent = localStorage.getItem(storageKey)
  hasAcknowledgedCurrent.value = (ackedContent === announcementContent.value)
}

const acknowledgeAnnouncement = () => {
  if (!authStore.user?.current_project_id || !announcementContent.value) return
  const storageKey = `ack_announcement_${authStore.user.current_project_id}`
  localStorage.setItem(storageKey, announcementContent.value)
  hasAcknowledgedCurrent.value = true
  ElMessage.success('已确认收到公告')
  showAnnouncementDialog.value = false
}

const fetchAnnouncement = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get('/ide/content?path=announcement.md')
    announcementContent.value = res.data.content || ''
    checkAcknowledged()
  } catch (err) {
    announcementContent.value = ''
  }
}

const openAnnouncementDialog = () => {
  closeUserPopover()
  showAnnouncementDialog.value = true
  isEditingAnnouncement.value = false
  checkAcknowledged()
}

const startEditingAnnouncement = () => {
  announcementDraft.value = announcementContent.value
  isEditingAnnouncement.value = true
}

const saveAnnouncement = async () => {
  if (!announcementDraft.value.trim()) {
    ElMessage.warning('公告内容不能为空')
    return
  }
  isSavingAnnouncement.value = true
  try {
    await api.post('/ide/content', {
      path: 'announcement.md',
      content: announcementDraft.value
    })
    announcementContent.value = announcementDraft.value
    isEditingAnnouncement.value = false
    checkAcknowledged()
    ElMessage.success('公告发布成功')
  } catch (err) {
    ElMessage.error('公告发布失败')
  } finally {
    isSavingAnnouncement.value = false
  }
}

watch(showAnnouncementDialog, (newVal) => {
  if (!newVal) {
    const today = getTodayDateString()
    const storageKey = `hide_announcement_${authStore.user?.current_project_id}`
    if (hideAnnouncementToday.value) {
      localStorage.setItem(storageKey, today)
    } else {
      localStorage.removeItem(storageKey)
    }
  }
})

const checkAutoShowAnnouncement = async () => {
  if (!authStore.user?.current_project_id) return
  await fetchAnnouncement()
  
  if (!announcementContent.value && !isLeader.value) return
  if (hasAcknowledgedCurrent.value) return
  
  const today = getTodayDateString()
  const storageKey = `hide_announcement_${authStore.user?.current_project_id}`
  const hiddenDate = localStorage.getItem(storageKey)
  
  if (hiddenDate !== today) {
    hideAnnouncementToday.value = false
    setTimeout(() => {
      showAnnouncementDialog.value = true
    }, 800)
  } else {
    hideAnnouncementToday.value = true
  }
}

onMounted(() => {
  if (isLeader.value && authStore.user?.current_project_id) {
    fetchNotifications()
  }
  checkAutoShowAnnouncement()
})
</script>

<style>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease-out;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
