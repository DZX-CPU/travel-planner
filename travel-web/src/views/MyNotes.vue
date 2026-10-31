<template>
  <div class="my-notes">
    <el-card style="margin-bottom: 20px">
      <h3 style="margin-bottom: 12px">发布游记</h3>
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="游记标题" />
        </el-form-item>
        <el-form-item label="城市">
          <el-select v-model="form.city" placeholder="相关城市" clearable style="width: 180px">
            <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="写点旅行中的见闻…" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="publish">发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <h3 style="margin-bottom: 12px">我的游记</h3>
      <el-table :data="list" stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="viewCount" label="浏览量" width="90" />
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/note/' + row.id)">查看</el-button>
            <el-popconfirm title="确定删除吗？" @confirm="del(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const cities = ['北京', '上海', '杭州', '苏州', '南京', '成都', '重庆', '西安', '厦门', '广州', '深圳', '长沙', '武汉', '青岛', '三亚']
const form = reactive({ title: '', city: '', content: '' })
const list = ref([])

async function load() {
  const data = await request.get('/note/my', { params: { page: 1, size: 50 } })
  list.value = data.records
}

async function publish() {
  if (!form.title || !form.content) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  await request.post('/note', form)
  ElMessage.success('发布成功')
  form.title = ''
  form.city = ''
  form.content = ''
  load()
}

async function del(id) {
  await request.delete('/note/' + id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.my-notes {
  max-width: 900px;
  margin: 28px auto;
  padding: 0 24px;
}
</style>
