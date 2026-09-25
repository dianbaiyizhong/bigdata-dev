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

          <v-radio-group
            v-model="form.jobType"
            inline
            density="comfortable"
            class="mb-3"
          >
            <v-radio label="Spark JAR" value="JAR" />
            <v-radio label="PySpark" value="PYTHON" />
          </v-radio-group>

          <!-- JAR 上传 -->
          <template v-if="form.jobType === 'JAR'">
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
                <input ref="fileInput" type="file" accept=".jar" hidden @change="handleFileSelect" />
                <template v-if="selectedFile">
                  <v-icon color="success" size="24" class="mr-1">mdi-check-circle</v-icon>
                  <v-icon color="primary" size="24" class="mr-2">mdi-file-code</v-icon>
                  <span class="text-body-2">{{ selectedFile.name }}</span>
                  <span class="text-caption text-medium-emphasis ml-2">({{ formatSize(selectedFile.size) }})</span>
                  <v-icon size="18" class="ml-3" color="grey" @click.stop="selectedFile = null">mdi-close</v-icon>
                </template>
                <template v-else>
                  <v-icon color="grey" size="40" class="mb-2">mdi-cloud-upload</v-icon>
                  <span class="text-body-2 text-medium-emphasis">拖拽 JAR 文件到此处，或点击选择</span>
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
          </template>

          <!-- PySpark 上传 -->
          <template v-if="form.jobType === 'PYTHON'">
            <div class="mb-3">
              <label class="text-body-2 mb-1 d-block">上传 Python 文件（.py 或 .zip 工程包）</label>
              <div
                class="drop-zone drop-zone--py"
                :class="{ 'drop-zone--active': pyDragOver, 'drop-zone--has-file': pyScriptFile }"
                @dragover.prevent="pyDragOver = true"
                @dragleave.prevent="pyDragOver = false"
                @drop.prevent="handlePyDrop($event)"
                @click="$refs.pyScriptInput.click()"
              >
                <input ref="pyScriptInput" type="file" accept=".py,.zip" hidden @change="handlePyScriptSelect" />
                <template v-if="pyScriptFile">
                  <v-icon color="success" size="24" class="mr-1">mdi-check-circle</v-icon>
                  <v-icon color="primary" size="24" class="mr-2">mdi-language-python</v-icon>
                  <span class="text-body-2">{{ pyScriptFile.name }}</span>
                  <span class="text-caption text-medium-emphasis ml-2">({{ formatSize(pyScriptFile.size) }})</span>
                  <v-icon size="18" class="ml-3" color="grey" @click.stop="pyScriptFile = null">mdi-close</v-icon>
                </template>
                <template v-else>
                  <v-icon color="grey" size="40" class="mb-2">mdi-language-python</v-icon>
                  <span class="text-body-2 text-medium-emphasis">拖拽 .py 或 .zip 到此处，或点击选择</span>
                </template>
                <Icon icon="streamline-logos:spark-logo-solid" class="spark-icon" />
              </div>
            </div>

            <v-text-field
              v-model="form.entryFile"
              label="入口文件"
              placeholder="main.py 或 src/main.py"
              hint="上传 .zip 时指定入口文件路径；上传 .py 时随意填写"
              persistent-hint
              variant="outlined"
              density="comfortable"
            />

            <v-select
              v-model="form.pySparkZipId"
              :items="pySparkZipList"
              :item-title="(item) => `${item.name} (${formatSize(item.fileSize)})`"
              :item-subtitle="(item) => item.hdfsPath || ''"
              item-value="id"
              label="选择 PySpark 依赖包"
              hint="在「PySpark包」页面管理依赖包，上传到 HDFS"
              persistent-hint
              variant="outlined"
              density="comfortable"
              clearable
            />
          </template>

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
              :loading="depLoading"
              :item-title="dependencyItemTitle"
            item-value="id"
            multiple
            chips
            label="选择依赖组"
            hint="选择依赖组（该组所有JAR加入classpath）"
            persistent-hint
            variant="outlined"
            density="comfortable"
            @click="loadDeps"
          />

          <div class="submit-bar">
            <v-btn
              color="primary"
              type="submit"
              :loading="submitting"
              :disabled="templateSaving"
              class="mr-3"
            >
              提交任务
            </v-btn>
            <v-btn
              type="button"
              color="primary"
              variant="tonal"
              :loading="templateSaving"
              :disabled="submitting || templateSaving"
              class="mr-3"
              @click="openTemplateDialog"
            >
              保存任务模板
            </v-btn>
            <v-btn
              type="button"
              @click="resetForm"
            >
              重置
            </v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>

    <v-dialog v-model="templateDialogVisible" max-width="480">
      <v-card>
        <v-card-title class="text-h6">保存任务模板</v-card-title>
        <v-divider />
        <v-card-text>
          <v-text-field
            v-model.trim="templateForm.templateName"
            label="模板名称"
            placeholder="请输入模板名称"
            maxlength="255"
            :rules="[v => !!v || '请输入模板名称']"
            variant="outlined"
            density="comfortable"
            autofocus
          />
          <v-textarea
            v-model="templateForm.description"
            label="描述"
            placeholder="描述（可选）"
            rows="3"
            variant="outlined"
            density="comfortable"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            type="button"
            variant="text"
            :disabled="templateSaving"
            @click="templateDialogVisible = false"
          >
            取消
          </v-btn>
          <v-btn
            type="button"
            color="primary"
            variant="text"
            :loading="templateSaving"
            :disabled="!templateForm.templateName.trim()"
            @click="saveTemplate"
          >
            保存
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { Codemirror } from 'vue-codemirror'
import { json } from '@codemirror/lang-json'
import { oneDark } from '@codemirror/theme-one-dark'
import { submitJob } from '../api/job'
import { saveJobTemplate } from '../api/jobTemplate'
import { getDependencyList, getDependencyJars } from '../api/dependency'
import { getPySparkZipList } from '../api/pyspark'
import { useMessage } from '../composables/message'

export default {
  name: 'JobSubmit',
  components: { Icon, Codemirror },
  setup() {
    const formRef = ref(null)
    const fileInput = ref(null)
    const pyScriptInput = ref(null)
    const submitting = ref(false)
    const templateDialogVisible = ref(false)
    const templateSaving = ref(false)
    const depList = ref([])
    const pySparkZipList = ref([])
    const selectedFile = ref(null)
    const dragOver = ref(false)
    const pyScriptFile = ref(null)
    const pyDragOver = ref(false)
    const message = useMessage()

    const jsonExtensions = [json(), oneDark]

    const form = reactive({
      jobName: '',
      jobType: 'JAR',
      entryFile: '',
      mainClass: '',
      appArgs: '',
      driverMemory: 1024,
      driverCores: 1,
      executorMemory: 1024,
      executorCores: 1,
      numExecutors: 2,
      sparkProperties: '',
      dependencyIds: [],
      pySparkZipId: null,
      deployMode: 'cluster',
      master: 'yarn'
    })

    const templateForm = reactive({
      templateName: '',
      description: ''
    })

    const depLoading = ref(false)

    const dependencyItemTitle = (item) => {
      const countText = item._jarLoadError ? '加载失败' : `${item._jarCount || 0} 个JAR`
      return `${item.name || '未命名依赖组'} (${countText})`
    }

    const loadDeps = async () => {
      depLoading.value = true
      try {
        const res = await getDependencyList(1, 500, '', true)
        const data = res?.data && typeof res.data === 'object' ? res.data : {}
        depList.value = Array.isArray(data.records) ? data.records : []
        for (const dep of depList.value) {
          dep._jarLoadError = false
          try {
            const jRes = await getDependencyJars(dep.id)
            dep._jarCount = Array.isArray(jRes?.data) ? jRes.data.length : 0
          } catch (e) {
            dep._jarCount = null
            dep._jarLoadError = true
            message.error(`依赖组「${dep.name || dep.id}」的JAR加载失败`)
          }
        }
      } catch (e) {
        depList.value = []
        message.error('加载依赖失败: ' + (e.message || '未知错误'))
      } finally {
        depLoading.value = false
      }
    }

    const handleDrop = (e) => {
      dragOver.value = false
      const files = e.dataTransfer?.files
      if (files && files.length > 0 && files[0].name.endsWith('.jar')) {
        selectedFile.value = files[0]
        message.success(`已选择文件：${files[0].name} (${formatSize(files[0].size)})`)
      } else {
        message.warning('请选择 .jar 文件')
      }
    }

    const handleFileSelect = (e) => {
      const file = e.target.files?.[0]
      if (file) {
        selectedFile.value = file
        message.success(`已选择文件：${file.name} (${formatSize(file.size)})`)
      }
    }

    const handlePyDrop = (e) => {
      pyDragOver.value = false
      const files = e.dataTransfer?.files
      if (!files || files.length === 0) return
      const name = files[0].name.toLowerCase()
      if (name.endsWith('.py') || name.endsWith('.zip')) {
        pyScriptFile.value = files[0]
        message.success(`已选择文件：${files[0].name} (${formatSize(files[0].size)})`)
      } else {
        message.warning('请选择 .py 或 .zip 文件')
      }
    }

    const handlePyScriptSelect = (e) => {
      const file = e.target.files?.[0]
      if (file) {
        pyScriptFile.value = file
        message.success(`已选择文件：${file.name} (${formatSize(file.size)})`)
      }
    }

    const formatSize = (bytes) => {
      if (!bytes) return '0 B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }

    const validateSubmission = async () => {
      const isPySpark = form.jobType === 'PYTHON'

      if (isPySpark && !pyScriptFile.value) {
        message.warning('请上传 Python 脚本或工程包')
        return false
      }
      if (!isPySpark && !selectedFile.value) {
        message.warning('请上传 JAR 文件')
        return false
      }

      const result = formRef.value ? await formRef.value.validate() : { valid: false }
      return result?.valid === true
    }

    const buildTemplateFormData = () => {
      const fd = new FormData()
      if (form.jobType === 'PYTHON') {
        fd.append('pyScript', pyScriptFile.value)
      } else {
        fd.append('file', selectedFile.value)
      }
      fd.append('templateName', templateForm.templateName.trim())
      fd.append('description', templateForm.description || '')
      fd.append('jobType', form.jobType)
      fd.append('mainClass', form.mainClass || '')
      fd.append('entryFile', form.entryFile || '')
      fd.append('appArgs', form.appArgs || '')
      fd.append('sparkProperties', form.sparkProperties || '')
      fd.append('deployMode', form.deployMode || 'cluster')
      fd.append('master', form.master || 'yarn')
      fd.append('driverMemory', form.driverMemory ?? '')
      fd.append('driverCores', form.driverCores ?? '')
      fd.append('executorMemory', form.executorMemory ?? '')
      fd.append('executorCores', form.executorCores ?? '')
      fd.append('numExecutors', form.numExecutors ?? '')
      const dependencyIds = Array.isArray(form.dependencyIds) ? form.dependencyIds : []
      fd.append('dependencyIds', dependencyIds.join(','))
      if (form.pySparkZipId !== null && form.pySparkZipId !== undefined && form.pySparkZipId !== '') {
        fd.append('pySparkZipId', form.pySparkZipId)
      }
      return fd
    }

    const openTemplateDialog = async () => {
      if (!(await validateSubmission())) return
      templateForm.templateName = form.jobName
      templateForm.description = ''
      templateDialogVisible.value = true
    }

    const saveTemplate = async () => {
      if (!templateForm.templateName.trim()) {
        message.warning('请输入模板名称')
        return
      }
      if (!(await validateSubmission())) return
      if (form.jobType === 'PYTHON' && pyScriptFile.value?.name.toLowerCase().endsWith('.zip') && !String(form.entryFile || '').trim()) {
        message.warning('Python ZIP工程包必须填写入口文件')
        return
      }

      templateSaving.value = true
      try {
        await saveJobTemplate(buildTemplateFormData())
        templateDialogVisible.value = false
        message.success('任务模板保存成功')
      } catch (e) {
        message.error('保存任务模板失败: ' + (e.message || '未知错误'))
      } finally {
        templateSaving.value = false
      }
    }

    const handleSubmit = async () => {
      if (!(await validateSubmission())) return

      const isPySpark = form.jobType === 'PYTHON'
      submitting.value = true
      try {
        const fd = new FormData()
        if (isPySpark) {
          fd.append('pyScript', pyScriptFile.value)
          if (form.entryFile) fd.append('entryFile', form.entryFile)
          if (form.pySparkZipId) fd.append('pySparkZipId', form.pySparkZipId)
        } else {
          fd.append('file', selectedFile.value)
        }
        fd.append('jobName', form.jobName)
        fd.append('jobType', form.jobType)
        if (form.mainClass) fd.append('mainClass', form.mainClass)
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
      } catch (e) {
        message.error('提交失败: ' + (e.message || '未知错误'))
      } finally {
        submitting.value = false
      }
    }

    const resetFormFields = () => {
      selectedFile.value = null
      pyScriptFile.value = null
    }

    const resetForm = () => {
      formRef.value?.reset()
      resetFormFields()
    }

    onMounted(loadDeps)

    const loadPySparkZips = async () => {
      try {
        const res = await getPySparkZipList()
        pySparkZipList.value = res.data || []
      } catch (e) { console.error('加载 PySpark 包失败', e) }
    }

    onMounted(loadPySparkZips)

    const reload = () => {
      loadDeps()
      loadPySparkZips()
    }

    return {
      formRef, fileInput, pyScriptInput, form, submitting, depList, depLoading, pySparkZipList,
      selectedFile, dragOver, pyScriptFile, pyDragOver, jsonExtensions,
      templateDialogVisible, templateSaving, templateForm,
      handleDrop, handleFileSelect, handlePyDrop, handlePyScriptSelect,
      formatSize, dependencyItemTitle, openTemplateDialog, saveTemplate, handleSubmit, resetForm, loadDeps, reload
    }
  }
}
</script>

<style scoped>
.job-submit {
  padding-bottom: 80px;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
  border-top: 1px solid rgba(0, 0, 0, 0.12);
  text-align: right;
  white-space: nowrap;
}

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
  min-height: 140px;
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
  min-height: 140px;
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
