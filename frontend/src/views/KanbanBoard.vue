<template>
  <div class="h-full flex flex-col" v-if="authStore.user?.current_project_id">
    <!-- Header Controls -->
    <div class="flex justify-between items-center mb-6 shrink-0">
      <div class="flex items-center space-x-4">
        <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">任务看板</h2>
        <div class="flex bg-zinc-100 rounded-md p-1">
          <button 
            @click="viewMode = 'kanban'" 
            :class="['px-3 py-1 rounded text-sm font-medium transition-all', viewMode === 'kanban' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500 hover:text-zinc-700']"
          >
            看板视图
          </button>
          <button 
            @click="viewMode = 'list'" 
            :class="['px-3 py-1 rounded text-sm font-medium transition-all', viewMode === 'list' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500 hover:text-zinc-700']"
          >
            列表视图
          </button>
        </div>
      </div>
      
      <div class="flex space-x-3">
        <el-input v-model="searchQuery" placeholder="搜索任务..." class="w-64" :prefix-icon="Search" />
        <el-button type="primary" @click="openCreateDialog">
          <el-icon class="mr-1"><Plus /></el-icon> 新建任务
        </el-button>
      </div>
    </div>

    <!-- Kanban Matrix View -->
    <div v-if="viewMode === 'kanban'" class="flex-1 flex overflow-x-auto overflow-y-hidden pb-2 space-x-4 custom-scrollbar">
      
      <!-- Columns -->
      <div 
        v-for="col in columns" 
        :key="col.status"
        class="flex-shrink-0 w-80 flex flex-col bg-zinc-50/50 rounded-md border border-zinc-200"
      >
        <!-- Column Header -->
        <div class="px-4 py-3 border-b border-zinc-200 flex items-center justify-between bg-zinc-50 rounded-t-md">
          <div class="flex items-center space-x-2">
            <div :class="`w-2.5 h-2.5 rounded-full ${col.dotColor}`"></div>
            <span class="font-semibold text-zinc-700 text-sm">{{ col.title }}</span>
          </div>
          <span class="bg-zinc-200 text-zinc-600 text-[10px] px-2 py-0.5 rounded font-semibold">
            {{ filteredTasks(col.status).length }}
          </span>
        </div>

        <!-- Task List container -->
        <div 
          class="flex-1 overflow-y-auto p-3 space-y-3 custom-scrollbar min-h-[150px]"
          @dragover.prevent
          @drop="onDrop($event, col.status)"
        >
          <div 
            v-for="task in filteredTasks(col.status)" 
            :key="task.id"
            :draggable="isLeader"
            @dragstart="isLeader ? onDragStart($event, task) : null"
            @click="openTaskDetail(task)"
            class="bg-white rounded border border-zinc-200 p-3 shadow-sm hover:border-zinc-300 transition-colors relative group"
            :class="[isOverdue(task.deadline) && task.status !== 'done' ? 'border-red-200 bg-red-50/30' : '', isLeader ? 'cursor-grab active:cursor-grabbing' : 'cursor-pointer']"
          >
            <!-- Top: Deadline & Quick Actions -->
            <div class="flex justify-between items-start mb-2">
              <span v-if="task.deadline" :class="['text-[10px] font-medium px-2 py-0.5 rounded', isOverdue(task.deadline) && task.status !== 'done' ? 'text-red-600 bg-red-100' : 'text-zinc-600 bg-zinc-100']">
                {{ formatDate(task.deadline) }}
              </span>
              <span v-else></span>
              
              <!-- Quick Actions on hover -->
              <div v-if="isLeader" class="opacity-0 group-hover:opacity-100 transition-opacity">
                <button class="text-zinc-400 hover:text-red-500 transition-colors p-1" @click.stop="handleDeleteTask(task.id)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
            
            <h4 class="font-medium text-zinc-900 text-sm mb-1 leading-snug">{{ task.title }}</h4>
            <p class="text-xs text-zinc-500 line-clamp-2 mb-3 leading-relaxed">{{ task.description || '暂无描述' }}</p>
            
            <!-- Bottom Tags: Priority, Status, Assignee, Phase, Weight -->
            <div class="flex flex-wrap gap-1.5 mt-auto pt-2 border-t border-zinc-100">
              <span :class="['text-[10px] font-medium px-1.5 py-0.5 rounded', getPriorityBg(task.priority)]">
                {{ getPriorityLabel(task.priority) }}
              </span>
              <span :class="['text-[10px] font-medium px-1.5 py-0.5 rounded', getStatusBg(task.status)]">
                {{ getStatusLabel(task.status) }}
              </span>
              <span v-if="task.phase !== 'none'" class="text-[10px] font-medium px-1.5 py-0.5 rounded bg-purple-100 text-purple-700">
                {{ getPhaseLabel(task.phase) }}
              </span>
              <span class="text-[10px] font-medium px-1.5 py-0.5 rounded bg-zinc-100 text-zinc-600">
                权重: {{ task.weight || 1 }}
              </span>
              <div class="ml-auto flex items-center">
                <div class="w-5 h-5 rounded bg-zinc-100 flex items-center justify-center font-medium text-zinc-600 text-[10px] border border-zinc-200" :title="task.assigned_to?.username || '未分配'">
                  {{ (task.assigned_to?.username || '?').charAt(0).toUpperCase() }}
                </div>
              </div>
            </div>
          </div>
          
          <!-- Drop Target Placeholder -->
          <div v-if="filteredTasks(col.status).length === 0" class="h-24 border border-dashed border-zinc-300 rounded flex items-center justify-center text-zinc-400 text-xs font-medium">
            拖拽任务至此
          </div>
        </div>
      </div>
    </div>
    
    <!-- List View -->
    <div v-else class="flex-1 overflow-auto bg-white rounded-md border border-zinc-200 shadow-sm">
      <el-table :data="allFilteredTasks" class="w-full minimal-table" :row-class-name="tableRowClassName">
        <el-table-column prop="title" label="任务标题" min-width="250">
          <template #default="{ row }">
            <div class="font-medium text-zinc-900 cursor-pointer hover:underline" @click="openTaskDetail(row)">
              {{ row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <span :class="['text-xs font-medium px-2 py-1 rounded', getStatusBg(row.status)]">
              {{ getStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <span :class="['text-xs font-medium px-2 py-1 rounded', getPriorityBg(row.priority)]">
              {{ getPriorityLabel(row.priority) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="负责人" width="120">
          <template #default="{ row }">
            <div class="flex items-center text-sm text-zinc-700">
              <div class="w-5 h-5 rounded bg-zinc-100 flex items-center justify-center font-medium text-zinc-600 text-[10px] border border-zinc-200 mr-2">
                {{ (row.assigned_to?.username || '?').charAt(0).toUpperCase() }}
              </div>
              {{ row.assigned_to?.username || '未分配' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止日期" width="140">
          <template #default="{ row }">
            <span :class="['text-sm', isOverdue(row.deadline) && row.status !== 'done' ? 'text-red-600 font-medium' : 'text-zinc-500']">
              {{ formatDate(row.deadline) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="right">
          <template #default="{ row }">
            <el-button v-if="isLeader" type="primary" link size="small" @click="openTaskDetail(row)">编辑</el-button>
            <el-button v-else type="primary" link size="small" @click="openTaskDetail(row)">查看</el-button>
            <el-button v-if="isLeader" type="danger" link size="small" @click="handleDeleteTask(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Minimal Create Task Dialog -->
    <el-dialog v-model="createDialogVisible" width="500px" :show-close="false" align-center>
      <div class="p-2">
        <div class="flex justify-between items-center mb-5">
          <h3 class="text-base font-semibold text-zinc-900 tracking-tight">新建任务</h3>
          <el-button circle plain @click="createDialogVisible = false" class="border-0 bg-transparent hover:bg-zinc-100 text-zinc-500"><el-icon><Close /></el-icon></el-button>
        </div>
        
        <el-form :model="newTask" label-width="70px" label-position="left" class="minimal-form">
          <el-form-item label="任务标题">
            <el-input v-model="newTask.title" placeholder="输入简明扼要的标题..." />
          </el-form-item>
          
          <el-form-item label="任务描述">
            <el-input v-model="newTask.description" type="textarea" :rows="3" placeholder="添加详细描述..." />
          </el-form-item>
          
          <div class="flex gap-4">
            <el-form-item label="优先级" class="flex-1">
              <el-select v-model="newTask.priority" class="w-full">
                <el-option label="低" value="low" />
                <el-option label="中" value="medium" />
                <el-option label="高" value="high" />
                <el-option label="紧急" value="urgent" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" class="flex-1">
              <el-select v-model="newTask.status" class="w-full">
                <el-option label="待办" value="todo" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="待审核" value="review" />
                <el-option label="已完成" value="done" />
              </el-select>
            </el-form-item>
          </div>

          <div class="flex gap-4">
            <el-form-item label="负责人" class="flex-1">
              <el-select v-model="newTask.assigned_to_id" class="w-full" clearable placeholder="指派成员">
                <el-option v-for="mem in projectMembers" :key="mem.user.id" :label="mem.user.username" :value="mem.user.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="截止日期" class="flex-1">
              <el-date-picker v-model="newTask.deadline" type="date" placeholder="选择日期" class="w-full" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>
        </el-form>

        <div class="bg-zinc-50 border border-zinc-200 p-4 rounded-md mt-4">
          <div class="flex items-center text-zinc-900 font-semibold text-xs mb-2">
            <el-icon class="mr-1 text-zinc-500"><MagicStick /></el-icon> AI 智能拆解
          </div>
          <p class="text-xs text-zinc-500 mb-3">输入宏观目标，AI 将为您自动拆解为多个颗粒度极细的开发子任务。</p>
          <div class="flex gap-2">
            <el-input v-model="aiMacroPrompt" placeholder="例如：开发用户登录模块..." class="flex-1" @keyup.enter="handleAIBreakdown" />
            <el-button @click="handleAIBreakdown" :loading="isAIBreakingDown" plain>智能拆解</el-button>
          </div>
        </div>

        <div class="mt-6 flex justify-end space-x-2 border-t border-zinc-100 pt-4">
          <el-button plain @click="createDialogVisible = false" size="small">取消</el-button>
          <el-button type="primary" @click="createTask" size="small">确认创建</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Task Detail / Edit Dialog -->
    <el-dialog v-model="editDialogVisible" width="520px" :show-close="false" align-center>
      <div class="p-2" v-if="editingTask">
        <div class="flex justify-between items-center mb-5">
          <h3 class="text-base font-semibold text-zinc-900 tracking-tight">{{ isLeader ? '编辑任务' : '任务详情' }}</h3>
          <el-button circle plain @click="editDialogVisible = false" class="border-0 bg-transparent hover:bg-zinc-100 text-zinc-500"><el-icon><Close /></el-icon></el-button>
        </div>
        
        <el-form :model="editingTask" label-width="70px" label-position="left" class="minimal-form" :disabled="!isLeader">
          <el-form-item label="任务标题">
            <el-input v-model="editingTask.title" placeholder="任务标题" />
          </el-form-item>
          
          <el-form-item label="任务描述">
            <el-input v-model="editingTask.description" type="textarea" :rows="3" placeholder="添加详细描述..." />
          </el-form-item>
          
          <el-form-item label="交付内容" v-if="editingTask.commit_link">
            <div class="bg-zinc-50 p-3 rounded-lg border border-zinc-200 text-sm text-zinc-700 w-full whitespace-pre-wrap break-all">
              <template v-if="editingTask.commit_link.startsWith('文件: ')">
                <div class="flex items-center text-blue-600">
                  <el-icon class="mr-2 text-lg"><Document /></el-icon>
                  <span>提交了文件: {{ editingTask.commit_link.substring(4).split('/').pop() }}</span>
                </div>
              </template>
              <template v-else-if="editingTask.commit_link.startsWith('链接: ')">
                <div class="flex items-center text-green-600">
                  <el-icon class="mr-2 text-lg"><Link /></el-icon>
                  <a :href="editingTask.commit_link.substring(4)" target="_blank" class="hover:underline">提交了链接: {{ editingTask.commit_link.substring(4) }}</a>
                </div>
              </template>
              <template v-else-if="editingTask.commit_link.startsWith('说明: ')">
                <div class="flex items-start">
                  <el-icon class="mr-2 mt-0.5 text-lg text-orange-500"><ChatDotRound /></el-icon>
                  <div>
                    <div class="text-xs text-zinc-400 mb-1">提交了说明:</div>
                    <div class="text-zinc-700">{{ editingTask.commit_link.substring(4) }}</div>
                  </div>
                </div>
              </template>
              <template v-else>
                {{ editingTask.commit_link }}
              </template>
            </div>
          </el-form-item>
          
          <div class="flex gap-4">
            <el-form-item label="优先级" class="flex-1">
              <el-select v-model="editingTask.priority" class="w-full">
                <el-option label="低" value="low" />
                <el-option label="中" value="medium" />
                <el-option label="高" value="high" />
                <el-option label="紧急" value="urgent" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" class="flex-1">
              <el-select v-model="editingTask.status" class="w-full">
                <el-option label="待办" value="todo" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="待审核" value="review" />
                <el-option label="已完成" value="done" />
              </el-select>
            </el-form-item>
          </div>

          <div class="flex gap-4">
            <el-form-item label="负责人" class="flex-1">
              <el-select v-model="editingTask.assigned_to_id" class="w-full" clearable placeholder="指派成员">
                <el-option v-for="mem in projectMembers" :key="mem.user.id" :label="mem.user.username" :value="mem.user.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="截止日期" class="flex-1">
              <el-date-picker v-model="editingTask.deadline" type="date" placeholder="选择日期" class="w-full" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>

          <div class="flex gap-4">
            <el-form-item label="阶段" class="flex-1">
              <el-select v-model="editingTask.phase" class="w-full" clearable placeholder="选择阶段">
                <el-option label="需求" value="requirement" />
                <el-option label="设计" value="design" />
                <el-option label="编码" value="coding" />
                <el-option label="测试" value="test" />
              </el-select>
            </el-form-item>
            <el-form-item label="权重" class="flex-1">
              <el-input-number v-model="editingTask.weight" :min="1" :max="20" class="w-full" />
            </el-form-item>
          </div>
        </el-form>

        <div class="mt-6 flex justify-end space-x-2 border-t border-zinc-100 pt-4">
          <el-button plain @click="editDialogVisible = false" size="small">{{ isLeader ? '取消' : '关闭' }}</el-button>
          
          <template v-if="!editingTask.assigned_to_id">
            <el-button type="success" @click="memberTakeTask" size="small" :loading="isSavingTask">
              主动接取
            </el-button>
          </template>
          
          <template v-if="editingTask.assigned_to_id === authStore.user?.id && editingTask.status !== 'review' && editingTask.status !== 'done'">
            <el-button type="success" plain @click="openFocusSetup" size="small">
              <el-icon class="mr-1"><Timer /></el-icon> 专注模式
            </el-button>
            <el-button type="primary" @click="memberSubmitTask" size="small" :loading="isSavingTask">
              提交审核
            </el-button>
          </template>

          <template v-if="isLeader && editingTask.status === 'review'">
            <el-button type="primary" @click="openLeaderReview" size="small">
              开始审核
            </el-button>
          </template>
          
          <el-button v-if="isLeader" type="primary" @click="saveTask" :loading="isSavingTask" size="small">保存修改</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 提交审核弹窗 -->
    <el-dialog v-model="submitReviewDialogVisible" width="500px" title="提交审核交付物" align-center>
      <el-tabs v-model="submitType">
        <el-tab-pane label="填写说明" name="content">
          <el-input v-model="submitContent" type="textarea" :rows="4" placeholder="输入工作说明、测试报告等..." />
        </el-tab-pane>
        <el-tab-pane label="提供链接" name="link">
          <el-input v-model="submitLink" placeholder="例如：GitHub PR链接、在线文档链接..." />
        </el-tab-pane>
        <el-tab-pane label="上传文件" name="file">
          <div class="flex flex-col items-center justify-center p-8 border-2 border-dashed border-zinc-300 rounded-lg bg-zinc-50 hover:bg-zinc-100 cursor-pointer" @click="triggerFileInput">
            <el-icon class="text-4xl text-zinc-400 mb-2"><Plus /></el-icon>
            <div class="text-zinc-600 text-sm">点击选择文件上传</div>
          </div>
          <div v-if="submitFileName" class="mt-3 text-green-600 text-sm flex items-center justify-center">
            <el-icon class="mr-1"><MagicStick /></el-icon> 已选择: {{ submitFileName }}
          </div>
          <input type="file" ref="fileInputRef" class="hidden" @change="handleFileSelect" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <span class="dialog-footer flex justify-end space-x-2">
          <el-button @click="submitReviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSubmitTask" :loading="isSubmittingTask">确认提交</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 组长审核弹窗 -->
    <el-dialog v-model="leaderReviewDialogVisible" width="520px" title="任务审核" align-center>
      <div v-if="editingTask" class="space-y-4">
        <div class="text-sm text-zinc-500">任务名称</div>
        <div class="text-base font-semibold text-zinc-900">{{ editingTask.title }}</div>
        
        <div class="text-sm text-zinc-500 mt-4">组员提交的交付内容</div>
        <div v-if="editingTask.commit_link" class="bg-zinc-50 p-4 rounded-lg border border-zinc-200 text-sm text-zinc-700 whitespace-pre-wrap break-all">
          <template v-if="editingTask.commit_link.startsWith('文件: ')">
            <div class="flex items-center text-blue-600">
              <el-icon class="mr-2 text-lg"><Document /></el-icon>
              <span>提交了文件: {{ editingTask.commit_link.substring(4).split('/').pop() }}</span>
            </div>
          </template>
          <template v-else-if="editingTask.commit_link.startsWith('链接: ')">
            <div class="flex items-center text-green-600">
              <el-icon class="mr-2 text-lg"><Link /></el-icon>
              <a :href="editingTask.commit_link.substring(4)" target="_blank" class="hover:underline">提交了链接: {{ editingTask.commit_link.substring(4) }}</a>
            </div>
          </template>
          <template v-else-if="editingTask.commit_link.startsWith('说明: ')">
            <div class="flex items-start">
              <el-icon class="mr-2 mt-0.5 text-lg text-orange-500"><ChatDotRound /></el-icon>
              <div>
                <div class="text-xs text-zinc-400 mb-1">提交了说明:</div>
                <div class="text-zinc-700">{{ editingTask.commit_link.substring(4) }}</div>
              </div>
            </div>
          </template>
          <template v-else>
            {{ editingTask.commit_link }}
          </template>
        </div>
        <div v-else class="text-zinc-400 text-sm">未提供交付内容</div>
      </div>
      <template #footer>
        <span class="flex justify-end space-x-2">
          <el-button @click="leaderReviewDialogVisible = false">取消</el-button>
          <el-button type="warning" @click="leaderRejectTask" :loading="isSavingTask">驳回修改</el-button>
          <el-button type="success" @click="leaderApproveTask" :loading="isSavingTask">审核通过</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Focus Setup Dialog -->
    <el-dialog v-model="focusSetupVisible" width="400px" title="设置专注时长" align-center>
      <div class="py-4 text-center">
        <el-radio-group v-model="selectedFocusMinutes" size="large">
          <el-radio-button :label="15">15分钟</el-radio-button>
          <el-radio-button :label="25">25分钟</el-radio-button>
          <el-radio-button :label="45">45分钟</el-radio-button>
          <el-radio-button :label="60">60分钟</el-radio-button>
        </el-radio-group>
        <p class="text-xs text-zinc-500 mt-6">
          专注结束将根据时长发放资产积分奖励 (每 5 分钟 = 1 积分)
        </p>
      </div>
      <template #footer>
        <span class="flex justify-end space-x-2">
          <el-button @click="focusSetupVisible = false">取消</el-button>
          <el-button type="primary" @click="startFocusMode">开始专注</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Zen Focus Mode Overlay -->
    <div v-if="isFocusMode" class="fixed inset-0 z-[200] bg-zinc-950 flex flex-col items-center justify-center text-white transition-opacity duration-500">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-zinc-800 to-zinc-950 opacity-80 pointer-events-none"></div>
      
      <div class="relative z-10 flex flex-col items-center">
        <div class="text-zinc-400 text-lg mb-2 flex items-center">
          <el-icon class="mr-2"><Timer /></el-icon> 正在专注
        </div>
        <h2 class="text-3xl font-medium text-white mb-16 tracking-wide">{{ editingTask?.title }}</h2>
        
        <div class="text-[8rem] font-bold tabular-nums tracking-tighter leading-none mb-16 text-transparent bg-clip-text bg-gradient-to-b from-white to-zinc-500 drop-shadow-lg">
          {{ formatFocusTime }}
        </div>
        
        <div class="flex space-x-6">
          <button @click="cancelFocusMode" class="px-8 py-3 rounded-full border border-zinc-700 text-zinc-400 hover:text-white hover:border-zinc-500 transition-colors bg-zinc-900/50 backdrop-blur-sm">
            放弃专注
          </button>
          <button @click="finishFocusMode(false)" class="px-8 py-3 rounded-full bg-white text-zinc-900 font-semibold hover:bg-zinc-200 transition-colors shadow-[0_0_20px_rgba(255,255,255,0.3)]">
            提前完成
          </button>
        </div>
      </div>
    </div>


  </div>
  
  <div v-else class="text-center py-24 flex flex-col items-center justify-center h-full">
    <div class="w-16 h-16 rounded border border-zinc-200 flex items-center justify-center mb-4 text-zinc-400 bg-zinc-50">
      <el-icon class="text-2xl"><Lock /></el-icon>
    </div>
    <h2 class="text-base font-semibold text-zinc-900 mb-1">未连接项目</h2>
    <p class="text-zinc-500 text-sm mb-6">请先选择项目以加载看板</p>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { Plus, Search, Delete, MagicStick, Lock, Close, Edit, Document, Link, ChatDotRound, Timer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const tasks = ref([])
const searchQuery = ref('')
const viewMode = ref('kanban')

const columns = [
  { status: 'todo', title: '待办', dotColor: 'bg-zinc-400' },
  { status: 'in_progress', title: '进行中', dotColor: 'bg-blue-500' },
  { status: 'review', title: '待审核', dotColor: 'bg-amber-500' },
  { status: 'done', title: '已完成', dotColor: 'bg-green-500' }
]

const isLeader = computed(() => {
  if (!authStore.user) return false
  if (authStore.user.role === 'teacher') return true
  const myMem = projectMembers.value.find(m => m.user.id === authStore.user.id)
  return myMem?.role === 'leader' || false
})

const normalizeTask = (task) => ({
  ...task,
  commit_link: task?.commit_link || task?.commitLink || ''
})

const fetchTasks = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const res = await api.get(`/tasks/`, { params: { project_id: authStore.user.current_project_id } })
    tasks.value = res.data.map(normalizeTask)
  } catch (error) {
    ElMessage.error('获取任务失败')
  }
}

const allFilteredTasks = computed(() => {
  if (!searchQuery.value) return tasks.value
  const q = searchQuery.value.toLowerCase()
  return tasks.value.filter(t => t.title.toLowerCase().includes(q) || (t.description && t.description.toLowerCase().includes(q)))
})

const filteredTasks = (status) => {
  return allFilteredTasks.value.filter(t => t.status === status)
}

// Drag and Drop
const onDragStart = (e, task) => {
  e.dataTransfer.setData('taskId', task.id)
  e.dataTransfer.effectAllowed = 'move'
}

const onDrop = async (e, newStatus) => {
  const taskId = e.dataTransfer.getData('taskId')
  if (!taskId) return
  
  const taskIndex = tasks.value.findIndex(t => t.id === parseInt(taskId))
  if (taskIndex === -1) return
  
  const originalStatus = tasks.value[taskIndex].status
  if (originalStatus === newStatus) return
  if (newStatus === 'review') {
    ElMessage.warning('待审核必须由接取人通过提交审核填写交付物')
    return
  }
  
  tasks.value[taskIndex].status = newStatus
  
  try {
    await api.put(`/tasks/${taskId}`, { status: newStatus })
  } catch (error) {
    tasks.value[taskIndex].status = originalStatus
    ElMessage.error('状态更新失败')
  }
}

// Creation
const createDialogVisible = ref(false)
const projectMembers = ref([])
const newTask = ref({
  title: '',
  description: '',
  status: 'todo',
  priority: 'medium',
  assigned_to_id: null,
  deadline: ''
})

const openCreateDialog = async () => {
  newTask.value = { title: '', description: '', status: 'todo', priority: 'medium', assigned_to_id: null, deadline: '' }
  createDialogVisible.value = true
  if (projectMembers.value.length === 0) {
    await loadMembers()
  }
}

const loadMembers = async () => {
  try {
    const res = await api.get(`/projects/${authStore.user.current_project_id}/members`)
    projectMembers.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const createTask = async () => {
  if (!newTask.value.title) {
    ElMessage.warning('任务标题不能为空')
    return
  }
  try {
    const payload = { ...newTask.value, project_id: authStore.user.current_project_id }
    await api.post(`/tasks/`, payload)
    ElMessage.success('任务创建成功')
    createDialogVisible.value = false
    fetchTasks()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleDeleteTask = (taskId) => {
  ElMessageBox.confirm('确定要删除该任务吗？此操作不可恢复。', '确认删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await api.delete(`/tasks/${taskId}`)
      ElMessage.success('任务已删除')
      fetchTasks()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// AI Logic
const aiMacroPrompt = ref('')
const isAIBreakingDown = ref(false)

const handleAIBreakdown = async () => {
  if (!aiMacroPrompt.value.trim()) {
    ElMessage.warning('请输入拆解目标')
    return
  }
  isAIBreakingDown.value = true
  try {
    await api.post(`/ai/breakdown-task`, {
      project_id: authStore.user.current_project_id,
      goal: aiMacroPrompt.value
    })
    ElMessage.success('AI 拆解完成，子任务已加入看板！')
    aiMacroPrompt.value = ''
    createDialogVisible.value = false
    fetchTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || '拆解失败')
  } finally {
    isAIBreakingDown.value = false
  }
}

// Edit task
const editDialogVisible = ref(false)
const editingTask = ref(null)
const isSavingTask = ref(false)

const toEditingTask = (task) => {
  const normalized = normalizeTask(task)
  return {
    id: normalized.id,
    title: normalized.title,
    description: normalized.description || '',
    status: normalized.status,
    priority: normalized.priority || 'medium',
    assigned_to_id: normalized.assigned_to_id || normalized.assigned_to?.id || null,
    deadline: normalized.deadline || '',
    phase: normalized.phase || '',
    weight: normalized.weight || 1,
    commit_link: normalized.commit_link
  }
}

const openTaskDetail = async (task) => {
  editingTask.value = toEditingTask(task)
  editDialogVisible.value = true
  if (projectMembers.value.length === 0) {
    await loadMembers()
  }
}

const memberTakeTask = async () => {
  isSavingTask.value = true
  try {
    await api.post(`/tasks/${editingTask.value.id}/take`)
    ElMessage.success('成功接取任务')
    editDialogVisible.value = false
    fetchTasks()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || err.response?.data?.message || '操作失败')
  } finally {
    isSavingTask.value = false
  }
}

const submitReviewDialogVisible = ref(false)
const submitType = ref('content')
const submitContent = ref('')
const submitLink = ref('')
const submitFileName = ref('')
const submitFileObj = ref(null)
const fileInputRef = ref(null)
const isSubmittingTask = ref(false)

const memberSubmitTask = () => {
  submitType.value = 'content'
  submitContent.value = ''
  submitLink.value = ''
  submitFileName.value = ''
  submitFileObj.value = null
  submitReviewDialogVisible.value = true
}

const triggerFileInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if (file) {
    submitFileName.value = file.name
    submitFileObj.value = file
  }
}

const confirmSubmitTask = async () => {
  let finalCommitData = ''
  
  if (submitType.value === 'content') {
    if (!submitContent.value.trim()) return ElMessage.warning('请填写工作说明')
    finalCommitData = '说明: ' + submitContent.value
  } else if (submitType.value === 'link') {
    if (!submitLink.value.trim()) return ElMessage.warning('请提供链接')
    finalCommitData = '链接: ' + submitLink.value
  } else if (submitType.value === 'file') {
    if (!submitFileObj.value) return ElMessage.warning('请先选择要上传的文件')
    
    isSubmittingTask.value = true
    try {
      const formData = new FormData()
      formData.append('file', submitFileObj.value)
      formData.append('path', '/.task_uploads')
      
      await api.post('/ide/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      finalCommitData = '文件: /.task_uploads/' + submitFileName.value
    } catch (err) {
      isSubmittingTask.value = false
      return ElMessage.error('文件上传失败: ' + (err.response?.data?.detail || err.message))
    }
  }

  isSubmittingTask.value = true
  try {
    await api.put(`/tasks/${editingTask.value.id}`, { 
      status: 'review', 
      commit_link: finalCommitData 
    })
    ElMessage.success('已提交审核')
    submitReviewDialogVisible.value = false
    editDialogVisible.value = false
    fetchTasks()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || err.response?.data?.message || '操作失败')
  } finally {
    isSubmittingTask.value = false
  }
}

const leaderReviewDialogVisible = ref(false)

const openLeaderReview = async () => {
  await fetchTasks()
  const latest = tasks.value.find(t => t.id === editingTask.value?.id)
  if (latest) {
    editingTask.value = toEditingTask(latest)
  }
  leaderReviewDialogVisible.value = true
}

const leaderApproveTask = async () => {
  isSavingTask.value = true
  try {
    await api.put(`/tasks/${editingTask.value.id}`, { status: 'done' })
    ElMessage.success('已审核通过')
    leaderReviewDialogVisible.value = false
    editDialogVisible.value = false
    fetchTasks()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '审核失败')
  } finally {
    isSavingTask.value = false
  }
}

const leaderRejectTask = async () => {
  isSavingTask.value = true
  try {
    await api.put(`/tasks/${editingTask.value.id}`, { status: 'in_progress' })
    ElMessage.warning('已驳回修改，组员将收到通知')
    leaderReviewDialogVisible.value = false
    editDialogVisible.value = false
    fetchTasks()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '驳回失败')
  } finally {
    isSavingTask.value = false
  }
}

const saveTask = async () => {
  if (!editingTask.value.title) {
    ElMessage.warning('任务标题不能为空')
    return
  }
  isSavingTask.value = true
  try {
    await api.put(`/tasks/${editingTask.value.id}`, {
      title: editingTask.value.title,
      description: editingTask.value.description,
      status: editingTask.value.status,
      priority: editingTask.value.priority,
      assigned_to_id: editingTask.value.assigned_to_id,
      deadline: editingTask.value.deadline || null,
      phase: editingTask.value.phase || null,
      weight: editingTask.value.weight
    })
    ElMessage.success('任务更新成功')
    editDialogVisible.value = false
    fetchTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || '更新失败')
  } finally {
    isSavingTask.value = false
  }
}

// Utils
const formatDate = (dateStr) => {
  if (!dateStr) return '无'
  const d = new Date(dateStr)
  return `${d.getMonth()+1}月${d.getDate()}日`
}

const isOverdue = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) < new Date(new Date().setHours(0,0,0,0))
}

// Focus Mode Logic
const focusSetupVisible = ref(false)
const selectedFocusMinutes = ref(25)
const isFocusMode = ref(false)
const focusTimeRemaining = ref(0)
const focusTimer = ref(null)

const openFocusSetup = () => {
  selectedFocusMinutes.value = 25
  focusSetupVisible.value = true
}

const startFocusMode = () => {
  if (selectedFocusMinutes.value <= 0) return
  focusTimeRemaining.value = selectedFocusMinutes.value * 60
  isFocusMode.value = true
  focusSetupVisible.value = false
  editDialogVisible.value = false
  
  focusTimer.value = setInterval(() => {
    focusTimeRemaining.value--
    if (focusTimeRemaining.value <= 0) {
      finishFocusMode(true)
    }
  }, 1000)
}

const formatFocusTime = computed(() => {
  const m = Math.floor(focusTimeRemaining.value / 60).toString().padStart(2, '0')
  const s = (focusTimeRemaining.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const cancelFocusMode = () => {
  if (focusTimer.value) clearInterval(focusTimer.value)
  isFocusMode.value = false
  ElMessage.info('已放弃专注，不记录时长')
}

const finishFocusMode = async (auto = false) => {
  if (focusTimer.value) clearInterval(focusTimer.value)
  
  const totalSeconds = selectedFocusMinutes.value * 60
  const completedSeconds = totalSeconds - (focusTimeRemaining.value > 0 ? focusTimeRemaining.value : 0)
  const completedMinutes = Math.floor(completedSeconds / 60)
  
  isFocusMode.value = false
  
  if (completedMinutes > 0) {
    try {
      const res = await api.post(`/projects/${authStore.user.current_project_id}/focus`, { minutes: completedMinutes })
      const earned = res.data.earned_points || 0
      
      const meRes = await api.get('/auth/me')
      authStore.user = meRes.data
      
      ElMessage({
        message: auto ? `专注完成！记录 ${completedMinutes} 分钟，获得 ${earned} 积分` : `提前结束专注，记录 ${completedMinutes} 分钟，获得 ${earned} 积分`,
        type: 'success',
        duration: 5000
      })
    } catch (err) {
      ElMessage.error('记录专注时长失败')
    }
  } else {
    ElMessage.info('专注时间不足 1 分钟，未记录时长')
  }
}

onUnmounted(() => {
  if (focusTimer.value) clearInterval(focusTimer.value)
})

const getPriorityLabel = (p) => {
  const map = { 'low': '低', 'medium': '中', 'high': '高', 'urgent': '紧急' }
  return map[p] || p
}
const getPhaseLabel = (s) => {
  const map = { 'requirement': '需求', 'design': '设计', 'coding': '编码', 'test': '测试' }
  return map[s] || s
}
const getPriorityBg = (p) => {
  const map = { 
    'low': 'bg-zinc-100 text-zinc-600', 
    'medium': 'bg-green-100 text-green-700', 
    'high': 'bg-orange-100 text-orange-700', 
    'urgent': 'bg-red-100 text-red-700' 
  }
  return map[p] || 'bg-zinc-100 text-zinc-600'
}
const getStatusLabel = (s) => {
  const map = { 'todo': '待办', 'in_progress': '进行中', 'review': '待审核', 'done': '已完成' }
  return map[s] || s
}
const getStatusBg = (s) => {
  const map = { 
    'todo': 'bg-zinc-100 text-zinc-600', 
    'in_progress': 'bg-blue-100 text-blue-700', 
    'review': 'bg-amber-100 text-amber-700', 
    'done': 'bg-emerald-100 text-emerald-700' 
  }
  return map[s] || 'bg-zinc-100 text-zinc-600'
}
const tableRowClassName = ({ row }) => {
  if (row.status === 'done') return 'opacity-50'
  return ''
}

watch(() => authStore.user?.current_project_id, () => {
  fetchTasks()
  loadMembers()
})

onMounted(() => {
  fetchTasks()
  loadMembers()
})
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  height: 6px;
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e4e4e7;
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.minimal-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #3f3f46;
  font-weight: 500;
}
</style>
