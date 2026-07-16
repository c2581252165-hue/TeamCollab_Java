<template>
  <div class="fixed bottom-8 right-8 z-50 flex flex-col items-end">
    <!-- Chat Window -->
    <transition name="el-zoom-in-bottom">
      <div v-if="isOpen" class="bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl border border-zinc-200 w-80 md:w-96 mb-4 overflow-hidden flex flex-col" style="height: 550px;">
        <div class="bg-white/80 backdrop-blur-md p-4 text-zinc-900 flex justify-between items-center z-10 border-b border-zinc-200">
          <div class="flex items-center space-x-2">
            <el-icon class="text-xl text-zinc-900"><MagicStick /></el-icon>
            <span class="font-bold tracking-tight">AI 智能助手</span>
          </div>
          <el-icon class="cursor-pointer text-zinc-400 hover:text-zinc-900 transition-colors" @click="isOpen = false"><Close /></el-icon>
        </div>
        
        <div class="flex-1 overflow-y-auto p-4 custom-scrollbar bg-zinc-50/50 flex flex-col space-y-4" ref="chatContainer">
          
          <!-- Chat Messages -->
          <div v-for="(msg, index) in messages" :key="index" class="flex items-start space-x-2" :class="{'flex-row-reverse space-x-reverse': msg.role === 'user'}">
            <!-- Avatar -->
            <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 border" :class="msg.role === 'user' ? 'bg-white border-zinc-200 text-zinc-900 shadow-sm' : 'bg-zinc-900 border-zinc-900 text-white shadow-sm'">
              <el-icon v-if="msg.role === 'assistant'"><Monitor /></el-icon>
              <el-icon v-else><User /></el-icon>
            </div>
            
            <!-- Bubble -->
            <div class="p-3 rounded-2xl shadow-sm text-sm border" 
                 :class="msg.role === 'user' ? 'bg-zinc-900 text-white border-zinc-900 rounded-tr-none' : 'bg-white text-zinc-800 border-zinc-200 rounded-tl-none'"
                 style="max-width: 85%;">
              <div class="whitespace-pre-wrap leading-relaxed">{{ msg.text }}</div>
              
              <!-- Download button if it's a report -->
              <div v-if="msg.isReport" class="mt-3 pt-2 border-t border-slate-100 flex justify-end">
                <el-button size="small" type="primary" plain @click="downloadReport(msg.text)" class="!rounded-lg">
                  <el-icon class="mr-1"><Download /></el-icon> 下载报告 (MD)
                </el-button>
              </div>
            </div>
          </div>
          
          <!-- Loading Bubble -->
          <div v-if="isGeneratingReport" class="flex items-start space-x-2">
            <div class="w-8 h-8 rounded-full bg-zinc-900 text-white border border-zinc-900 shadow-sm flex items-center justify-center flex-shrink-0">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div class="bg-white p-3 rounded-2xl rounded-tl-none shadow-sm text-sm text-zinc-500 border border-zinc-200">
              <div class="flex items-center space-x-2">
                <span>正在思考并撰写中，请耐心等待</span>
                <span class="flex space-x-1">
                  <span class="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-bounce"></span>
                  <span class="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></span>
                  <span class="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-bounce" style="animation-delay: 0.4s"></span>
                </span>
              </div>
            </div>
          </div>

        </div>

        <!-- Input Area / Actions -->
        <div class="p-4 bg-white/90 backdrop-blur-md border-t border-zinc-200 shadow-[0_-4px_20px_rgba(0,0,0,0.02)] z-10 relative shrink-0">
          <!-- Quick Actions Row -->
          <div v-if="!mode" class="flex space-x-2 mb-3 overflow-x-auto pb-1 custom-scrollbar">
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="mode = 'breakdown'">
              <el-icon class="text-zinc-700"><Position /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">任务拆解</span>
            </div>
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="generateReport">
              <el-icon class="text-zinc-700"><Document /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">项目周报</span>
            </div>
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="generateStandupSummary">
              <el-icon class="text-zinc-700"><EditPen /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">站会总结</span>
            </div>
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="generateRiskAssessment">
              <el-icon class="text-zinc-700"><Warning /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">风险评估</span>
            </div>
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="generateWorkloadAnalysis">
              <el-icon class="text-zinc-700"><PieChart /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">工作量分析</span>
            </div>
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="mode = 'polish'">
              <el-icon class="text-zinc-700"><MagicStick /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">润色需求</span>
            </div>
            <div class="flex-shrink-0 bg-white px-3 py-1.5 rounded-md border border-zinc-200 hover:border-zinc-400 hover:shadow-sm transition-all cursor-pointer flex items-center space-x-1" @click="mode = 'code'">
              <el-icon class="text-zinc-700"><Monitor /></el-icon>
              <span class="font-medium text-zinc-700 text-xs">诊断报错</span>
            </div>
          </div>
          
          <!-- Mode Warning -->
          <div v-if="mode" class="flex justify-between items-center mb-2 px-1">
            <span v-if="mode === 'breakdown'" class="text-xs font-semibold text-zinc-700 flex items-center"><el-icon class="mr-1"><Position /></el-icon> 请输入宏观目标，让我为您拆分</span>
            <span v-if="mode === 'polish'" class="text-xs font-semibold text-zinc-700 flex items-center"><el-icon class="mr-1"><MagicStick /></el-icon> 请输入大白话，为您扩写为需求文档</span>
            <span v-if="mode === 'code'" class="text-xs font-semibold text-zinc-700 flex items-center"><el-icon class="mr-1"><Monitor /></el-icon> 请粘贴报错日志或代码，我来诊断排查</span>
            <el-icon class="cursor-pointer text-zinc-400 hover:text-red-500 bg-zinc-100 rounded p-0.5 transition-colors" @click="mode = ''"><Close /></el-icon>
          </div>
          
          <!-- Chat Input -->
          <div class="relative flex items-end bg-white border border-zinc-200 rounded-lg overflow-hidden focus-within:border-zinc-500 focus-within:ring-1 focus-within:ring-zinc-500 transition-all shadow-sm">
            <el-input 
              v-model="aiGoal" 
              type="textarea" 
              :autosize="{ minRows: 1, maxRows: 4 }"
              :placeholder="mode === 'breakdown' ? '例如：开发登录与注册模块...' : '问我任何关于项目的问题...'"
              class="flex-1 custom-chat-input"
              @keydown.enter.prevent="sendMessage"
            />
            <div class="p-1.5">
              <button 
                @click="sendMessage" 
                :disabled="!aiGoal.trim() || loadingAI || isGeneratingReport"
                class="w-8 h-8 rounded flex items-center justify-center transition-all"
                :class="aiGoal.trim() && !loadingAI && !isGeneratingReport ? 'bg-zinc-900 text-white hover:bg-zinc-800 cursor-pointer shadow-sm' : 'bg-zinc-100 text-zinc-400 cursor-not-allowed'"
              >
                <el-icon><Position /></el-icon>
              </button>
            </div>
          </div>
        </div>

      </div>
    </transition>

    <!-- Floating Button -->
    <div 
      @click="isOpen = !isOpen"
      class="w-14 h-14 bg-zinc-900 rounded-full shadow-lg shadow-zinc-900/20 flex items-center justify-center text-white cursor-pointer hover:scale-105 hover:bg-zinc-800 active:scale-95 transition-all duration-300"
    >
      <el-icon :size="24"><MagicStick /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { MagicStick, Close, Monitor, ChatDotRound, User, Download, Loading, Position, Document, EditPen, Warning, PieChart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const isOpen = ref(false)
const mode = ref('')
const aiGoal = ref('')
const loadingAI = ref(false)
const isGeneratingReport = ref(false)
const chatContainer = ref(null)

const messages = ref([
  { role: 'assistant', text: '您好！我是您的专属 AI 助手。您可以随时使用底部的快捷指令让我为您打工。' }
])

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!aiGoal.value.trim() || loadingAI.value || isGeneratingReport.value) return
  
  if (mode.value === 'breakdown') {
    await generateSubtasks()
  } else if (mode.value === 'polish') {
    await sendChat("请帮我将以下大白话扩写成一份专业、详细的项目需求文档，注重结构清晰和术语准确：\n\n" + aiGoal.value.trim())
    mode.value = ''
  } else if (mode.value === 'code') {
    await sendChat("请帮我分析以下代码或报错信息，并给出修复建议或优化方案：\n\n" + aiGoal.value.trim())
    mode.value = ''
  } else {
    await sendChat()
  }
}

const sendChat = async (payloadText = null) => {
  if (!authStore.user?.current_project_id) {
    ElMessage.error('请先加入一个团队')
    return
  }
  
  const userText = aiGoal.value.trim()
  const textToSend = payloadText || userText
  aiGoal.value = ''
  
  messages.value.push({ role: 'user', text: userText })
  scrollToBottom()
  
  isGeneratingReport.value = true
  
  try {
    const res = await api.post('/ai/chat', { message: textToSend, project_id: authStore.user.current_project_id })
    messages.value.push({ 
      role: 'assistant', 
      text: res.data.reply
    })
  } catch (err) {
    messages.value.push({ 
      role: 'assistant', 
      text: `❌ 抱歉，我脑子卡壳了：${err.response?.data?.detail || '网络错误'}`
    })
  } finally {
    isGeneratingReport.value = false
    scrollToBottom()
  }
}

const generateSubtasks = async () => {
  if (!aiGoal.value) {
    ElMessage.warning('请输入您的宏观目标')
    return
  }
  if (!authStore.user?.current_project_id) {
    ElMessage.error('请先加入一个团队')
    return
  }
  
  messages.value.push({ role: 'user', text: `请帮我拆解任务：${aiGoal.value}` })
  scrollToBottom()
  
  loadingAI.value = true
  isGeneratingReport.value = true
  
  try {
    const res = await api.post('/ai/breakdown-task', { goal: aiGoal.value, project_id: authStore.user.current_project_id })
    messages.value.push({ 
      role: 'assistant', 
      text: `✅ 搞定！我已经为您成功拆解了 ${res.data.length} 个子任务，并直接挂载到了任务看板中，快去看看吧！`
    })
    aiGoal.value = ''
    mode.value = ''
  } catch (err) {
    messages.value.push({ 
      role: 'assistant', 
      text: `❌ 抱歉，拆解失败了：${err.response?.data?.detail || '网络错误'}`
    })
  } finally {
    loadingAI.value = false
    isGeneratingReport.value = false
    scrollToBottom()
  }
}

const generateReport = async () => {
  if (!authStore.user?.current_project_id) {
    ElMessage.error('请先加入一个团队')
    return
  }
  
  messages.value.push({ role: 'user', text: '请帮我生成本周的项目进度报告' })
  isGeneratingReport.value = true
  scrollToBottom()
  
  try {
    const res = await api.post('/ai/weekly-report', { project_id: authStore.user.current_project_id })
    messages.value.push({ 
      role: 'assistant', 
      text: res.data.report,
      isReport: true
    })
  } catch (err) {
    messages.value.push({ 
      role: 'assistant', 
      text: `❌ 生成周报失败了：${err.response?.data?.detail || '网络错误'}`
    })
  } finally {
    isGeneratingReport.value = false
    scrollToBottom()
  }
}

const generateStandupSummary = async () => {
  if (!authStore.user?.current_project_id) {
    ElMessage.error('请先加入一个团队')
    return
  }
  messages.value.push({ role: 'user', text: '请帮我一键生成今日团队站会总结' })
  isGeneratingReport.value = true
  scrollToBottom()
  try {
    const res = await api.post('/ai/standup-summary', { project_id: authStore.user.current_project_id })
    messages.value.push({ role: 'assistant', text: res.data.reply })
  } catch (err) {
    messages.value.push({ role: 'assistant', text: `❌ 生成站会总结失败：${err.response?.data?.detail || '网络错误'}` })
  } finally {
    isGeneratingReport.value = false
    scrollToBottom()
  }
}

const generateRiskAssessment = async () => {
  if (!authStore.user?.current_project_id) {
    ElMessage.error('请先加入一个团队')
    return
  }
  messages.value.push({ role: 'user', text: '请帮我评估当前项目进度是否存在延期风险' })
  isGeneratingReport.value = true
  scrollToBottom()
  try {
    const res = await api.post('/ai/risk-assessment', { project_id: authStore.user.current_project_id })
    messages.value.push({ role: 'assistant', text: res.data.reply })
  } catch (err) {
    messages.value.push({ role: 'assistant', text: `❌ 生成风险评估失败：${err.response?.data?.detail || '网络错误'}` })
  } finally {
    isGeneratingReport.value = false
    scrollToBottom()
  }
}

const generateWorkloadAnalysis = async () => {
  if (!authStore.user?.current_project_id) {
    ElMessage.error('请先加入一个团队')
    return
  }
  messages.value.push({ role: 'user', text: '请帮我分析团队成员目前的工作负荷情况' })
  isGeneratingReport.value = true
  scrollToBottom()
  try {
    const res = await api.post('/ai/workload-analysis', { project_id: authStore.user.current_project_id })
    messages.value.push({ role: 'assistant', text: res.data.reply })
  } catch (err) {
    messages.value.push({ role: 'assistant', text: `❌ 生成工作量分析失败：${err.response?.data?.detail || '网络错误'}` })
  } finally {
    isGeneratingReport.value = false
    scrollToBottom()
  }
}

const downloadReport = (content) => {
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `项目周报_${new Date().toLocaleDateString().replace(/\//g, '-')}.md`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('报告已开始下载');
}
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
:deep(.custom-chat-input .el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  resize: none !important;
  padding: 12px 14px !important;
  color: #334155;
}
:deep(.custom-chat-input .el-textarea__inner:focus) {
  box-shadow: none !important;
}
</style>
