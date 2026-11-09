<template>
  <div class="login-page">
    <div class="panel">
      <div class="brand-side">
        <div class="brand-logo">智游<span>助手</span></div>
        <h2 class="brand-slogan">让每一次出发<br />都有一份好行程</h2>
        <ul class="brand-points">
          <li>
            <span class="dot" style="background:#0ea5e9"></span>
            <div><b>AI 智能规划</b><p>大模型结合真实景点数据，几秒生成按天行程</p></div>
          </li>
          <li>
            <span class="dot" style="background:#f97316"></span>
            <div><b>地图可视化</b><p>路线自动标点连线，每天的走向一目了然</p></div>
          </li>
          <li>
            <span class="dot" style="background:#22c55e"></span>
            <div><b>游记分享</b><p>记录旅途见闻，看看别人都怎么玩</p></div>
          </li>
        </ul>
        <div class="brand-footer">基于大语言模型的智慧文旅行程规划系统</div>
      </div>

      <div class="form-side">
        <el-card class="login-card">
          <h3 class="title">欢迎回来</h3>
          <p class="subtitle">登录后继续你的旅行规划</p>
          <el-form :model="form" @keyup.enter="onLogin">
            <el-form-item>
              <el-input v-model="form.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onLogin">
                登 录
              </el-button>
            </el-form-item>
            <div class="tips">
              还没有账号？<router-link to="/register" style="color: #0ea5e9">去注册</router-link>
            </div>
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
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    // 管理员进对应的后台界面，普通用户回首页
    router.push(userStore.userInfo?.role === 1 ? '/admin' : '/home')
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
  background: linear-gradient(160deg, #f0f9ff 0%, #ffffff 70%);
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
.tips {
  text-align: center;
  font-size: 13px;
  color: #64748b;
}

/* 窄屏时收起品牌区 */
@media (max-width: 768px) {
  .brand-side {
    display: none;
  }
}
</style>
