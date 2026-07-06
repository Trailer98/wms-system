package com.example.wms.admin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.admin.model.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface InventoryMapper extends BaseMapper<Inventory> {

    @Update("update inventory set reserved_quantity = reserved_quantity + #{qty}, updated_at = now() " +
            "where id = #{id} and quantity - reserved_quantity - frozen_quantity >= #{qty}")
    int tryLock(@Param("id") Long id, @Param("qty") int qty);

    @Update("update inventory set reserved_quantity = reserved_quantity - #{qty}, updated_at = now() " +
            "where id = #{id} and reserved_quantity >= #{qty}")
    int releaseLock(@Param("id") Long id, @Param("qty") int qty);

    @Update("update inventory set quantity = quantity - #{qty}, reserved_quantity = reserved_quantity - #{qty}, updated_at = now() " +
            "where id = #{id} and quantity >= #{qty} and reserved_quantity >= #{qty}")
    int confirmShip(@Param("id") Long id, @Param("qty") int qty);
}
