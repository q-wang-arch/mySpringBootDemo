package org.example.springbootdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.auth.RequireRoles;
import org.example.springbootdemo.auth.Roles;
import org.example.springbootdemo.dto.ApiResponse;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.dto.PageQuery;
import org.example.springbootdemo.entity.AnalysisTask;
import org.example.springbootdemo.entity.TaskStepLog;
import org.example.springbootdemo.mapper.AnalysisTaskMapper;
import org.example.springbootdemo.mapper.TaskStepLogMapper;
import org.example.springbootdemo.service.AgentIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 贷后智能体 - 数据接入与任务查询接口
 */
@RestController
@RequestMapping("/api/agent")
// 类级默认约束：任务查询类接口仅对内部三类角色开放，"接口调用方"不在其列
@RequireRoles({Roles.RISK_ADMIN, Roles.RISK_APPROVER, Roles.SYS_ADMIN})
public class AgentController {

    @Autowired
    private AgentIngestService agentIngestService;

    @Autowired
    private AnalysisTaskMapper analysisTaskMapper;

    @Autowired
    private TaskStepLogMapper taskStepLogMapper;

    /**
     * POST /api/agent/ingest
     * 接收源头系统推送的贷后数据，触发智能体分析流程
     *
     * <p>方法级注解覆盖类级约束：本接口额外允许"接口调用方"（源头系统），
     * 以 API_CLIENT 身份通过 X-Auth-Token 或 Bearer 令牌调用。
     */
    @PostMapping("/ingest")
    @RequireRoles({Roles.API_CLIENT, Roles.RISK_ADMIN, Roles.SYS_ADMIN})
    public ApiResponse ingest(@RequestBody BorrowerData data) {
        try {
            String taskId = agentIngestService.ingest(data);
            // 异步模式：分析在线程池中执行，接口立即返回 PENDING 状态
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "PENDING");
            result.put("message", "分析任务已创建，正在异步执行");
            result.put("queryUrl", "/api/agent/task/" + taskId);
            return ApiResponse.success("数据已接收，分析任务已提交异步执行", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(409, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.serverError("数据接入失败：" + e.getMessage());
        }
    }

    /**
     * GET /api/agent/task/{taskId}
     * 查询分析任务状态
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse getTask(@PathVariable String taskId) {
        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>().eq(AnalysisTask::getTaskId, taskId));
        if (task == null) {
            return ApiResponse.error(404, "任务不存在: " + taskId);
        }

        // 查询步骤日志
        List<TaskStepLog> logs = taskStepLogMapper.selectList(
                new LambdaQueryWrapper<TaskStepLog>()
                        .eq(TaskStepLog::getTaskId, taskId)
                        .orderByAsc(TaskStepLog::getStepOrder));

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("borrowerId", task.getBorrowerId());
        result.put("status", task.getStatus());
        result.put("currentStep", task.getCurrentStep());
        result.put("riskScore", task.getRiskScore());
        result.put("riskGrade", task.getRiskGrade());
        result.put("reportId", task.getReportId());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());
        result.put("steps", logs);

        return ApiResponse.success("查询成功", result);
    }

    /**
     * GET /api/agent/tasks?borrowerId=&status=&page=1&size=20
     * 查询任务列表
     */
    @GetMapping("/tasks")
    public ApiResponse listTasks(
            @RequestParam(required = false) String borrowerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageQuery pageQuery = PageQuery.of(page, size);

        LambdaQueryWrapper<AnalysisTask> listWrapper = taskFilter(borrowerId, status);
        listWrapper.orderByDesc(AnalysisTask::getCreateTime);
        listWrapper.last(pageQuery.toLimitClause());
        List<AnalysisTask> records = analysisTaskMapper.selectList(listWrapper);

        // count 复用与 list 同一份过滤条件，避免两处条件写不一致导致 total 与 records 对不上
        long total = analysisTaskMapper.selectCount(taskFilter(borrowerId, status));

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", pageQuery.getPage());
        result.put("size", pageQuery.getSize());
        result.put("records", records);

        return ApiResponse.success("查询成功", result);
    }

    /**
     * 任务列表的过滤条件——list 与 count 共用的<b>唯一来源</b>。
     *
     * <p>只放 where 条件，不要在这里加 order by 或 LIMIT：count 查询会复用本方法，
     * 带上排序或分页会让总数算错。
     *
     * <p>用 {@code StringUtils.hasText} 而不是 {@code != null && !isEmpty()}，
     * 空白串（"  "）也视为未传，避免拼出 {@code borrower_id = '  '} 这种永远查不到的查询。
     */
    private LambdaQueryWrapper<AnalysisTask> taskFilter(String borrowerId, String status) {
        return new LambdaQueryWrapper<AnalysisTask>()
                .eq(StringUtils.hasText(borrowerId), AnalysisTask::getBorrowerId, borrowerId)
                .eq(StringUtils.hasText(status), AnalysisTask::getStatus, status);
    }
}

