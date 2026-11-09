<template>
  <div class="admin-page">
    <div class="page-head">
      <h2>后台管理</h2>
      <p class="sub">用户、景点、行程、游记与运营数据统一维护</p>
    </div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 数据统计 -->
      <el-tab-pane label="数据统计" name="stats">
        <el-row :gutter="16" class="stat-row">
          <el-col :span="8">
            <el-card><el-statistic title="用户总数" :value="stats.userCount || 0" /></el-card>
          </el-col>
          <el-col :span="8">
            <el-card><el-statistic title="行程总数" :value="stats.planCount || 0" /></el-card>
          </el-col>
          <el-col :span="8">
            <el-card><el-statistic title="游记总数" :value="stats.noteCount || 0" /></el-card>
          </el-col>
        </el-row>

        <el-card class="chart-card">
          <h4>各城市行程数量</h4>
          <div ref="cityChartRef" class="chart"></div>
        </el-card>

        <el-card class="chart-card">
          <h4>热门景点 TOP10</h4>
          <div ref="hotChartRef" class="chart"></div>
        </el-card>
      </el-tab-pane>

      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input v-model="userKeyword" placeholder="按用户名搜索" style="width: 220px" clearable
                    @keyup.enter="searchUsers" @clear="searchUsers" />
          <el-button @click="searchUsers">搜索</el-button>
        </div>
        <el-table :data="users" v-loading="loadingUsers" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="nickname" label="昵称" width="140" />
          <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
          <el-table-column prop="createTime" label="注册时间" width="180" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该用户？" @confirm="delUser(row.id)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="userPage" :page-size="10" :total="userTotal"
                       layout="total, prev, pager, next" @current-change="loadUsers"
                       class="pager" />
      </el-tab-pane>

      <!-- 景点管理 -->
      <el-tab-pane label="景点管理" name="spots">
        <div class="toolbar">
          <div class="filters">
            <el-select v-model="spotCity" placeholder="城市筛选" clearable style="width: 150px" @change="searchSpots">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
            <el-input v-model="spotKeyword" placeholder="景点名称/地址" style="width: 200px" clearable
                      @keyup.enter="searchSpots" @clear="searchSpots" />
            <el-button @click="searchSpots">搜索</el-button>
          </div>
          <el-button type="primary" @click="openCreateSpot">+ 新增景点</el-button>
        </div>
        <el-table :data="spots" v-loading="loadingSpots" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="景点名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="city" label="城市" width="80" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
          <el-table-column label="门票" width="90">
            <template #default="{ row }">{{ row.ticketPrice != null ? '¥' + row.ticketPrice : '-' }}</template>
          </el-table-column>
          <el-table-column prop="rating" label="评分" width="70" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditSpot(row)">编辑</el-button>
              <el-popconfirm title="确定删除该景点？" @confirm="delSpot(row.id)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="spotPage" :page-size="10" :total="spotTotal"
                       layout="total, prev, pager, next" @current-change="loadSpots"
                       class="pager" />

        <el-dialog v-model="spotDialog" :title="editingSpotId ? '编辑景点' : '新增景点'" width="560px">
          <el-form ref="spotFormRef" :model="spotForm" :rules="spotRules" label-width="80px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="spotForm.name" placeholder="景点名称" />
            </el-form-item>
            <el-form-item label="城市" prop="city">
              <el-select v-model="spotForm.city" filterable allow-create default-first-option
                         placeholder="选择或输入城市" style="width: 100%">
                <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-input v-model="spotForm.type" placeholder="风景名胜/公园/博物馆…" />
            </el-form-item>
            <el-form-item label="地址">
              <el-input v-model="spotForm.address" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="经度" prop="longitude">
                  <el-input-number v-model="spotForm.longitude" :precision="6" :step="0.000001"
                                   :controls="false" placeholder="120.150000" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="纬度" prop="latitude">
                  <el-input-number v-model="spotForm.latitude" :precision="6" :step="0.000001"
                                   :controls="false" placeholder="30.250000" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="门票">
                  <el-input-number v-model="spotForm.ticketPrice" :min="0" :precision="2" :controls="false"
                                   placeholder="元" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="评分">
                  <el-input-number v-model="spotForm.rating" :min="0" :max="5" :step="0.1" :precision="1"
                                   :controls="false" placeholder="0-5" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="图片">
              <el-input v-model="spotForm.imageUrl" placeholder="图片 URL" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="spotForm.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="spotDialog = false">取消</el-button>
            <el-button type="primary" :loading="spotSaving" @click="saveSpot">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 行程管理 -->
      <el-tab-pane label="行程管理" name="plans">
        <div class="toolbar">
          <el-input v-model="planKeyword" placeholder="按标题/城市搜索" style="width: 240px" clearable
                    @keyup.enter="searchPlans" @clear="searchPlans" />
          <el-button @click="searchPlans">搜索</el-button>
        </div>
        <el-table :data="plans" v-loading="loadingPlans" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="city" label="城市" width="80" />
          <el-table-column prop="days" label="天数" width="70" />
          <el-table-column prop="people" label="人数" width="70" />
          <el-table-column label="预算" width="110">
            <template #default="{ row }">{{ row.budget != null ? '¥' + row.budget : '-' }}</template>
          </el-table-column>
          <el-table-column label="作者" width="110">
            <template #default="{ row }">{{ row.username || ('用户#' + row.userId) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push('/plan/' + row.id)">查看</el-button>
              <el-popconfirm title="确定删除该行程？" @confirm="delPlan(row.id)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="planPage" :page-size="10" :total="planTotal"
                       layout="total, prev, pager, next" @current-change="loadPlans"
                       class="pager" />
      </el-tab-pane>

      <!-- 游记管理 -->
      <el-tab-pane label="游记管理" name="notes">
        <div class="toolbar">
          <el-input v-model="noteKeyword" placeholder="按标题/正文搜索" style="width: 240px" clearable
                    @keyup.enter="searchNotes" @clear="searchNotes" />
          <el-button @click="searchNotes">搜索</el-button>
        </div>
        <el-table :data="notes" v-loading="loadingNotes" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="city" label="城市" width="90" />
          <el-table-column label="作者" width="110">
            <template #default="{ row }">{{ row.username || ('用户#' + row.userId) }}</template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览量" width="90" />
          <el-table-column prop="createTime" label="发布时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push('/note/' + row.id)">查看</el-button>
              <el-popconfirm title="确定删除该游记？" @confirm="delNote(row.id)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="notePage" :page-size="10" :total="noteTotal"
                       layout="total, prev, pager, next" @current-change="loadNotes"
                       class="pager" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '../utils/request'

const cities = ['北京', '上海', '杭州', '苏州', '南京', '成都', '重庆', '西安', '厦门', '广州', '深圳', '长沙', '武汉', '青岛', '三亚']

const tab = ref('stats')

/* ---------------- 数据统计 ---------------- */
const stats = ref({})
const cityChartRef = ref(null)
const hotChartRef = ref(null)
let cityChart = null
let hotChart = null

async function loadStats() {
  stats.value = await request.get('/admin/stats')
  await nextTick()
  renderStats()
}

function renderStats() {
  if (cityChartRef.value) {
    if (!cityChart) cityChart = echarts.init(cityChartRef.value)
    const entries = Object.entries(stats.value.cityStats || {}).sort((a, b) => b[1] - a[1])
    cityChart.setOption({
      grid: { left: 45, right: 24, top: 20, bottom: 30 },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: entries.map(e => e[0]), axisLabel: { interval: 0, rotate: entries.length > 8 ? 30 : 0 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: entries.map(e => e[1]), barMaxWidth: 36, itemStyle: { color: '#0ea5e9' } }]
    }, true)
  }

  if (hotChartRef.value) {
    if (!hotChart) hotChart = echarts.init(hotChartRef.value)
    // 横向柱状图从下往上画，反转一下让第一名在最上面
    const rows = (stats.value.hotSpots || []).slice().reverse()
    hotChart.setOption({
      grid: { left: 130, right: 36, top: 16, bottom: 28 },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: {
        type: 'category',
        data: rows.map(r => r.name),
        axisLabel: { width: 118, overflow: 'truncate' }
      },
      series: [{ type: 'bar', data: rows.map(r => r.count), barMaxWidth: 18, itemStyle: { color: '#f97316' } }]
    }, true)
  }
}

/* ---------------- 用户管理 ---------------- */
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')
const loadingUsers = ref(false)

async function loadUsers() {
  loadingUsers.value = true
  try {
    const data = await request.get('/admin/user/page', {
      params: { page: userPage.value, size: 10, keyword: userKeyword.value.trim() || undefined }
    })
    users.value = data.records
    userTotal.value = Number(data.total)
  } finally {
    loadingUsers.value = false
  }
}

function searchUsers() {
  userPage.value = 1
  loadUsers()
}

async function delUser(id) {
  await request.delete('/admin/user/' + id)
  ElMessage.success('已删除')
  loadUsers()
  loadStats()
}

/* ---------------- 景点管理 ---------------- */
const spots = ref([])
const spotTotal = ref(0)
const spotPage = ref(1)
const spotCity = ref('')
const spotKeyword = ref('')
const loadingSpots = ref(false)
const spotDialog = ref(false)
const spotSaving = ref(false)
const spotFormRef = ref(null)
const editingSpotId = ref(null)
const emptySpot = () => ({
  name: '',
  city: '',
  type: '',
  address: '',
  longitude: null,
  latitude: null,
  ticketPrice: null,
  rating: null,
  imageUrl: '',
  description: ''
})
const spotForm = reactive(emptySpot())
const spotRules = {
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  city: [{ required: true, message: '请选择或输入城市', trigger: 'change' }],
  longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }],
  latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }]
}

async function loadSpots() {
  loadingSpots.value = true
  try {
    const data = await request.get('/admin/spot/page', {
      params: {
        page: spotPage.value,
        size: 10,
        city: spotCity.value || undefined,
        keyword: spotKeyword.value.trim() || undefined
      }
    })
    spots.value = data.records
    spotTotal.value = Number(data.total)
  } finally {
    loadingSpots.value = false
  }
}

function searchSpots() {
  spotPage.value = 1
  loadSpots()
}

function openCreateSpot() {
  editingSpotId.value = null
  Object.assign(spotForm, emptySpot())
  spotDialog.value = true
}

function openEditSpot(row) {
  editingSpotId.value = row.id
  Object.assign(spotForm, {
    name: row.name || '',
    city: row.city || '',
    type: row.type || '',
    address: row.address || '',
    longitude: row.longitude == null ? null : Number(row.longitude),
    latitude: row.latitude == null ? null : Number(row.latitude),
    ticketPrice: row.ticketPrice == null ? null : Number(row.ticketPrice),
    rating: row.rating == null ? null : Number(row.rating),
    imageUrl: row.imageUrl || '',
    description: row.description || ''
  })
  spotDialog.value = true
}

async function saveSpot() {
  await spotFormRef.value.validate()
  spotSaving.value = true
  try {
    const payload = { ...spotForm }
    if (editingSpotId.value) {
      await request.put('/admin/spot/' + editingSpotId.value, payload)
    } else {
      await request.post('/admin/spot', payload)
    }
    ElMessage.success('已保存')
    spotDialog.value = false
    loadSpots()
    loadStats()
  } finally {
    spotSaving.value = false
  }
}

async function delSpot(id) {
  await request.delete('/admin/spot/' + id)
  ElMessage.success('已删除')
  loadSpots()
  loadStats()
}

/* ---------------- 行程管理 ---------------- */
const plans = ref([])
const planTotal = ref(0)
const planPage = ref(1)
const planKeyword = ref('')
const loadingPlans = ref(false)

async function loadPlans() {
  loadingPlans.value = true
  try {
    const data = await request.get('/admin/plan/page', {
      params: { page: planPage.value, size: 10, keyword: planKeyword.value.trim() || undefined }
    })
    plans.value = data.records
    planTotal.value = Number(data.total)
  } finally {
    loadingPlans.value = false
  }
}

function searchPlans() {
  planPage.value = 1
  loadPlans()
}

async function delPlan(id) {
  await request.delete('/admin/plan/' + id)
  ElMessage.success('已删除')
  loadPlans()
  loadStats()
}

/* ---------------- 游记管理 ---------------- */
const notes = ref([])
const noteTotal = ref(0)
const notePage = ref(1)
const noteKeyword = ref('')
const loadingNotes = ref(false)

async function loadNotes() {
  loadingNotes.value = true
  try {
    const data = await request.get('/admin/note/page', {
      params: { page: notePage.value, size: 10, keyword: noteKeyword.value.trim() || undefined }
    })
    notes.value = data.records
    noteTotal.value = Number(data.total)
  } finally {
    loadingNotes.value = false
  }
}

function searchNotes() {
  notePage.value = 1
  loadNotes()
}

async function delNote(id) {
  await request.delete('/admin/note/' + id)
  ElMessage.success('已删除')
  loadNotes()
  loadStats()
}

/* ---------------- 页签切换 ---------------- */
function onTabChange(name) {
  if (name === 'stats') loadStats()
  else if (name === 'users') loadUsers()
  else if (name === 'spots') loadSpots()
  else if (name === 'plans') loadPlans()
  else if (name === 'notes') loadNotes()
}

onMounted(() => {
  loadStats()
  loadUsers()
})
</script>

<style scoped>
.admin-page {
  max-width: 1240px;
  margin: 28px auto;
  padding: 0 24px 40px;
}
.page-head {
  margin-bottom: 18px;
}
.page-head h2 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 4px;
}
.page-head .sub {
  font-size: 13px;
  color: #94a3b8;
}
.stat-row {
  margin-bottom: 16px;
}
.chart-card {
  margin-bottom: 16px;
}
.chart-card h4 {
  margin-bottom: 10px;
  font-size: 15px;
}
.chart {
  width: 100%;
  height: 300px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
