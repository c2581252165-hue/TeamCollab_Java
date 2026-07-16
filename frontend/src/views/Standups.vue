<template>
  <div class="h-full flex flex-col animate-fade-in max-w-7xl mx-auto w-full" v-if="authStore.user?.current_project_id">
    <div class="mb-6 shrink-0">
      <h2 class="text-3xl font-extrabold text-slate-800 tracking-tight">每日站会</h2>
      <p class="text-slate-500 text-sm mt-1">同步您每日的进度、遇到的阻碍以及未来的计划。</p>
    </div>

    <div class="flex-1 flex flex-col lg:flex-row gap-8 min-h-0 pb-6">
      <!-- Submit Section -->
      <div class="w-full lg:w-5/12 bg-white p-6 rounded-2xl shadow-[0_2px_12px_rgba(0,0,0,0.04)] border border-slate-100 relative flex flex-col shrink-0 lg:h-full overflow-y-auto custom-scrollbar">
        <div class="absolute right-0 top-0 w-32 h-32 bg-gradient-to-bl from-blue-500/10 to-transparent pointer-events-none rounded-bl-full"></div>
        
        <div class="flex items-center justify-between mb-6 shrink-0">
          <h3 class="font-bold text-lg text-slate-800 flex items-center">
            <el-icon class="mr-2 text-blue-500"><EditPen /></el-icon>
            记录今日进展
          </h3>
          <el-button 
            @click="toggleVoiceRecord" 
            :type="isRecording ? 'danger' : 'primary'" 
            plain 
            class="!rounded-full shadow-sm shadow-blue-500/20"
            :class="{'animate-pulse': isRecording}"
            :loading="isFormattingVoice"
          >
            <span v-if="isFormattingVoice">🧠 AI 正在解析...</span>
            <span v-else-if="isRecording">🔴 正在倾听...</span>
            <span v-else>🎤 语音代写</span>
          </el-button>
        </div>

        <!-- Real-time transcription feedback -->
        <div v-if="isRecording || isFormattingVoice" class="bg-blue-50/50 rounded-xl p-4 border border-blue-100 mb-6 text-sm text-blue-800 italic shadow-inner shrink-0">
          <div class="flex items-center space-x-2 mb-2 font-semibold not-italic">
            <el-icon class="text-blue-500 animate-pulse"><Microphone /></el-icon>
            <span>{{ isFormattingVoice ? '录音完成，正在解析大意...' : '正在为您实时转写文字...' }}</span>
          </div>
          <div class="min-h-[2rem]">{{ liveTranscriptText || '请开始说话...' }}</div>
        </div>
        
        <el-form :model="newLog" label-position="top" class="flex-1 flex flex-col">
          <div class="space-y-4">
            <el-form-item class="font-semibold text-slate-700 !mb-2">
              <template #label>
                <div class="flex items-center justify-between w-full">
                  <span>昨天完成了什么？</span>
                  <el-button link type="primary" @click="prefillRecentTasks" size="small">
                    <el-icon class="mr-1"><MagicStick /></el-icon> 自动同步
                  </el-button>
                </div>
              </template>
              <el-input v-model="newLog.yesterday" type="textarea" :rows="3" placeholder="昨天我完成了..." class="custom-textarea" />
            </el-form-item>
            <el-form-item label="今天计划完成什么？" class="font-semibold text-slate-700 !mb-2">
              <el-input v-model="newLog.today" type="textarea" :rows="3" placeholder="今天我计划..." class="custom-textarea" />
            </el-form-item>
            <el-form-item label="遇到阻碍了吗？（可选）" class="font-semibold text-slate-700">
              <el-input v-model="newLog.blockers" type="textarea" :rows="2" placeholder="无" class="custom-textarea" />
            </el-form-item>
          </div>
          <div class="flex justify-end mt-auto pt-6">
            <el-button type="primary" @click="submitStandup" :loading="loading" class="!rounded-xl !h-10 !px-8 !bg-blue-600 !border-none shadow-md shadow-blue-500/30 hover:!bg-blue-700 transition-colors w-full">
              提交今日站会
            </el-button>
          </div>
        </el-form>
      </div>

      <!-- History Timeline -->
      <div class="w-full lg:w-7/12 bg-white p-6 rounded-2xl shadow-[0_2px_12px_rgba(0,0,0,0.04)] border border-slate-100 flex-1 overflow-hidden flex flex-col h-full">
        <h3 class="font-bold text-lg text-slate-800 mb-6 shrink-0 flex items-center">
          <el-icon class="mr-2 text-blue-500"><List /></el-icon>
          团队动态历史
        </h3>
        
        <div class="flex-1 overflow-y-auto custom-scrollbar pr-4 pb-4">
          <el-timeline v-if="logs.length">
            <el-timeline-item
              v-for="(log, index) in logs"
              :key="log.id"
              :type="index === 0 ? 'primary' : 'info'"
              :hollow="index !== 0"
              size="large"
              :timestamp="new Date(log.created_at || log.date).toLocaleString()"
              placement="top"
            >
              <div class="bg-slate-50 p-5 rounded-xl border border-slate-100 hover:border-slate-200 hover:bg-slate-50/80 transition-colors mt-2">
                <div class="flex items-center space-x-2 mb-4 pb-3 border-b border-slate-200/60">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-400 to-indigo-500 text-white flex items-center justify-center font-bold shadow-sm">
                    {{ log.user?.username.charAt(0).toUpperCase() }}
                  </div>
                  <div class="font-bold text-slate-700">{{ log.user?.username }}</div>
                </div>
                
                <div class="space-y-4">
                  <div>
                    <div class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">昨天完成了</div>
                    <div class="text-slate-700 text-sm leading-relaxed">{{ log.yesterday }}</div>
                  </div>
                  <div>
                    <div class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">今天计划做</div>
                    <div class="text-slate-700 text-sm leading-relaxed">{{ log.today }}</div>
                  </div>
                  <div v-if="log.blockers && log.blockers.toLowerCase() !== 'none' && log.blockers !== '无'">
                    <div class="text-xs font-bold text-red-400 uppercase tracking-wider mb-1 flex items-center">
                      <el-icon class="mr-1"><Warning /></el-icon> 遇到的阻碍
                    </div>
                    <div class="text-slate-700 text-sm leading-relaxed bg-red-50 p-2 rounded-lg border border-red-100">{{ log.blockers }}</div>
                  </div>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          
          <div v-else class="h-40 flex items-center justify-center text-slate-400">
            还没有任何记录。快来抢占沙发吧！
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <div v-else class="text-center py-20 animate-fade-in">
    <h2 class="text-2xl font-bold text-slate-700">您尚未加入任何团队</h2>
    <p class="text-slate-500 mt-2">请先在团队切换界面加入或创建一个团队。</p>
    <el-button type="primary" class="mt-6" @click="$router.push('/teams')">去切换/加入团队</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { useAuthStore } from '../stores/auth'
import { Microphone, EditPen, List, MagicStick, Warning } from '@element-plus/icons-vue'

const authStore = useAuthStore()

const newLog = ref({
  project_id: authStore.user?.current_project_id || 1,
  yesterday: '',
  today: '',
  blockers: ''
})

const logs = ref([])
const loading = ref(false)

const loadLogs = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get(`/standups/?project_id=${authStore.user.current_project_id}`)
    logs.value = res.data
  } catch (e) {
    ElMessage.error('加载历史记录失败')
  }
}

const prefillRecentTasks = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get(`/tasks/?project_id=${authStore.user.current_project_id}`)
    const recentTasks = res.data.filter(t => 
      t.assigned_to_id === authStore.user.id && 
      (t.status === 'review' || t.status === 'done')
    )
    if (recentTasks.length > 0) {
      newLog.value.yesterday = recentTasks.map(t => `✅ 推进/完成了任务：《${t.title}》`).join('\n')
      ElMessage.success('已自动同步您近期完成的任务！')
    } else {
      ElMessage.info('没有找到近期完成的任务。')
    }
  } catch(e) {}
}

const isRecording = ref(false)
const isFormattingVoice = ref(false)
const liveTranscriptText = ref('')
let recognition = null
let finalTranscript = ''
let currentInterim = ''

const initSpeechRecognition = () => {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    return false
  }
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  recognition = new SpeechRecognition()
  recognition.continuous = true
  recognition.interimResults = true
  recognition.lang = 'zh-CN'
  
  recognition.onstart = () => {
    finalTranscript = ''
    currentInterim = ''
    liveTranscriptText.value = ''
  }
  
  recognition.onresult = (event) => {
    currentInterim = ''
    for (let i = event.resultIndex; i < event.results.length; ++i) {
      if (event.results[i].isFinal) {
        finalTranscript += event.results[i][0].transcript
      } else {
        currentInterim += event.results[i][0].transcript
      }
    }
    liveTranscriptText.value = finalTranscript + currentInterim
  }
  
  recognition.onerror = (event) => {
    isRecording.value = false
    liveTranscriptText.value = ''
    if (event.error === 'not-allowed') {
      ElMessage.error('您拒绝了麦克风权限，无法使用语音功能。')
    } else if (event.error === 'no-speech') {
      ElMessage.warning('未检测到声音，已自动结束。')
    } else {
      ElMessage.error('语音识别出错: ' + event.error)
    }
  }

  recognition.onend = async () => {
    if (!isRecording.value && !liveTranscriptText.value) return // Prevent double firing if onerror already handled it
    isRecording.value = false
    const textToSend = (finalTranscript + currentInterim).trim()
    
    // Reset buffers for safety
    finalTranscript = ''
    currentInterim = ''
    
    if (textToSend !== '') {
      isFormattingVoice.value = true
      try {
        const res = await api.post('/ai/format-standup', { raw_text: textToSend })
        let d = res.data || {}
        if (d.standup) d = d.standup
        if (d.data) d = d.data
        
        const getVal = (target) => {
          if (!d || typeof d !== 'object') return null
          const key = Object.keys(d).find(k => k.toLowerCase().includes(target))
          return key ? d[key] : null
        }
        
        const y = getVal('yesterday') || getVal('昨天') || getVal('昨日') || d.yesterday
        const t = getVal('today') || getVal('今天') || getVal('今日') || d.today
        const b = getVal('blockers') || getVal('阻塞') || getVal('问题') || getVal('困难') || d.blockers
        
        let filled = false
        if (y && y !== '无' && y !== '解析失败') { newLog.value.yesterday = y; filled = true }
        if (t && t !== '无' && t !== '解析失败') { newLog.value.today = t; filled = true }
        if (b && b !== '无' && b !== '解析失败') { newLog.value.blockers = b; filled = true }
        
        if (!filled || (y === '解析失败' && t === '解析失败')) {
          newLog.value.yesterday = textToSend // fallback to raw text to ensure speech is never lost
        }
        
        ElMessage.success('语音已成功转换为文本！')
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || 'AI 解析语音失败')
      } finally {
        isFormattingVoice.value = false
        liveTranscriptText.value = ''
      }
    } else {
      liveTranscriptText.value = ''
    }
  }
  return true
}

const toggleVoiceRecord = () => {
  if (!recognition) {
    if (!initSpeechRecognition()) {
      ElMessage.error('您的浏览器不支持语音识别功能，请使用最新版 Chrome。')
      return
    }
  }
  
  if (isRecording.value) {
    recognition.stop()
  } else {
    try {
      recognition.start()
      isRecording.value = true
      ElMessage.success('开始录音，请讲述您的昨日工作和今日计划...')
    } catch(e) {
      recognition.stop()
    }
  }
}

onMounted(() => {
  loadLogs()
  prefillRecentTasks() // Auto fetch on load
})

const submitStandup = async () => {
  if (!newLog.value.yesterday || !newLog.value.today) {
    ElMessage.warning('请填写昨天和今天的计划。')
    return
  }
  
  loading.value = true
  try {
    const payload = { ...newLog.value, project_id: authStore.user.current_project_id }
    if (!payload.blockers) payload.blockers = '无'
    
    await api.post('/standups/', payload)
    ElMessage.success('提交成功！')
    
    // reset form
    newLog.value.yesterday = ''
    newLog.value.today = ''
    newLog.value.blockers = ''
    
    // reload
    loadLogs()
  } catch (err) {
    ElMessage.error('提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<style>
.custom-textarea .el-textarea__inner {
  border-radius: 12px;
  background-color: #f8fafc;
  border-color: #e2e8f0;
  padding: 12px;
  transition: all 0.3s;
}
.custom-textarea .el-textarea__inner:focus {
  background-color: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}
</style>
