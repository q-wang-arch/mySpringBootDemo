<template>
  <div class="data-ingest">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据接入 - 推送贷后数据触发智能体分析</span>
          <el-button type="primary" @click="fillSampleData">填充示例数据</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="140px" class="ingest-form">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="借款人ID" required>
              <el-input v-model="form.borrowerId" placeholder="BR20260001" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="借款人姓名" required>
              <el-input v-model="form.borrowerName" placeholder="张三" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="贷款合同编号" required>
              <el-input v-model="form.loanId" placeholder="LN20260001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="报告期次" required>
              <el-input v-model="form.reportPeriod" placeholder="2026-09" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">贷款信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="贷款金额">
              <el-input-number v-model="form.loanInfo.loanAmount" :precision="2" :step="10000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="贷款余额">
              <el-input-number v-model="form.loanInfo.loanBalance" :precision="2" :step="10000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="贷款类型">
              <el-select v-model="form.loanInfo.loanType" placeholder="请选择">
                <el-option label="经营贷" value="经营贷" />
                <el-option label="消费贷" value="消费贷" />
                <el-option label="抵押贷" value="抵押贷" />
                <el-option label="信用贷" value="信用贷" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="利率(%)">
              <el-input-number v-model="form.loanInfo.interestRate" :precision="2" :step="0.5" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="放款日期">
              <el-date-picker v-model="form.loanInfo.loanStartDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期">
              <el-date-picker v-model="form.loanInfo.loanEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">还款信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="总期数">
              <el-input-number v-model="form.repaymentInfo.totalTerms" :min="1" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="已完成期数">
              <el-input-number v-model="form.repaymentInfo.completedTerms" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="准时次数">
              <el-input-number v-model="form.repaymentInfo.ontimeCount" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="逾期次数">
              <el-input-number v-model="form.repaymentInfo.overdueCount" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大逾期天数">
              <el-input-number v-model="form.repaymentInfo.maxOverdueDays" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前逾期天数">
              <el-input-number v-model="form.repaymentInfo.currentOverdueDays" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">财务信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="月收入">
              <el-input-number v-model="form.financialInfo.monthlyIncome" :precision="2" :step="1000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="收入变化率">
              <el-input-number v-model="form.financialInfo.monthlyIncomeChange" :precision="2" :step="0.05" :min="-1" :max="1" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="负债率">
              <el-input-number v-model="form.financialInfo.debtRatio" :precision="2" :step="0.05" :min="0" :max="1" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="负债率变化">
              <el-input-number v-model="form.financialInfo.debtRatioChange" :precision="2" :step="0.05" :min="-1" :max="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="现金流状态">
              <el-select v-model="form.financialInfo.cashFlowStatus" placeholder="请选择">
                <el-option label="正常" value="正常" />
                <el-option label="良好" value="良好" />
                <el-option label="偏紧" value="偏紧" />
                <el-option label="紧张" value="紧张" />
                <el-option label="恶化" value="恶化" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">风险信号</el-divider>
        <el-row :gutter="20">
          <el-col :span="5">
            <el-form-item label="多头借贷">
              <el-switch v-model="form.riskSignals.multiLending" />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="担保链异常">
              <el-switch v-model="form.riskSignals.guaranteeChainAbnormal" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="诉讼记录">
              <el-switch v-model="form.riskSignals.litigationRecord" />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="经营异常">
              <el-switch v-model="form.riskSignals.businessAbnormal" />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="资产转移">
              <el-switch v-model="form.riskSignals.assetTransfer" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">外部数据（选填）</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="信用评分">
              <el-input-number v-model="externalData.creditScore" :min="0" :max="1000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="信用分变化">
              <el-input-number v-model="externalData.creditScoreChange" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="涉诉次数">
              <el-input-number v-model="externalData.courtFilingCount" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="submitData" :loading="loading">提交并触发分析</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <el-dialog v-model="showResult" title="任务已提交" width="500px">
        <el-result icon="info" title="分析任务已提交" sub-title="智能体正在异步执行，请稍后查看进度">
          <template #extra>
            <el-descriptions :column="1" border style="margin-bottom:16px">
              <el-descriptions-item label="任务ID">{{ result.taskId }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusType(result.status)">{{ result.status }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <el-button type="primary" @click="goTaskDetail">查看任务进度</el-button>
            <el-button @click="showResult = false">继续提交</el-button>
          </template>
        </el-result>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { agentApi, isAuthError } from '../api'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['viewTask'])

const loading = ref(false)
const showResult = ref(false)
const result = reactive({})

const form = reactive({
  borrowerId: '', borrowerName: '', loanId: '', reportPeriod: '',
  loanInfo: { loanAmount: 0, loanBalance: 0, loanType: '', loanStartDate: '', loanEndDate: '', interestRate: 0 },
  repaymentInfo: { totalTerms: 12, completedTerms: 0, ontimeCount: 0, overdueCount: 0, maxOverdueDays: 0, currentOverdueDays: 0 },
  financialInfo: { monthlyIncome: 0, monthlyIncomeChange: 0, debtRatio: 0, debtRatioChange: 0, cashFlowStatus: '正常' },
  riskSignals: { multiLending: false, guaranteeChainAbnormal: false, litigationRecord: false, businessAbnormal: false, assetTransfer: false },
})
const externalData = reactive({ creditScore: 0, creditScoreChange: 0, courtFilingCount: 0 })

function fillSampleData() {
  Object.assign(form, {
    borrowerId: 'BR20260001', borrowerName: '张三', loanId: 'LN20260001', reportPeriod: '2026-09',
    loanInfo: { loanAmount: 500000, loanBalance: 320000, loanType: '经营贷', loanStartDate: '2026-01-15', loanEndDate: '2027-01-14', interestRate: 5.85 },
    repaymentInfo: { totalTerms: 12, completedTerms: 8, ontimeCount: 7, overdueCount: 1, maxOverdueDays: 5, currentOverdueDays: 0 },
    financialInfo: { monthlyIncome: 35000, monthlyIncomeChange: -0.08, debtRatio: 0.42, debtRatioChange: 0.05, cashFlowStatus: '正常' },
    riskSignals: { multiLending: false, guaranteeChainAbnormal: false, litigationRecord: false, businessAbnormal: false, assetTransfer: false },
  })
  Object.assign(externalData, { creditScore: 720, creditScoreChange: -10, courtFilingCount: 0 })
}

function submitData() {
  if (!form.borrowerId || !form.borrowerName || !form.loanId || !form.reportPeriod) {
    ElMessage.warning('请填写基本信息')
    return
  }
  const data = { ...form }
  if (externalData.creditScore > 0) data.externalData = { ...externalData }
  loading.value = true
  agentApi.ingest(data).then(res => {
    Object.assign(result, res.data)
    showResult.value = true
    ElMessage.success('任务已提交，正在异步执行')
  }).catch(err => {
    // 401/403 已由 axios 拦截器统一提示，这里不重复弹窗
    if (!isAuthError(err)) ElMessage.error(err.response?.data?.message || '提交失败')
  }).finally(() => {
    loading.value = false
  })
}

function goTaskDetail() {
  if (!result.taskId) return
  showResult.value = false
  emit('viewTask', result.taskId)
}

function resetForm() {
  Object.assign(form, {
    borrowerId: '', borrowerName: '', loanId: '', reportPeriod: '',
    loanInfo: { loanAmount: 0, loanBalance: 0, loanType: '', loanStartDate: '', loanEndDate: '', interestRate: 0 },
    repaymentInfo: { totalTerms: 12, completedTerms: 0, ontimeCount: 0, overdueCount: 0, maxOverdueDays: 0, currentOverdueDays: 0 },
    financialInfo: { monthlyIncome: 0, monthlyIncomeChange: 0, debtRatio: 0, debtRatioChange: 0, cashFlowStatus: '正常' },
    riskSignals: { multiLending: false, guaranteeChainAbnormal: false, litigationRecord: false, businessAbnormal: false, assetTransfer: false },
  })
}

function statusType(s) {
  return { COMPLETED: 'success', RUNNING: 'warning', PENDING: 'info', FAILED: 'danger' }[s] || 'info'
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
