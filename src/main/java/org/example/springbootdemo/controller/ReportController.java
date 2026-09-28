package org.example.springbootdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.dto.ApiResponse;
import org.example.springbootdemo.entity.Report;
import org.example.springbootdemo.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分析报告查询接口
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportMapper reportMapper;

    /**
     * GET /api/report/list?borrowerId=&riskGrade=&page=1&size=20
     * 报告列表（按生成时间倒序）
     */
    @GetMapping("/list")
    public ApiResponse list(
            @RequestParam(required = false) String borrowerId,
            @RequestParam(required = false) String riskGrade,
            @RequestParam(required = false) String reportPeriod,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (borrowerId != null && !borrowerId.isEmpty()) {
            wrapper.eq(Report::getBorrowerId, borrowerId);
        }
        if (riskGrade != null && !riskGrade.isEmpty()) {
            wrapper.eq(Report::getRiskGrade, riskGrade);
        }
        if (reportPeriod != null && !reportPeriod.isEmpty()) {
            wrapper.eq(Report::getReportPeriod, reportPeriod);
        }
        wrapper.orderByDesc(Report::getCreateTime);

        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + ", " + size);
        List<Report> records = reportMapper.selectList(wrapper);

        // 总数（不带 LIMIT）
        LambdaQueryWrapper<Report> countWrapper = new LambdaQueryWrapper<>();
        if (borrowerId != null && !borrowerId.isEmpty()) {
            countWrapper.eq(Report::getBorrowerId, borrowerId);
        }
        if (riskGrade != null && !riskGrade.isEmpty()) {
            countWrapper.eq(Report::getRiskGrade, riskGrade);
        }
        if (reportPeriod != null && !reportPeriod.isEmpty()) {
            countWrapper.eq(Report::getReportPeriod, reportPeriod);
        }
        long total = reportMapper.selectCount(countWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("records", records);

        return ApiResponse.success("查询成功", result);
    }

    /**
     * GET /api/report/{reportId}
     * 报告详情
     */
    @GetMapping("/{reportId}")
    public ApiResponse detail(@PathVariable String reportId) {
        Report report = reportMapper.selectOne(
                new LambdaQueryWrapper<Report>().eq(Report::getReportId, reportId));
        if (report == null) {
            return ApiResponse.error(404, "报告不存在: " + reportId);
        }
        return ApiResponse.success("查询成功", report);
    }
}
