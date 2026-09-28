import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 响应拦截：解包后端 ApiResponse { code, message, data, timestamp }
// 业务数据提到 res.data，前端可直接 res.data.xxx 访问
request.interceptors.response.use(
  response => {
    const apiRes = response.data
    // 兼容非 ApiResponse 的响应（如直接返回 List）
    if (apiRes && typeof apiRes === 'object' && 'code' in apiRes) {
      if (apiRes.code !== 200) {
        return Promise.reject({ response: { data: apiRes } })
      }
      response.data = apiRes.data
      response.message = apiRes.message
    }
    return response
  },
  error => Promise.reject(error)
)

// ============ 贷后智能体 ============
export const agentApi = {
  // 数据接入
  ingest(data) { return request.post('/agent/ingest', data) },
  // 查询单个任务
  getTask(taskId) { return request.get(`/agent/task/${taskId}`) },
  // 查询任务列表
  listTasks(params) { return request.get('/agent/tasks', { params }) },
  // 查询报告列表
  listReports(params) { return request.get('/report/list', { params }) },
  // 查询报告详情
  getReport(reportId) { return request.get(`/report/${reportId}`) },
  // 查询预警列表
  listAlerts(params) { return request.get('/alert/list', { params }) },
  // 处置预警
  handleAlert(alertId, data) { return request.post(`/alert/${alertId}/handle`, data) },
}
