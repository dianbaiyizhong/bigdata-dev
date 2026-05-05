<template>
  <div class="pyspark-manage">
    <h2 class="mb-5">PySpark 依赖包管理</h2>

    <v-card class="mb-5">
      <v-card-text>
        <h3 class="mb-4">上传 PySpark 依赖包</h3>
        <v-row align="center">
          <v-col cols="auto" style="flex: 1; max-width: 500px">
            <v-file-input
              v-model="uploadFile"
              label="选择 .zip 依赖包"
              accept=".zip"
              prepend-icon="mdi-folder-zip"
              variant="outlined"
              density="comfortable"
              hide-details
            />
          </v-col>
          <v-col cols="auto">
            <v-btn
              color="primary"
              :loading="uploading"
              :disabled="!uploadFile"
              @click="handleUpload"
            >
              上传到 HDFS
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-text>
        <div class="d-flex align-center mb-4">
          <h3>依赖包列表</h3>
          <v-spacer />
          <v-btn
            icon="mdi-refresh"
            size="small"
            variant="text"
            :loading="loading"
            @click="loadList"
          />
        </div>

        <v-data-table
          :items="tableData"
          :headers="headers"
          :loading="loading"
          hover
          density="comfortable"
          class="elevation-0"
          hide-default-footer
          no-data-text="暂无依赖包，请上传"
        >
          <template #item.name="{ item }">
            <v-tooltip location="top" :text="item.name">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.name }}</span>
              </template>
            </v-tooltip>
          </template>
          <template #item.hdfsPath="{ item }">
            <v-tooltip location="top" :text="item.hdfsPath">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.hdfsPath }}</span>
              </template>
            </v-tooltip>
          </template>
          <template #item.fileSize="{ item }">
            {{ formatSize(item.fileSize) }}
          </template>
          <template #item.createTime="{ item }">
            {{ formatTime(item.createTime) }}
          </template>
          <template #item.updateTime="{ item }">
            {{ formatTime(item.updateTime) }}
          </template>
          <template #item.actions="{ item }">
            <v-btn
              color="error"
              size="small"
              variant="tonal"
              @click="handleDelete(item)"
            >
              删除
            </v-btn>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { uploadPySparkZip, getPySparkZipList, deletePySparkZip } from '../api/pyspark'
import { useMessage } from '../composables/message'

export default {
  name: 'PySparkManage',
  setup() {
    const uploadFile = ref(null)
    const uploading = ref(false)
    const loading = ref(false)
    const tableData = ref([])
    const message = useMessage()

    const headers = [
      { title: '文件名', key: 'name' },
      { title: 'HDFS路径', key: 'hdfsPath' },
      { title: '大小', key: 'fileSize', width: 100 },
      { title: '创建时间', key: 'createTime', width: 160 },
      { title: '更新时间', key: 'updateTime', width: 160 },
      { title: '操作', key: 'actions', width: 80 }
    ]

    const loadList = async () => {
      loading.value = true
      try {
        const res = await getPySparkZipList()
        tableData.value = res.data || []
      } catch (e) {
        message.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const handleUpload = async () => {
      if (!uploadFile.value) return
      uploading.value = true
      try {
        const fd = new FormData()
        fd.append('file', uploadFile.value)
        await uploadPySparkZip(fd)
        message.success('上传成功，已存储到 HDFS')
        uploadFile.value = null
        loadList()
      } catch (e) {
        message.error('上传失败: ' + (e.message || ''))
      } finally {
        uploading.value = false
      }
    }

    const handleDelete = async (row) => {
      try {
        await message.confirm('确定要删除该依赖包吗？将从 HDFS 和数据库中同时删除')
        await deletePySparkZip(row.id)
        message.success('已删除')
        loadList()
      } catch (e) {
        if (e !== 'cancel') message.error('删除失败')
      }
    }

    const formatSize = (bytes) => {
      if (!bytes) return '-'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }

    const formatTime = (t) => {
      if (!t) return '-'
      return t.replace('T', ' ')
    }

    onMounted(loadList)

    return {
      uploadFile, uploading, loading, tableData, headers,
      loadList, handleUpload, handleDelete, formatSize, formatTime
    }
  }
}
</script>
