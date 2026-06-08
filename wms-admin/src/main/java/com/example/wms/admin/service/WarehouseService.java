package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.mapper.WarehouseMapper;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.WarehouseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        if (warehouseMapper.selectCount(Wrappers.lambdaQuery(Warehouse.class).eq(Warehouse::getCode, request.code())) > 0) {
            throw new BusinessException("warehouse code already exists");
        }
        Warehouse warehouse = new Warehouse(request.code(), request.name(), request.address());
        warehouseMapper.insert(warehouse);
        return WarehouseResponse.from(warehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> list() {
        return warehouseMapper.selectList(Wrappers.lambdaQuery(Warehouse.class).orderByAsc(Warehouse::getCode)).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Warehouse getById(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException("warehouse not found");
        }
        return warehouse;
    }
}
