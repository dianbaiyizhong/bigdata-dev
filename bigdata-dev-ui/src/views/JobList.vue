<template>
  <div class="job-list">
    <h2 style="margin-bottom: 20px">Spark 任务列表</h2>
    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="jobName" label="任务名称" min-width="150" />
        <el-table-column prop="mainClass" label="主类" min-width="180" show-overflow-tooltip />
        <el-table-column prop="master" label="Master" width="100" />
        <el-table-column prop="deployMode" label="部署模式" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleKill(row)" :disabled="row.status !== 'RUNNING'">
              终止
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; text-align: right">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getJobList, killJob } from '../api/job'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'JobList',
  setup() {
    const loading = ref(false)
    const tableData = ref([])
    const page = ref(1)
    const size = ref(10)
    const total = ref(0)

    const loadData = async () => {
      loading.value = true
      try {
        const res = await getJobList(page.value, size.value)
        tableData.value = res.data.records || []
        total.value = res.data.total || 0
      } catch (e) {
        ElMessage.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const handleKill = async (row) => {
      try {
        await ElMessageBox.confirm('确定要终止该任务吗？', '确认操作', { type: 'warning' })
        await killJob(row.id)
        ElMessage.success('已终止')
        loadData()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('操作失败')
      }
    }

    const statusType = (status) => {
      const map = { RUNNING: 'success', FINISHED: 'info', FAILED: 'danger', KILLED: 'warning', SUBMITTING: '' }
      return map[status] || 'info'
    }

    const statusLabel = (status) => {
      const map = { SUBMITTING: '提交中', RUNNING: '运行中', FINISHED: '已完成', FAILED: '失败', KILLED: '已终止' }
      return map[status] || status
    }

    onMounted(loadData)

    return { loading, tableData, page, size, total, loadData, handleKill, statusType, statusLabel }
  }
}
</script>
