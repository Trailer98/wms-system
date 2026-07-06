package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.Inventory;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.admin.model.mapper.InventoryMapper;
import com.example.wms.admin.model.mapper.WarehouseAreaMapper;
import com.example.wms.admin.model.mapper.WarehouseLocationMapper;
import com.example.wms.admin.view.dto.CreateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.WarehouseAreaQuery;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AreaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WarehouseAreaService {

    private final WarehouseAreaMapper warehouseAreaMapper;
    private final WarehouseLocationMapper warehouseLocationMapper;
    private final InventoryMapper inventoryMapper;
    private final WarehouseService warehouseService;

    public WarehouseAreaService(
            WarehouseAreaMapper warehouseAreaMapper,
            WarehouseLocationMapper warehouseLocationMapper,
            InventoryMapper inventoryMapper,
            WarehouseService warehouseService
    ) {
        this.warehouseAreaMapper = warehouseAreaMapper;
        this.warehouseLocationMapper = warehouseLocationMapper;
        this.inventoryMapper = inventoryMapper;
        this.warehouseService = warehouseService;
    }

    @Transactional
    public WarehouseAreaResponse create(CreateWarehouseAreaRequest request) {
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        ensureAreaCodeUnique(warehouse.getId(), request.areaCode(), null);

        WarehouseArea area = new WarehouseArea(
                warehouse,
                request.areaCode(),
                request.areaName(),
                request.areaType(),
                request.pickPriority(),
                request.remark()
        );
        warehouseAreaMapper.insert(area);
        return WarehouseAreaResponse.from(area);
    }

    @Transactional
    public WarehouseAreaResponse update(Long id, UpdateWarehouseAreaRequest request) {
        WarehouseArea area = assemble(getById(id));
        area.update(request.areaName(), request.areaType(), request.pickPriority(), request.remark());
        warehouseAreaMapper.updateById(area);
        return WarehouseAreaResponse.from(area);
    }

    @Transactional
    public WarehouseAreaResponse updateStatus(Long id, UpdateStatusRequest request) {
        AreaStatus status = parseStatus(request.status());
        WarehouseArea area = assemble(getById(id));
        if (status == AreaStatus.DISABLED) {
            ensureNoActiveChildren(id);
        }
        area.changeStatus(status);
        warehouseAreaMapper.updateById(area);
        return WarehouseAreaResponse.from(area);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        ensureNoActiveChildren(id);
        warehouseAreaMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseAreaResponse> search(WarehouseAreaQuery query) {
        Page<WarehouseArea> page = warehouseAreaMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(WarehouseArea.class)
                        .eq(query.getWarehouseId() != null, WarehouseArea::getWarehouseId, query.getWarehouseId())
                        .like(StringUtils.hasText(query.getAreaCode()), WarehouseArea::getAreaCode, query.getAreaCode())
                        .like(StringUtils.hasText(query.getAreaName()), WarehouseArea::getAreaName, query.getAreaName())
                        .eq(query.getAreaType() != null, WarehouseArea::getAreaType, query.getAreaType())
                        .eq(query.getStatus() != null, WarehouseArea::getStatus, query.getStatus())
                        .orderByAsc(WarehouseArea::getWarehouseId, WarehouseArea::getAreaCode)
        );

        return PageResponse.from(page, area -> WarehouseAreaResponse.from(assemble(area)));
    }

    @Transactional(readOnly = true)
    public List<WarehouseAreaResponse> listByWarehouse(Long warehouseId) {
        List<WarehouseArea> areas = warehouseAreaMapper.selectList(
                Wrappers.lambdaQuery(WarehouseArea.class)
                        .eq(WarehouseArea::getWarehouseId, warehouseId)
                        .orderByAsc(WarehouseArea::getPickPriority, WarehouseArea::getAreaCode)
        );
        return areas.stream().map(area -> WarehouseAreaResponse.from(assemble(area))).toList();
    }

    @Transactional(readOnly = true)
    public WarehouseAreaResponse getDetail(Long id) {
        return WarehouseAreaResponse.from(assemble(getById(id)));
    }

    @Transactional(readOnly = true)
    public WarehouseArea getById(Long id) {
        WarehouseArea area = warehouseAreaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException("warehouse area not found");
        }
        return area;
    }

    private void ensureAreaCodeUnique(Long warehouseId, String areaCode, Long excludeId) {
        long count = warehouseAreaMapper.selectCount(Wrappers.lambdaQuery(WarehouseArea.class)
                .eq(WarehouseArea::getWarehouseId, warehouseId)
                .eq(WarehouseArea::getAreaCode, areaCode)
                .ne(excludeId != null, WarehouseArea::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("area code already exists in this warehouse");
        }
    }

    private void ensureNoActiveChildren(Long areaId) {
        long locationCount = warehouseLocationMapper.selectCount(Wrappers.lambdaQuery(WarehouseLocation.class)
                .eq(WarehouseLocation::getAreaId, areaId));
        if (locationCount > 0) {
            throw new BusinessException("area still has active locations, cannot disable or delete");
        }
        long inventoryCount = inventoryMapper.selectCount(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getAreaId, areaId)
                .gt(Inventory::getQuantity, 0));
        if (inventoryCount > 0) {
            throw new BusinessException("area still has inventory, cannot disable or delete");
        }
    }

    private AreaStatus parseStatus(String status) {
        try {
            return AreaStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("invalid area status: " + status);
        }
    }

    private WarehouseArea assemble(WarehouseArea area) {
        area.attachWarehouse(warehouseService.getById(area.getWarehouseId()));
        return area;
    }
}
