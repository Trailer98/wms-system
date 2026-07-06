package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.OutboundOrder;
import com.example.wms.admin.model.entity.OutboundOrderItem;
import com.example.wms.admin.model.entity.OutboundStockLock;
import com.example.wms.common.enums.OutboundOrderStatus;
import com.example.wms.admin.model.entity.Customer;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.mapper.OutboundOrderItemMapper;
import com.example.wms.admin.model.mapper.OutboundOrderMapper;
import com.example.wms.admin.model.mapper.OutboundStockLockMapper;
import com.example.wms.admin.view.dto.CreateOutboundOrderRequest;
import com.example.wms.admin.view.dto.OrderItemRequest;
import com.example.wms.admin.view.dto.OutboundOrderQuery;
import com.example.wms.admin.view.dto.OutboundOrderResponse;
import com.example.wms.admin.view.dto.UpdateOutboundOrderRequest;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class OutboundOrderService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final OutboundStockLockMapper outboundStockLockMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;
    private final InventoryService inventoryService;
    private final CustomerService customerService;
    private final WarehouseAreaService warehouseAreaService;
    private final WarehouseLocationService warehouseLocationService;

    public OutboundOrderService(
            OutboundOrderMapper outboundOrderMapper,
            OutboundOrderItemMapper outboundOrderItemMapper,
            OutboundStockLockMapper outboundStockLockMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            InventoryService inventoryService,
            CustomerService customerService,
            WarehouseAreaService warehouseAreaService,
            WarehouseLocationService warehouseLocationService
    ) {
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundOrderItemMapper = outboundOrderItemMapper;
        this.outboundStockLockMapper = outboundStockLockMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.inventoryService = inventoryService;
        this.customerService = customerService;
        this.warehouseAreaService = warehouseAreaService;
        this.warehouseLocationService = warehouseLocationService;
    }

    @Transactional
    public OutboundOrderResponse create(CreateOutboundOrderRequest request) {
        if (outboundOrderMapper.selectCount(Wrappers.lambdaQuery(OutboundOrder.class)
                .eq(OutboundOrder::getOrderNo, request.orderNo())) > 0) {
            throw new BusinessException("outbound order number already exists");
        }
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        Customer customer = request.customerId() != null ? customerService.getById(request.customerId()) : null;
        OutboundOrder order = new OutboundOrder(request.orderNo(), warehouse, customer);

        addItems(order, request.items());

        outboundOrderMapper.insert(order);
        order.getItems().forEach(item -> {
            item.assignOrderId(order.getId());
            outboundOrderItemMapper.insert(item);
        });

        return OutboundOrderResponse.from(order);
    }

    @Transactional
    public OutboundOrderResponse update(Long id, UpdateOutboundOrderRequest request) {
        OutboundOrder order = getById(id);
        if (order.getStatus() != OutboundOrderStatus.CREATED) {
            throw new BusinessException("outbound order can only be edited before it is locked");
        }

        Customer customer = request.customerId() != null ? customerService.getById(request.customerId()) : null;
        order.updateCustomer(customer);

        outboundOrderItemMapper.delete(Wrappers.lambdaQuery(OutboundOrderItem.class)
                .eq(OutboundOrderItem::getOrderId, id));
        addItems(order, request.items());
        order.getItems().forEach(outboundOrderItemMapper::insert);

        outboundOrderMapper.updateById(order);
        return getDetail(id);
    }

    private void addItems(OutboundOrder order, List<OrderItemRequest> itemRequests) {
        for (OrderItemRequest item : itemRequests) {
            Sku sku = skuService.getById(item.skuId());
            order.addItem(sku, item.quantity());
        }
    }

    @Transactional(readOnly = true)
    public OutboundOrderResponse getDetail(Long id) {
        return OutboundOrderResponse.from(assemble(getById(id)));
    }

    @Transactional
    public void delete(Long id) {
        OutboundOrder order = getById(id);
        if (order.getStatus() == OutboundOrderStatus.SHIPPED) {
            throw new BusinessException("outbound orders that have already been shipped cannot be deleted");
        }
        if (order.getStatus() == OutboundOrderStatus.LOCKED) {
            inventoryService.releaseLocks(order.getId(), order.getOrderNo());
        }

        outboundOrderItemMapper.delete(Wrappers.lambdaQuery(OutboundOrderItem.class)
                .eq(OutboundOrderItem::getOrderId, id));
        outboundOrderMapper.deleteById(id);
    }

    @Transactional
    public OutboundOrderResponse lock(Long id) {
        OutboundOrder order = assemble(getById(id));
        if (order.getStatus() != OutboundOrderStatus.CREATED) {
            throw new BusinessException("outbound order is not in a lockable state");
        }

        for (OutboundOrderItem item : order.getItems()) {
            inventoryService.lockForOutbound(
                    order.getWarehouse(),
                    item.getSku(),
                    item.getQuantity(),
                    order.getId(),
                    item.getId(),
                    order.getOrderNo()
            );
        }

        order.markLocked();
        outboundOrderMapper.updateById(order);
        return OutboundOrderResponse.from(assemble(order));
    }

    @Transactional
    public OutboundOrderResponse ship(Long id) {
        OutboundOrder order = assemble(getById(id));
        if (order.getStatus() != OutboundOrderStatus.LOCKED) {
            throw new BusinessException("outbound order must be locked before shipping");
        }

        inventoryService.confirmShip(order.getId(), order.getOrderNo());
        order.markShipped();
        outboundOrderMapper.updateById(order);
        return OutboundOrderResponse.from(assemble(order));
    }

    @Transactional
    public OutboundOrderResponse cancel(Long id) {
        OutboundOrder order = assemble(getById(id));
        if (order.getStatus() == OutboundOrderStatus.SHIPPED || order.getStatus() == OutboundOrderStatus.CANCELLED) {
            throw new BusinessException("outbound order cannot be cancelled in its current state");
        }

        if (order.getStatus() == OutboundOrderStatus.LOCKED) {
            inventoryService.releaseLocks(order.getId(), order.getOrderNo());
        }

        order.markCancelled();
        outboundOrderMapper.updateById(order);
        return OutboundOrderResponse.from(assemble(order));
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
        if (order.getCustomerId() != null) {
            order.attachCustomer(customerService.getById(order.getCustomerId()));
        }
        List<OutboundOrderItem> items = outboundOrderItemMapper.selectList(Wrappers.lambdaQuery(OutboundOrderItem.class)
                .eq(OutboundOrderItem::getOrderId, order.getId())
                .orderByAsc(OutboundOrderItem::getId));
        items.forEach(item -> item.attachSku(skuService.getById(item.getSkuId())));
        order.setItems(items);

        List<OutboundStockLock> allocations = outboundStockLockMapper.selectList(Wrappers.lambdaQuery(OutboundStockLock.class)
                .eq(OutboundStockLock::getOutboundOrderId, order.getId())
                .orderByAsc(OutboundStockLock::getId));
        allocations.forEach(lock -> {
            lock.attachSku(skuService.getById(lock.getSkuId()));
            lock.attachWarehouse(warehouseService.getById(lock.getWarehouseId()));
            lock.attachArea(warehouseAreaService.getById(lock.getAreaId()));
            lock.attachLocation(warehouseLocationService.getById(lock.getLocationId()));
        });
        order.setAllocations(allocations);

        return order;
    }
}
