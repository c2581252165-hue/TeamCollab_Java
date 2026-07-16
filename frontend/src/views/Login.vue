<template>
  <div class="w-full min-h-screen relative flex items-center justify-center overflow-hidden bg-zinc-50">
    <!-- Minimal Background Pattern -->
    <div class="absolute inset-0 z-0">
      <div class="absolute inset-0 bg-[linear-gradient(rgba(0,0,0,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(0,0,0,0.03)_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_60%_at_50%_50%,#000_70%,transparent_100%)]"></div>
    </div>

    <!-- Login/Register Card -->
    <div class="relative z-10 w-full max-w-md px-6">
      <div class="bg-white/80 backdrop-blur-xl border border-zinc-200 shadow-xl rounded-2xl p-10 transform transition-all duration-500">
        
        <div class="text-center mb-10">
          <div class="inline-flex items-center justify-center w-14 h-14 rounded-xl bg-zinc-900 text-white shadow-md mb-6 transform transition-transform duration-300 hover:scale-105">
            <el-icon :size="28"><Monitor /></el-icon>
          </div>
          <h2 class="text-3xl font-bold text-zinc-900 tracking-tight">TeamCollab Java</h2>
          <p class="text-zinc-500 mt-2 font-medium text-sm">
            {{ isRegistering ? '创建您的账号以开始使用。' : "欢迎回来，让我们共创佳绩。" }}
          </p>
        </div>

        <el-form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="space-y-4">
            <div>
              <el-input 
                v-model="username" 
                placeholder="用户名" 
                :prefix-icon="User" 
                size="large"
                class="minimal-input"
              />
            </div>
            
            <div>
              <el-input 
                v-model="password" 
                type="password" 
                placeholder="密码" 
                :prefix-icon="Lock" 
                size="large" 
                show-password 
                class="minimal-input"
              />
            </div>

            <!-- Role Selection only when registering -->
            <transition name="el-zoom-in-top">
              <div v-if="isRegistering">
                <el-select v-model="role" placeholder="请选择您的角色" size="large" class="w-full minimal-select">
                  <el-option label="学生 (Student)" value="member" />
                  <el-option label="指导老师 (Teacher)" value="teacher" />
                </el-select>
              </div>
            </transition>
          </div>

          <el-button 
            type="primary" 
            native-type="submit" 
            class="w-full h-12 text-base font-semibold rounded-lg !bg-zinc-900 !text-white !border-0 hover:!bg-zinc-800 shadow-md transform hover:-translate-y-0.5 transition-all duration-200" 
            :loading="loading">
            {{ isRegistering ? '注 册' : '登 录' }}
          </el-button>
        </el-form>
      </div>
      
      <p class="text-center mt-8 text-zinc-500 text-sm cursor-pointer hover:text-zinc-900 font-medium transition-colors" @click="isRegistering = !isRegistering">
        {{ isRegistering ? "已有账号？点击这里登录。" : "还没有账号？点击这里注册。" }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { User, Lock, Monitor } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const isRegistering = ref(false)
const username = ref('')
const password = ref('')
const role = ref('member')
const loading = ref(false)
const router = useRouter()
const authStore = useAuthStore()

const handlePostLoginRoute = async () => {
  if (authStore.user?.current_project_id) {
    router.push({ name: 'Dashboard' })
  } else {
    try {
      const memRes = await api.get('/projects/my')
      const memberships = memRes.data
      const approved = memberships.filter(m => m.is_approved)
      if (approved.length > 0) {
        const defaultProject = approved[0].project_id
        await api.post('/projects/switch', { project_id: defaultProject })
        authStore.user.current_project_id = defaultProject
        authStore.user.current_project = approved[0].project
        localStorage.setItem('user', JSON.stringify(authStore.user))
        router.push({ name: 'Dashboard' })
      } else {
        router.push({ name: 'TeamSelect' })
      }
    } catch (err) {
      router.push({ name: 'TeamSelect' })
    }
  }
}

const handleSubmit = async () => {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  
  try {
    if (isRegistering.value) {
      const payload = { username: username.value, password: password.value, role: role.value }
      await api.post('/auth/register', payload)
      ElMessage.success('注册成功，正在为您登录...')
      
      const loginRes = await api.post('/auth/login', { username: username.value, password: password.value }, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
      authStore.token = loginRes.data.access_token
      authStore.user = loginRes.data.user
      
      await handlePostLoginRoute()
    } else {
      const loginRes = await api.post('/auth/login', { username: username.value, password: password.value }, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
      authStore.token = loginRes.data.access_token
      authStore.user = loginRes.data.user
      ElMessage.success('欢迎回来！')
      
      await handlePostLoginRoute()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.detail || '发生了未知错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
:deep(.minimal-input .el-input__wrapper),
:deep(.minimal-select .el-input__wrapper) {
  background-color: #f4f4f5 !important; /* zinc-100 */
  border-radius: 8px;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  padding: 8px 12px;
}
:deep(.minimal-input .el-input__wrapper:hover),
:deep(.minimal-input .el-input__wrapper.is-focus),
:deep(.minimal-select .el-input__wrapper:hover),
:deep(.minimal-select .el-input__wrapper.is-focus) {
  background-color: #ffffff !important;
  border-color: #d4d4d8 !important; /* zinc-300 */
  box-shadow: 0 0 0 1px #d4d4d8 inset !important;
}
:deep(.minimal-input .el-input__inner),
:deep(.minimal-select .el-input__inner) {
  color: #18181b !important; /* zinc-900 */
  font-weight: 500;
}
:deep(.minimal-input .el-input__inner::placeholder),
:deep(.minimal-select .el-input__inner::placeholder) {
  color: #a1a1aa !important; /* zinc-400 */
}
:deep(.minimal-input .el-input__prefix-inner) {
  color: #71717a; /* zinc-500 */
}
</style>
