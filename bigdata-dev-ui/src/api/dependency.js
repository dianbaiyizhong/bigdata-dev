import api from './index'

export function createDependency(name, description) {
  return api.post('/dependency', null, { params: { name, description } })
}

export function getDependencyList(page = 1, size = 10, keyword = '', fresh = false) {
  const params = { page, size, keyword }
  if (fresh) params._t = Date.now()
  return api.get('/dependency/list', { params })
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
