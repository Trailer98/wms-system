package com.example.wms.admin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.admin.model.entity.BizSequence;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BizSequenceMapper extends BaseMapper<BizSequence> {

    @Select("select * from biz_sequence where biz_type = #{bizType} and seq_date = #{seqDate} for update")
    BizSequence selectForUpdate(@Param("bizType") String bizType, @Param("seqDate") String seqDate);
}
