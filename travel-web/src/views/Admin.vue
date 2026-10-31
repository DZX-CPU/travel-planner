<template>
  <div class="admin-page">
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card><el-statistic title="用户总数" :value="stats.userCount || 0" /></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><el-statistic title="行程总数" :value="stats.planCount || 0" /></el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 20px">
      <h4 style="margin-bottom: 10px">各城市行程数量</h4>
      <div ref="chartRef" style="width: 100%; height: 300px"></div>
    </el-card>

    <el-card>
      <div class="toolbar">
        <h4>用户管理</h4>
        <el-input v-model="keyword" placeholder="按用户名搜索" style="width: 200px" clearable @keyup.enter="loadUsers" />
      </div>
      <el-table :data="users" v-loading="loadingUsers" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该用户？" @confirm="delUser(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="10" :total="total"
                     layout="prev, pager, next" @current-change="loadUsers" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '../utils/request'
import * as echarts from 'echarts'

const stats = ref({})
const users = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const loadingUsers = ref(false)
const chartRef = ref(null)

async function loadStats() {
  stats.value = await request.get('/admin/stats')
  renderChart(stats.value.cityStats || {})
}

function renderChart(cityStats) {
  const chart = echarts.init(chartRef.value)
  const entries = Object.entries(cityStats).sort((a, b) => b[1] - a[1])
  chart.setOption({
    xAxis: { type: 'category', data: entries.map(e => e[0]) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: entries.map(e => e[1]), barMaxWidth: 40 }],
    tooltip: { trigger: 'axis' }
  })
}

async function loadUsers() {
  loadingUsers.value = true
  try {
    const data = await request.get('/admin/user/page', {
      params: { page: page.value, size: 10, keyword: keyword.value || undefined }
    })
    users.value = data.records
    total.value = Number(data.total)
  } finally {
    loadingUsers.value = false
  }
}

async function delUser(id) {
  await request.delete('/admin/user/' + id)
  loadUsers()
}

onMounted(() => {
  loadStats()
  loadUsers()
})
</script>

<style scoped>
.admin-page {
  max-width: 1100px;
  margin: 28px auto;
  padding: 0 24px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
