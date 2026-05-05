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
          :to="{ path: '/job/submit' }"
          :active="$route.path === '/job/submit'"
          color="primary"
        />
        <v-list-item
          prepend-icon="mdi-format-list-bulleted"
          title="任务列表"
          :to="{ path: '/job/list' }"
          :active="$route.path === '/job/list'"
          color="primary"
        />
        <v-list-item
          prepend-icon="mdi-package-variant-closed"
          title="依赖管理"
          :to="{ path: '/dependency' }"
          :active="$route.path === '/dependency'"
          color="primary"
        />
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-container fluid class="pa-6">
        <router-view />
      </v-container>
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
import { reactive, provide } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'App',
  setup() {
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

    return { snackbar, confirmDialog }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>
