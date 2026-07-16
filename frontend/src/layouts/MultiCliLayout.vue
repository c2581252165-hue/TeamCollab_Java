<template>
  <div class="light-bg"></div>

  <FakeWord v-if="isIdeMode" @close="isIdeMode = false" />
  
  <div v-show="!isIdeMode" class="flex flex-row-reverse h-screen bg-white text-zinc-900 font-sans overflow-hidden">
    
    <!-- Sidebar -->
    <aside class="w-64 border-l border-zinc-200 bg-zinc-50/50 flex flex-col shrink-0 transition-all duration-300">
      <div class="h-14 flex items-center px-4 border-b border-zinc-200">
        <div class="w-7 h-7 bg-zinc-900 text-white rounded flex items-center justify-center font-bold mr-3 text-sm">M</div>
        <span class="font-semibold text-sm tracking-tight">MultiCLI Hub</span>
      </div>
      
      <div class="flex-1 overflow-y-auto py-4 px-3 space-y-1 custom-scrollbar">
        <!-- Navigation Links -->
        <router-link to="/multicli" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><DataBoard /></el-icon> 首页
          </div>
        </router-link>
        <router-link to="/multicli/projects" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><Folder /></el-icon> 项目
          </div>
        </router-link>
        <router-link to="/multicli/tasks" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><List /></el-icon> 任务
          </div>
        </router-link>
        <router-link to="/multicli/approvals" custom v-slot="{ navigate, isActive }">
          <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
            <el-icon class="mr-3 text-lg"><Check /></el-icon> 审批中心
          </div>
        </router-link>
        
        <div class="pt-4 pb-2" v-if="isLeader">
          <div class="px-3 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider mb-2">系统管理</div>
          <router-link to="/multicli/admin/engines" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><Setting /></el-icon> 执行引擎
            </div>
          </router-link>
          <router-link to="/multicli/admin/supervisors" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><Setting /></el-icon> Supervisor
            </div>
          </router-link>
          <router-link to="/multicli/admin/workers" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><Setting /></el-icon> Worker
            </div>
          </router-link>
          <router-link to="/multicli/billing/plans" custom v-slot="{ navigate, isActive }">
            <div @click="navigate" :class="['flex items-center px-3 py-2 rounded-md cursor-pointer text-sm font-medium transition-colors', isActive ? 'bg-zinc-200/60 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-200/40 hover:text-zinc-900']">
              <el-icon class="mr-3 text-lg"><ShoppingCart /></el-icon> 账单计划
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
              <div class="flex justify-between items-center px-2 py-1.5 hover:bg-zinc-100 rounded text-sm text-zinc-700">
                <span class="flex items-center"><el-icon class="mr-2"><Document /></el-icon>沉浸文档</span>
                <el-switch v-model="isIdeMode" size="small" />
              </div>
              <div class="flex justify-between items-center px-2 py-1.5 hover:bg-zinc-100 rounded text-sm text-zinc-700">
                <span class="flex items-center"><el-icon class="mr-2"><Monitor /></el-icon>multiCLi</span>
                <el-switch :model-value="true" @change="$router.push('/')" size="small" />
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
        <div class="flex items-center space-x-2 text-sm text-zinc-600 font-medium">
          <span>MultiCLI Hub</span>
          <span class="text-zinc-300">/</span>
          <span class="font-semibold text-zinc-900">{{ currentRouteName }}</span>
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
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { DataBoard, List, ChatLineSquare, Bell, Monitor, SwitchButton, Switch, Delete, Document, Calendar, ShoppingCart, Notification, Close, MoreFilled, Folder, Check, Setting } from '@element-plus/icons-vue'
import AIAssistant from '../components/AIAssistant.vue'
import FakeWord from '../components/FakeWord.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentRouteName = computed(() => {
  const map = {
    '/multicli': '首页',
    '/multicli/projects': '项目',
    '/multicli/tasks': '任务',
    '/multicli/approvals': '审批中心',
    '/multicli/admin/engines': '执行引擎',
    '/multicli/admin/supervisors': 'Supervisor',
    '/multicli/admin/workers': 'Worker',
    '/multicli/billing/plans': '账单计划',
  }
  return map[route.path] || ''
})

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

const isIdeMode = ref(false)

const isLeader = computed(() => {
  if (authStore.user?.role === 'teacher') return true;
  const membership = authStore.user?.project_memberships?.find(m => m.project_id === authStore.user?.current_project_id);
  return membership?.role === 'leader';
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
