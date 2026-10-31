<template>
  <el-container style="height: 100vh">
    <el-header class="header">
      <div class="logo" @click="$router.push('/home')">智游<span>助手</span></div>
      <el-menu mode="horizontal" :default-active="activeMenu" router :ellipsis="false" class="menu">
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/spots">找景点</el-menu-item>
        <el-menu-item index="/generate">AI 行程定制</el-menu-item>
        <el-menu-item index="/my-plans">我的行程</el-menu-item>
        <el-menu-item index="/notes">游记</el-menu-item>
        <el-menu-item v-if="userStore.userInfo?.role === 1" index="/admin">后台管理</el-menu-item>
      </el-menu>
      <el-dropdown v-if="userStore.userInfo" @command="onCommand">
        <span class="user-info">
          <el-avatar :size="32" style="background: #409eff">{{ userStore.userInfo.nickname?.[0] || 'U' }}</el-avatar>
          <span style="margin-left: 8px">{{ userStore.userInfo.nickname }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="my-notes">我的游记</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-header>
    <el-main style="padding: 0; background: #f5f7fa">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/plan/')) return '/my-plans'
  return route.path
})

onMounted(() => {
  // 刷新页面时拉一下用户信息
  if (!userStore.userInfo) {
    userStore.fetchInfo()
  }
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'my-notes') {
    router.push('/my-notes')
  }
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #f1f5f9;
  padding: 0 32px;
  height: 60px;
}
.logo {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-right: 48px;
  cursor: pointer;
  white-space: nowrap;
}
.logo span {
  color: #0ea5e9;
}
.menu {
  flex: 1;
  border-bottom: none;
}
.menu :deep(.el-menu-item) {
  font-weight: 500;
  border-bottom-width: 2.5px;
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-info:hover {
  background: #f1f5f9;
}
</style>
