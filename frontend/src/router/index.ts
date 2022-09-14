import { createRouter, createWebHistory, RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import { baseURL } from '../common/config';
import AdminSettings from '../components/AdminSettings.vue';
import CreateVault from '../components/CreateVault.vue';
import DeviceList from '../components/DeviceList.vue';
import LoginComponent from '../components/Login.vue';
import LogoutComponent from '../components/Logout.vue';
import MainComponent from '../components/Main.vue';
import NotFoundComponent from '../components/NotFoundComponent.vue';
import Settings from '../components/Settings.vue';
import UnlockError from '../components/UnlockError.vue';
import UnlockSuccess from '../components/UnlockSuccess.vue';
import VaultDetails from '../components/VaultDetails.vue';
import VaultList from '../components/VaultList.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/app'
  },
  {
    path: '/app',
    component: LoginComponent,
    meta: { skipAuth: true },
    beforeEnter: async () => {
      const auth = await authPromise;
      if (auth.isAuthenticated()) {
        return '/app/vaults';  //TODO:currently not working, since silent single sign-on is missing
      }
    }
  },
  {
    path: '/app/logout',
    component: LogoutComponent,
    meta: { skipAuth: true },
    beforeEnter: (to, from, next) => {
      authPromise.then(async auth => {
        if (auth.isAuthenticated()) {
          const loggedOutUri = `${location.origin}${router.resolve('/').href}`;
          await auth.logout(loggedOutUri);
        } else {
          next();
        }
      }).catch(error => {
        next(error);
      });
    }
  },
  {
    path: '/app', /* required but unused */
    component: MainComponent,
    children: [
      {
        path: 'vaults',
        component: VaultList
      },
      {
        path: 'vaults/create',
        component: CreateVault
      },
      {
        path: 'vaults/:id',
        component: VaultDetails,
        props: (route) => ({ vaultId: route.params.id })
      },
      {
        path: 'devices',
        component: DeviceList
      },
      {
        path: 'settings',
        component: Settings
      },
      {
        path: 'admin',
        component: AdminSettings,
        props: (route) => ({ token: route.query.token }),
        beforeEnter: async (_to, _from) => {
          const auth = await authPromise;
          return auth.isAdmin(); //TODO: reroute to NotFound Screen/ AccessDeniedScreen?
        }
      },
    ]
  },
  {
    path: '/app/unlock-success',
    component: UnlockSuccess,
    props: (route) => ({ vaultId: route.query.vault, deviceId: route.query.device })
  },
  {
    path: '/app/unlock-error',
    component: UnlockError,
    meta: { skipAuth: true }
  },
  {
    path: '/app/:pathMatch(.+)', //necessary due to using history mode in router
    component: NotFoundComponent,
    meta: { skipAuth: true },
    name: 'NotFound'
  },
];

const router = createRouter({
  history: createWebHistory(baseURL),
  routes: routes,
});

// FIRST check auth
router.beforeEach((to, from, next) => {
  if (to.meta.skipAuth) {
    next();
  } else {
    authPromise.then(async auth => {
      const redirect: RouteLocationRaw = { query: { sync_me: null } };
      const redirectUri = `${location.origin}${router.resolve(redirect, to).href}`;
      await auth.loginIfRequired(redirectUri);
      next();
    });
  }
});

// SECOND update user data (requires auth)
router.beforeEach((to, from, next) => {
  if ('sync_me' in to.query) {
    authPromise.then(async auth => {
      if (auth.isAuthenticated()) {
        await backend.users.syncMe();
      }
    }).finally(() => {
      delete to.query.sync_me; // remove sync_me query parameter to avoid endless recursion
      next({ path: to.path, query: to.query, params: to.params, replace: true });
    });
  } else {
    next();
  }
});

export default router;
