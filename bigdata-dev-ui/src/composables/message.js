import { inject } from 'vue'

export function useMessage() {
  const message = inject('$message', null)
  if (!message) {
    return {
      success: (msg) => console.log('[Success]', msg),
      error: (msg) => console.error('[Error]', msg),
      warning: (msg) => console.warn('[Warning]', msg),
      confirm: (msg) => Promise.resolve(window.confirm(msg))
    }
  }
  return message
}
