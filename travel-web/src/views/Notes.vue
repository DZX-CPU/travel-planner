<template>
  <div class="notes-page">
    <div class="toolbar">
      <h2>旅行游记</h2>
      <div style="display: flex; gap: 12px">
        <el-select v-model="city" placeholder="按城市筛选" clearable style="width: 180px" @change="load">
          <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="$router.push('/my-notes')">写游记</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="还没有游记，来写下第一篇吧">
      <el-button type="primary" @click="$router.push('/my-notes')">写游记</el-button>
    </el-empty>

    <div class="note-grid">
      <el-card v-for="note in list" :key="note.id" class="note-card" shadow="hover"
               @click="$router.push('/note/' + note.id)">
        <h3>{{ note.title }}</h3>
        <p class="excerpt">{{ note.content?.slice(0, 120) }}…</p>
        <div class="meta">
          <el-tag size="small" v-if="note.city">{{ note.city }}</el-tag>
          <span>{{ note.createTime }}</span>
          <span>{{ note.viewCount }} 次浏览</span>
        </div>
      </el-card>
    </div>

    <div v-if="list.length > 0" style="display: flex; justify-content: center; margin-top: 28px">
      <el-pagination v-model:current-page="page" :page-size="10" :total="total"
                     layout="prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '../utils/request'

const cities = ['北京', '上海', '杭州', '苏州', '南京', '成都', '重庆', '西安', '厦门', '广州', '深圳', '长沙', '武汉', '青岛', '三亚']
const list = ref([])
const total = ref(0)
const page = ref(1)
const city = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await request.get('/note/list', {
      params: { page: page.value, size: 10, city: city.value || undefined }
    })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.notes-page {
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
.note-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.note-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.25s ease-out;
}
.note-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.10);
}
.note-card h3 {
  font-size: 16px;
  margin-bottom: 8px;
}
.excerpt {
  color: #64748b;
  font-size: 13px;
  margin: 8px 0 12px;
  line-height: 1.7;
}
.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #94a3b8;
  font-size: 12px;
}
@media (max-width: 768px) {
  .note-grid {
    grid-template-columns: 1fr;
  }
}
</style>
