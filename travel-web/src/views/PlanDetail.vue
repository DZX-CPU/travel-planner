<template>
  <div class="plan-detail" v-loading="loading">
    <template v-if="plan">
      <div class="head">
        <div>
          <h2>{{ plan.title }}</h2>
          <p class="sub">
            {{ plan.city }} · {{ plan.days }}天
            <span v-if="plan.people"> · {{ plan.people }}人</span>
            <span v-if="plan.budget"> · 总预算 ¥{{ plan.budget }}</span>
            <span v-if="plan.budget && plan.people > 1 && perCapita">（人均 ¥{{ perCapita }}）</span>
            <span v-if="plan.arrivalTime"> · 抵达：{{ plan.arrivalTime }}</span>
            <span v-if="plan.preference"> · 偏好：{{ plan.preference }}</span>
          </p>
          <p class="summary" v-if="plan.aiSummary">{{ plan.aiSummary }}</p>
        </div>
      </div>

      <div class="content">
        <div class="timeline-col">
          <div v-for="day in days" :key="day.id" class="day-block">
            <h4>Day {{ day.dayNo }} <span class="theme">{{ day.theme }}</span></h4>
            <el-timeline>
              <el-timeline-item
                v-for="item in itemsByDay[day.id] || []"
                :key="item.id"
                :timestamp="item.startTime + ' - ' + (item.endTime || '')"
                :type="dotType(item.activity)"
              >
                <div class="item-card">
                  <div class="item-title">
                    <el-tag size="small" :type="tagType(item.activity)">{{ item.activity || '行程' }}</el-tag>
                    <b style="margin-left: 8px">{{ item.spotName }}</b>
                    <span v-if="item.cost" class="cost">¥{{ item.cost }}</span>
                  </div>
                  <p class="item-desc">{{ item.description }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>

        <div class="map-col">
          <div class="map-wrap">
            <div ref="mapRef" class="map"></div>
            <!-- 悬停信息卡片：绝对定位在地图左下角，不跟地图动也不会被裁切 -->
            <transition name="fade">
              <div v-if="hoverInfo" class="spot-card">
                <div class="spot-card-head">
                  <el-tag size="small" :color="'transparent'" style="border-color: transparent">Day{{ hoverInfo.dayNo }}</el-tag>
                  <b>{{ hoverInfo.name }}</b>
                  <el-tag size="small" type="info">{{ hoverInfo.activity || '行程' }}</el-tag>
                </div>
                <div class="spot-card-meta">
                  <span>{{ hoverInfo.startTime || '' }} - {{ hoverInfo.endTime || '' }}</span>
                  <span v-if="hoverInfo.cost" class="cost">¥{{ hoverInfo.cost }}</span>
                </div>
                <p class="spot-card-desc">{{ hoverInfo.description }}</p>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { loadAmap } from '../utils/amap'

const route = useRoute()
const planId = route.params.id

const plan = ref(null)
const days = ref([])
const itemsByDay = ref({})
const loading = ref(true)
const mapRef = ref(null)
const hoverInfo = ref(null)

// 老数据改表前没人数，按 1 人算，正好等于它原来的"元/人"
const perCapita = computed(() => {
  if (!plan.value || !plan.value.budget || !plan.value.people) return 0
  return Math.floor(plan.value.budget / plan.value.people)
})

function dotType(activity) {
  if (activity === '用餐') return 'warning'
  if (activity === '住宿') return 'success'
  return 'primary'
}
function tagType(activity) {
  if (activity === '用餐') return 'warning'
  if (activity === '住宿') return 'success'
  if (activity === '交通') return 'info'
  return ''
}

onMounted(async () => {
  const data = await request.get('/plan/' + planId)
  plan.value = data.plan
  days.value = data.days
  itemsByDay.value = data.itemsByDay
  loading.value = false

  await nextTick()
  renderMap()
})

async function renderMap() {
  // 收集所有有坐标的条目
  const points = []
  for (const day of days.value) {
    for (const item of itemsByDay.value[day.id] || []) {
      if (item.longitude && item.latitude) {
        points.push({
          lng: Number(item.longitude),
          lat: Number(item.latitude),
          name: item.spotName,
          dayNo: day.dayNo,
          item
        })
      }
    }
  }
  if (points.length === 0) return

  const AMap = await loadAmap()
  // center先给第一个点，避免加载瞬间视野飘到别处
  const map = new AMap.Map(mapRef.value, {
    zoom: 12,
    center: [points[0].lng, points[0].lat]
  })

  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#9c27b0', '#00bcd4', '#7cb342']

  // 悬停信息直接更新左下角卡片，不用InfoWindow，地图不动、信息也不裁切
  function showCard(p) {
    hoverInfo.value = {
      dayNo: p.dayNo,
      name: p.name,
      activity: p.item.activity,
      startTime: p.item.startTime,
      endTime: p.item.endTime,
      cost: p.item.cost,
      description: p.item.description
    }
  }
  function hideCard() {
    hoverInfo.value = null
  }

  // 起点旗标 / 终点方格旗 / 途经点小圆点，都是按天配色
  function markerHtml(p, kind) {
    const c = colors[(p.dayNo - 1) % colors.length]
    if (kind === 'start') {
      return `<div style="display:flex;align-items:center;gap:2px">
        <div style="padding:2px 6px;border-radius:3px;background:${c};color:#fff;font-size:11px;font-weight:bold;white-space:nowrap">起</div>
      </div>`
    }
    if (kind === 'end') {
      return `<div style="display:flex;align-items:center;gap:2px">
        <div style="padding:2px 6px;border-radius:3px;background:${c};color:#fff;font-size:11px;font-weight:bold;white-space:nowrap">终</div>
      </div>`
    }
    return `<div style="width:10px;height:10px;border-radius:50%;background:${c};border:1.5px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,0.3)"></div>`
  }

  points.forEach((p, i) => {
    const isStart = i === 0 || points[i - 1].dayNo !== p.dayNo
    const isEnd = i === points.length - 1 || points[i + 1].dayNo !== p.dayNo
    const kind = isStart ? 'start' : (isEnd ? 'end' : 'way')

    const marker = new AMap.Marker({
      position: [p.lng, p.lat],
      content: markerHtml(p, kind),
      anchor: 'center',
      cursor: 'pointer'
    })
    marker.on('mouseover', () => showCard(p))
    marker.on('mouseout', hideCard)
    marker.on('click', () => showCard(p))
    map.add(marker)

    // 同一天内的点按顺序连线；线色跟当天，showDir画出方向箭头
    if (i > 0 && points[i].dayNo === points[i - 1].dayNo) {
      map.add(new AMap.Polyline({
        path: [[points[i - 1].lng, points[i - 1].lat], [p.lng, p.lat]],
        strokeColor: colors[(p.dayNo - 1) % colors.length],
        strokeWeight: 4,
        strokeOpacity: 0.85,
        showDir: true
      }))
    }
  })

  // 点和线都加完再适配视野，把整条路线框进来
  map.setFitView()
}
</script>

<style scoped>
.plan-detail {
  max-width: 1200px;
  margin: 28px auto;
  padding: 0 24px;
}
.head h2 {
  margin-bottom: 10px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.sub {
  color: #94a3b8;
  font-size: 14px;
}
.summary {
  margin-top: 14px;
  padding: 16px 18px;
  background: #f0f9ff;
  border: 1px solid #e0f2fe;
  border-radius: 12px;
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
}
.content {
  display: flex;
  gap: 24px;
  margin-top: 28px;
}
.timeline-col {
  flex: 1;
  min-width: 0;
}
.day-block {
  margin-bottom: 32px;
}
.day-block h4 {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e2e8f0;
}
.theme {
  color: #0ea5e9;
  font-size: 13px;
  font-weight: 400;
  margin-left: 10px;
}
.item-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 16px;
  border: 1px solid #f1f5f9;
  transition: all 0.2s ease-out;
}
.item-card:hover {
  border-color: #e0f2fe;
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.08);
}
.item-title {
  display: flex;
  align-items: center;
}
.cost {
  margin-left: auto;
  color: #f97316;
  font-weight: 600;
}
.item-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}
.map-col {
  width: 460px;
  flex-shrink: 0;
}
.map-wrap {
  position: sticky;
  top: 20px;
}
.map {
  width: 100%;
  height: 600px;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
}
/* 悬停信息卡片：叠在地图左下角 */
.spot-card {
  position: absolute;
  left: 14px;
  bottom: 14px;
  max-width: 300px;
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.15);
  z-index: 200;
  font-size: 13px;
}
.spot-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.spot-card-meta {
  display: flex;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 6px;
}
.spot-card-desc {
  color: #475569;
  line-height: 1.6;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
