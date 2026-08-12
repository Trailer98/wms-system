package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Supplier;
import com.example.wms.admin.model.mapper.SupplierMapper;
import com.example.wms.admin.view.dto.CreateSupplierRequest;
import com.example.wms.admin.view.dto.SupplierQuery;
import com.example.wms.admin.view.dto.SupplierResponse;
import com.example.wms.admin.view.dto.UpdateSupplierEnabledRequest;
import com.example.wms.admin.view.dto.UpdateSupplierRequest;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        if (supplierMapper.selectCount(Wrappers.lambdaQuery(Supplier.class).eq(Supplier::getCode, request.code())) > 0) {
            throw new BusinessException("supplier code already exists");
        }
        Supplier supplier = new Supplier(
                request.code(),
                request.name(),
                request.contactName(),
                request.contactPhone(),
                request.address()
        );
        supplierMapper.insert(supplier);
        return SupplierResponse.from(supplier);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(SupplierQuery query) {
        Page<Supplier> page = supplierMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Supplier.class)
                        .like(StringUtils.hasText(query.getCode()), Supplier::getCode, query.getCode())
                        .like(StringUtils.hasText(query.getName()), Supplier::getName, query.getName())
                        .orderByAsc(Supplier::getCode)
        );

        return PageResponse.from(page, SupplierResponse::from);
    }

    @Transactional(readOnly = true)
    public Supplier getById(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException("supplier not found");
        }
        return supplier;
    }

    @Transactional
    public SupplierResponse update(UpdateSupplierRequest request) {
        getById(request.id());
        LambdaUpdateWrapper<Supplier> updateWrapper = Wrappers.lambdaUpdate(Supplier.class)
                .eq(Supplier::getId, request.id())
                .set(Supplier::getName, request.name())
                .set(Supplier::getContactName, request.contactName())
                .set(Supplier::getContactPhone, request.contactPhone())
                .set(Supplier::getAddress, request.address());
        supplierMapper.update(updateWrapper);
        return SupplierResponse.from(getById(request.id()));
    }

    @Transactional
    public void changeEnabled(UpdateSupplierEnabledRequest request) {
        getById(request.id());
        LambdaUpdateWrapper<Supplier> updateWrapper = Wrappers.lambdaUpdate(Supplier.class)
                .eq(Supplier::getId, request.id())
                .set(Supplier::isEnabled, request.enabled());
        supplierMapper.update(updateWrapper);
    }

    // 跟 CustomerService.deleteCustomer 一样不加子级校验：Supplier 被 inbound_orders.supplier_id
    // 引用，但那是历史单据的普通外键引用，不是 Warehouse→WarehouseArea 那种结构性父子层级
    // （删仓库会留下指向不存在仓库的孤儿库区记录，删供应商不会让入库单本身失效，只是历史记录
    // 里的供应商信息保持不变），沿用 Customer 已有的先例，不额外加保护。
    @Transactional
    public void delete(Long id) {
        getById(id);
        supplierMapper.deleteById(id);
    }
}
