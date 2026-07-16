import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/teams',
    name: 'TeamSelect',
    component: () => import('../views/TeamSelect.vue'),
    meta: { requiresAuth: true }
  },

  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'projects/:id/kanban',
        name: 'KanbanBoard',
        component: () => import('../views/KanbanBoard.vue')
      },
      {
        path: 'standups',
        name: 'Standups',
        component: () => import('../views/Standups.vue')
      },
      {
        path: 'recycle-bin',
        name: 'RecycleBin',
        component: () => import('../views/RecycleBin.vue')
      },
      {
        path: 'logs',
        name: 'GlobalLogs',
        component: () => import('../views/GlobalLogs.vue')
      },
      {
        path: 'mall',
        name: 'Mall',
        component: () => import('../views/Mall.vue')
      },
      {
        path: 'gantt',
        name: 'GanttChart',
        component: () => import('../views/GanttChart.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // Clean up corrupted state (e.g. from other apps on localhost)
  if (authStore.token && !authStore.user) {
    authStore.logout()
  }

  if (to.meta.requiresAuth && (!authStore.token || !authStore.user)) {
    next({ name: 'Login' })
  } else if (to.name === 'Login' && authStore.token && authStore.user) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
