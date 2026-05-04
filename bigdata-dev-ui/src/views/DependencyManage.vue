<template>
  <div class="dependency-manage">
    <h2 style="margin-bottom: 20px">依赖管理</h2>

    <el-card style="margin-bottom: 20px">
      <h3 style="margin-bottom: 16px">新建依赖组</h3>
      <el-form :inline="true" :model="newDepForm">
        <el-form-item label="名称" required>
          <el-input v-model="newDepForm.name" placeholder="例如: MySQL Connector" style="width: 240px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newDepForm.description" placeholder="描述（可选）" style="width: 300px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createDep" :loading="creating">创建依赖组</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
        <h3>依赖组列表</h3>
        <el-input v-model="keyword" placeholder="搜索" style="width: 260px" clearable @clear="loadList" @keyup.enter="loadList">
          <template #append>
            <el-button @click="loadList"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe row-key="id">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="JAR数量" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row._jarCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openJarDialog(row)">JAR管理</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- JAR管理弹窗 -->
    <el-dialog v-model="jarDialogVisible" :title="`${currentDep?.name} - JAR管理`" width="700px" destroy-on-close>
      <div style="margin-bottom: 16px">
        <el-upload
          ref="jarUploadRef"
          :auto-upload="false"
          multiple
          :on-change="handleJarChange"
          :on-remove="handleJarRemove"
          accept=".jar"
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽 JAR 文件到此处或 <em>点击上传</em></div>
        </el-upload>
        <div style="margin-top: 12px; text-align: right">
          <el-button type="primary" @click="uploadJarsToDep" :loading="jarUploading" :disabled="pendingFiles.length === 0">
            确认上传 ({{ pendingFiles.length }} 个文件)
          </el-button>
        </div>
      </div>
      <el-divider />
      <el-table :data="currentJars" v-loading="jarsLoading" size="small" stripe empty-text="暂无JAR文件">
        <el-table-column prop="name" label="文件名" show-overflow-tooltip />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ row.fileSize ? formatSize(row.fileSize) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteJar(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import {
  createDependency, getDependencyList, deleteDependency,
  uploadDependencyJars, getDependencyJars, deleteDependencyJar
} from '../api/dependency'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'DependencyManage',
  setup() {
    const creating = ref(false)
    const newDepForm = reactive({ name: '', description: '' })

    const loading = ref(false)
    const tableData = ref([])
    const page = ref(1)
    const size = ref(10)
    const total = ref(0)
    const keyword = ref('')

    // JAR弹窗
    const jarDialogVisible = ref(false)
    const currentDep = ref(null)
    const jarUploadRef = ref(null)
    const jarUploading = ref(false)
    const jarsLoading = ref(false)
    const currentJars = ref([])
    const pendingFiles = ref([])

    const createDep = async () => {
      if (!newDepForm.name) { ElMessage.warning('请输入依赖组名称'); return }
      creating.value = true
      try {
        await createDependency(newDepForm.name, newDepForm.description)
        ElMessage.success('创建成功')
        newDepForm.name = ''
        newDepForm.description = ''
        loadList()
      } catch (e) {
        ElMessage.error('创建失败')
      } finally {
        creating.value = false
      }
    }

    const openJarDialog = async (row) => {
      currentDep.value = row
      pendingFiles.value = []
      jarUploadRef.value?.clearFiles()
      jarDialogVisible.value = true
      await loadJars()
    }

    const closeJarDialog = () => {
      jarDialogVisible.value = false
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

    const handleJarChange = (file) => {
      pendingFiles.value.push(file.raw)
    }

    const handleJarRemove = (file) => {
      pendingFiles.value = pendingFiles.value.filter(f => f !== file.raw)
    }

    const uploadJarsToDep = async () => {
      if (!currentDep.value || pendingFiles.value.length === 0) return
      jarUploading.value = true
      try {
        const fd = new FormData()
        pendingFiles.value.forEach(f => fd.append('files', f))
        await uploadDependencyJars(currentDep.value.id, fd)
        ElMessage.success(`成功上传 ${pendingFiles.value.length} 个文件`)
        pendingFiles.value = []
        jarUploadRef.value?.clearFiles()
        await loadJars()
      } catch (e) {
        ElMessage.error('上传失败')
      } finally {
        jarUploading.value = false
      }
    }

    const deleteJar = async (jarId) => {
      try {
        await ElMessageBox.confirm('确定要删除该JAR吗？', '确认', { type: 'warning' })
        await deleteDependencyJar(jarId)
        ElMessage.success('已删除')
        await loadJars()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
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
        // 加载每个依赖组的jar数量
        for (const dep of tableData.value) {
          try {
            const jRes = await getDependencyJars(dep.id)
            dep._jarCount = (jRes.data || []).length
          } catch (e) {
            dep._jarCount = 0
          }
        }
      } catch (e) {
        ElMessage.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除该依赖组及其所有JAR吗？', '确认操作', { type: 'warning' })
        await deleteDependency(row.id)
        ElMessage.success('已删除')
        loadList()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
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
      loading, tableData, page, size, total, keyword, loadList, handleDelete,
      jarDialogVisible, currentDep, jarUploadRef, jarUploading, jarsLoading,
      currentJars, pendingFiles,
      openJarDialog, closeJarDialog, handleJarChange, handleJarRemove,
      uploadJarsToDep, deleteJar, formatSize
    }
  }
}
</script>
