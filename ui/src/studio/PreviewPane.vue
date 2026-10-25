<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import RiComputerLine from '~icons/ri/computer-line'
import RiLoader4Line from '~icons/ri/loader-4-line'
import RiPlayLine from '~icons/ri/play-line'
import RiSmartphoneLine from '~icons/ri/smartphone-line'
import RiTabletLine from '~icons/ri/tablet-line'

import { PREVIEW_VIEWS, previewUrl, type PreviewDisplay, type PreviewState, type PreviewView } from '../preview/preview-url'
import { useFitScale } from '../preview/useFitScale'

const props = defineProps<{ draft?: string; template?: string; updating?: boolean; failed?: boolean }>()

const view = ref<PreviewView>('login')
const display = ref<PreviewDisplay>('page')
const state = ref<PreviewState>('default')
const device = ref<'desktop' | 'tablet' | 'mobile'>('desktop')
const revision = ref(0)
const frame = ref<HTMLIFrameElement>()
const stage = ref<HTMLElement>()
const modalHeight = ref(560)

const devices = [
  { id: 'desktop', label: '桌面', icon: RiComputerLine, width: 1440, height: 900 },
  { id: 'tablet', label: '平板', icon: RiTabletLine, width: 820, height: 1180 },
  { id: 'mobile', label: '手机', icon: RiSmartphoneLine, width: 390, height: 844 },
] as const

const canvas = computed(() => devices.find((item) => item.id === device.value)!)
// The mobile frame draws an 8px outline outside its box, so leave room for it.
const outline = computed(() => (device.value === 'mobile' ? 16 : 0))
const { scale } = useFitScale(
  stage,
  computed(() => canvas.value.width + outline.value),
  computed(() => canvas.value.height + outline.value),
)

const states = computed<{ value: PreviewState; label: string }[]>(() =>
  view.value === 'login'
    ? [
        { value: 'default', label: '默认' },
        { value: 'local', label: '密码登录' },
        { value: 'error', label: '密码错误' },
        { value: 'logout', label: '已退出' },
      ]
    : [],
)

const src = computed(() =>
  previewUrl(view.value, display.value, {
    draft: props.draft,
    template: props.template,
    state: states.value.length ? state.value : 'default',
    revision: revision.value,
  }),
)

// The page reports its content height; the modal preview grows with it.
function onMessage(event: MessageEvent) {
  if (event.origin !== location.origin || event.source !== frame.value?.contentWindow) return
  if (event.data?.type === 'auth-ui:resize' && Number.isFinite(event.data.height)) {
    modalHeight.value = Math.max(240, Math.min(900, event.data.height))
  }
}
onMounted(() => window.addEventListener('message', onMessage))
onBeforeUnmount(() => window.removeEventListener('message', onMessage))

/** Replays entrance animations without reloading the design. */
function replay() {
  revision.value++
}
defineExpose({ replay })
</script>

<template>
  <section class="st-preview" aria-label="实时预览">
    <div class="st-preview-bar">
      <div class="st-tabs" role="tablist" aria-label="预览页面">
        <button
          v-for="item in PREVIEW_VIEWS"
          :key="item.id"
          type="button"
          role="tab"
          :aria-selected="view === item.id"
          @click="view = item.id"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="st-preview-tools">
        <span class="st-preview-status" :data-state="failed ? 'error' : updating ? 'busy' : 'ok'" role="status">
          <RiLoader4Line v-if="updating" class="st-spin" width="14" height="14" aria-hidden="true" />
          {{ failed ? '预览更新失败' : updating ? '更新中' : '实时预览' }}
        </span>
        <div class="st-segment is-compact" role="group" aria-label="展示方式">
          <button type="button" :aria-pressed="display === 'page'" @click="display = 'page'">独立页</button>
          <button type="button" :aria-pressed="display === 'modal'" @click="display = 'modal'">弹窗</button>
        </div>
        <div v-if="display === 'page'" class="st-segment is-compact" role="group" aria-label="设备">
          <button
            v-for="item in devices"
            :key="item.id"
            type="button"
            :aria-pressed="device === item.id"
            :title="item.label"
            :aria-label="item.label"
            @click="device = item.id"
          >
            <component :is="item.icon" width="15" height="15" />
          </button>
        </div>
        <button type="button" class="st-icon-button" title="重新播放动画" aria-label="重新播放动画" @click="replay">
          <RiPlayLine width="16" height="16" />
        </button>
      </div>
    </div>

    <div v-if="states.length" class="st-preview-states" role="group" aria-label="页面状态">
      <span>状态</span>
      <button
        v-for="item in states"
        :key="item.value"
        type="button"
        :aria-pressed="state === item.value"
        @click="state = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <div ref="stage" class="st-stage" :data-display="display" :data-device="device">
      <div v-if="display === 'modal'" class="st-modal-site" aria-hidden="true"><i /><i /><i /><i /></div>
      <div v-if="display === 'modal'" class="st-frame" :style="{ height: `${modalHeight}px` }">
        <iframe ref="frame" :key="src" :src="src" title="登录界面预览" />
      </div>
      <!-- Pages render at the device's real size and are scaled down to fit the pane. -->
      <div
        v-else
        class="st-frame is-scaled"
        :style="{ width: `${canvas.width * scale}px`, height: `${canvas.height * scale}px` }"
      >
        <iframe
          ref="frame"
          :key="src"
          :src="src"
          title="登录界面预览"
          :style="{ width: `${canvas.width}px`, height: `${canvas.height}px`, transform: `scale(${scale})` }"
        />
      </div>
    </div>
  </section>
</template>
