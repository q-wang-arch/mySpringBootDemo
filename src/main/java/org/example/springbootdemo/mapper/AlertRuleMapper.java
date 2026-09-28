package org.example.springbootdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springbootdemo.entity.AlertRule;

@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {
}
