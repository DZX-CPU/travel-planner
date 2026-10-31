<template>
  <div class="note-detail" v-loading="loading">
    <el-card v-if="note">
      <h2>{{ note.title }}</h2>
      <div class="meta">
        <el-tag size="small" v-if="note.city">{{ note.city }}</el-tag>
        <span>{{ note.createTime }}</span>
        <span>{{ note.viewCount }} 次浏览</span>
        <el-button v-if="note.userId === myUserId" link type="danger" size="small" @click="del">删除</el-button>
      </div>
      <div class="content">{{ note.content }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const note = ref(null)
const loading = ref(true)
const myUserId = ref(null)

async function load() {
  note.value = await request.get('/note/' + route.params.id)
  loading.value = false
  const info = await request.get('/user/info')
  myUserId.value = info.id
}

async function del() {
  await request.delete('/note/' + route.params.id)
  ElMessage.success('已删除')
  router.push('/notes')
}

onMounted(load)
</script>

<style scoped>
.note-detail {
  max-width: 800px;
  margin: 28px auto;
  padding: 0 24px;
}
.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #999;
  font-size: 13px;
  margin: 12px 0 20px;
}
.content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #333;
}
</style>
