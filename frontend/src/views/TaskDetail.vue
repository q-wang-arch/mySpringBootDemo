<template>
  <div class="task-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>任务详情 - {{ task.taskId }}</span>
          <div>
            <el-tag v-if="polling" type="warning" effect="plain" style="margin-right:8px">
              <i class="el-icon-loading"></i> 自动刷新中
            </el-tag>
            <el-button @click="$emit('back')">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border v-if="task.taskId">
        <el-descriptions-item label="任务ID">{{ task.taskId }}</el-descriptions-item>
        <el-descriptions-item label="借款人ID">{{ task.borrowerId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(task.status)">{{ task.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前步骤">{{ task.currentStep || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险评分">
          <span v-if="task.riskScore" style="font-size:18px;font-weight:bold;color:#409EFF">{{ task.riskScore }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="风险评级">
          <el-tag v-if="task.riskGrade" :type="gradeType(task.riskGrade)" size="large">{{ task.riskGrade }}级</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="报告ID">{{ task.reportId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ task.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ task.endTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">步骤执行日志</el-divider>

      <el-timeline v-if="steps.length > 0">
        <el-timeline-item
          v-for="step in steps"
          :key="step.id"
          :timestamp="step.startTime"
          :type="step.status === 'SUCCESS' ? 'success' : 'danger'"
          :hollow="step.status === 'FAILED'"
        >
          <el-card>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:bold">Step{{ step.stepOrder }}: {{ step.stepName }}</span>
              <div>
                <el-tag size="small" :type="step.status === 'SUCCESS' ? 'success' : 'danger'">{{ step.status }}</el-tag>
                <span v-if="step.durationMs != null" style="margin-left:8px;color:#999;font-size:12px">{{ step.durationMs }}ms</span>
              </div>
            </div>
            <p v-if="step.outputData" style="margin-top:8px;color:#666">{{ step.outputData }}</p>
            <p v-if="step.errorMsg" style="margin-top:8px;color:#F56C6C">{{ step.errorMsg }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无执行日志" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, defineEmits, defineProps } from 'vue'
import { agentApi } from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({ taskId: String })
const emit = defineEmits(['back'])

const loading = ref(false)
const polling = ref(false)
const task = ref({})
const steps = ref([])
let pollTimer = null

// 异步任务状态：未终态 → 继续轮询；终态 → 停止
const POLL_INTERVAL = 2000
const RUNNING_STATES = ['PENDING', 'RUNNING']

function loadDetail(silent = false) {
  if (!silent) loading.value = true
  return agentApi.getTask(props.taskId).then(res => {
    const prevStatus = task.value.status
    task.value = res.data
    steps.value = res.data.steps || []

    const status = res.data.status
    if (RUNNING_STATES.includes(status)) {
      startPolling()
    } else {
      stopPolling()
      // 状态从运行中切到终态时提示
      if (prevStatus && RUNNING_STATES.includes(prevStatus)) {
        if (status === 'COMPLETED') ElMessage.success('分析完成')
        else if (status === 'FAILED') ElMessage.error('分析失败：' + (res.data.errorMsg || ''))
      }
    }
  }).finally(() => { loading.value = false })
}

function startPolling() {
  if (pollTimer) return
  polling.value = true
  pollTimer = setInterval(() => loadDetail(true), POLL_INTERVAL)
}

function stopPolling() {
  polling.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function statusType(s) {
  return { COMPLETED: 'success', RUNNING: 'warning', PENDING: 'info', FAILED: 'danger' }[s] || 'info'
}
function gradeType(g) {
  return { A: 'success', B: '', C: 'warning', D: 'danger', E: 'danger' }[g] || 'info'
}

onMounted(() => loadDetail())
onBeforeUnmount(() => stopPolling())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
