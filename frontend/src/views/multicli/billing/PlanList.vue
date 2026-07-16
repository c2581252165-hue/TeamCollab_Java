<template>
  <div class="h-full space-y-6 animate-in pb-8">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-zinc-900 tracking-tight">账单与套餐计划</h2>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-4">
      <div v-for="p in plans" :key="p.id" class="bg-white rounded-xl shadow-sm border flex flex-col relative transition-all duration-200" :class="sub?.planId === p.id ? 'border-zinc-900 ring-1 ring-zinc-900' : 'border-zinc-200 hover:border-zinc-300'">
        <div v-if="sub?.planId === p.id" class="absolute -top-3 left-1/2 -translate-x-1/2 bg-zinc-900 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">当前生效</div>
        <div class="p-6 border-b border-zinc-100 flex-1">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-bold text-zinc-900">{{ p.planName }}</h3>
            <span class="px-2 py-1 bg-zinc-100 text-zinc-600 rounded text-[10px] font-semibold">{{ p.planCode }}</span>
          </div>
          <div class="flex items-baseline mb-2">
            <span class="text-3xl font-extrabold text-zinc-900">¥{{ p.monthlyPriceCny }}</span>
            <span class="text-sm font-medium text-zinc-500 ml-1">/月</span>
          </div>
          <p class="text-sm text-zinc-500 mb-6 min-h-[40px]">{{ p.description }}</p>
          
          <ul class="space-y-3 text-sm text-zinc-700">
            <li class="flex items-center"><el-icon class="text-green-500 mr-2"><Check /></el-icon>最多 {{ p.maxProjects }} 个项目</li>
            <li class="flex items-center"><el-icon class="text-green-500 mr-2"><Check /></el-icon>每月 {{ p.maxTasksPerMonth }} 个任务</li>
            <li class="flex items-center"><el-icon class="text-green-500 mr-2"><Check /></el-icon>{{ p.maxConcurrency }} 线程并发执行</li>
            <li class="flex items-center"><el-icon class="text-green-500 mr-2"><Check /></el-icon>赠送 {{ p.monthlyCredits }} AI 积分</li>
          </ul>
        </div>
        <div class="p-6 bg-zinc-50 rounded-b-xl">
          <el-button v-if="sub?.planId !== p.id" type="primary" class="w-full !bg-zinc-900 !border-none hover:!bg-zinc-800 transition-colors" @click="buy(p.id)">升级计划</el-button>
          <el-button v-else plain disabled class="w-full !bg-zinc-200 !text-zinc-500 !border-transparent">正在使用</el-button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../../utils/api'
import { ElMessage } from 'element-plus'
const plans = ref([]); const sub = ref(null)
onMounted(async () => { 
  try { const r = await api.get('/billing/plans'); plans.value = r.data; } catch(e){ console.error(e) }
  try { const s = await api.get('/billing/subscription'); sub.value = s.data } catch(e){} 
})
async function buy(planId) {
  try { 
    const r = await api.post(`/billing/orders?planId=${planId}`); 
    await api.post(`/billing/orders/${r.data.id}/mock-pay`); 
    ElMessage.success('购买成功！套餐已激活'); 
    const s = await api.get('/billing/subscription'); sub.value = s.data;
  } catch(e) { ElMessage.error(e.response?.data?.message||'购买失败') }
}
</script>
<style scoped>
.active { border: 2px solid #409eff; }
</style>
