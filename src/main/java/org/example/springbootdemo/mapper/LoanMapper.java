package org.example.springbootdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springbootdemo.entity.Loan;

@Mapper
public interface LoanMapper extends BaseMapper<Loan> {
}
