<template>
  <div class="job-template">
    <h2 class="mb-5">任务模板</h2>

    <v-card>
      <v-card-text>
        <v-row align="center" class="mb-4">
          <v-col>
            <h3>模板列表</h3>
          </v-col>
          <v-col cols="auto">
            <v-text-field
              v-model="keyword"
              label="搜索模板名称"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              style="width: 280px"
              @click:clear="searchList"
              @keyup.enter="searchList"
            >
              <template #append-inner>
                <v-btn icon size="small" variant="text" @click="searchList">
                  <v-icon>mdi-magnify</v-icon>
                </v-btn>
              </template>
            </v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn
              icon="mdi-refresh"
              size="small"
              variant="text"
              :loading="loading"
              @click="loadList"
            />
          </v-col>
        </v-row>

        <v-data-table
          :items="tableData"
          :headers="headers"
          :loading="loading"
          hover
          density="comfortable"
          class="elevation-0"
          hide-default-footer
          no-data-text="暂无任务模板"
        >
          <template #item.templateName="{ item }">
            <v-tooltip location="top" :text="item.templateName">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.templateName }}</span>
              </template>
            </v-tooltip>
          </template>
          <template #item.jobType="{ item }">
            <v-chip :color="jobTypeColor(item.jobType)" size="small" label>
              {{ jobTypeLabel(item.jobType) }}
            </v-chip>
          </template>
          <template #item.programFile="{ item }">
            <v-tooltip location="top" :text="getProgramFilePath(item)">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ getProgramFile(item) }}</span>
              </template>
            </v-tooltip>
          </template>
          <template #item.mainClass="{ item }">
            <v-tooltip location="top" :text="item.mainClass || '-'">
              <template #activator="{ props }">
                <span v-bind="props" class="d-block text-truncate">{{ item.mainClass || '-' }}</span>
              </template>
            </v-tooltip>
          </template>
          <template #item.dependencyCount="{ item }">
            <v-chip size="small" label>{{ getDependencyCount(item) }}</v-chip>
          </template>
          <template #item.updateTime="{ item }">
            {{ formatTime(item.updateTime) }}
          </template>
          <template #item.actions="{ item }">
            <v-btn
              color="primary"
              size="small"
              variant="tonal"
              class="mr-2"
              :loading="openingRunId === item.id"
              :disabled="openingRunId !== null && openingRunId !== item.id"
              @click="openRunDialog(item)"
            >
              <Icon icon="mdi:play" size="16" class="mr-1" />
              运行
            </v-btn>
            <v-btn
              color="error"
              size="small"
              variant="tonal"
              @click="handleDelete(item)"
            >
              <Icon icon="mdi:delete-outline" size="16" class="mr-1" />
              删除
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
              @update:model-value="handleSizeChange"
            />
          </v-col>
          <v-col cols="auto">
            <span class="text-body-2 text-medium-emphasis mr-3">共 {{ total }} 条</span>
          </v-col>
          <v-col cols="auto">
            <v-pagination
              v-model="page"
              :length="pageCount"
              :total-visible="5"
              density="comfortable"
              @update:model-value="loadList"
            />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-dialog
      v-model="runDialogVisible"
      max-width="900"
      :persistent="runSubmitting"
      @update:model-value="handleRunDialogUpdate"
    >
      <v-card v-if="currentTemplate">
        <v-card-title class="text-h6">运行任务模板</v-card-title>
        <v-divider />
        <v-card-text>
          <v-alert type="info" variant="tonal" density="comfortable" class="mb-4">
            程序文件、主类和依赖组直接使用模板配置，无需重新上传或填写。
          </v-alert>

          <v-table density="compact" class="template-summary">
            <tbody>
              <tr>
                <td class="text-medium-emphasis summary-label">模板类型</td>
                <td>{{ jobTypeLabel(currentTemplate.jobType) }}</td>
              </tr>
              <tr>
                <td class="text-medium-emphasis summary-label">
                  {{ isPythonTemplate(currentTemplate) ? 'Python脚本文件' : 'JAR文件' }}
                </td>
                <td class="text-break">{{ getProgramFile(currentTemplate) }}</td>
              </tr>
              <tr>
                <td class="text-medium-emphasis summary-label">主类</td>
                <td class="text-break">
                  {{ currentTemplate.mainClass || (isPythonTemplate(currentTemplate) ? '不适用（PySpark）' : '-') }}
                </td>
              </tr>
              <tr v-if="isPythonTemplate(currentTemplate) && currentTemplate.entryFile">
                <td class="text-medium-emphasis summary-label">入口文件</td>
                <td class="text-break">{{ currentTemplate.entryFile }}</td>
              </tr>
              <tr>
                <td class="text-medium-emphasis summary-label">依赖组</td>
                <td>
                  <v-progress-linear v-if="dependencyLoading" indeterminate color="primary" class="mb-2" />
                  <template v-else-if="dependencyError">
                    <v-alert type="error" variant="tonal" density="compact" class="mb-2">
                      {{ dependencyError }}
                    </v-alert>
                    <span class="text-caption text-medium-emphasis">
                      依赖组ID：{{ dependencyIdsText(currentTemplate) }}
                    </span>
                  </template>
                  <div v-else-if="dependencyGroups.length > 0">
                    <v-chip
                      v-for="group in dependencyGroups"
                      :key="group.id"
                      color="primary"
                      variant="tonal"
                      size="small"
                      class="mr-1 mb-1"
                    >
                      {{ dependencyGroupLabel(group) }} · {{ group.jarCount }} 个JAR
                    </v-chip>
                  </div>
                  <span v-else class="text-medium-emphasis">无依赖组</span>
                </td>
              </tr>
            </tbody>
          </v-table>

          <v-divider class="my-5">
            <span class="text-caption text-medium-emphasis">运行参数</span>
          </v-divider>

          <v-form ref="runFormRef" @submit.prevent="submitTemplateJob">
            <v-text-field
              v-model.trim="runForm.jobName"
              label="任务名称"
              placeholder="请输入任务名称"
              :rules="[v => !!v || '请输入任务名称']"
              variant="outlined"
              density="comfortable"
            />

            <label class="text-body-2 mb-1 d-block">应用参数 (JSON)</label>
            <div class="cm-editor-wrapper mb-4">
              <Codemirror
                v-model="runForm.appArgs"
                :extensions="jsonExtensions"
                :style="{ height: '120px' }"
                placeholder='{ "key": "value" }'
              />
            </div>

            <v-divider class="mb-4">
              <span class="text-caption text-medium-emphasis">资源配置</span>
            </v-divider>

            <v-row>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model.number="runForm.driverMemory"
                  label="Driver内存(MB)"
                  type="number"
                  :min="256"
                  variant="outlined"
                  density="comfortable"
                />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model.number="runForm.driverCores"
                  label="Driver核数"
                  type="number"
                  :min="1"
                  variant="outlined"
                  density="comfortable"
                />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model.number="runForm.numExecutors"
                  label="Executor数量"
                  type="number"
                  :min="1"
                  variant="outlined"
                  density="comfortable"
                />
              </v-col>
            </v-row>

            <v-row>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model.number="runForm.executorMemory"
                  label="Executor内存(MB)"
                  type="number"
                  :min="256"
                  :step="256"
                  variant="outlined"
                  density="comfortable"
                />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model.number="runForm.executorCores"
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
              v-model="runForm.sparkProperties"
              label="自定义配置"
              placeholder="spark.sql.shuffle.partitions=200&#10;spark.default.parallelism=100"
              rows="4"
              variant="outlined"
              density="comfortable"
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            type="button"
            variant="text"
            :disabled="runSubmitting"
            @click="runDialogVisible = false"
          >
            取消
          </v-btn>
          <v-btn
            type="button"
            color="primary"
            variant="text"
            :loading="runSubmitting"
            @click="submitTemplateJob"
          >
            提交任务
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { Icon } from '@iconify/vue'
import { Codemirror } from 'vue-codemirror'
import { json } from '@codemirror/lang-json'
import { oneDark } from '@codemirror/theme-one-dark'
import { getDependencyJars, getDependencyList } from '../api/dependency'
import { submitJob } from '../api/job'
import { deleteJobTemplate, getJobTemplateById, getJobTemplateList } from '../api/jobTemplate'
import { useMessage } from '../composables/message'

export default {
  name: 'JobTemplate',
  components: { Icon, Codemirror },
  setup() {
    const message = useMessage()
    const loading = ref(false)
    const tableData = ref([])
    const page = ref(1)
    const size = ref(10)
    const total = ref(0)
    const keyword = ref('')

    const runDialogVisible = ref(false)
    const runFormRef = ref(null)
    const currentTemplate = ref(null)
    const dependencyGroups = ref([])
    const dependencyLoading = ref(false)
    const dependencyError = ref('')
    const runSubmitting = ref(false)
    const openingRunId = ref(null)

    const runForm = reactive({
      jobName: '',
      appArgs: '',
      driverMemory: 1024,
      driverCores: 1,
      executorMemory: 1024,
      executorCores: 1,
      numExecutors: 2,
      sparkProperties: ''
    })

    const jsonExtensions = [json(), oneDark]

    const headers = [
      { title: '模板名称', key: 'templateName', minWidth: 180 },
      { title: '类型', key: 'jobType', width: 110 },
      { title: '固定程序文件', key: 'programFile', minWidth: 200 },
      { title: '主类', key: 'mainClass', minWidth: 180 },
      { title: '依赖组数', key: 'dependencyCount', width: 110 },
      { title: '更新时间', key: 'updateTime', minWidth: 170 },
      { title: '操作', key: 'actions', sortable: false, width: 190 }
    ]

    const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value) || 1))

    const toText = (value) => {
      if (value === null || value === undefined) return ''
      if (typeof value === 'object') return JSON.stringify(value, null, 2)
      return String(value)
    }

    const valueOrNumber = (value, fallback) => {
      if (value === null || value === undefined || value === '') return fallback
      const number = Number(value)
      return Number.isFinite(number) ? number : fallback
    }

    const getDependencyIds = (template) => {
      let value = template?.dependencyIds ?? template?.dependencyGroupIds
      if (typeof value === 'string' && value.trim().startsWith('[')) {
        try {
          value = JSON.parse(value)
        } catch {}
      }
      if (value === null || value === undefined || value === '') {
        value = Array.isArray(template?.dependencyGroups)
          ? template.dependencyGroups.map(item => typeof item === 'object' ? item?.id : item)
          : []
      }
      if (!Array.isArray(value)) value = String(value).split(',')
      return [...new Set(value
        .map(item => typeof item === 'object' ? item?.id : item)
        .filter(id => id !== null && id !== undefined && String(id).trim() !== '')
        .map(id => String(id).trim()))]
    }

    const getDependencyCount = (template) => {
      const ids = getDependencyIds(template)
      if (ids.length > 0) return ids.length
      const count = Number(template?.dependencyCount ?? template?.dependencyGroupCount)
      return Number.isFinite(count) && count >= 0 ? count : 0
    }

    const dependencyIdsText = (template) => {
      const ids = getDependencyIds(template)
      return ids.length > 0 ? ids.join('、') : '无'
    }

    const getProgramFilePath = (template) => {
      const value = [
        template?.fileName,
        template?.programFileName,
        template?.jarName,
        template?.jarFileName,
        template?.scriptName,
        template?.scriptFileName,
        template?.pyScriptName,
        template?.pyScriptFileName,
        template?.jarOriginalName,
        template?.scriptOriginalName,
        template?.jarPath,
        template?.scriptPath,
        template?.pyScriptPath,
        template?.filePath,
        template?.file
      ].find(item => item !== null && item !== undefined && String(item).trim() !== '')
      if (!value) return '-'
      if (typeof value === 'object') return value.name || value.originalName || '-'
      return String(value)
    }

    const getProgramFile = (template) => {
      const path = getProgramFilePath(template)
      if (path === '-') return '-'
      return path.split(/[\\/]/).pop() || path
    }

    const isPythonTemplate = (template) => String(template?.jobType || '').toUpperCase() === 'PYTHON'

    const jobTypeLabel = (jobType) => {
      if (String(jobType || '').toUpperCase() === 'PYTHON') return 'PySpark'
      if (String(jobType || '').toUpperCase() === 'JAR') return 'Spark JAR'
      return jobType || '-'
    }

    const jobTypeColor = (jobType) => {
      return isPythonTemplate({ jobType }) ? 'info' : 'primary'
    }

    const normalizeTemplate = (template) => ({
      ...(template || {}),
      templateName: template?.templateName || template?.name || '-',
      jobType: template?.jobType || 'JAR'
    })

    const formatTime = (value) => {
      if (value === null || value === undefined || value === '') return '-'
      if (value instanceof Date) return value.toLocaleString()
      return String(value).replace('T', ' ')
    }

    const formatTimestamp = (date) => {
      const pad = value => String(value).padStart(2, '0')
      return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}-${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
    }

    const loadList = async () => {
      loading.value = true
      try {
        const res = await getJobTemplateList(page.value, size.value, keyword.value || '')
        const data = res?.data && typeof res.data === 'object' ? res.data : {}
        const records = Array.isArray(data.records) ? data.records : []
        const parsedTotal = Number(data.total)
        tableData.value = records.filter(item => item && typeof item === 'object').map(normalizeTemplate)
        total.value = Number.isFinite(parsedTotal) && parsedTotal >= 0 ? parsedTotal : tableData.value.length
      } catch (e) {
        tableData.value = []
        total.value = 0
        message.error('加载任务模板失败: ' + (e.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }

    const searchList = () => {
      page.value = 1
      loadList()
    }

    const handleSizeChange = (value) => {
      size.value = Number(value) || 10
      page.value = 1
      loadList()
    }

    const loadDependencyGroups = async (template) => {
      const ids = getDependencyIds(template)
      dependencyGroups.value = []
      dependencyError.value = ''
      if (ids.length === 0) return

      dependencyLoading.value = true
      try {
        const res = await getDependencyList(1, 500, '', true)
        const data = res?.data && typeof res.data === 'object' ? res.data : {}
        const records = Array.isArray(data.records) ? data.records : []
        const dependencyMap = new Map(records
          .filter(item => item && item.id !== null && item.id !== undefined)
          .map(item => [String(item.id), item]))
        const missingIds = ids.filter(id => !dependencyMap.has(String(id)))
        if (missingIds.length > 0) {
          throw new Error(`依赖组 ${missingIds.join('、')} 不存在`)
        }
        const groups = await Promise.all(ids.map(async (id) => {
          const dependency = dependencyMap.get(String(id))
          const jarsRes = await getDependencyJars(id)
          const jars = Array.isArray(jarsRes?.data) ? jarsRes.data : []
          return { ...dependency, jarCount: jars.length }
        }))
        dependencyGroups.value = groups
      } catch (e) {
        const text = '加载依赖组失败: ' + (e.message || '未知错误')
        dependencyError.value = text
        message.error(text)
      } finally {
        dependencyLoading.value = false
      }
    }

    const dependencyGroupLabel = (group) => group.name || `依赖组 ${group.id}`

    const resetRunForm = (template) => {
      const name = template.templateName && template.templateName !== '-' ? template.templateName : '任务模板'
      runForm.jobName = `${name}-${formatTimestamp(new Date())}`
      runForm.appArgs = toText(template.appArgs)
      runForm.driverMemory = valueOrNumber(template.driverMemory, 1024)
      runForm.driverCores = valueOrNumber(template.driverCores, 1)
      runForm.executorMemory = valueOrNumber(template.executorMemory, 1024)
      runForm.executorCores = valueOrNumber(template.executorCores, 1)
      runForm.numExecutors = valueOrNumber(template.numExecutors, 2)
      runForm.sparkProperties = toText(template.sparkProperties)
    }

    const openRunDialog = async (row) => {
      if (!row?.id) {
        message.warning('模板ID无效')
        return
      }
      if (openingRunId.value !== null) return

      openingRunId.value = row.id
      try {
        const res = await getJobTemplateById(row.id)
        if (!res?.data || typeof res.data !== 'object') {
          throw new Error('模板详情为空')
        }
        currentTemplate.value = normalizeTemplate(res.data)
        resetRunForm(currentTemplate.value)
        runDialogVisible.value = true
        await loadDependencyGroups(currentTemplate.value)
      } catch (e) {
        message.error('加载模板详情失败: ' + (e.message || '未知错误'))
      } finally {
        openingRunId.value = null
      }
    }

    const handleRunDialogUpdate = (visible) => {
      if (visible) return
      currentTemplate.value = null
      dependencyGroups.value = []
      dependencyError.value = ''
    }

    const submitTemplateJob = async () => {
      if (!currentTemplate.value?.id) {
        message.warning('模板ID无效')
        return
      }
      const result = runFormRef.value ? await runFormRef.value.validate() : { valid: false }
      if (!result?.valid) return

      runSubmitting.value = true
      try {
        const fd = new FormData()
        fd.append('templateId', currentTemplate.value.id)
        fd.append('jobName', runForm.jobName.trim())
        fd.append('jobType', currentTemplate.value.jobType)
        fd.append('appArgs', toText(runForm.appArgs))
        fd.append('driverMemory', runForm.driverMemory ?? '')
        fd.append('driverCores', runForm.driverCores ?? '')
        fd.append('executorMemory', runForm.executorMemory ?? '')
        fd.append('executorCores', runForm.executorCores ?? '')
        fd.append('numExecutors', runForm.numExecutors ?? '')
        fd.append('sparkProperties', toText(runForm.sparkProperties))
        await submitJob(fd)
        runDialogVisible.value = false
        message.success('任务提交成功')
        await loadList()
      } catch (e) {
        message.error('提交失败: ' + (e.message || '未知错误'))
      } finally {
        runSubmitting.value = false
      }
    }

    const handleDelete = async (row) => {
      try {
        await message.confirm(`确定要删除任务模板「${row.templateName}」吗？`)
        await deleteJobTemplate(row.id)
        message.success('任务模板已删除')
        if (tableData.value.length === 1 && page.value > 1) page.value -= 1
        await loadList()
      } catch (e) {
        if (e !== 'cancel') message.error('删除失败: ' + (e.message || '未知错误'))
      }
    }

    onMounted(loadList)

    const reload = () => { loadList() }

    return {
      loading, tableData, headers, page, size, total, keyword, pageCount,
      runDialogVisible, runFormRef, currentTemplate, dependencyGroups,
      dependencyLoading, dependencyError, runSubmitting, openingRunId,
      runForm, jsonExtensions, loadList, reload, searchList, handleSizeChange,
      getDependencyCount, getProgramFile, getProgramFilePath, isPythonTemplate,
      jobTypeLabel, jobTypeColor, formatTime, dependencyIdsText, dependencyGroupLabel,
      openRunDialog, handleRunDialogUpdate, submitTemplateJob, handleDelete
    }
  }
}
</script>

<style scoped>
.template-summary {
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.summary-label {
  width: 140px;
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
</style>
