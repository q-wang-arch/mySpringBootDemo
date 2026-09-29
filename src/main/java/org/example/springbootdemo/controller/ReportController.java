package org.example.springbootdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.auth.RequireRoles;
import org.example.springbootdemo.auth.Roles;
import org.example.springbootdemo.dto.ApiResponse;
import org.example.springbootdemo.dto.PageQuery;
import org.example.springbootdemo.entity.Report;
import org.example.springbootdemo.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分析报告查询接口
 */
@RestController
@RequestMapping("/api/report")
@RequireRoles({Roles.RISK_ADMIN, Roles.RISK_APPROVER, Roles.SYS_ADMIN})
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

        PageQuery pageQuery = PageQuery.of(page, size);

        LambdaQueryWrapper<Report> listWrapper = reportFilter(borrowerId, riskGrade, reportPeriod);
        listWrapper.orderByDesc(Report::getCreateTime);
        listWrapper.last(pageQuery.toLimitClause());
        List<Report> records = reportMapper.selectList(listWrapper);

        // count 复用与 list 同一份过滤条件，避免两处条件写不一致导致 total 与 records 对不上
        long total = reportMapper.selectCount(reportFilter(borrowerId, riskGrade, reportPeriod));

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", pageQuery.getPage());
        result.put("size", pageQuery.getSize());
        result.put("records", records);

        return ApiResponse.success("查询成功", result);
    }

    /**
     * 报告列表的过滤条件——list 与 count 共用的<b>唯一来源</b>。
     *
     * <p>只放 where 条件，不要在这里加 order by 或 LIMIT：count 查询会复用本方法。
     */
    private LambdaQueryWrapper<Report> reportFilter(String borrowerId, String riskGrade, String reportPeriod) {
        return new LambdaQueryWrapper<Report>()
                .eq(StringUtils.hasText(borrowerId), Report::getBorrowerId, borrowerId)
                .eq(StringUtils.hasText(riskGrade), Report::getRiskGrade, riskGrade)
                .eq(StringUtils.hasText(reportPeriod), Report::getReportPeriod, reportPeriod);
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
