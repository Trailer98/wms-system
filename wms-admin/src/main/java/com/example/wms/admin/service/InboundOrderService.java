package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.InboundOrder;
import com.example.wms.admin.model.entity.InboundOrderItem;
import com.example.wms.common.enums.InboundOrderStatus;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.mapper.InboundOrderItemMapper;
import com.example.wms.admin.model.mapper.InboundOrderMapper;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.InboundOrderQuery;
import com.example.wms.admin.view.dto.InboundOrderResponse;
import com.example.wms.admin.view.dto.OrderItemRequest;
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

    public InboundOrderService(
            InboundOrderMapper inboundOrderMapper,
            InboundOrderItemMapper inboundOrderItemMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            InventoryService inventoryService
    ) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderItemMapper = inboundOrderItemMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public InboundOrderResponse create(CreateInboundOrderRequest request) {
        if (inboundOrderMapper.selectCount(Wrappers.lambdaQuery(InboundOrder.class)
                .eq(InboundOrder::getOrderNo, request.orderNo())) > 0) {
            throw new BusinessException("inbound order number already exists");
        }
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        InboundOrder order = new InboundOrder(request.orderNo(), warehouse, request.supplierName());

        for (OrderItemRequest item : request.items()) {
            Sku sku = skuService.getById(item.skuId());
            order.addItem(sku, item.quantity());
        }

        inboundOrderMapper.insert(order);
        order.getItems().forEach(item -> {
            item.assignOrderId(order.getId());
            inboundOrderItemMapper.insert(item);
        });

        return InboundOrderResponse.from(order);
    }

    @Transactional
    public InboundOrderResponse receive(Long id) {
        InboundOrder order = assemble(getById(id));
        if (order.getStatus() != InboundOrderStatus.CREATED) {
            throw new BusinessException("inbound order has already been received");
        }

        order.getItems().forEach(item -> inventoryService.receive(
                order.getWarehouse(),
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
        List<InboundOrderItem> items = inboundOrderItemMapper.selectList(Wrappers.lambdaQuery(InboundOrderItem.class)
                .eq(InboundOrderItem::getOrderId, order.getId())
                .orderByAsc(InboundOrderItem::getId));
        items.forEach(item -> item.attachSku(skuService.getById(item.getSkuId())));
        order.setItems(items);
        return order;
    }
}
