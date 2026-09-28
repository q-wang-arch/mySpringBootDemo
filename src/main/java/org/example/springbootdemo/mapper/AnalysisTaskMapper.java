package org.example.springbootdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springbootdemo.entity.AnalysisTask;

@Mapper
public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask> {
}
