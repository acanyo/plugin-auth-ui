import { definePlugin } from '@halo-dev/ui-shared'
import { markRaw } from 'vue'
import RiShieldUserLine from '~icons/ri/shield-user-line'

import { ROUTE_NAME, STUDIO_ROUTE_NAME } from './workbench/navigation'

const permissions = ['system:plugins:manage']

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/auth-ui',
        name: ROUTE_NAME,
        component: () => import('./workbench/WorkbenchView.vue'),
        meta: {
          title: '认证界面',
          searchable: true,
          permissions,
          menu: {
            name: '认证界面',
            group: 'interface',
            icon: markRaw(RiShieldUserLine),
            priority: 10,
          },
        },
      },
    },
    {
      parentName: 'Root',
      route: {
        path: '/auth-ui/designs/:name',
        name: STUDIO_ROUTE_NAME,
        component: () => import('./studio/DesignStudio.vue'),
        meta: { title: '登录设计编辑器', permissions, hideFooter: true },
      },
    },
  ],
  extensionPoints: {},
})
