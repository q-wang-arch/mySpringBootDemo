<template>
  <div class="alert-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预警管理</span>
          <div>
            <el-select v-model="filters.status" placeholder="状态" clearable style="width:140px;margin-right:8px" @change="loadData">
              <el-option label="已触发" value="TRIGGERED" />
              <el-option label="已处置" value="HANDLED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
            <el-select v-model="filters.level" placeholder="级别" clearable style="width:120px;margin-right:8px" @change="loadData">
              <el-option label="紧急" value="URGENT" />
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
            </el-select>
            <el-button type="primary" @click="loadData">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="alertId" label="预警ID" width="220" show-overflow-tooltip />
        <el-table-column prop="borrowerName" label="借款人" width="100" />
        <el-table-column prop="ruleName" label="规则名称" width="140" />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="handleComment" label="处理意见" show-overflow-tooltip />
        <el-table-column prop="createTime" label="触发时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'TRIGGERED'" size="small" type="warning" @click="openHandle(row)">处置</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="showHandle" title="预警处置" width="500px">
        <el-form label-width="80px">
          <el-form-item label="预警">
            <span>{{ currentAlert.ruleName }} - {{ currentAlert.borrowerName }}</span>
          </el-form-item>
          <el-form-item label="处理人">
            <el-input v-model="handleForm.handler" placeholder="请输入处理人姓名" />
          </el-form-item>
          <el-form-item label="处理意见">
            <el-input v-model="handleForm.handleComment" type="textarea" :rows="4" placeholder="请输入处理意见" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showHandle = false">取消</el-button>
          <el-button type="primary" @click="submitHandle">提交</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { agentApi, isAuthError } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const filters = reactive({ status: '', level: '' })
const showHandle = ref(false)
const currentAlert = reactive({})
const handleForm = reactive({ handler: '', handleComment: '' })

function loadData() {
  loading.value = true
  agentApi.listAlerts(filters).then(res => {
    tableData.value = res.data?.records || res.data || []
  }).catch(() => { tableData.value = [] }).finally(() => { loading.value = false })
}

function openHandle(row) {
  Object.assign(currentAlert, row)
  handleForm.handler = ''
  handleForm.handleComment = ''
  showHandle.value = true
}

function submitHandle() {
  if (!handleForm.handler) { ElMessage.warning('请输入处理人'); return }
  agentApi.handleAlert(currentAlert.alertId, handleForm).then(() => {
    ElMessage.success('处置成功')
    showHandle.value = false
    loadData()
  }).catch(err => {
    // 401/403 已由 axios 拦截器统一提示，这里不重复弹窗
    if (!isAuthError(err)) ElMessage.error(err.response?.data?.message || '处置失败')
  })
}

function levelType(l) {
  return { URGENT: 'danger', HIGH: 'danger', MEDIUM: 'warning' }[l] || 'info'
}
function levelLabel(l) {
  return { URGENT: '紧急', HIGH: '高', MEDIUM: '中' }[l] || l
}
function statusType(s) {
  return { TRIGGERED: 'warning', HANDLED: 'success', CLOSED: 'info' }[s] || 'info'
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
