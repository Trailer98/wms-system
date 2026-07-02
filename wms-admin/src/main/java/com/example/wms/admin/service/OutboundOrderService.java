package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.OutboundOrder;
import com.example.wms.admin.model.entity.OutboundOrderItem;
import com.example.wms.common.enums.OutboundOrderStatus;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.mapper.OutboundOrderItemMapper;
import com.example.wms.admin.model.mapper.OutboundOrderMapper;
import com.example.wms.admin.view.dto.CreateOutboundOrderRequest;
import com.example.wms.admin.view.dto.OrderItemRequest;
import com.example.wms.admin.view.dto.OutboundOrderQuery;
import com.example.wms.admin.view.dto.OutboundOrderResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class OutboundOrderService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;
    private final InventoryService inventoryService;

    public OutboundOrderService(
            OutboundOrderMapper outboundOrderMapper,
            OutboundOrderItemMapper outboundOrderItemMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            InventoryService inventoryService
    ) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OutboundOrderResponse create(CreateOutboundOrderRequest request) {
        if (outboundOrderMapper.selectCount(Wrappers.lambdaQuery(OutboundOrder.class)
                .eq(OutboundOrder::getOrderNo, request.orderNo())) > 0) {
            throw new BusinessException("outbound order number already exists");
        }
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        OutboundOrder order = new OutboundOrder(request.orderNo(), warehouse, request.customerName());

        for (OrderItemRequest item : request.items()) {
            Sku sku = skuService.getById(item.skuId());
            order.addItem(sku, item.quantity());
        }

        outboundOrderMapper.insert(order);
        order.getItems().forEach(item -> {
            item.assignOrderId(order.getId());
            outboundOrderItemMapper.insert(item);
        });

        return OutboundOrderResponse.from(order);
    }

    @Transactional
    public OutboundOrderResponse ship(Long id) {
        OutboundOrder order = assemble(getById(id));
        if (order.getStatus() != OutboundOrderStatus.CREATED) {
            throw new BusinessException("outbound order has already been shipped");
        }

        order.getItems().forEach(item -> inventoryService.ship(
                order.getWarehouse(),
                item.getSku(),
                item.getQuantity(),
                order.getOrderNo(),
                "ship outbound order"
        ));
        order.markShipped();
        outboundOrderMapper.updateById(order);
        return OutboundOrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboundOrderResponse> search(OutboundOrderQuery query) {
        Page<OutboundOrder> page = outboundOrderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(OutboundOrder.class)
                        .like(StringUtils.hasText(query.getOrderNo()), OutboundOrder::getOrderNo, query.getOrderNo())
                        .eq(query.getStatus() != null, OutboundOrder::getStatus, query.getStatus())
                        .eq(query.getWarehouseId() != null, OutboundOrder::getWarehouseId, query.getWarehouseId())
                        .orderByDesc(OutboundOrder::getCreatedAt)
        );

        return PageResponse.from(page, order -> OutboundOrderResponse.from(assemble(order)));
    }

    private OutboundOrder getById(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("outbound order not found");
        }
        return order;
    }

    private OutboundOrder assemble(OutboundOrder order) {
        order.attachWarehouse(warehouseService.getById(order.getWarehouseId()));
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(Wrappers.lambdaQuery(OutboundOrderItem.class)
                .eq(OutboundOrderItem::getOrderId, order.getId())
                .orderByAsc(OutboundOrderItem::getId));
        items.forEach(item -> item.attachSku(skuService.getById(item.getSkuId())));
        order.setItems(items);
        return order;
    }
}
