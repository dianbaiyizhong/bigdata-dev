import api from './index'

export function submitJob(formData) {
  return api.post('/job/submit', formData)
}

export function getJobList(page = 1, size = 10) {
  return api.get('/job/list', { params: { page, size } })
}

export function getJobById(id) {
  return api.get(`/job/${id}`)
}

export function killJob(id) {
  return api.post(`/job/${id}/kill`)
}
