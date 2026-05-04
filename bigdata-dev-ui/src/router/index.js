import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/job/submit' },
  { path: '/job/submit', name: 'JobSubmit', component: () => import('../views/JobSubmit.vue') },
  { path: '/job/list', name: 'JobList', component: () => import('../views/JobList.vue') },
  { path: '/dependency', name: 'DependencyManage', component: () => import('../views/DependencyManage.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
