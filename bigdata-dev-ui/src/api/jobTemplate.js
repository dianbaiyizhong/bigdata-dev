import api from './index'

export function saveJobTemplate(formData) {
  return api.post('/job-template', formData)
}

export function getJobTemplateList(page = 1, size = 10, keyword = '') {
  return api.get('/job-template/list', { params: { page, size, keyword } })
}

export function getJobTemplateById(id) {
  return api.get(`/job-template/${id}`)
}

export function deleteJobTemplate(id) {
  return api.delete(`/job-template/${id}`)
}
