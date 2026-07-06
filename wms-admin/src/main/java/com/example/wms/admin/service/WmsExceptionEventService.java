package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.WmsExceptionEvent;
import com.example.wms.admin.model.mapper.WmsExceptionEventMapper;
import com.example.wms.admin.view.dto.WmsExceptionEventQuery;
import com.example.wms.admin.view.dto.WmsExceptionEventResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WmsExceptionEventService {

    private final WmsExceptionEventMapper wmsExceptionEventMapper;
    private final SkuService skuService;
    private final WarehouseService warehouseService;
    private final WarehouseAreaService warehouseAreaService;
    private final WarehouseLocationService warehouseLocationService;

    public WmsExceptionEventService(
            WmsExceptionEventMapper wmsExceptionEventMapper,
            SkuService skuService,
            WarehouseService warehouseService,
            WarehouseAreaService warehouseAreaService,
            WarehouseLocationService warehouseLocationService
    ) {
        this.wmsExceptionEventMapper = wmsExceptionEventMapper;
        this.skuService = skuService;
        this.warehouseService = warehouseService;
        this.warehouseAreaService = warehouseAreaService;
        this.warehouseLocationService = warehouseLocationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ExceptionType exceptionType, String bizNo, Long skuId, Long warehouseId, Long areaId, Long locationId, String message) {
        wmsExceptionEventMapper.insert(new WmsExceptionEvent(exceptionType, bizNo, skuId, warehouseId, areaId, locationId, message));
    }

    @Transactional(readOnly = true)
    public PageResponse<WmsExceptionEventResponse> search(WmsExceptionEventQuery query) {
        Page<WmsExceptionEvent> page = wmsExceptionEventMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(WmsExceptionEvent.class)
                        .eq(query.getExceptionType() != null, WmsExceptionEvent::getExceptionType, query.getExceptionType())
                        .like(StringUtils.hasText(query.getBizNo()), WmsExceptionEvent::getBizNo, query.getBizNo())
                        .eq(query.getSkuId() != null, WmsExceptionEvent::getSkuId, query.getSkuId())
                        .eq(query.getWarehouseId() != null, WmsExceptionEvent::getWarehouseId, query.getWarehouseId())
                        .eq(query.getAreaId() != null, WmsExceptionEvent::getAreaId, query.getAreaId())
                        .eq(query.getLocationId() != null, WmsExceptionEvent::getLocationId, query.getLocationId())
                        .eq(StringUtils.hasText(query.getStatus()), WmsExceptionEvent::getStatus, query.getStatus())
                        .ge(query.getStartTime() != null, WmsExceptionEvent::getCreateTime, query.getStartTime())
                        .le(query.getEndTime() != null, WmsExceptionEvent::getCreateTime, query.getEndTime())
                        .orderByDesc(WmsExceptionEvent::getCreateTime)
        );

        return PageResponse.from(page, event -> WmsExceptionEventResponse.from(assemble(event)));
    }

    @Transactional(readOnly = true)
    public WmsExceptionEventResponse getDetail(Long id) {
        return WmsExceptionEventResponse.from(assemble(getById(id)));
    }

    @Transactional
    public WmsExceptionEventResponse markHandled(Long id) {
        WmsExceptionEvent event = getById(id);
        if (WmsExceptionEvent.STATUS_HANDLED.equals(event.getStatus())) {
            throw new BusinessException("exception event has already been handled");
        }
        event.markHandled();
        wmsExceptionEventMapper.updateById(event);
        return WmsExceptionEventResponse.from(assemble(event));
    }

    private WmsExceptionEvent getById(Long id) {
        WmsExceptionEvent event = wmsExceptionEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException("exception event not found");
        }
        return event;
    }

    private WmsExceptionEvent assemble(WmsExceptionEvent event) {
        if (event.getSkuId() != null) {
            event.attachSku(skuService.getById(event.getSkuId()));
        }
        if (event.getWarehouseId() != null) {
            event.attachWarehouse(warehouseService.getById(event.getWarehouseId()));
        }
        if (event.getAreaId() != null) {
            event.attachArea(warehouseAreaService.getById(event.getAreaId()));
        }
        if (event.getLocationId() != null) {
            event.attachLocation(warehouseLocationService.getById(event.getLocationId()));
        }
        return event;
    }
}
