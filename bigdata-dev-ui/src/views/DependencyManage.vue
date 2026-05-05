<template>
  <div class="dependency-manage">
    <h2 class="mb-5">依赖管理</h2>

    <v-card class="mb-5">
      <v-card-text>
        <h3 class="mb-4">新建依赖组</h3>
        <v-row align="center">
          <v-col cols="auto">
            <v-text-field
              v-model="newDepForm.name"
              label="名称"
              placeholder="例如: MySQL Connector"
              variant="outlined"
              density="comfortable"
              hide-details
              style="width: 240px"
            />
          </v-col>
          <v-col cols="auto">
            <v-text-field
              v-model="newDepForm.description"
              label="描述"
              placeholder="描述（可选）"
              variant="outlined"
              density="comfortable"
              hide-details
              style="width: 300px"
            />
          </v-col>
          <v-col cols="auto">
            <v-btn color="primary" :loading="creating" @click="createDep">创建依赖组</v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-text>
        <v-row align="center" class="mb-4">
          <v-col>
            <h3>依赖组列表</h3>
          </v-col>
          <v-col cols="auto">
            <v-text-field
              v-model="keyword"
              label="搜索"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              style="width: 260px"
              @click:clear="loadList"
              @keyup.enter="loadList"
            >
              <template #append-inner>
                <v-btn icon size="small" variant="text" @click="loadList">
                  <v-icon>mdi-magnify</v-icon>
                </v-btn>
              </template>
            </v-text-field>
          </v-col>
        </v-row>

        <v-data-table
          :items="tableData"
          :headers="depHeaders"
          :loading="loading"
          hover
          density="comfortable"
          class="elevation-0"
          hide-default-footer
        >
          <template #item._jarCount="{ item }">
            <v-chip size="small" label>{{ item._jarCount || 0 }}</v-chip>
          </template>
          <template #item.description="{ item }">
            <span class="text-truncate d-inline-block" style="max-width: 200px">{{ item.description || '-' }}</span>
          </template>
          <template #item.actions="{ item }">
            <v-btn color="primary" size="small" variant="tonal" class="mr-2" @click="openJarDialog(item)">JAR管理</v-btn>
            <v-btn color="error" size="small" variant="tonal" @click="handleDelete(item)">删除</v-btn>
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
              @update:model-value="loadList"
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
              @update:model-value="loadList"
            />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- JAR管理弹窗 -->
    <v-dialog v-model="jarDialogVisible" max-width="700" @update:model-value="!jarDialogVisible && closeJarDialog()">
      <v-card>
        <v-card-title class="text-h6">
          {{ currentDep?.name }} - JAR管理
        </v-card-title>
        <v-card-text>
          <v-file-input
            v-model="pendingFiles"
            label="拖拽 JAR 文件到此处或点击上传"
            accept=".jar"
            multiple
            prepend-icon="mdi-upload"
            variant="outlined"
            density="comfortable"
            class="mb-3"
          />
          <div class="text-right">
            <v-btn
              color="primary"
              :loading="jarUploading"
              :disabled="!pendingFiles || pendingFiles.length === 0"
              @click="uploadJarsToDep"
            >
              确认上传 ({{ pendingFiles?.length || 0 }} 个文件)
            </v-btn>
          </div>

          <v-divider class="my-4" />

          <v-data-table
            :items="currentJars"
            :headers="jarHeaders"
            :loading="jarsLoading"
            density="compact"
            hover
            class="elevation-0"
            no-data-text="暂无JAR文件"
          >
            <template #item.name="{ item }">
              <span class="text-truncate d-inline-block" style="max-width: 280px">{{ item.name }}</span>
            </template>
            <template #item.fileSize="{ item }">
              {{ item.fileSize ? formatSize(item.fileSize) : '-' }}
            </template>
            <template #item.actions="{ item }">
              <v-btn color="error" size="small" variant="tonal" @click="deleteJar(item.id)">删除</v-btn>
            </template>
          </v-data-table>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="jarDialogVisible = false">关闭</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import {
  createDependency, getDependencyList, deleteDependency,
  uploadDependencyJars, getDependencyJars, deleteDependencyJar
} from '../api/dependency'
import { useMessage } from '../composables/message'

export default {
  name: 'DependencyManage',
  setup() {
    const creating = ref(false)
    const newDepForm = reactive({ name: '', description: '' })
    const message = useMessage()

    const loading = ref(false)
    const tableData = ref([])
    const page = ref(1)
    const size = ref(10)
    const total = ref(0)
    const keyword = ref('')

    const jarDialogVisible = ref(false)
    const currentDep = ref(null)
    const jarUploading = ref(false)
    const jarsLoading = ref(false)
    const currentJars = ref([])
    const pendingFiles = ref([])

    const depHeaders = [
      { title: 'ID', key: 'id', width: 60 },
      { title: '名称', key: 'name', minWidth: 200 },
      { title: '描述', key: 'description', minWidth: 200 },
      { title: 'JAR数量', key: '_jarCount', width: 100 },
      { title: '创建时间', key: 'createTime', width: 170 },
      { title: '操作', key: 'actions', width: 160 }
    ]

    const jarHeaders = [
      { title: '文件名', key: 'name' },
      { title: '大小', key: 'fileSize', width: 120 },
      { title: '上传时间', key: 'createTime', width: 170 },
      { title: '操作', key: 'actions', width: 80 }
    ]

    const createDep = async () => {
      if (!newDepForm.name) { message.warning('请输入依赖组名称'); return }
      creating.value = true
      try {
        await createDependency(newDepForm.name, newDepForm.description)
        message.success('创建成功')
        newDepForm.name = ''
        newDepForm.description = ''
        loadList()
      } catch (e) {
        message.error('创建失败')
      } finally {
        creating.value = false
      }
    }

    const openJarDialog = async (row) => {
      currentDep.value = row
      pendingFiles.value = []
      jarDialogVisible.value = true
      await loadJars()
    }

    const closeJarDialog = () => {
      currentDep.value = null
      pendingFiles.value = []
    }

    const loadJars = async () => {
      if (!currentDep.value) return
      jarsLoading.value = true
      try {
        const res = await getDependencyJars(currentDep.value.id)
        currentJars.value = res.data || []
        currentDep.value._jarCount = currentJars.value.length
      } catch (e) {
        currentJars.value = []
      } finally {
        jarsLoading.value = false
      }
    }

    const uploadJarsToDep = async () => {
      if (!currentDep.value || !pendingFiles.value || pendingFiles.value.length === 0) return
      jarUploading.value = true
      try {
        const fd = new FormData()
        pendingFiles.value.forEach(f => fd.append('files', f))
        await uploadDependencyJars(currentDep.value.id, fd)
        message.success(`成功上传 ${pendingFiles.value.length} 个文件`)
        pendingFiles.value = []
        await loadJars()
      } catch (e) {
        message.error('上传失败')
      } finally {
        jarUploading.value = false
      }
    }

    const deleteJar = async (jarId) => {
      try {
        await message.confirm('确定要删除该JAR吗？')
        await deleteDependencyJar(jarId)
        message.success('已删除')
        await loadJars()
      } catch (e) {
        if (e !== 'cancel') message.error('删除失败')
      }
    }

    const loadList = async () => {
      loading.value = true
      try {
        const res = await getDependencyList(page.value, size.value, keyword.value)
        tableData.value = (res.data.records || []).map(d => ({
          ...d,
          _jarCount: 0
        }))
        total.value = res.data.total || 0
        for (const dep of tableData.value) {
          try {
            const jRes = await getDependencyJars(dep.id)
            dep._jarCount = (jRes.data || []).length
          } catch (e) {
            dep._jarCount = 0
          }
        }
      } catch (e) {
        message.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const handleDelete = async (row) => {
      try {
        await message.confirm('确定要删除该依赖组及其所有JAR吗？')
        await deleteDependency(row.id)
        message.success('已删除')
        loadList()
      } catch (e) {
        if (e !== 'cancel') message.error('删除失败')
      }
    }

    const formatSize = (bytes) => {
      if (!bytes) return '-'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }

    onMounted(loadList)

    return {
      creating, newDepForm, createDep,
      loading, tableData, depHeaders, page, size, total, keyword, loadList, handleDelete,
      jarDialogVisible, currentDep, jarUploading, jarsLoading,
      currentJars, pendingFiles, jarHeaders,
      openJarDialog, closeJarDialog, uploadJarsToDep, deleteJar, formatSize
    }
  }
}
</script>
