import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        name: 'welcome',
        component: () => import('../views/WelcomeView.vue'),
        children: [
            {
                path: '',
                name: 'welcome-login',
                component: () => import('../views/welcome/LoginPage.vue'),
            },
            {
                path: 'register',
                name: 'welcome-register',
                component: () => import('../views/welcome/RegisterPage.vue'),
            },
            {
                path: 'reset',
                name: 'welcome-reset',
                component: () => import('../views/welcome/ResetPage.vue'),
            }
        ]
    },
    {
        path: '/index',
        name: 'index',
        component: () => import('../views/IndexView.vue')
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

export default router