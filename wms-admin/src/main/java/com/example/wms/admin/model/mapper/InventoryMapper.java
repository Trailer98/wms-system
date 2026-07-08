package com.example.wms.admin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.admin.model.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Update("update inventory set quantity = quantity - #{qty}, updated_at = now() " +
            "where id = #{id} and quantity - #{qty} >= reserved_quantity + frozen_quantity")
    int decreaseOnHand(@Param("id") Long id, @Param("qty") int qty);

    @Update("update inventory set quantity = quantity + #{qty}, updated_at = now() where id = #{id}")
    int increaseOnHand(@Param("id") Long id, @Param("qty") int qty);

    @Update("update inventory set frozen_quantity = frozen_quantity + #{qty}, updated_at = now() where id = #{id}")
    int increaseFrozen(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Locks the row for the rest of the caller's transaction. Used so an audit "before" snapshot is
     * always read after the row is locked, never from an unlocked read that a concurrent transaction
     * could invalidate before this transaction's own UPDATE runs.
     */
    @Select("select * from inventory where id = #{id} and enabled = 1 for update")
    Inventory selectByIdForUpdate(@Param("id") Long id);

    @Select("select * from inventory where warehouse_id = #{warehouseId} and sku_id = #{skuId} " +
            "and area_id = #{areaId} and location_id = #{locationId} and enabled = 1 for update")
    Inventory selectByDimensionForUpdate(@Param("warehouseId") Long warehouseId, @Param("skuId") Long skuId,
            @Param("areaId") Long areaId, @Param("locationId") Long locationId);
}
