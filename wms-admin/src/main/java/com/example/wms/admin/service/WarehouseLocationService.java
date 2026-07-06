package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.admin.model.mapper.WarehouseLocationMapper;
import com.example.wms.admin.view.dto.CreateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.WarehouseLocationQuery;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.LocationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WarehouseLocationService {

    private final WarehouseLocationMapper warehouseLocationMapper;
    private final WarehouseService warehouseService;
    private final WarehouseAreaService warehouseAreaService;

    public WarehouseLocationService(
            WarehouseLocationMapper warehouseLocationMapper,
            WarehouseService warehouseService,
            WarehouseAreaService warehouseAreaService
    ) {
        this.warehouseLocationMapper = warehouseLocationMapper;
        this.warehouseService = warehouseService;
        this.warehouseAreaService = warehouseAreaService;
    }

    @Transactional
    public WarehouseLocationResponse create(CreateWarehouseLocationRequest request) {
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        WarehouseArea area = warehouseAreaService.getById(request.areaId());
        ensureAreaBelongsToWarehouse(area, warehouse.getId());
        ensureLocationCodeUnique(warehouse.getId(), request.locationCode(), null);

        WarehouseLocation location = new WarehouseLocation(
                warehouse,
                area,
                request.locationCode(),
                request.locationName(),
                request.locationType(),
                request.capacityQty(),
                request.allowMixedSku(),
                request.pickPriority(),
                request.remark()
        );
        warehouseLocationMapper.insert(location);
        return WarehouseLocationResponse.from(location);
    }

    @Transactional
    public WarehouseLocationResponse update(Long id, UpdateWarehouseLocationRequest request) {
        WarehouseLocation location = assemble(getById(id));
        if (request.capacityQty() < location.getUsedQty()) {
            throw new BusinessException("capacity qty must not be less than used qty");
        }
        location.update(request.locationName(), request.locationType(), request.capacityQty(), request.allowMixedSku(), request.pickPriority(), request.remark());
        warehouseLocationMapper.updateById(location);
        return WarehouseLocationResponse.from(location);
    }

    @Transactional
    public WarehouseLocationResponse updateStatus(Long id, UpdateStatusRequest request) {
        LocationStatus status = parseStatus(request.status());
        WarehouseLocation location = assemble(getById(id));
        if (status == LocationStatus.DISABLED && location.getUsedQty() > 0) {
            throw new BusinessException("location still has inventory, cannot disable");
        }
        location.changeStatus(status);
        warehouseLocationMapper.updateById(location);
        return WarehouseLocationResponse.from(location);
    }

    @Transactional
    public void delete(Long id) {
        WarehouseLocation location = getById(id);
        if (location.getUsedQty() > 0) {
            throw new BusinessException("location still has inventory, cannot delete");
        }
        warehouseLocationMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseLocationResponse> search(WarehouseLocationQuery query) {
        Page<WarehouseLocation> page = warehouseLocationMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(WarehouseLocation.class)
                        .eq(query.getWarehouseId() != null, WarehouseLocation::getWarehouseId, query.getWarehouseId())
                        .eq(query.getAreaId() != null, WarehouseLocation::getAreaId, query.getAreaId())
                        .like(StringUtils.hasText(query.getLocationCode()), WarehouseLocation::getLocationCode, query.getLocationCode())
                        .eq(query.getStatus() != null, WarehouseLocation::getStatus, query.getStatus())
                        .orderByAsc(WarehouseLocation::getWarehouseId, WarehouseLocation::getLocationCode)
        );

        return PageResponse.from(page, location -> WarehouseLocationResponse.from(assemble(location)));
    }

    @Transactional(readOnly = true)
    public List<WarehouseLocationResponse> listByWarehouse(Long warehouseId) {
        return warehouseLocationMapper.selectList(Wrappers.lambdaQuery(WarehouseLocation.class)
                        .eq(WarehouseLocation::getWarehouseId, warehouseId)
                        .orderByAsc(WarehouseLocation::getPickPriority, WarehouseLocation::getLocationCode))
                .stream().map(location -> WarehouseLocationResponse.from(assemble(location))).toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseLocationResponse> listByArea(Long areaId) {
        return warehouseLocationMapper.selectList(Wrappers.lambdaQuery(WarehouseLocation.class)
                        .eq(WarehouseLocation::getAreaId, areaId)
                        .orderByAsc(WarehouseLocation::getPickPriority, WarehouseLocation::getLocationCode))
                .stream().map(location -> WarehouseLocationResponse.from(assemble(location))).toList();
    }

    @Transactional(readOnly = true)
    public WarehouseLocationResponse getDetail(Long id) {
        return WarehouseLocationResponse.from(assemble(getById(id)));
    }

    @Transactional(readOnly = true)
    public WarehouseLocation getById(Long id) {
        WarehouseLocation location = warehouseLocationMapper.selectById(id);
        if (location == null) {
            throw new BusinessException("warehouse location not found");
        }
        return location;
    }

    private void ensureAreaBelongsToWarehouse(WarehouseArea area, Long warehouseId) {
        if (!area.getWarehouseId().equals(warehouseId)) {
            throw new BusinessException("area does not belong to the given warehouse");
        }
    }

    private void ensureLocationCodeUnique(Long warehouseId, String locationCode, Long excludeId) {
        long count = warehouseLocationMapper.selectCount(Wrappers.lambdaQuery(WarehouseLocation.class)
                .eq(WarehouseLocation::getWarehouseId, warehouseId)
                .eq(WarehouseLocation::getLocationCode, locationCode)
                .ne(excludeId != null, WarehouseLocation::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("location code already exists in this warehouse");
        }
    }

    private LocationStatus parseStatus(String status) {
        try {
            return LocationStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("invalid location status: " + status);
        }
    }

    private WarehouseLocation assemble(WarehouseLocation location) {
        location.attachWarehouse(warehouseService.getById(location.getWarehouseId()));
        location.attachArea(warehouseAreaService.getById(location.getAreaId()));
        return location;
    }
}
