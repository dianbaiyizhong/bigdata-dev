import api from './index'

export function uploadPySparkZip(formData) {
  return api.post('/pyspark/upload', formData)
}

export function getPySparkZipList() {
  return api.get('/pyspark/list')
}

export function deletePySparkZip(id) {
  return api.delete(`/pyspark/${id}`)
}
