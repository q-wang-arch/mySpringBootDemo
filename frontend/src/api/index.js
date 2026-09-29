import axios from 'axios'
import { ElMessage } from 'element-plus'

// 访问令牌来自环境变量（根目录 .env.local 中的 VITE_API_TOKEN），不写死在源码里。
// 本地开发：复制 .env.example 为 .env.local 并填入令牌值（须与后端 APP_TOKEN_* 一致）。
const API_TOKEN = (import.meta.env.VITE_API_TOKEN || '').trim()

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：附加访问令牌，对应后端 Authorization: Bearer <token>
let missingTokenWarned = false
request.interceptors.request.use(
  config => {
    if (API_TOKEN) {
      config.headers.Authorization = `Bearer ${API_TOKEN}`
    } else if (!missingTokenWarned) {
      missingTokenWarned = true
      console.warn('[api] 未配置 VITE_API_TOKEN，后端若已开启鉴权，所有接口都会返回 401')
    }
    return config
  },
  error => Promise.reject(error)
)

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
  error => {
    // 鉴权失败统一在这里提示并打标记，避免各视图重复弹窗
    const status = error.response?.status
    if (status === 401) {
      error.authHandled = true
      ElMessage.error(error.response?.data?.message || '未认证或令牌已失效，请检查 .env.local 中的 VITE_API_TOKEN')
    } else if (status === 403) {
      error.authHandled = true
      ElMessage.error(error.response?.data?.message || '当前身份无权访问该接口')
    }
    return Promise.reject(error)
  }
)

/** 是否为鉴权类错误（401/403）——接口层已弹过提示，视图里用它避免重复提示 */
export function isAuthError(err) {
  return !!(err && err.authHandled)
}

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
