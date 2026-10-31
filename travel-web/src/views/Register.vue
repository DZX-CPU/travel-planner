<template>
  <div class="login-page">
    <div class="panel">
      <div class="brand-side">
        <div class="brand-logo">智游<span>助手</span></div>
        <h2 class="brand-slogan">三秒钟注册<br />开启第一份 AI 行程</h2>
        <ul class="brand-points">
          <li>
            <span class="dot" style="background:#0ea5e9"></span>
            <div><b>免费使用</b><p>注册即可使用全部功能，不限次数</p></div>
          </li>
          <li>
            <span class="dot" style="background:#f97316"></span>
            <div><b>我的行程库</b><p>生成过的行程都存在账号里，随时回看</p></div>
          </li>
          <li>
            <span class="dot" style="background:#22c55e"></span>
            <div><b>游记社区</b><p>分享你的旅行故事，帮到后面出发的人</p></div>
          </li>
        </ul>
        <div class="brand-footer">基于大语言模型的智慧文旅行程规划系统</div>
      </div>

      <div class="form-side">
        <el-card class="login-card">
          <h3 class="title">创建账号</h3>
          <p class="subtitle">已有账号？<router-link to="/login" style="color: #0ea5e9">去登录</router-link></p>
          <el-form :model="form" @keyup.enter="onRegister">
            <el-form-item>
              <el-input v-model="form.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.nickname" placeholder="昵称（选填）" size="large" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

async function onRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  loading.value = true
  try {
    await request.post('/user/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
.panel {
  display: flex;
  width: 920px;
  min-height: 560px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.10);
}
.brand-side {
  flex: 1;
  padding: 56px 44px;
  background: linear-gradient(160deg, #fff7ed 0%, #ffffff 70%);
  display: flex;
  flex-direction: column;
}
.brand-logo {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 40px;
}
.brand-logo span {
  color: #0ea5e9;
}
.brand-slogan {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 40px;
}
.brand-points {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 26px;
}
.brand-points li {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.brand-points b {
  font-size: 15px;
}
.brand-points p {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.6;
}
.brand-footer {
  margin-top: auto;
  font-size: 12px;
  color: #94a3b8;
}
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.login-card {
  width: 100%;
  max-width: 340px;
  border: none;
  box-shadow: none;
}
.title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 6px;
}
.subtitle {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 26px;
}

@media (max-width: 768px) {
  .brand-side {
    display: none;
  }
}
</style>
