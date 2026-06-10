package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.mapper.WarehouseMapper;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.WarehouseQuery;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public PageResponse<WarehouseResponse> search(WarehouseQuery query) {
        Page<Warehouse> page = warehouseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Warehouse.class)
                        .like(StringUtils.hasText(query.getCode()), Warehouse::getCode, query.getCode())
                        .like(StringUtils.hasText(query.getName()), Warehouse::getName, query.getName())
                        .orderByAsc(Warehouse::getCode)
        );

        return PageResponse.from(page, WarehouseResponse::from);
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
