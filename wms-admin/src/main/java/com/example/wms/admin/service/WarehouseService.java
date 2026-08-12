package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.mapper.WarehouseAreaMapper;
import com.example.wms.admin.model.mapper.WarehouseMapper;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseEnabledRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseRequest;
import com.example.wms.admin.view.dto.WarehouseQuery;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final WarehouseAreaMapper warehouseAreaMapper;

    public WarehouseService(WarehouseMapper warehouseMapper, WarehouseAreaMapper warehouseAreaMapper) {
        this.warehouseMapper = warehouseMapper;
        this.warehouseAreaMapper = warehouseAreaMapper;
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

    @Transactional
    public WarehouseResponse update(UpdateWarehouseRequest request) {
        getById(request.id());
        LambdaUpdateWrapper<Warehouse> updateWrapper = Wrappers.lambdaUpdate(Warehouse.class)
                .eq(Warehouse::getId, request.id())
                .set(Warehouse::getName, request.name())
                .set(Warehouse::getAddress, request.address());
        warehouseMapper.update(updateWrapper);
        return WarehouseResponse.from(getById(request.id()));
    }

    @Transactional
    public void changeEnabled(UpdateWarehouseEnabledRequest request) {
        getById(request.id());
        if (!request.enabled()) {
            ensureNoActiveAreas(request.id());
        }
        LambdaUpdateWrapper<Warehouse> updateWrapper = Wrappers.lambdaUpdate(Warehouse.class)
                .eq(Warehouse::getId, request.id())
                .set(Warehouse::isEnabled, request.enabled());
        warehouseMapper.update(updateWrapper);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        ensureNoActiveAreas(id);
        warehouseMapper.deleteById(id);
    }

    // 仓库不是叶子实体：warehouse_areas.warehouse_id 没有 DB 外键约束（这个 schema 里没用外键，
    // 见 V1__init_schema.sql），删除/禁用一个还挂着库区的仓库不会被数据库拦下来，只会留下指向
    // 不存在仓库的孤儿库区记录。照抄 WarehouseAreaService.ensureNoActiveChildren 的思路在这里也
    // 挡一道，customer/sku 没有这个检查是因为它们本来就是叶子实体，没有子级需要保护。
    private void ensureNoActiveAreas(Long warehouseId) {
        long areaCount = warehouseAreaMapper.selectCount(Wrappers.lambdaQuery(WarehouseArea.class)
                .eq(WarehouseArea::getWarehouseId, warehouseId));
        if (areaCount > 0) {
            throw new BusinessException("warehouse still has warehouse areas, cannot disable or delete");
        }
    }
}
