import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * Scale that fits a fixed-size canvas into a container, keeping its proportions.
 * Used to render a page at a real desktop width and shrink it into a smaller frame.
 */
export function useFitScale(
  container: Ref<HTMLElement | undefined>,
  width: Ref<number>,
  height: Ref<number> = ref(Number.POSITIVE_INFINITY),
) {
  const scale = ref(1)
  let observer: ResizeObserver | undefined

  function update() {
    const box = contentBox(container.value)
    if (!box || !box.width) return
    const byWidth = box.width / width.value
    const byHeight = Number.isFinite(height.value) && box.height ? box.height / height.value : byWidth
    scale.value = Math.min(1, byWidth, byHeight)
  }

  onMounted(() => {
    update()
    if (typeof ResizeObserver === 'function' && container.value) {
      observer = new ResizeObserver(update)
      observer.observe(container.value)
    }
  })
  onBeforeUnmount(() => observer?.disconnect())

  return { scale, update }
}

/** Size available to children: the element's box minus its padding. */
function contentBox(element: HTMLElement | undefined) {
  if (!element) return undefined
  const box = element.getBoundingClientRect()
  const style = getComputedStyle(element)
  const px = (value: string) => Number.parseFloat(value) || 0
  return {
    width: box.width - px(style.paddingLeft) - px(style.paddingRight),
    height: box.height - px(style.paddingTop) - px(style.paddingBottom),
  }
}
