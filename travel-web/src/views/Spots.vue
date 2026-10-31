<template>
  <div class="spots-page">
    <div class="toolbar">
      <div>
        <h2>找景点</h2>
        <p class="sub">来自高德地图的真实景点数据（数据加载后由本系统缓存）</p>
      </div>
      <div style="display: flex; gap: 12px">
        <el-select v-model="city" filterable placeholder="选择城市" style="width: 200px" @change="load">
          <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索景点名称" style="width: 240px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">搜索</el-button>
      </div>
    </div>
    <div class="count-bar" v-if="spots.length">
      共找到 <b>{{ spots.length }}</b> 个景点
      <el-divider direction="vertical" />
      想直接要一份完整行程？
      <el-button link type="primary" @click="$router.push({ path: '/generate' })">去 AI 定制 →</el-button>
    </div>

    <el-table :data="spots" v-loading="loading" stripe>
      <el-table-column prop="name" label="景点名称" min-width="180" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
      <el-table-column label="门票" width="100">
        <template #default="{ row }">
          {{ row.ticketPrice != null ? '¥' + row.ticketPrice : '未知' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="showMap(row)">看地图</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="mapVisible" :title="currentSpot?.name" width="700px">
      <div ref="mapRef" style="width: 100%; height: 400px"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { loadAmap } from '../utils/amap'

const route = useRoute()
const cities = ['北京', '上海', '杭州', '苏州', '南京', '成都', '重庆', '西安', '厦门', '广州', '深圳', '长沙', '武汉', '青岛', '三亚']
const city = ref(route.query.city || '杭州')
const keyword = ref('')
const spots = ref([])
const loading = ref(false)

const mapVisible = ref(false)
const currentSpot = ref(null)
const mapRef = ref(null)

async function load() {
  if (!city.value) return
  loading.value = true
  try {
    spots.value = await request.get('/spot/search', { params: { city: city.value, keyword: keyword.value } })
  } finally {
    loading.value = false
  }
}

async function showMap(spot) {
  currentSpot.value = spot
  mapVisible.value = true
  await nextTick()
  const AMap = await loadAmap()
  const map = new AMap.Map(mapRef.value, {
    zoom: 15,
    center: [spot.longitude, spot.latitude]
  })
  new AMap.Marker({
    position: [spot.longitude, spot.latitude],
    title: spot.name
  }).setMap(map)
}

onMounted(load)
</script>

<style scoped>
.spots-page {
  max-width: 1100px;
  margin: 28px auto;
  padding: 0 24px;
  min-height: calc(100vh - 60px);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16px;
}
.toolbar h2 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}
.sub {
  font-size: 13px;
  color: #94a3b8;
}
.count-bar {
  background: #f0f9ff;
  border: 1px solid #e0f2fe;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 13px;
  color: #475569;
  margin-bottom: 14px;
}
</style>
