package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Supplier;
import com.example.wms.admin.model.mapper.SupplierMapper;
import com.example.wms.admin.view.dto.CreateSupplierRequest;
import com.example.wms.admin.view.dto.SupplierQuery;
import com.example.wms.admin.view.dto.SupplierResponse;
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
}
