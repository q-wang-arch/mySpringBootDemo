<template>
  <div class="task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分析任务列表</span>
          <div>
            <el-input v-model="filters.borrowerId" placeholder="借款人ID" clearable style="width:200px;margin-right:8px" @clear="loadData" />
            <el-select v-model="filters.status" placeholder="状态" clearable style="width:140px;margin-right:8px" @change="loadData">
              <el-option label="待执行" value="PENDING" />
              <el-option label="执行中" value="RUNNING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-button type="primary" @click="loadData">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskId" label="任务ID" width="220" show-overflow-tooltip />
        <el-table-column prop="borrowerId" label="借款人ID" width="140" />
        <el-table-column prop="reportPeriod" label="报告期次" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentStep" label="当前步骤" show-overflow-tooltip />
        <el-table-column prop="riskScore" label="风险评分" width="100">
          <template #default="{ row }">
            <span v-if="row.riskScore" style="font-weight:bold;color:#409EFF">{{ row.riskScore }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="riskGrade" label="评级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.riskGrade" :type="gradeType(row.riskGrade)" size="small">{{ row.riskGrade }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="完成时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$emit('viewTask', row.taskId)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.page"
        v-model:page-size="page.size"
        :total="page.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end;display:flex"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, defineEmits } from 'vue'
import { agentApi } from '../api'

const emit = defineEmits(['viewTask'])

const loading = ref(false)
const tableData = ref([])
const filters = reactive({ borrowerId: '', status: '' })
const page = reactive({ page: 1, size: 20, total: 0 })

function loadData() {
  loading.value = true
  agentApi.listTasks({ ...filters, ...page }).then(res => {
    tableData.value = res.data.records || []
    page.total = res.data.total || 0
  }).finally(() => { loading.value = false })
}

function statusType(s) {
  return { COMPLETED: 'success', RUNNING: 'warning', PENDING: 'info', FAILED: 'danger' }[s] || 'info'
}
function gradeType(g) {
  return { A: 'success', B: '', C: 'warning', D: 'danger', E: 'danger' }[g] || 'info'
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
