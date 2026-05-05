<template>
  <div class="job-submit">
    <h2 class="mb-5">提交 Spark 任务</h2>
    <v-card>
      <v-card-text>
        <v-form ref="formRef" @submit.prevent="handleSubmit">
          <v-text-field
            v-model="form.jobName"
            label="任务名称"
            placeholder="请输入任务名称"
            :rules="[v => !!v || '请输入任务名称']"
            variant="outlined"
            density="comfortable"
          />

          <v-file-input
            v-model="selectedFile"
            label="上传JAR文件"
            accept=".jar"
            prepend-icon="mdi-upload"
            variant="outlined"
            density="comfortable"
            class="mb-3"
          />

          <v-text-field
            v-model="form.mainClass"
            label="主类"
            placeholder="com.example.MainClass"
            :rules="[v => !!v || '请输入主类名']"
            variant="outlined"
            density="comfortable"
          />

          <v-textarea
            v-model="form.appArgs"
            label="应用参数"
            placeholder="程序参数，空格分隔"
            rows="2"
            variant="outlined"
            density="comfortable"
          />

          <v-divider class="mb-4">
            <span class="text-caption text-medium-emphasis">资源配置</span>
          </v-divider>

          <v-row>
            <v-col cols="4">
              <v-text-field
                v-model.number="form.driverMemory"
                label="Driver内存(MB)"
                type="number"
                :min="256"
                variant="outlined"
                density="comfortable"
              />
            </v-col>
            <v-col cols="4">
              <v-text-field
                v-model.number="form.driverCores"
                label="Driver核数"
                type="number"
                :min="1"
                variant="outlined"
                density="comfortable"
              />
            </v-col>
            <v-col cols="4">
              <v-text-field
                v-model.number="form.numExecutors"
                label="Executor数量"
                type="number"
                :min="1"
                variant="outlined"
                density="comfortable"
              />
            </v-col>
          </v-row>

          <v-row>
            <v-col cols="4">
              <v-text-field
                v-model.number="form.executorMemory"
                label="Executor内存(MB)"
                type="number"
                :min="256"
                :step="256"
                variant="outlined"
                density="comfortable"
              />
            </v-col>
            <v-col cols="4">
              <v-text-field
                v-model.number="form.executorCores"
                label="Executor核数"
                type="number"
                :min="1"
                variant="outlined"
                density="comfortable"
              />
            </v-col>
          </v-row>

          <v-divider class="mb-4">
            <span class="text-caption text-medium-emphasis">Spark 配置</span>
          </v-divider>

          <v-textarea
            v-model="form.sparkProperties"
            label="自定义配置"
            placeholder="spark.sql.shuffle.partitions=200&#10;spark.default.parallelism=100"
            rows="4"
            variant="outlined"
            density="comfortable"
          />

          <v-divider class="mb-4">
            <span class="text-caption text-medium-emphasis">依赖组</span>
          </v-divider>

          <v-select
            v-model="form.dependencyIds"
            :items="depList"
            :item-title="(item) => `${item.name} (${item._jarCount || 0} 个JAR)`"
            item-value="id"
            multiple
            chips
            label="选择依赖组"
            hint="选择依赖组（该组所有JAR加入classpath）"
            persistent-hint
            variant="outlined"
            density="comfortable"
          />

          <div class="mt-4">
            <v-btn
              color="primary"
              type="submit"
              :loading="submitting"
              class="mr-3"
            >
              提交任务
            </v-btn>
            <v-btn @click="resetForm">
              重置
            </v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { submitJob } from '../api/job'
import { getDependencyList, getDependencyJars } from '../api/dependency'
import { useMessage } from '../composables/message'

export default {
  name: 'JobSubmit',
  setup() {
    const formRef = ref(null)
    const submitting = ref(false)
    const depList = ref([])
    const selectedFile = ref(null)
    const message = useMessage()

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
        message.warning('请上传JAR文件')
        return
      }
      const result = await formRef.value.validate()
      if (!result.valid) return

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
        message.success('任务提交成功')
        selectedFile.value = null
      } catch (e) {
        message.error('提交失败: ' + (e.message || '未知错误'))
      } finally {
        submitting.value = false
      }
    }

    const resetForm = () => {
      formRef.value?.reset()
      selectedFile.value = null
    }

    onMounted(loadDeps)

    return {
      formRef, form, submitting, depList, selectedFile,
      handleSubmit, resetForm
    }
  }
}
</script>
