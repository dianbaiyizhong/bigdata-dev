<template>
  <v-app>
    <v-navigation-drawer
      permanent
      width="220"
      color="#304156"
      theme="dark"
    >
      <v-list-item
        class="pa-4"
        title="Spark 任务管理"
        subtitle="管理平台"
      />

      <v-divider />

      <v-list nav density="compact">
        <v-list-item
          prepend-icon="mdi-upload"
          title="提交任务"
          :active="activePath === '/job/submit'"
          color="primary"
          @click="switchTab('/job/submit')"
        />
        <v-list-item
          prepend-icon="mdi-format-list-bulleted"
          title="任务列表"
          :active="activePath === '/job/list'"
          color="primary"
          @click="switchTab('/job/list')"
        />
        <v-list-item
          prepend-icon="mdi-package-variant-closed"
          title="依赖管理"
          :active="activePath === '/dependency'"
          color="primary"
          @click="switchTab('/dependency')"
        />
        <v-list-item
          prepend-icon="mdi-language-python"
          title="PySpark包"
          :active="activePath === '/pyspark'"
          color="primary"
          @click="switchTab('/pyspark')"
        />
      </v-list>
    </v-navigation-drawer>

    <v-main style="display: flex; flex-direction: column; background: #f5f5f5;">
      <div class="tab-bar">
        <div class="tab-bar-inner">
          <div
            v-for="tab in tabs"
            :key="tab.path"
            class="tab-item"
            :class="{ active: activePath === tab.path }"
            @click="switchTab(tab.path)"
          >
            <v-icon size="16" class="mr-1">{{ tab.icon }}</v-icon>
            <span>{{ tab.title }}</span>
            <v-icon
              v-if="tabs.length > 1"
              size="16"
              class="ml-2 tab-close"
              @click.stop="closeTab(tab)"
              >mdi-close</v-icon>
          </div>
        </div>
      </div>

      <div style="flex: 1; padding: 24px; overflow-y: auto;">
        <JobSubmit v-show="activePath === '/job/submit'" />
        <JobList v-show="activePath === '/job/list'" />
        <DependencyManage v-show="activePath === '/dependency'" />
        <PySparkManage v-show="activePath === '/pyspark'" />
      </div>
    </v-main>

    <v-snackbar
      v-model="snackbar.show"
      :color="snackbar.color"
      :timeout="3000"
      location="top"
    >
      {{ snackbar.text }}
      <template #actions>
        <v-btn variant="text" @click="snackbar.show = false">关闭</v-btn>
      </template>
    </v-snackbar>

    <v-dialog v-model="confirmDialog.show" max-width="400">
      <v-card>
        <v-card-title class="text-h6">确认操作</v-card-title>
        <v-card-text>{{ confirmDialog.text }}</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="confirmDialog.show = false; confirmDialog.reject('cancel')">取消</v-btn>
          <v-btn color="primary" variant="text" @click="confirmDialog.show = false; confirmDialog.resolve(true)">确定</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script>
import { reactive, provide, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import JobSubmit from './views/JobSubmit.vue'
import JobList from './views/JobList.vue'
import DependencyManage from './views/DependencyManage.vue'
import PySparkManage from './views/PySparkManage.vue'

const routeMeta = {
  '/job/submit': { title: '提交任务', icon: 'mdi-upload' },
  '/job/list': { title: '任务列表', icon: 'mdi-format-list-bulleted' },
  '/dependency': { title: '依赖管理', icon: 'mdi-package-variant-closed' },
  '/pyspark': { title: 'PySpark包', icon: 'mdi-language-python' }
}

export default {
  name: 'App',
  components: { JobSubmit, JobList, DependencyManage, PySparkManage },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const tabs = ref([])
    const activePath = ref(route.path === '/' ? '/job/submit' : route.path)

    const switchTab = (path) => {
      activePath.value = path
      router.replace(path)
      if (!tabs.value.find(t => t.path === path)) {
        tabs.value.push({ path, ...routeMeta[path] })
      }
    }

    const closeTab = (tab) => {
      const idx = tabs.value.findIndex(t => t.path === tab.path)
      tabs.value.splice(idx, 1)
      if (activePath.value === tab.path) {
        const next = tabs.value[idx] || tabs.value[tabs.value.length - 1]
        if (next) {
          activePath.value = next.path
          router.replace(next.path)
        }
      }
    }

    watch(() => route.path, (path) => {
      if (path !== '/') {
        activePath.value = path
        if (!tabs.value.find(t => t.path === path)) {
          tabs.value.push({ path, ...routeMeta[path] })
        }
      }
    })

    // init first tab
    tabs.value = [{ path: '/job/submit', ...routeMeta['/job/submit'] }]

    const snackbar = reactive({
      show: false,
      text: '',
      color: 'success'
    })

    const confirmDialog = reactive({
      show: false,
      text: '',
      resolve: null,
      reject: null
    })

    const showMessage = (text, color = 'success') => {
      snackbar.text = text
      snackbar.color = color
      snackbar.show = true
    }

    const confirm = (text) => {
      return new Promise((resolve, reject) => {
        confirmDialog.text = text
        confirmDialog.resolve = resolve
        confirmDialog.reject = reject
        confirmDialog.show = true
      })
    }

    provide('$message', {
      success: (msg) => showMessage(msg, 'success'),
      error: (msg) => showMessage(msg, 'error'),
      warning: (msg) => showMessage(msg, 'warning'),
      confirm
    })

    return { snackbar, confirmDialog, tabs, activePath, switchTab, closeTab }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.tab-bar {
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  overflow-x: auto;
  white-space: nowrap;
  flex-shrink: 0;
}

.tab-bar-inner {
  display: inline-flex;
  padding: 0 4px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-right: 1px solid #e0e0e0;
  user-select: none;
  transition: background 0.15s;
  position: relative;
}

.tab-item:hover {
  background: #e8e8e8;
}

.tab-item.active {
  background: #f5f5f5;
  color: #1976D2;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #1976D2;
}

.tab-close {
  opacity: 0.4;
  transition: opacity 0.15s, color 0.15s;
}

.tab-close:hover {
  opacity: 1;
  color: #e53935;
}
</style>
