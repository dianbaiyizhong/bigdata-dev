import api from './index'

export function createDependency(name, description) {
  return api.post('/dependency', null, { params: { name, description } })
}

export function getDependencyList(page = 1, size = 10, keyword = '') {
  return api.get('/dependency/list', { params: { page, size, keyword } })
}

export function deleteDependency(id) {
  return api.delete(`/dependency/${id}`)
}

export function uploadDependencyJars(id, formData) {
  return api.post(`/dependency/${id}/upload`, formData)
}

export function getDependencyJars(id) {
  return api.get(`/dependency/${id}/jars`)
}

export function deleteDependencyJar(jarId) {
  return api.delete(`/dependency/jar/${jarId}`)
}
