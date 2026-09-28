<template>
  <div class="report-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ report.reportId ? '报告详情 - ' + report.reportId : '报告列表' }}</span>
          <el-button v-if="report.reportId" @click="backToList">返回列表</el-button>
        </div>
      </template>

      <!-- 报告列表 -->
      <div v-if="!report.reportId" v-loading="loading">
        <el-table :data="tableData" stripe>
          <el-table-column prop="reportId" label="报告ID" width="200" show-overflow-tooltip />
          <el-table-column prop="borrowerName" label="借款人" width="100" />
          <el-table-column prop="loanId" label="贷款编号" width="140" />
          <el-table-column prop="reportPeriod" label="报告期次" width="100" />
          <el-table-column prop="riskScore" label="风险评分" width="100">
            <template #default="{ row }">
              <span style="font-weight:bold;color:#409EFF">{{ row.riskScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="riskGrade" label="评级" width="80">
            <template #default="{ row }">
              <el-tag :type="gradeType(row.riskGrade)" size="small">{{ row.riskGrade }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="previousGrade" label="上期评级" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.previousGrade" size="small" type="info">{{ row.previousGrade }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" width="160" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="viewReport(row.reportId)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="page.page"
          :total="page.total"
          :page-size="20"
          layout="total, prev, pager, next"
          style="margin-top:16px;justify-content:flex-end;display:flex"
          @current-change="loadList"
        />
      </div>

      <!-- 报告详情 -->
      <div v-else v-loading="detailLoading">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="借款人">{{ report.borrowerName }}</el-descriptions-item>
          <el-descriptions-item label="报告期次">{{ report.reportPeriod }}</el-descriptions-item>
          <el-descriptions-item label="风险评分">
            <span style="font-size:18px;font-weight:bold;color:#409EFF">{{ report.riskScore }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="风险评级">
            <el-tag :type="gradeType(report.riskGrade)" size="large">{{ report.riskGrade }}级</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="还款评分">{{ report.repaymentScore || '-' }}</el-descriptions-item>
          <el-descriptions-item label="财务评分">{{ report.financialScore || '-' }}</el-descriptions-item>
          <el-descriptions-item label="风险扣分">{{ report.riskSignalDeduction || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上期评级">{{ report.previousGrade || '首次' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">分析结论</el-divider>
        <el-alert :type="alertType(report.riskGrade)" :title="report.conclusion" show-icon :closable="false" />

        <el-divider content-position="left">建议措施</el-divider>
        <el-timeline v-if="report.suggestions">
          <el-timeline-item v-for="(s, i) in report.suggestions.split(';')" :key="i" type="primary">
            {{ s }}
          </el-timeline-item>
        </el-timeline>

        <el-divider content-position="left">报告完整内容</el-divider>
        <el-tabs>
          <el-tab-pane label="格式化展示">
            <div v-if="reportContent" class="report-content">
              <el-card v-for="(section, key) in reportContent" :key="key" style="margin-bottom:12px">
                <template #header>
                  <span style="font-weight:bold">{{ sectionTitle[key] || key }}</span>
                </template>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item v-for="(val, k) in section" :key="k" :label="fieldLabel(k)">
                    <span v-if="typeof val === 'object'">{{ JSON.stringify(val) }}</span>
                    <span v-else>{{ val }}</span>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </div>
            <el-empty v-else description="无内容" />
          </el-tab-pane>
          <el-tab-pane label="原始JSON">
            <pre style="background:#f5f7fa;padding:12px;border-radius:4px;overflow-x:auto">{{ JSON.stringify(reportContent, null, 2) }}</pre>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { agentApi } from '../api'

const loading = ref(false)
const detailLoading = ref(false)
const tableData = ref([])
const report = ref({})
const page = reactive({ page: 1, total: 0 })

const sectionTitle = {
  borrowerOverview: '一、借款人概况',
  repaymentAnalysis: '二、还款行为分析',
  financialAnalysis: '三、财务状况分析',
  riskSignals: '四、风险信号',
  externalData: '五、外部数据',
  riskRating: '六、风险评级'
}

const reportContent = computed(() => {
  if (!report.value.reportContent) return null
  try { return JSON.parse(report.value.reportContent) } catch { return null }
})

function fieldLabel(k) {
  const map = {
    borrowerName: '借款人', borrowerId: '借款人ID', loanId: '贷款编号',
    loanAmount: '贷款金额', loanBalance: '贷款余额', loanType: '类型',
    loanStartDate: '放款日', loanEndDate: '到期日',
    totalTerms: '总期数', completedTerms: '已完成', ontimeCount: '准时次数',
    overdueCount: '逾期次数', maxOverdueDays: '最大逾期天数', currentOverdueDays: '当前逾期天数',
    score: '评分', comment: '评语',
    monthlyIncome: '月收入', incomeChange: '收入变化', debtRatio: '负债率',
    debtRatioChange: '负债率变化', cashFlowStatus: '现金流',
    multiLending: '多头借贷', guaranteeChainAbnormal: '担保链异常',
    litigationRecord: '诉讼记录', businessAbnormal: '经营异常', assetTransfer: '资产转移',
    deduction: '扣分',
    creditScore: '信用评分', creditScoreChange: '信用分变化', courtFilingCount: '涉诉次数', taxArrears: '欠税',
    riskScore: '综合评分', riskGrade: '评级', previousGrade: '上期评级',
    conclusion: '结论', suggestions: '建议'
  }
  return map[k] || k
}

function loadList() {
  loading.value = true
  agentApi.listReports({ page: page.page, size: 20 }).then(res => {
    tableData.value = res.data?.records || []
    page.total = res.data?.total || 0
  }).catch(() => { tableData.value = [] }).finally(() => { loading.value = false })
}

function viewReport(reportId) {
  detailLoading.value = true
  agentApi.getReport(reportId).then(res => {
    report.value = res.data
  }).finally(() => { detailLoading.value = false })
}

function backToList() {
  report.value = {}
  loadList()
}

function gradeType(g) {
  return { A: 'success', B: '', C: 'warning', D: 'danger', E: 'danger' }[g] || 'info'
}
function alertType(g) {
  return { A: 'success', B: 'success', C: 'warning', D: 'error', E: 'error' }[g] || 'info'
}

onMounted(() => loadList())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
