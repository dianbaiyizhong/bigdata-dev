<template>
  <div class="job-list">
    <div class="d-flex align-center mb-5">
      <h2 class="me-3">Spark 任务列表</h2>
      <v-btn
        icon="mdi-refresh"
        size="small"
        variant="text"
        :loading="loading"
        @click="loadData"
      />
    </div>
    <v-card>
      <v-card-text>
        <v-data-table
          :items="tableData"
          :headers="headers"
          :loading="loading"
          hover
          density="comfortable"
          class="elevation-0"
          hide-default-footer
        >
          <template #item.status="{ item }">
            <v-chip
              :color="statusColor(item.status)"
              size="small"
              label
            >
              {{ statusLabel(item.status) }}
            </v-chip>
          </template>
          <template #item.appId="{ item }">
            <v-tooltip location="top" :text="item.appId || '-'">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.appId || '-' }}</span>
              </template>
            </v-tooltip>
          </template>
          <template #item.jobName="{ item }">
            <v-tooltip location="top">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.jobName }}</span>
              </template>
              <span>{{ item.jobName }}</span>
            </v-tooltip>
          </template>
          <template #item.mainClass="{ item }">
            <v-tooltip location="top">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.mainClass }}</span>
              </template>
              <span>{{ item.mainClass }}</span>
            </v-tooltip>
          </template>
          <template #item.master="{ item }">
            <v-tooltip location="top">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.master }}</span>
              </template>
              <span>{{ item.master }}</span>
            </v-tooltip>
          </template>
          <template #item.deployMode="{ item }">
            <v-tooltip location="top">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.deployMode }}</span>
              </template>
              <span>{{ item.deployMode }}</span>
            </v-tooltip>
          </template>
          <template #item.errorMsg="{ item }">
            <v-tooltip location="top">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.errorMsg || '-' }}</span>
              </template>
              <span>{{ item.errorMsg || '-' }}</span>
            </v-tooltip>
          </template>
          <template #item.createTime="{ item }">
            {{ formatTime(item.createTime) }}
          </template>
          <template #item.actions="{ item }">
            <v-btn
              color="error"
              size="small"
              variant="tonal"
              :disabled="item.status !== 'RUNNING'"
              @click="handleKill(item)"
            >
              终止
            </v-btn>
          </template>
        </v-data-table>

        <v-row class="mt-4" justify="end" align="center">
          <v-col cols="auto">
            <v-select
              v-model="size"
              :items="[10, 20, 50]"
              label="每页条数"
              density="compact"
              variant="outlined"
              hide-details
              style="width: 120px"
              @update:model-value="loadData"
            />
          </v-col>
          <v-col cols="auto">
            <span class="text-body-2 text-medium-emphasis mr-3">共 {{ total }} 条</span>
          </v-col>
          <v-col cols="auto">
            <v-pagination
              v-model="page"
              :length="Math.ceil(total / size) || 1"
              :total-visible="5"
              density="comfortable"
              @update:model-value="loadData"
            />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { getJobList, killJob } from '../api/job'
import { useMessage } from '../composables/message'

export default {
  name: 'JobList',
  setup() {
    const loading = ref(false)
    const tableData = ref([])
    const page = ref(1)
    const size = ref(10)
    const total = ref(0)
    const message = useMessage()

    const headers = [
      { title: 'ID', key: 'id', width: 60 },
      { title: 'AppId', key: 'appId', minWidth: 160 },
      { title: '任务名称', key: 'jobName', minWidth: 120 },
      { title: '主类', key: 'mainClass', minWidth: 150 },
      { title: 'Master', key: 'master', minWidth: 100 },
      { title: '部署模式', key: 'deployMode', minWidth: 90 },
      { title: '状态', key: 'status', minWidth: 80 },
      { title: '错误信息', key: 'errorMsg', minWidth: 120 },
      { title: '创建时间', key: 'createTime', minWidth: 150 },
      { title: '操作', key: 'actions', minWidth: 80 }
    ]

    const loadData = async () => {
      loading.value = true
      try {
        const res = await getJobList(page.value, size.value)
        tableData.value = res.data.records || []
        total.value = res.data.total || 0
      } catch (e) {
        message.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const handleKill = async (row) => {
      try {
        await message.confirm('确定要终止该任务吗？')
        await killJob(row.id)
        message.success('已终止')
        loadData()
      } catch (e) {
        if (e !== 'cancel') message.error('操作失败')
      }
    }

    const statusColor = (status) => {
      const map = { RUNNING: 'success', FINISHED: 'info', FAILED: 'error', KILLED: 'warning', SUBMITTING: 'default' }
      return map[status] || 'info'
    }

    const statusLabel = (status) => {
      const map = { SUBMITTING: '提交中', RUNNING: '运行中', FINISHED: '已完成', FAILED: '失败', KILLED: '已终止' }
      return map[status] || status
    }

    const formatTime = (t) => {
      if (!t) return '-'
      return t.replace('T', ' ')
    }

    onMounted(loadData)

    return { loading, tableData, headers, page, size, total, loadData, handleKill, statusColor, statusLabel, formatTime }
  }
}
</script>
