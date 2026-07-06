package com.example.wms.admin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.admin.model.entity.WarehouseLocation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface WarehouseLocationMapper extends BaseMapper<WarehouseLocation> {

    @Update("update warehouse_locations set used_qty = used_qty + #{qty}, updated_at = now() " +
            "where id = #{id} and status = 'ENABLED' and used_qty + #{qty} <= capacity_qty")
    int tryOccupy(@Param("id") Long id, @Param("qty") int qty);

    @Update("update warehouse_locations set used_qty = used_qty - #{qty}, updated_at = now() " +
            "where id = #{id} and used_qty >= #{qty}")
    int release(@Param("id") Long id, @Param("qty") int qty);
}
