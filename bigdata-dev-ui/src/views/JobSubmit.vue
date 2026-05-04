<template>
  <div class="job-submit">
    <h2 style="margin-bottom: 20px">提交 Spark 任务</h2>
    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" size="default">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="上传JAR" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".jar"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 JAR 文件到此处或 <em>点击上传</em></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="主类" prop="mainClass">
          <el-input v-model="form.mainClass" placeholder="com.example.MainClass" />
        </el-form-item>
        <el-form-item label="应用参数">
          <el-input v-model="form.appArgs" type="textarea" :rows="2" placeholder="程序参数，空格分隔" />
        </el-form-item>
        <el-divider content-position="left">资源配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Driver内存(MB)">
              <el-input-number v-model="form.driverMemory" :min="256" :step="256" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Driver核数">
              <el-input-number v-model="form.driverCores" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Executor数量">
              <el-input-number v-model="form.numExecutors" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Executor内存(MB)">
              <el-input-number v-model="form.executorMemory" :min="256" :step="256" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Executor核数">
              <el-input-number v-model="form.executorCores" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">Spark 配置</el-divider>
        <el-form-item label="自定义配置">
          <el-input v-model="form.sparkProperties" type="textarea" :rows="4"
            placeholder="spark.sql.shuffle.partitions=200&#10;spark.default.parallelism=100" />
        </el-form-item>
        <el-divider content-position="left">依赖组</el-divider>
        <el-form-item label="选择依赖组">
          <el-select v-model="form.dependencyIds" multiple filterable placeholder="选择依赖组（该组所有JAR加入classpath）" style="width: 100%">
            <el-option v-for="item in depList" :key="item.id"
              :label="`${item.name} (${item._jarCount || 0} 个JAR)`" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交任务</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { submitJob } from '../api/job'
import { getDependencyList, getDependencyJars } from '../api/dependency'
import { ElMessage } from 'element-plus'

export default {
  name: 'JobSubmit',
  setup() {
    const formRef = ref(null)
    const uploadRef = ref(null)
    const submitting = ref(false)
    const depList = ref([])
    const selectedFile = ref(null)

    const form = reactive({
      jobName: '',
      mainClass: '',
      appArgs: '',
      driverMemory: 1024,
      driverCores: 1,
      executorMemory: 1024,
      executorCores: 1,
      numExecutors: 2,
      sparkProperties: '',
      dependencyIds: []
    })

    const rules = {
      jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
      mainClass: [{ required: true, message: '请输入主类名', trigger: 'blur' }]
    }

    const handleFileChange = (file) => {
      selectedFile.value = file.raw
    }
    const handleFileRemove = () => {
      selectedFile.value = null
    }

    const loadDeps = async () => {
      try {
        const res = await getDependencyList(1, 500, '')
        depList.value = res.data.records || []
        for (const dep of depList.value) {
          try {
            const jRes = await getDependencyJars(dep.id)
            dep._jarCount = (jRes.data || []).length
          } catch (e) { dep._jarCount = 0 }
        }
      } catch (e) { console.error('加载依赖失败', e) }
    }

    const handleSubmit = async () => {
      if (!selectedFile.value) {
        ElMessage.warning('请上传JAR文件')
        return
      }
      const valid = await formRef.value.validate().catch(() => false)
      if (!valid) return

      submitting.value = true
      try {
        const fd = new FormData()
        fd.append('file', selectedFile.value)
        fd.append('jobName', form.jobName)
        fd.append('mainClass', form.mainClass)
        if (form.appArgs) fd.append('appArgs', form.appArgs)
        if (form.driverMemory) fd.append('driverMemory', form.driverMemory)
        if (form.driverCores) fd.append('driverCores', form.driverCores)
        if (form.executorMemory) fd.append('executorMemory', form.executorMemory)
        if (form.executorCores) fd.append('executorCores', form.executorCores)
        if (form.numExecutors) fd.append('numExecutors', form.numExecutors)
        if (form.sparkProperties) fd.append('sparkProperties', form.sparkProperties)
        if (form.dependencyIds.length > 0) fd.append('dependencyIds', form.dependencyIds.join(','))

        await submitJob(fd)
        ElMessage.success('任务提交成功')
        selectedFile.value = null
        uploadRef.value?.clearFiles()
      } catch (e) {
        ElMessage.error('提交失败: ' + (e.message || '未知错误'))
      } finally {
        submitting.value = false
      }
    }

    const resetForm = () => {
      formRef.value?.resetFields()
      selectedFile.value = null
      uploadRef.value?.clearFiles()
    }

    onMounted(loadDeps)

    return {
      formRef, uploadRef, form, rules, submitting, depList,
      handleFileChange, handleFileRemove, handleSubmit, resetForm
    }
  }
}
</script>
