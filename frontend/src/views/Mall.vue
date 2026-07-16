<template>
  <div>
    <div class="h-full flex flex-col p-8 space-y-6 animate-fade-in relative z-10" v-if="authStore.user?.current_project_id">
    <div class="flex justify-between items-center mb-4">
      <div>
        <h1 class="text-3xl font-extrabold text-slate-800 tracking-tight">积分商城</h1>
        <p class="text-slate-500 mt-1">努力完成任务，兑换属于你的专属特权！</p>
      </div>
      <div class="flex items-center space-x-4">
        <div class="bg-indigo-50 px-4 py-2 rounded-xl border border-indigo-100 flex items-center shadow-sm">
          <span class="text-indigo-400 mr-2 text-xl">💰</span>
          <span class="text-sm font-medium text-slate-600 mr-2">我的余额:</span>
          <span class="text-xl font-black text-indigo-600">{{ authStore.user.points }} 积分</span>
        </div>
        <el-button v-if="isLeader" type="success" @click="showAddItemModal = true" class="!rounded-xl shadow-md">
          <el-icon class="mr-1"><Plus /></el-icon> 新增商品
        </el-button>
      </div>
    </div>

    <!-- Items Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div v-for="item in items" :key="item.id" 
           class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 hover:shadow-xl hover:-translate-y-1 transition-all group flex flex-col relative overflow-hidden">
        <div class="absolute -right-6 -top-6 w-24 h-24 bg-gradient-to-br from-indigo-100 to-purple-50 rounded-full opacity-50 group-hover:scale-150 transition-transform duration-500"></div>
        <div class="text-5xl mb-4 relative z-10">{{ item.icon }}</div>
        <h3 class="text-lg font-bold text-slate-800 relative z-10">{{ item.name }}</h3>
        <p class="text-sm text-slate-500 mt-2 mb-6 flex-1 relative z-10 line-clamp-3">{{ item.description }}</p>
        
        <div class="flex items-center justify-between mt-auto relative z-10">
          <span class="text-lg font-black text-indigo-600">{{ item.price }} 积分</span>
          <div class="flex space-x-2">
            <el-button v-if="isLeader" type="danger" plain circle size="small" @click="deleteItem(item.id)" title="删除商品">
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button type="primary" :disabled="authStore.user.points < item.price" @click="purchaseItem(item)" class="!rounded-xl font-bold px-6 shadow-md shadow-indigo-500/20">
              兑换
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Purchase History -->
    <div class="mt-12 bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
      <h2 class="text-xl font-bold text-slate-800 mb-6 flex items-center">
        <el-icon class="mr-2 text-indigo-500"><List /></el-icon>
        最近兑换记录
      </h2>
      <el-table :data="history" style="width: 100%" :empty-text="'暂无兑换记录'">
        <el-table-column prop="user.username" label="兑换人" width="150" />
        <el-table-column label="商品">
          <template #default="{ row }">
            <span class="font-medium text-slate-700">{{ row.item.icon }} {{ row.item.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="花费积分" width="120">
          <template #default="{ row }">
            <span class="text-indigo-600 font-bold">-{{ row.item.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            <span class="text-slate-500 text-sm">{{ dayjs(row.purchased_at).format('YYYY-MM-DD HH:mm') }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
  
  <div v-else class="text-center py-20 animate-fade-in relative z-10">
    <h2 class="text-2xl font-bold text-slate-700">您尚未加入任何团队</h2>
    <p class="text-slate-500 mt-2">请先在团队切换界面加入或创建一个团队。</p>
    <el-button type="primary" class="mt-6" @click="$router.push('/teams')">去切换/加入团队</el-button>
  </div>

  <!-- Add Item Modal -->
  <el-dialog v-model="showAddItemModal" title="新增商品" width="400px" custom-class="rounded-2xl">
    <el-form :model="newItem" label-width="80px">
      <el-form-item label="商品图标">
        <el-input v-model="newItem.icon" placeholder="例如：🎁、☕ 等 Emoji" />
      </el-form-item>
      <el-form-item label="商品名称">
        <el-input v-model="newItem.name" placeholder="例如：请喝奶茶" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="newItem.description" type="textarea" placeholder="详细描述这个奖励..." />
      </el-form-item>
      <el-form-item label="所需积分">
        <el-input-number v-model="newItem.price" :min="1" :step="50" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showAddItemModal = false">取消</el-button>
        <el-button type="primary" @click="submitNewItem" :loading="isSubmitting">添加</el-button>
      </span>
    </template>
  </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { List, Plus, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import api from '../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const authStore = useAuthStore()
const items = ref([])
const history = ref([])
const isLeader = ref(false)

const showAddItemModal = ref(false)
const isSubmitting = ref(false)
const newItem = ref({
  name: '',
  description: '',
  price: 100,
  icon: '🎁'
})

const fetchMallData = async () => {
  if (!authStore.user?.current_project_id) return
  try {
    const [resItems, resHistory, resMembers] = await Promise.all([
      api.get(`/mall/items/${authStore.user.current_project_id}`),
      api.get(`/mall/history/${authStore.user.current_project_id}`),
      api.get(`/projects/${authStore.user.current_project_id}/members`)
    ])
    items.value = resItems.data
    history.value = resHistory.data
    
    // Check if current user is leader
    const myMembership = resMembers.data.find(m => m.user_id === authStore.user.id)
    if (myMembership && myMembership.role === 'leader') {
      isLeader.value = true
    } else if (authStore.user.role === 'teacher') {
      isLeader.value = true // Teacher also has leader privileges
    }
  } catch (err) {
    ElMessage.error('加载商城数据失败')
  }
}

const purchaseItem = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要花费 ${item.price} 积分兑换【${item.name}】吗？`, '兑换确认', {
      confirmButtonText: '确定兑换',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await api.post(`/mall/purchase/${item.id}`)
    ElMessage.success(`🎉 恭喜！成功兑换【${item.name}】`)
    
    // Refresh user points
    const res = await api.get('/auth/me')
    authStore.user = res.data
    
    fetchMallData()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.detail || '兑换失败')
    }
  }
}

const submitNewItem = async () => {
  if (!newItem.value.name || !newItem.value.description || !newItem.value.icon) {
    ElMessage.warning('请完整填写商品信息')
    return
  }
  isSubmitting.value = true
  try {
    await api.post('/mall/items', {
      project_id: authStore.user.current_project_id,
      name: newItem.value.name,
      description: newItem.value.description,
      price: newItem.value.price,
      icon: newItem.value.icon
    })
    ElMessage.success('商品添加成功！')
    showAddItemModal.value = false
    // reset form
    newItem.value = { name: '', description: '', price: 100, icon: '🎁' }
    fetchMallData()
  } catch (err) {
    ElMessage.error(err.response?.data?.detail || '添加失败')
  } finally {
    isSubmitting.value = false
  }
}

const deleteItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定要下架此商品吗？已兑换的记录不会受影响。', '删除确认', {
      confirmButtonText: '确定下架',
      cancelButtonText: '取消',
      type: 'danger'
    })
    await api.delete(`/mall/items/${itemId}`)
    ElMessage.success('已下架商品')
    fetchMallData()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchMallData()
})
</script>
