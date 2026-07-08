package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.InboundOrder;
import com.example.wms.admin.model.entity.InboundOrderItem;
import com.example.wms.common.enums.BizNoType;
import com.example.wms.common.enums.InboundOrderStatus;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.Supplier;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.admin.model.mapper.InboundOrderItemMapper;
import com.example.wms.admin.model.mapper.InboundOrderMapper;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.InboundOrderItemRequest;
import com.example.wms.admin.view.dto.InboundOrderQuery;
import com.example.wms.admin.view.dto.InboundOrderResponse;
import com.example.wms.admin.view.dto.UpdateInboundOrderRequest;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final WarehouseAreaService warehouseAreaService;
    private final WarehouseLocationService warehouseLocationService;
    private final BizNoGeneratorService bizNoGeneratorService;

    public InboundOrderService(
            InboundOrderMapper inboundOrderMapper,
            InboundOrderItemMapper inboundOrderItemMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            InventoryService inventoryService,
            SupplierService supplierService,
            WarehouseAreaService warehouseAreaService,
            WarehouseLocationService warehouseLocationService,
            BizNoGeneratorService bizNoGeneratorService
    ) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.inventoryService = inventoryService;
        this.supplierService = supplierService;
        this.warehouseAreaService = warehouseAreaService;
        this.warehouseLocationService = warehouseLocationService;
        this.bizNoGeneratorService = bizNoGeneratorService;
    }

    /** The system always mints its own order number; any {@code orderNo} the client sends is ignored. */
    @Transactional
    public InboundOrderResponse create(CreateInboundOrderRequest request) {
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        Supplier supplier = request.supplierId() != null ? supplierService.getById(request.supplierId()) : null;
        InboundOrder order = new InboundOrder(bizNoGeneratorService.generate(BizNoType.INBOUND_ORDER), warehouse, supplier);

        addItems(order, warehouse, request.items());

        inboundOrderMapper.insert(order);
        order.getItems().forEach(item -> {
            item.assignOrderId(order.getId());
            inboundOrderItemMapper.insert(item);
        });

        return InboundOrderResponse.from(order);
    }

    @Transactional
    public InboundOrderResponse update(Long id, UpdateInboundOrderRequest request) {
        InboundOrder order = getById(id);
        if (order.getStatus() != InboundOrderStatus.CREATED) {
            throw new BusinessException("inbound order can only be edited before it is received");
        }

        Warehouse warehouse = warehouseService.getById(order.getWarehouseId());
        Supplier supplier = request.supplierId() != null ? supplierService.getById(request.supplierId()) : null;
        order.updateSupplier(supplier);

        inboundOrderItemMapper.delete(Wrappers.lambdaQuery(InboundOrderItem.class)
                .eq(InboundOrderItem::getOrderId, id));
        addItems(order, warehouse, request.items());
        order.getItems().forEach(inboundOrderItemMapper::insert);

        inboundOrderMapper.updateById(order);
        return getDetail(id);
    }

    private void addItems(InboundOrder order, Warehouse warehouse, List<InboundOrderItemRequest> itemRequests) {
        for (InboundOrderItemRequest item : itemRequests) {
            Sku sku = skuService.getById(item.skuId());
            WarehouseArea area = warehouseAreaService.getById(item.areaId());
            if (!area.getWarehouseId().equals(warehouse.getId())) {
                throw new BusinessException("area does not belong to the given warehouse");
            }
            WarehouseLocation location = warehouseLocationService.getById(item.locationId());
            if (!location.getAreaId().equals(area.getId())) {
                throw new BusinessException("location does not belong to the given area");
            }
            order.addItem(sku, area, location, item.quantity());
        }
    }

    @Transactional(readOnly = true)
    public InboundOrderResponse getDetail(Long id) {
        return InboundOrderResponse.from(assemble(getById(id)));
    }

    @Transactional
    public void delete(Long id) {
        InboundOrder order = getById(id);
        if (order.getStatus() != InboundOrderStatus.CREATED) {
            throw new BusinessException("only inbound orders that have not been received can be deleted");
        }
        inboundOrderItemMapper.delete(Wrappers.lambdaQuery(InboundOrderItem.class)
                .eq(InboundOrderItem::getOrderId, id));
        inboundOrderMapper.deleteById(id);
    }

    @Transactional
    public InboundOrderResponse receive(Long id) {
        InboundOrder order = assemble(getById(id));
        if (order.getStatus() != InboundOrderStatus.CREATED) {
            throw new BusinessException("inbound order has already been received");
        }

        order.getItems().forEach(item -> inventoryService.receive(
                order.getWarehouse(),
                item.getArea(),
                item.getLocation(),
                item.getSku(),
                item.getQuantity(),
                order.getOrderNo(),
                "receive inbound order"
        ));
        order.markReceived();
        inboundOrderMapper.updateById(order);
        return InboundOrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<InboundOrderResponse> search(InboundOrderQuery query) {
        Page<InboundOrder> page = inboundOrderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(InboundOrder.class)
                        .like(StringUtils.hasText(query.getOrderNo()), InboundOrder::getOrderNo, query.getOrderNo())
                        .eq(query.getStatus() != null, InboundOrder::getStatus, query.getStatus())
                        .eq(query.getWarehouseId() != null, InboundOrder::getWarehouseId, query.getWarehouseId())
                        .orderByDesc(InboundOrder::getCreatedAt)
        );

        return PageResponse.from(page, order -> InboundOrderResponse.from(assemble(order)));
    }

    private InboundOrder getById(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("inbound order not found");
        }
        return order;
    }

    private InboundOrder assemble(InboundOrder order) {
        order.attachWarehouse(warehouseService.getById(order.getWarehouseId()));
        if (order.getSupplierId() != null) {
            order.attachSupplier(supplierService.getById(order.getSupplierId()));
        }
        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(Wrappers.lambdaQuery(InboundOrderItem.class)
                .eq(InboundOrderItem::getOrderId, order.getId())
                .orderByAsc(InboundOrderItem::getId));
        items.forEach(item -> {
            item.attachSku(skuService.getById(item.getSkuId()));
            item.attachArea(warehouseAreaService.getById(item.getAreaId()));
            item.attachLocation(warehouseLocationService.getById(item.getLocationId()));
        });
        order.setItems(items);
        return order;
    }
}
