<template>
  <div class="profile-page">
    <el-card style="max-width: 500px">
      <h3 style="margin-bottom: 16px">个人中心</h3>
      <el-form :model="form" label-width="80px" v-loading="loading">
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(true)
const form = reactive({ username: '', nickname: '', email: '', phone: '' })

onMounted(async () => {
  const info = await request.get('/user/info')
  Object.assign(form, info)
  loading.value = false
})

async function save() {
  await request.put('/user/info', form)
  ElMessage.success('已保存')
  userStore.fetchInfo()
}
</script>

<style scoped>
.profile-page {
  max-width: 700px;
  margin: 28px auto;
  padding: 0 24px;
}
</style>
