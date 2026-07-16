<template>
  <div class="fixed inset-0 z-50 flex flex-col font-sans transition-colors duration-300" :class="isCodeIdeMode ? 'bg-[#1e1e1e] text-[#d4d4d4] is-ide-mode' : 'bg-zinc-100 text-zinc-900'">
    
    <!-- Title Bar -->
    <div class="h-10 text-white flex items-center px-4 justify-between select-none shadow-sm transition-colors" :class="isCodeIdeMode ? 'bg-[#333333]' : 'bg-blue-600'">
      <div class="flex items-center space-x-4">
        <el-icon class="text-xl"><Document /></el-icon>
        <span class="font-semibold text-sm">TeamCollab Docs</span>
        <span v-if="activeFile" class="text-xs opacity-80 border-l border-white/30 pl-3">{{ activeFile.replace(/\//g, ' > ') }}</span>
      </div>
      
      <div class="flex items-center space-x-3">
        <el-button type="warning" @click="isCodeIdeMode = !isCodeIdeMode" class="!border-none !h-7 !px-3 !text-xs !rounded shadow" :class="isCodeIdeMode ? '!bg-amber-600 hover:!bg-amber-700 !text-white' : '!bg-amber-500 hover:!bg-amber-600 !text-white'">
          <el-icon class="mr-1"><Monitor /></el-icon> {{ isCodeIdeMode ? '退出 IDE 模式' : 'IDE 模式启动' }}
        </el-button>
        <el-button type="primary" v-if="activeFile" @click="saveFile" :loading="isSaving" class="!bg-blue-500 hover:!bg-blue-400 !text-white !border-none !h-7 !px-3 !text-xs !rounded shadow">
          <el-icon class="mr-1"><Check /></el-icon> 保存 (Ctrl+S)
        </el-button>
        <el-button type="info" @click="downloadZip" class="!bg-blue-700 hover:!bg-blue-800 !text-white !border-none !h-7 !px-3 !text-xs !rounded shadow">
          <el-icon class="mr-1"><Download /></el-icon> 导出打包
        </el-button>
        <el-button type="danger" @click="closeWord" class="!bg-red-500 hover:!bg-red-600 !text-white !border-none !h-7 !px-3 !text-xs !rounded shadow">
          <el-icon class="mr-1"><SwitchButton /></el-icon> 退出文档模式
        </el-button>
      </div>
    </div>

    <!-- Office Ribbon -->
    <div v-show="!isCodeIdeMode" class="bg-zinc-50 border-b border-zinc-200 select-none shadow-sm z-10">
      <div class="flex px-2 pt-2 space-x-1 text-sm">
        <div class="px-4 py-1.5 cursor-pointer text-blue-600 font-medium border-b-2 border-blue-600">开始 (Home)</div>
        <div class="px-4 py-1.5 cursor-pointer text-zinc-600 hover:bg-zinc-100 rounded-t" @click="showNotAvailable('插入')">插入 (Insert)</div>
        <div class="px-4 py-1.5 cursor-pointer text-zinc-600 hover:bg-zinc-100 rounded-t" @click="showNotAvailable('布局')">布局 (Layout)</div>
        <div class="px-4 py-1.5 cursor-pointer text-zinc-600 hover:bg-zinc-100 rounded-t" @click="showNotAvailable('审阅')">审阅 (Review)</div>
        <div class="px-4 py-1.5 cursor-pointer text-zinc-600 hover:bg-zinc-100 rounded-t" @click="showNotAvailable('视图')">视图 (View)</div>
      </div>
      <div class="bg-white px-4 py-2 flex items-center space-x-6 border-t border-zinc-200">
        <div class="flex items-center space-x-2">
          <select class="border border-zinc-300 rounded px-2 py-1 text-sm text-zinc-700 bg-white outline-none cursor-pointer">
            <option>宋体 (SimSun)</option>
            <option>微软雅黑 (YaHei)</option>
            <option>Times New Roman</option>
            <option>Arial</option>
          </select>
          <select class="border border-zinc-300 rounded px-2 py-1 text-sm text-zinc-700 bg-white outline-none cursor-pointer w-16">
            <option>12</option>
            <option>14</option>
            <option>16</option>
            <option selected>18</option>
            <option>24</option>
            <option>36</option>
          </select>
        </div>
        <div class="h-6 w-px bg-zinc-300"></div>
        <div class="flex items-center space-x-1">
          <div class="w-8 h-8 flex items-center justify-center rounded hover:bg-zinc-100 cursor-pointer font-bold font-serif" @click="showNotAvailable('加粗')">B</div>
          <div class="w-8 h-8 flex items-center justify-center rounded hover:bg-zinc-100 cursor-pointer italic font-serif" @click="showNotAvailable('倾斜')">I</div>
          <div class="w-8 h-8 flex items-center justify-center rounded hover:bg-zinc-100 cursor-pointer underline font-serif" @click="showNotAvailable('下划线')">U</div>
        </div>
        <div class="h-6 w-px bg-zinc-300"></div>
        <div class="flex items-center space-x-1">
          <el-icon class="w-8 h-8 flex items-center justify-center rounded hover:bg-zinc-100 cursor-pointer text-lg"><Files /></el-icon>
          <el-icon class="w-8 h-8 flex items-center justify-center rounded hover:bg-zinc-100 cursor-pointer text-lg"><Picture /></el-icon>
        </div>
        <div class="h-6 w-px bg-zinc-300"></div>
        <div class="flex items-center space-x-2">
          <el-button size="small" type="primary" plain @click="createNewFile">新建空白文档</el-button>
          <el-button size="small" @click="toggleSidebar">
            {{ showSidebar ? '隐藏文档列表' : '展开文档列表' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex flex-1 overflow-hidden relative">
      
      <!-- Document Sidebar -->
      <div v-show="showSidebar" class="w-64 flex flex-col shrink-0 transition-colors z-20" :class="isCodeIdeMode ? 'bg-[#252526] border-r border-[#3c3c3c]' : 'bg-zinc-50 border-r border-zinc-200'">
        <div class="px-4 py-3 text-xs font-bold tracking-wider border-b transition-colors" :class="isCodeIdeMode ? 'bg-[#2d2d2d] border-[#3c3c3c] text-zinc-400' : 'text-zinc-500 bg-zinc-100 border-zinc-200'">我的文档空间</div>
        <div class="flex-1 overflow-y-auto custom-scrollbar p-2">
          <el-tree
            v-if="fileTree"
            :data="[fileTree]"
            :props="defaultProps"
            node-key="path"
            :default-expanded-keys="['']"
            @node-click="handleNodeClick"
            class="word-tree"
            empty-text="加载文档中..."
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node flex items-center justify-between w-full pr-2 py-1 group">
                <div class="flex items-center space-x-2 truncate">
                  <el-icon v-if="data.type === 'directory'" class="text-yellow-500 text-lg"><Folder /></el-icon>
                  <el-icon v-else class="text-blue-500 text-lg"><Document /></el-icon>
                  <span class="text-sm truncate transition-colors" :class="activeFile === data.path ? 'font-bold text-blue-500' : (isCodeIdeMode ? 'text-zinc-300' : 'text-zinc-700')" :title="node.label">{{ node.label }}</span>
                </div>
                
                <el-dropdown trigger="click" @command="cmd => handleFileAction(cmd, data)" v-if="data.path !== ''">
                  <span class="el-dropdown-link opacity-0 group-hover:opacity-100 transition-opacity p-1 hover:bg-zinc-200 rounded cursor-pointer" @click.stop>
                    <el-icon><MoreFilled /></el-icon>
                  </span>
                  <template #dropdown>
                      <el-dropdown-item v-if="data.type === 'directory'" command="new_file"><el-icon><DocumentAdd /></el-icon>新建文档</el-dropdown-item>
                      <el-dropdown-item v-if="data.type === 'directory'" command="new_folder"><el-icon><FolderAdd /></el-icon>新建文件夹</el-dropdown-item>
                      <el-dropdown-item v-if="data.type === 'directory'" command="upload"><el-icon><Upload /></el-icon>上传文件</el-dropdown-item>
                      <el-dropdown-item v-if="data.type === 'file'" command="download"><el-icon><Download /></el-icon>下载</el-dropdown-item>
                      <el-dropdown-item command="rename"><el-icon><EditPen /></el-icon>重命名</el-dropdown-item>
                      <el-dropdown-item command="move"><el-icon><Position /></el-icon>移动到...</el-dropdown-item>
                      <el-dropdown-item command="delete" class="!text-red-500"><el-icon><Delete /></el-icon>删除</el-dropdown-item>
                  </template>
                </el-dropdown>
                
                <!-- Root node special actions -->
                <el-dropdown trigger="click" @command="cmd => handleFileAction(cmd, data)" v-else>
                  <span class="el-dropdown-link opacity-0 group-hover:opacity-100 transition-opacity p-1 hover:bg-zinc-200 rounded cursor-pointer" @click.stop>
                    <el-icon><MoreFilled /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="new_file"><el-icon><DocumentAdd /></el-icon>新建文档</el-dropdown-item>
                      <el-dropdown-item command="new_folder"><el-icon><FolderAdd /></el-icon>新建文件夹</el-dropdown-item>
                      <el-dropdown-item command="upload"><el-icon><Upload /></el-icon>上传文件</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- Editor Area -->
      <div class="flex-1 flex relative" :class="isCodeIdeMode ? 'bg-[#1e1e1e] p-0 flex-row items-stretch overflow-hidden' : 'bg-zinc-200/60 flex-col items-center py-8 overflow-auto custom-scrollbar'">
        
        <div class="flex-1 flex flex-col w-full h-full items-center relative">
          <div v-if="!activeFile" class="flex flex-col items-center justify-center h-full" :class="isCodeIdeMode ? 'text-zinc-500' : 'text-zinc-400'">
            <el-icon class="text-8xl mb-6 opacity-30"><Document /></el-icon>
            <p class="text-xl font-semibold mb-2" :class="isCodeIdeMode ? 'text-zinc-400' : 'text-zinc-500'">TeamCollab Java {{ isCodeIdeMode ? 'IDE' : 'Word' }}</p>
            <p class="text-sm">请在左侧选择一个文档，或点击上方“新建空白文档”。</p>
          </div>
          
          <div v-else-if="isLoadingFile" class="flex items-center justify-center h-full text-zinc-500">
            <el-icon class="is-loading mr-2"><Loading /></el-icon> 正在加载文档内容...
          </div>
  
          <!-- Active File in IDE Mode -->
          <div v-else-if="isCodeIdeMode" class="w-full h-full flex flex-col">
             <!-- IDE Tabs -->
             <div class="h-9 bg-[#2d2d2d] flex items-center px-3 border-b border-[#1e1e1e]">
               <div class="bg-[#1e1e1e] text-[#d4d4d4] px-4 h-full text-xs border-t-[3px] border-blue-500 flex items-center">
                 <el-icon class="mr-2 text-blue-400"><Document /></el-icon> {{ activeFile.split('/').pop() }}
               </div>
             </div>
             <!-- Gutter & Editor -->
             <div class="flex-1 flex bg-[#1e1e1e] overflow-hidden relative">
               <!-- Line numbers mockup -->
               <div class="w-12 bg-[#1e1e1e] border-r border-[#333] text-[#858585] text-right pr-3 pt-4 select-none font-mono text-[14px] leading-relaxed hidden sm:block">
                 <div v-for="i in Math.max(1, (fileContent.match(/\n/g)||[]).length + 1)" :key="i">{{ i }}</div>
               </div>
                <div v-if="isDocxPreview" class="flex-1 bg-[#1e1e1e] text-[#d4d4d4] p-8 pt-4 overflow-auto custom-ide-scrollbar prose prose-invert max-w-none font-sans" v-html="docxHtml"></div>
                <textarea v-else
                  class="flex-1 bg-[#1e1e1e] text-[#d4d4d4] p-4 pt-4 outline-none resize-none overflow-auto custom-ide-scrollbar font-mono text-[14px] leading-relaxed whitespace-pre"
                  spellcheck="false"
                  v-model="fileContent"
                  @keydown.ctrl.s.prevent="saveFile"
                  @keydown.meta.s.prevent="saveFile"
                  placeholder="/* Write code here... */"
                ></textarea>
             </div>
          </div>
  
          <!-- The "A4 Paper" (Word Mode) -->
          <div v-else class="w-[850px] min-h-[1100px] bg-white shadow-xl border border-zinc-200 flex flex-col mb-12 shrink-0">
            <!-- Page Header -->
            <div class="h-16 px-16 flex items-end pb-2 border-b border-zinc-100">
              <div class="w-full flex justify-between text-xs text-zinc-400 font-serif">
                <span>TeamCollab Document</span>
                <span>{{ activeFile.split('/').pop() }}</span>
              </div>
            </div>
            
            <!-- Page Body Textarea -->
            <div v-if="isDocxPreview" class="w-full flex-1 bg-transparent text-zinc-800 p-16 pt-10 overflow-hidden text-lg leading-relaxed font-sans prose max-w-none docx-preview-content" v-html="docxHtml"></div>
            <textarea v-else
              class="w-full flex-1 bg-transparent text-zinc-800 p-16 pt-10 outline-none resize-none overflow-hidden text-lg leading-relaxed font-sans"
              spellcheck="false"
              v-model="fileContent"
              @keydown.ctrl.s.prevent="saveFile"
              @keydown.meta.s.prevent="saveFile"
              placeholder="在这里开始您的创作..."
              @input="autoResize"
              ref="textareaRef"
            ></textarea>
          </div>
        </div>

        <!-- Right AI Sidebar (Only in IDE mode) -->
        <div v-if="isCodeIdeMode" class="w-[320px] bg-[#252526] border-l border-[#333333] flex flex-col shrink-0 text-[#cccccc] relative z-10">
           <!-- Title -->
           <div class="h-[35px] px-4 flex items-center justify-between text-[11px] uppercase tracking-wider text-[#cccccc] select-none">
             <span>Chat</span>
             <el-icon class="cursor-pointer hover:text-white"><MoreFilled /></el-icon>
           </div>
           
           <!-- Chat Area -->
           <div class="flex-1 overflow-y-auto px-4 py-2 space-y-6 custom-ide-scrollbar">
             <div v-for="(msg, idx) in aiChatMessages" :key="idx" class="flex flex-col">
               <div class="flex items-center space-x-2 mb-1.5">
                 <div v-if="msg.role === 'ai'" class="w-5 h-5 rounded-full bg-[#3c3c3c] flex items-center justify-center border border-[#4c4c4c]">
                   <el-icon class="text-[12px] text-blue-400"><Monitor /></el-icon>
                 </div>
                 <div v-else class="w-5 h-5 rounded-full bg-[#0e639c] flex items-center justify-center text-white">
                   <el-icon class="text-[12px]"><User /></el-icon>
                 </div>
                 <span class="text-[13px] font-semibold text-[#e5e5e5]">{{ msg.role === 'ai' ? 'Copilot' : 'You' }}</span>
               </div>
               <div class="text-[13px] leading-[1.6] text-[#cccccc] whitespace-pre-wrap ml-7">
                 {{ msg.content }}
               </div>
             </div>
             <div v-if="isSendingChat" class="flex flex-col">
               <div class="flex items-center space-x-2 mb-1.5">
                 <div class="w-5 h-5 rounded-full bg-[#3c3c3c] flex items-center justify-center border border-[#4c4c4c]">
                   <el-icon class="text-[12px] text-blue-400 is-loading"><Loading /></el-icon>
                 </div>
                 <span class="text-[13px] font-semibold text-[#e5e5e5]">Copilot</span>
               </div>
               <div class="text-[13px] leading-[1.6] text-[#858585] ml-7">
                 Thinking...
               </div>
             </div>
           </div>

           <!-- Input Area -->
           <div class="p-4 bg-[#252526]">
             <div class="flex flex-col bg-[#3c3c3c] border border-[#3c3c3c] focus-within:border-[#007fd4] rounded-[2px] transition-colors relative">
               <textarea 
                 v-model="aiChatInput"
                 @keydown.enter.prevent="sendAiMessage"
                 placeholder="Ask Copilot or type / for commands"
                 class="w-full bg-transparent text-[#cccccc] text-[13px] p-2 pb-8 outline-none resize-none h-20 custom-ide-scrollbar"
               ></textarea>
               <div class="absolute bottom-1 right-1 flex items-center">
                 <div @click="sendAiMessage" :class="aiChatInput.trim() && !isSendingChat ? 'text-[#cccccc] hover:bg-[#505050] cursor-pointer' : 'text-[#6b6b6b] cursor-default'" class="w-6 h-6 rounded flex items-center justify-center transition-colors">
                   <el-icon class="text-[14px]"><Position /></el-icon>
                 </div>
               </div>
             </div>
           </div>
        </div>

      </div>
    </div>

    <!-- Status Bar -->
    <div class="h-6 flex items-center px-4 justify-between text-xs select-none border-t transition-colors" :class="isCodeIdeMode ? 'bg-[#007acc] text-white border-[#007acc]' : 'bg-zinc-100 text-zinc-600 border-zinc-200'">
      <div class="flex items-center space-x-4">
        <el-dropdown trigger="click" @command="changeEncoding" placement="top">
          <span class="cursor-pointer hover:underline flex items-center h-full outline-none">
            {{ currentEncoding }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="UTF-8" :class="{'font-bold text-blue-500': currentEncoding === 'UTF-8'}">UTF-8 (Web/Linux/推荐)</el-dropdown-item>
              <el-dropdown-item command="GBK" :class="{'font-bold text-blue-500': currentEncoding === 'GBK'}">GBK (Windows 简体中文)</el-dropdown-item>
              <el-dropdown-item command="GB2312" :class="{'font-bold text-blue-500': currentEncoding === 'GB2312'}">GB2312 (早期中文标准)</el-dropdown-item>
              <el-dropdown-item command="ISO-8859-1" :class="{'font-bold text-blue-500': currentEncoding === 'ISO-8859-1'}">ISO-8859-1 (西欧/纯英文)</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <span v-if="!isCodeIdeMode">页面 1 / 1</span>
        <span>{{ fileContent.length }} {{ isCodeIdeMode ? 'Characters' : '个字数' }}</span>
      </div>
      <div class="flex items-center space-x-4">
        <span>{{ isCodeIdeMode ? 'Vue/JS' : '中文 (中国)' }}</span>
        <div class="flex items-center space-x-1 cursor-pointer">
          <el-icon><ZoomOut /></el-icon>
          <input type="range" min="50" max="150" value="100" :class="isCodeIdeMode ? 'accent-white' : 'accent-blue-600'" class="w-24">
          <el-icon><ZoomIn /></el-icon>
        </div>
        <span>100%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { Document, Folder, FolderAdd, SwitchButton, Check, Download, Upload, Picture, Files, Loading, ZoomIn, ZoomOut, MoreFilled, EditPen, Delete, Monitor, ChatDotRound, Position, User } from '@element-plus/icons-vue'
import api from '../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as mammoth from 'mammoth'

const emit = defineEmits(['close'])

const fileTree = ref(null)
const defaultProps = {
  children: 'children',
  label: 'name'
}

const showSidebar = ref(true)
const activeFile = ref(null)
const fileContent = ref('')
const isLoadingFile = ref(false)
const isSaving = ref(false)
const isGeneratingCode = ref(false)
const textareaRef = ref(null)

const isCodeIdeMode = ref(false)
const aiChatMessages = ref([
  { role: 'ai', content: '您好！我是您的 AI 结对编程助手。您可以用代码相关的任何问题来问我，也可以在左侧编辑器中使用 <AI>提示词</AI> 来让我自动生成代码。' }
])
const aiChatInput = ref('')
const isSendingChat = ref(false)
const currentEncoding = ref('UTF-8')
const isDocxPreview = ref(false)
const docxHtml = ref('')

const sendAiMessage = async () => {
  const text = aiChatInput.value.trim()
  if (!text || isSendingChat.value) return
  
  aiChatMessages.value.push({ role: 'user', content: text })
  aiChatInput.value = ''
  isSendingChat.value = true
  
  try {
    const res = await api.post('/ai/chat', { message: text })
    let reply = res.data.reply

    // Parse <create_file path="...">...</create_file>
    const fileRegex = /<create_file\s+(?:path|filename|name|file)=["']([^"']+)["']>([\s\S]*?)<\/create_file>/gi
    let match
    let createdFiles = []
    
    // Execute all file creations found in the response
    while ((match = fileRegex.exec(reply)) !== null) {
      const path = match[1]
      const content = match[2].trim()
      try {
        await api.post('/ide/content', { path, content })
        createdFiles.push(path)
      } catch (err) {
        console.error("Failed to create file via AI:", path, err)
      }
    }
    
    if (createdFiles.length > 0) {
      await fetchTree()
      // Replace the raw tags with a user-friendly message
      reply = reply.replace(fileRegex, '\n_[✨ 已经为您自动创建了文件: **$1**]_\n')
    }

    aiChatMessages.value.push({ role: 'ai', content: reply.trim() })
  } catch (err) {
    aiChatMessages.value.push({ role: 'ai', content: '抱歉，服务请求失败，请稍后再试。' })
  } finally {
    isSendingChat.value = false
    
    // Scroll chat to bottom
    setTimeout(() => {
      const chatArea = document.querySelector('.custom-ide-scrollbar.overflow-y-auto')
      if (chatArea) {
        chatArea.scrollTop = chatArea.scrollHeight
      }
    }, 50)
  }
}

const autoResize = () => {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = (el.scrollHeight) + 'px'
  }
}

watch(fileContent, async (newVal) => {
  nextTick(() => autoResize())
  
  const match = newVal.match(/<AI>([\s\S]*?)<\/AI>/)
  if (match && !isGeneratingCode.value && activeFile.value) {
    const prompt = match[1].trim()
    if (!prompt) return
    
    isGeneratingCode.value = true
    const loadingTag = `<AI>正在施展 AI 魔法为您生成内容...</AI>`
    fileContent.value = fileContent.value.replace(match[0], loadingTag)
    
    try {
      const res = await api.post('/ai/generate-code', { 
        prompt: prompt,
        filename: activeFile.value
      })
      const generatedCode = res.data.code
      fileContent.value = fileContent.value.replace(loadingTag, generatedCode)
      ElMessage.success('AI 已成功为您生成内容！')
      
      saveFile()
    } catch (err) {
      ElMessage.error(err.response?.data?.detail || 'AI 生成内容失败')
      fileContent.value = fileContent.value.replace(loadingTag, `/* AI 生成失败: ${prompt} */`)
    } finally {
      isGeneratingCode.value = false
    }
  }
})

const closeWord = () => {
  emit('close')
}

const toggleSidebar = () => {
  showSidebar.value = !showSidebar.value
}

const showNotAvailable = (feature) => {
  ElMessage.info(`[${feature}] 功能暂不可用。您现在可以使用纯文本模式进行创作。`)
}

const fetchTree = async () => {
  try {
    const res = await api.get('/ide/files')
    fileTree.value = res.data
  } catch (err) {
    ElMessage.error('无法读取项目目录树')
  }
}

const handleNodeClick = async (data) => {
  if (data.type === 'file') {
    activeFile.value = data.path
    isLoadingFile.value = true
    isDocxPreview.value = false
    docxHtml.value = ''
    try {
      if (data.path.toLowerCase().endsWith('.docx')) {
        const res = await api.get(`/ide/download-file?path=${encodeURIComponent(data.path)}`, { responseType: 'arraybuffer' })
        const result = await mammoth.convertToHtml({ arrayBuffer: res.data })
        docxHtml.value = result.value
        isDocxPreview.value = true
        fileContent.value = ''
      } else {
        const res = await api.get(`/ide/content?path=${encodeURIComponent(data.path)}&encoding=${encodeURIComponent(currentEncoding.value)}`)
        fileContent.value = res.data.content
        nextTick(() => autoResize())
      }
    } catch (err) {
      fileContent.value = `/* Error loading file: ${err.response?.data?.detail || err.message} */`
    } finally {
      isLoadingFile.value = false
    }
  }
}

const changeEncoding = async (enc) => {
  currentEncoding.value = enc
  if (activeFile.value) {
    isLoadingFile.value = true
    try {
      const res = await api.get(`/ide/content?path=${encodeURIComponent(activeFile.value)}&encoding=${encodeURIComponent(enc)}`)
      fileContent.value = res.data.content
      ElMessage.success(`已切换编码至 ${enc} 并重新加载`)
    } catch (err) {
      fileContent.value = `/* Error loading file with encoding ${enc}: ${err.response?.data?.detail || err.message} */`
    } finally {
      isLoadingFile.value = false
    }
  }
}

const saveFile = async () => {
  if (!activeFile.value) return
  if (isDocxPreview.value) {
    ElMessage.warning('预览模式下无法直接保存 Word 文档，请下载到本地编辑。')
    return
  }
  isSaving.value = true
  try {
    await api.post('/ide/content', {
      path: activeFile.value,
      content: fileContent.value,
      encoding: currentEncoding.value
    })
    ElMessage.success('文档已成功保存！')
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '保存失败')
  } finally {
    isSaving.value = false
  }
}

const createNewFile = () => {
  ElMessageBox.prompt('请输入新文档的名称 (例如: 会议纪要.md, requirements.txt)', '新建文档', {
    confirmButtonText: '创建',
    cancelButtonText: '取消',
    inputPattern: /^[^<>:"|?*]+$/,
    inputErrorMessage: '文档名包含非法字符'
  }).then(async ({ value }) => {
    if (!value) return
    try {
      await api.post('/ide/content', {
        path: value,
        content: ''
      })
      ElMessage.success(`文档 ${value} 创建成功`)
      await fetchTree()
      activeFile.value = value
      fileContent.value = ''
    } catch (err) {
      ElMessage.error(err.response?.data?.detail || '创建文档失败')
    }
  }).catch(() => {})
}

const handleFileAction = async (command, data) => {
  if (command === 'new_file') {
    ElMessageBox.prompt('请输入新文档名称', '新建文档', {
      inputPattern: /^[^<>:"|?*]+$/,
      inputErrorMessage: '包含非法字符'
    }).then(async ({ value }) => {
      if (!value) return
      try {
        const newPath = data.path ? `${data.path}/${value}` : value
        await api.post('/ide/content', { path: newPath, content: '' })
        ElMessage.success('文档创建成功')
        fetchTree()
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || err.message || '创建失败')
      }
    }).catch(() => {})
  } else if (command === 'new_folder') {
    ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      inputPattern: /^[^<>:"|?*]+$/,
      inputErrorMessage: '包含非法字符'
    }).then(async ({ value }) => {
      if (!value) return
      try {
        const newPath = data.path ? `${data.path}/${value}` : value
        await api.post('/ide/directory', { path: newPath })
        ElMessage.success('文件夹创建成功')
        fetchTree()
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || err.message || '创建失败')
      }
    }).catch(() => {})
  } else if (command === 'rename') {
    ElMessageBox.prompt('请输入新的名称', '重命名', {
      inputValue: data.name,
      inputPattern: /^[^<>:"|?*]+$/,
      inputErrorMessage: '包含非法字符'
    }).then(async ({ value }) => {
      if (!value || value === data.name) return
      try {
        let basePath = data.path.substring(0, data.path.lastIndexOf('/'))
        if (data.path.indexOf('/') === -1) basePath = ''
        const newPath = basePath ? `${basePath}/${value}` : value
        
        await api.put('/ide/rename', { old_path: data.path, new_path: newPath })
        ElMessage.success('重命名成功')
        if (activeFile.value === data.path) activeFile.value = newPath
        fetchTree()
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || err.message || '重命名失败')
      }
    }).catch(() => {})
  } else if (command === 'move') {
    const parentPath = data.path.indexOf('/') !== -1 ? data.path.substring(0, data.path.lastIndexOf('/')) : ''
    ElMessageBox.prompt('请输入目标文件夹路径 (移动到根目录请留空)', '移动文件/文件夹', {
      inputValue: parentPath
    }).then(async ({ value }) => {
      try {
        const newPath = value ? `${value}/${data.name}` : data.name
        if (newPath === data.path) return
        
        await api.put('/ide/rename', { old_path: data.path, new_path: newPath })
        ElMessage.success('移动成功')
        if (activeFile.value === data.path) activeFile.value = newPath
        else if (activeFile.value.startsWith(data.path + '/')) {
          activeFile.value = activeFile.value.replace(data.path, newPath)
        }
        fetchTree()
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || err.message || '移动失败, 请检查目标目录是否存在')
      }
    }).catch(() => {})
  } else if (command === 'download') {
    try {
      ElMessage.info('准备下载...')
      const res = await api.get(`/ide/download-file?path=${encodeURIComponent(data.path)}`, { responseType: 'blob' })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', data.name)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    } catch (err) {
      ElMessage.error('下载失败')
    }
  } else if (command === 'upload') {
    const input = document.createElement('input')
    input.type = 'file'
    input.onchange = async (e) => {
      const file = e.target.files[0]
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      formData.append('path', data.path)
      
      try {
        ElMessage.info('正在上传...')
        await api.post('/ide/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        ElMessage.success('文件上传成功')
        fetchTree()
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || err.message || '上传失败')
      }
    }
    input.click()
  } else if (command === 'delete') {
    ElMessageBox.confirm(`确定要删除 ${data.name} 吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await api.delete(`/ide/file?path=${encodeURIComponent(data.path)}`)
        ElMessage.success('删除成功')
        if (activeFile.value && (activeFile.value === data.path || activeFile.value.startsWith(data.path + '/'))) {
          activeFile.value = ''
          fileContent.value = ''
        }
        fetchTree()
      } catch (err) {
        ElMessage.error(err.response?.data?.detail || err.message || '删除失败')
      }
    }).catch(() => {})
  }
}

onMounted(() => {
  fetchTree()
})
</script>

<style scoped>
.word-tree {
  background-color: transparent;
}
:deep(.el-tree-node__content) {
  height: 2rem;
  border-radius: 6px;
  margin-bottom: 2px;
}
:deep(.el-tree-node__content:hover) {
  background-color: #e4e4e7;
}
:deep(.el-tree-node:focus > .el-tree-node__content) {
  background-color: #d4d4d8;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #d4d4d8;
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #a1a1aa;
}

.docx-preview-content h1 { font-size: 2em; font-weight: bold; margin-bottom: 0.5em; }
.docx-preview-content h2 { font-size: 1.5em; font-weight: bold; margin-bottom: 0.5em; }
.docx-preview-content h3 { font-size: 1.17em; font-weight: bold; margin-bottom: 0.5em; }
.docx-preview-content p { margin-bottom: 1em; }
.docx-preview-content table { border-collapse: collapse; width: 100%; margin-bottom: 1em; }
.docx-preview-content th, .docx-preview-content td { border: 1px solid #ccc; padding: 8px; }

/* IDE Theme overrides */
.is-ide-mode .el-tree {
  background-color: transparent !important;
  color: #e5e5e5 !important;
}
.is-ide-mode .el-tree-node__content:hover {
  background-color: #2a2d2e !important;
}
.is-ide-mode .el-tree-node:focus > .el-tree-node__content {
  background-color: #37373d !important;
}

.custom-ide-scrollbar::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
.custom-ide-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-ide-scrollbar::-webkit-scrollbar-thumb {
  background: #424242;
  border: 2px solid #1e1e1e;
  border-radius: 5px;
}
.custom-ide-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #4f4f4f;
}
</style>
