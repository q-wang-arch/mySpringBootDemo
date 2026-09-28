package org.example.springbootdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springbootdemo.entity.Alert;

@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
}
