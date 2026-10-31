<template>
  <div class="my-plans">
    <div class="toolbar">
      <h2>我的行程</h2>
      <el-button type="primary" @click="$router.push('/generate')">+ 新行程</el-button>
    </div>

    <el-empty v-if="!loading && plans.length === 0" description="还没有行程，让 AI 帮你规划第一个吧">
      <el-button type="primary" @click="$router.push('/generate')">AI 行程定制</el-button>
    </el-empty>

    <div class="plan-grid">
      <el-card v-for="plan in plans" :key="plan.id" class="plan-card" shadow="hover"
               @click="$router.push('/plan/' + plan.id)">
        <div class="plan-card-head">
          <el-tag size="small" effect="dark" color="#0ea5e9" style="color:#fff; border: none">{{ plan.city }}</el-tag>
          <span class="plan-days">{{ plan.days }}天</span>
        </div>
        <h3>{{ plan.title }}</h3>
        <p class="plan-summary">{{ plan.aiSummary || '暂无简介' }}</p>
        <div class="plan-meta">
          <span v-if="plan.budget">总预算 ¥{{ plan.budget }}</span>
          <span v-if="plan.people">{{ plan.people }}人</span>
          <span>{{ (plan.createTime || '').slice(0, 10) }}</span>
          <el-popconfirm title="确定删除这个行程吗？" @confirm="del(plan.id)">
            <template #reference>
              <el-button link type="danger" size="small" @click.stop>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const plans = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    plans.value = await request.get('/plan/my')
  } finally {
    loading.value = false
  }
}

async function del(id) {
  await request.delete('/plan/' + id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.my-plans {
  max-width: 1100px;
  margin: 28px auto;
  padding: 0 24px;
  min-height: calc(100vh - 60px);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.toolbar h2 {
  font-size: 20px;
  font-weight: 600;
}
.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.plan-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.25s ease-out;
}
.plan-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.10);
}
.plan-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.plan-days {
  font-size: 12px;
  color: #94a3b8;
}
.plan-card h3 {
  font-size: 15px;
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.plan-summary {
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.plan-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 12px;
}
.plan-meta .el-button {
  margin-left: auto;
}
@media (max-width: 900px) {
  .plan-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}
</style>
