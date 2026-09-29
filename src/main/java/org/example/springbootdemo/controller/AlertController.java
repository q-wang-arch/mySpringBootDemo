package org.example.springbootdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.auth.RequireRoles;
import org.example.springbootdemo.auth.Roles;
import org.example.springbootdemo.dto.ApiResponse;
import org.example.springbootdemo.entity.Alert;
import org.example.springbootdemo.mapper.AlertMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警工单查询与处置接口
 */
@RestController
@RequestMapping("/api/alert")
// 查询类接口：内部三类角色均可查看
@RequireRoles({Roles.RISK_ADMIN, Roles.RISK_APPROVER, Roles.SYS_ADMIN})
public class AlertController {

    @Autowired
    private AlertMapper alertMapper;

    /**
     * GET /api/alert/list?borrowerId=&status=&level=&page=1&size=20
     * 预警列表（按触发时间倒序）
     */
    @GetMapping("/list")
    public ApiResponse list(
            @RequestParam(required = false) String borrowerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        if (borrowerId != null && !borrowerId.isEmpty()) {
            wrapper.eq(Alert::getBorrowerId, borrowerId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Alert::getStatus, status);
        }
        if (level != null && !level.isEmpty()) {
            wrapper.eq(Alert::getLevel, level);
        }
        wrapper.orderByDesc(Alert::getCreateTime);

        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + ", " + size);
        List<Alert> records = alertMapper.selectList(wrapper);

        LambdaQueryWrapper<Alert> countWrapper = new LambdaQueryWrapper<>();
        if (borrowerId != null && !borrowerId.isEmpty()) {
            countWrapper.eq(Alert::getBorrowerId, borrowerId);
        }
        if (status != null && !status.isEmpty()) {
            countWrapper.eq(Alert::getStatus, status);
        }
        if (level != null && !level.isEmpty()) {
            countWrapper.eq(Alert::getLevel, level);
        }
        long total = alertMapper.selectCount(countWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);

        return ApiResponse.success("查询成功", result);
    }

    /**
     * POST /api/alert/{alertId}/handle
     * 处置预警工单
     * body: { handler, handleComment }
     *
     * <p>按 docs/03 的角色定义，"处置预警"属于风险管理员；
     * 风险审批人只负责审批处置结果（该接口尚未实现），因此这里用方法级注解把处置权收窄，
     * 审批人调用会得到 403——这不是缺陷，而是授权生效的证据。
     */
    @PostMapping("/{alertId}/handle")
    @RequireRoles({Roles.RISK_ADMIN, Roles.SYS_ADMIN})
    public ApiResponse handle(@PathVariable String alertId, @RequestBody Map<String, String> body) {
        Alert alert = alertMapper.selectOne(
                new LambdaQueryWrapper<Alert>().eq(Alert::getAlertId, alertId));
        if (alert == null) {
            return ApiResponse.error(404, "预警不存在: " + alertId);
        }
        if (!"TRIGGERED".equals(alert.getStatus())) {
            return ApiResponse.badRequest("当前状态不允许处置: " + alert.getStatus());
        }

        String handler = body.get("handler");
        String handleComment = body.get("handleComment");
        if (handler == null || handler.trim().isEmpty()) {
            return ApiResponse.badRequest("处理人不能为空");
        }

        alert.setHandler(handler);
        alert.setHandleComment(handleComment);
        alert.setStatus("HANDLED");
        alert.setHandleTime(LocalDateTime.now());
        alertMapper.updateById(alert);

        return ApiResponse.success("处置成功", alert);
    }
}
