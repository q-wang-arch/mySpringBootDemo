<template>
  <div id="app">
    <el-container>
      <el-header style="background:#409EFF;color:#fff;display:flex;align-items:center;justify-content:space-between">
        <span style="font-size:20px;font-weight:bold">贷后智能体管理系统</span>
        <span style="font-size:13px;opacity:0.8">Spring Boot 2.7.18 + Vue3 + Element Plus</span>
      </el-header>

      <el-main>
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="数据接入" name="ingest">
            <DataIngest @viewTask="goTaskDetail" />
          </el-tab-pane>

          <el-tab-pane label="任务列表" name="taskList">
            <TaskList @viewTask="goTaskDetail" />
          </el-tab-pane>

          <el-tab-pane v-if="currentTaskId" label="任务详情" name="taskDetail">
            <TaskDetail :task-id="currentTaskId" @back="backToTaskList" />
          </el-tab-pane>

          <el-tab-pane label="分析报告" name="report">
            <ReportView />
          </el-tab-pane>

          <el-tab-pane label="预警管理" name="alert">
            <AlertView />
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import DataIngest from './views/DataIngest.vue'
import TaskList from './views/TaskList.vue'
import TaskDetail from './views/TaskDetail.vue'
import ReportView from './views/ReportView.vue'
import AlertView from './views/AlertView.vue'

const activeTab = ref('ingest')
const currentTaskId = ref('')

function goTaskDetail(taskId) {
  currentTaskId.value = taskId
  activeTab.value = 'taskDetail'
}

function backToTaskList() {
  currentTaskId.value = ''
  activeTab.value = 'taskList'
}
</script>

<style>
body { margin: 0; }
#app { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; }
.el-header { padding: 0 24px; }
.el-main { padding: 16px; }
</style>
