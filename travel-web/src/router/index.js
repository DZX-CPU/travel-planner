import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'spots', name: 'Spots', component: () => import('../views/Spots.vue') },
      { path: 'generate', name: 'Generate', component: () => import('../views/GeneratePlan.vue') },
      { path: 'my-plans', name: 'MyPlans', component: () => import('../views/MyPlans.vue') },
      { path: 'plan/:id', name: 'PlanDetail', component: () => import('../views/PlanDetail.vue') },
      { path: 'notes', name: 'Notes', component: () => import('../views/Notes.vue') },
      { path: 'note/:id', name: 'NoteDetail', component: () => import('../views/NoteDetail.vue') },
      { path: 'my-notes', name: 'MyNotes', component: () => import('../views/MyNotes.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue') },
      {
        path: 'admin',
        name: 'Admin',
        meta: { requiresAdmin: true },
        component: () => import('../views/Admin.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 没登录就跳登录页；管理页再确认一次角色
router.beforeEach(async (to) => {
  const token = localStorage.getItem('token')
  if (!token && to.name !== 'Login' && to.name !== 'Register') {
    return { name: 'Login' }
  }
  if (to.meta.requiresAdmin) {
    // 动态引入，避免 router -> store -> request -> router 的循环依赖
    const { useUserStore } = await import('../stores/user')
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      await userStore.fetchInfo()
    }
    if (userStore.userInfo?.role !== 1) {
      return { name: 'Home' }
    }
  }
})

export default router
