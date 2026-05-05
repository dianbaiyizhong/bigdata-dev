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

          <div class="mb-3">
            <label class="text-body-2 mb-1 d-block">上传JAR文件</label>
            <div
              class="drop-zone"
              :class="{ 'drop-zone--active': dragOver, 'drop-zone--has-file': selectedFile }"
              @dragover.prevent="dragOver = true"
              @dragleave.prevent="dragOver = false"
              @drop.prevent="handleDrop"
              @click="$refs.fileInput.click()"
            >
              <input
                ref="fileInput"
                type="file"
                accept=".jar"
                hidden
                @change="handleFileSelect"
              />
              <template v-if="selectedFile">
                <v-icon color="primary" size="24" class="mr-2">mdi-file-code</v-icon>
                <span class="text-body-2">{{ selectedFile.name }}</span>
                <span class="text-caption text-medium-emphasis ml-2">({{ formatSize(selectedFile.size) }})</span>
                <v-icon
                  size="18"
                  class="ml-3"
                  color="grey"
                  @click.stop="selectedFile = null"
                >mdi-close</v-icon>
              </template>
              <template v-else>
                <v-icon color="grey" size="40" class="mb-2">mdi-cloud-upload</v-icon>
                <span class="text-body-2 text-medium-emphasis">拖拽 JAR，PY，ZIP 文件到此处，或点击选择</span>
              </template>
              <Icon icon="streamline-logos:spark-logo-solid" class="spark-icon" />
            </div>
          </div>

          <v-text-field
            v-model="form.mainClass"
            label="主类"
            placeholder="com.example.MainClass"
            :rules="[v => !!v || '请输入主类名']"
            variant="outlined"
            density="comfortable"
          />

          <label class="text-body-2 mb-1 d-block">应用参数 (JSON)</label>
          <div class="cm-editor-wrapper mb-3">
            <Codemirror
              v-model="form.appArgs"
              :extensions="jsonExtensions"
              :style="{ height: '120px' }"
              placeholder='{ "key": "value" }'
            />
          </div>

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
import { Icon } from '@iconify/vue'
import { Codemirror } from 'vue-codemirror'
import { json } from '@codemirror/lang-json'
import { oneDark } from '@codemirror/theme-one-dark'
import { submitJob } from '../api/job'
import { getDependencyList, getDependencyJars } from '../api/dependency'
import { useMessage } from '../composables/message'

export default {
  name: 'JobSubmit',
  components: { Icon, Codemirror },
  setup() {
    const formRef = ref(null)
    const fileInput = ref(null)
    const submitting = ref(false)
    const depList = ref([])
    const selectedFile = ref(null)
    const dragOver = ref(false)
    const message = useMessage()

    const jsonExtensions = [json(), oneDark]

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

    const handleDrop = (e) => {
      dragOver.value = false
      const files = e.dataTransfer?.files
      if (files && files.length > 0) {
        selectedFile.value = files[0]
      } else {
        message.warning('请选择 .jar .py .zip 文件')
      }
    }

    const handleFileSelect = (e) => {
      const file = e.target.files?.[0]
      if (file) selectedFile.value = file
    }

    const formatSize = (bytes) => {
      if (!bytes) return '0 B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
      formRef, fileInput, form, submitting, depList, selectedFile, dragOver, jsonExtensions,
      handleDrop, handleFileSelect, formatSize, handleSubmit, resetForm
    }
  }
}
</script>

<style scoped>
.cm-editor-wrapper {
  border: 1px solid rgba(0, 0, 0, 0.24);
  border-radius: 4px;
  overflow: hidden;
}

.cm-editor-wrapper:hover {
  border-color: rgba(0, 0, 0, 0.87);
}

.cm-editor-wrapper:focus-within {
  border-color: #1976D2;
  border-width: 2px;
}

.drop-zone {
  border: 2px dashed #bbb;
  border-radius: 8px;
  padding: 32px 24px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.drop-zone:hover {
  border-color: #1976D2;
  background: #f5f5f5;
}

.drop-zone--active {
  border-color: #1976D2;
  background: #e3f2fd;
}

.drop-zone--has-file {
  flex-direction: row;
  padding: 16px 24px;
}

.spark-icon {
  position: absolute;
  bottom: 4px;
  right: 4px;
  opacity: 0.15;
  transition: opacity 0.2s;
  font-size: 48px;
  color: #E25A1C;
}

.drop-zone:hover .spark-icon,
.drop-zone--active .spark-icon {
  opacity: 0.3;
}
</style>
