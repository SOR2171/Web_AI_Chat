import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

declare module 'vue-router' {
    interface RouteMeta {
        title?: string;
    }
}

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
                meta: { title: 'Welcome - login' },
            },
            {
                path: 'register',
                name: 'welcome-register',
                component: () => import('../views/welcome/RegisterPage.vue'),
                meta: { title: 'Welcome - register' },
            },
            {
                path: 'reset',
                name: 'welcome-reset',
                component: () => import('../views/welcome/ResetPage.vue'),
                meta: { title: 'Welcome - reset' },
            }
        ]
    },
    {
        path: '/home',
        name: 'home',
        component: () => import('../views/HomeView.vue'),
        meta: { title: 'home' },
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

router.beforeEach((to, _from, next) => {
    const nearestTitle = to.meta.title;

    if (nearestTitle) {
        document.title = nearestTitle;
    } else {
        document.title = 'frontend - page';
    }
    next();
});

export default router