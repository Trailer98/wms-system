package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.mapper.SkuMapper;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.SkuQuery;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SkuService {

    private final SkuMapper skuMapper;

    public SkuService(SkuMapper skuMapper) {
        this.skuMapper = skuMapper;
    }

    @Transactional
    public SkuResponse create(CreateSkuRequest request) {
        if (skuMapper.selectCount(Wrappers.lambdaQuery(Sku.class).eq(Sku::getCode, request.code())) > 0) {
            throw new BusinessException("sku code already exists");
        }
        Sku sku = new Sku(request.code(), request.name(), request.unit(), request.category());
        skuMapper.insert(sku);
        return SkuResponse.from(sku);
    }

    @Transactional(readOnly = true)
    public PageResponse<SkuResponse> search(SkuQuery query) {
        Page<Sku> page = skuMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Sku.class)
                        .like(StringUtils.hasText(query.getCode()), Sku::getCode, query.getCode())
                        .like(StringUtils.hasText(query.getName()), Sku::getName, query.getName())
                        .like(StringUtils.hasText(query.getCategory()), Sku::getCategory, query.getCategory())
                        .orderByAsc(Sku::getCode)
        );

        return PageResponse.from(page, SkuResponse::from);
    }

    @Transactional(readOnly = true)
    public Sku getById(Long id) {
        Sku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BusinessException("sku not found");
        }
        return sku;
    }
}
