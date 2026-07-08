package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.SysDictItem;
import com.example.wms.admin.model.entity.SysDictType;
import com.example.wms.admin.model.mapper.SysDictItemMapper;
import com.example.wms.admin.model.mapper.SysDictTypeMapper;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.view.dto.CreateSysDictItemRequest;
import com.example.wms.admin.view.dto.CreateSysDictTypeRequest;
import com.example.wms.admin.view.dto.SysDictItemResponse;
import com.example.wms.admin.view.dto.SysDictItemView;
import com.example.wms.admin.view.dto.SysDictTypeQuery;
import com.example.wms.admin.view.dto.SysDictTypeResponse;
import com.example.wms.admin.view.dto.UpdateSysDictItemRequest;
import com.example.wms.admin.view.dto.UpdateSysDictTypeRequest;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.CommonStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Business codes (operationType, movement/biz type, ...) stay as Java enums — this service only owns
 * their *display* layer (label/sort/tag color/enabled), so relabeling something in the admin UI can
 * never change what the backend actually does with that code.
 * <p>
 * {@link #getItems}/{@link #getBatch}/{@link #getLabel}/{@link #getTagType} are the read path every
 * business page and the stock-movement query go through; all four are cache-backed and, per the "must
 * never break a business query" requirement, never throw — a lookup miss (unknown dict, unknown value,
 * even a DB error mid-lookup) degrades to "show the raw code" rather than failing the caller.
 * <p>
 * The cache is a plain {@link ConcurrentHashMap} keyed by dictCode, holding only ENABLED items sorted
 * by sortOrder — intentionally not Spring's {@code @Cacheable}, since self-invocation from within this
 * same service (create/update calling straight into the cache map) would silently bypass a proxy-based
 * cache anyway. Every write path below evicts its dictCode before returning.
 */
@Service
public class SysDictService {

    private static final Logger log = LoggerFactory.getLogger(SysDictService.class);

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;
    private final Map<String, List<SysDictItemView>> itemCache = new ConcurrentHashMap<>();

    public SysDictService(SysDictTypeMapper sysDictTypeMapper, SysDictItemMapper sysDictItemMapper) {
        this.sysDictTypeMapper = sysDictTypeMapper;
        this.sysDictItemMapper = sysDictItemMapper;
    }

    // ---------------------------------------------------------------- business read path (cached) ----

    @Transactional(readOnly = true)
    public List<SysDictItemView> getItems(String dictCode) {
        try {
            return itemCache.computeIfAbsent(dictCode, this::loadEnabledItems);
        } catch (Exception unexpected) {
            log.warn("dictionary lookup failed for dictCode={}, falling back to empty list", dictCode, unexpected);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, List<SysDictItemView>> getBatch(List<String> dictCodes) {
        return dictCodes.stream().distinct().collect(Collectors.toMap(code -> code, this::getItems));
    }

    /** Never throws; falls back to the raw code itself when the dict/item is missing or disabled-and-uncached. */
    public String getLabel(String dictCode, String value) {
        if (value == null) {
            return null;
        }
        try {
            return getItems(dictCode).stream()
                    .filter(item -> item.value().equals(value))
                    .map(SysDictItemView::label)
                    .findFirst()
                    .orElseGet(() -> resolveDisabledOrMissingLabel(dictCode, value));
        } catch (Exception unexpected) {
            log.warn("dictionary label lookup failed for dictCode={}, value={}, falling back to raw value", dictCode, value, unexpected);
            return value;
        }
    }

    /** Never throws; returns null (no tag color) when the dict/item is missing. */
    public String getTagType(String dictCode, String value) {
        if (value == null) {
            return null;
        }
        try {
            return getItems(dictCode).stream()
                    .filter(item -> item.value().equals(value))
                    .map(SysDictItemView::tagType)
                    .findFirst()
                    .orElse(null);
        } catch (Exception unexpected) {
            log.warn("dictionary tag lookup failed for dictCode={}, value={}", dictCode, value, unexpected);
            return null;
        }
    }

    /**
     * A DISABLED item (or a historical value nobody ever configured) still needs to render something
     * sane on old stock-movement rows — fall back to a DB read bypassing the ENABLED-only cache before
     * giving up and just showing the raw code.
     */
    private String resolveDisabledOrMissingLabel(String dictCode, String value) {
        SysDictItem item = sysDictItemMapper.selectOne(Wrappers.lambdaQuery(SysDictItem.class)
                .eq(SysDictItem::getDictCode, dictCode)
                .eq(SysDictItem::getItemValue, value));
        return item != null ? item.getItemLabel() : value;
    }

    private List<SysDictItemView> loadEnabledItems(String dictCode) {
        return sysDictItemMapper.selectList(Wrappers.lambdaQuery(SysDictItem.class)
                        .eq(SysDictItem::getDictCode, dictCode)
                        .eq(SysDictItem::getStatus, CommonStatus.ENABLED)
                        .orderByAsc(SysDictItem::getSortOrder))
                .stream()
                .map(SysDictItemView::from)
                .toList();
    }

    private void evict(String dictCode) {
        itemCache.remove(dictCode);
    }

    // ---------------------------------------------------------------------------- admin: dict types ----

    @Transactional
    public SysDictTypeResponse createType(CreateSysDictTypeRequest request) {
        if (sysDictTypeMapper.selectCount(Wrappers.lambdaQuery(SysDictType.class)
                .eq(SysDictType::getDictCode, request.dictCode())) > 0) {
            throw new BusinessException("dict code already exists: " + request.dictCode());
        }
        SysDictType type = new SysDictType(request.dictCode(), request.dictName(), request.remark(), request.sortOrder(), currentUsername());
        sysDictTypeMapper.insert(type);
        return SysDictTypeResponse.from(type);
    }

    @Transactional
    public SysDictTypeResponse updateType(Long id, UpdateSysDictTypeRequest request) {
        SysDictType type = requireType(id);
        type.update(request.dictName(), request.remark(), request.sortOrder(), currentUsername());
        sysDictTypeMapper.updateById(type);
        return SysDictTypeResponse.from(type);
    }

    @Transactional
    public SysDictTypeResponse changeTypeStatus(Long id, String status) {
        SysDictType type = requireType(id);
        type.changeStatus(parseStatus(status), currentUsername());
        sysDictTypeMapper.updateById(type);
        evict(type.getDictCode());
        return SysDictTypeResponse.from(type);
    }

    @Transactional(readOnly = true)
    public PageResponse<SysDictTypeResponse> searchTypes(SysDictTypeQuery query) {
        Page<SysDictType> page = sysDictTypeMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(SysDictType.class)
                        .like(StringUtils.hasText(query.getDictCode()), SysDictType::getDictCode, query.getDictCode())
                        .like(StringUtils.hasText(query.getDictName()), SysDictType::getDictName, query.getDictName())
                        .eq(StringUtils.hasText(query.getStatus()), SysDictType::getStatus, query.getStatus())
                        .orderByAsc(SysDictType::getSortOrder)
        );
        return PageResponse.from(page, SysDictTypeResponse::from);
    }

    private SysDictType requireType(Long id) {
        SysDictType type = sysDictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException("dict type not found: " + id);
        }
        return type;
    }

    // ---------------------------------------------------------------------------- admin: dict items ----

    @Transactional
    public SysDictItemResponse createItem(CreateSysDictItemRequest request) {
        if (sysDictItemMapper.selectCount(Wrappers.lambdaQuery(SysDictItem.class)
                .eq(SysDictItem::getDictCode, request.dictCode())
                .eq(SysDictItem::getItemValue, request.itemValue())) > 0) {
            throw new BusinessException("dict item already exists for " + request.dictCode() + "/" + request.itemValue());
        }
        SysDictItem item = new SysDictItem(request.dictCode(), request.itemValue(), request.itemLabel(), request.itemLabelEn(),
                request.sortOrder(), request.tagType(), request.cssClass(), false, request.remark(), currentUsername());
        sysDictItemMapper.insert(item);
        evict(request.dictCode());
        return SysDictItemResponse.from(item);
    }

    @Transactional
    public SysDictItemResponse updateItem(Long id, UpdateSysDictItemRequest request) {
        SysDictItem item = requireItem(id);
        item.update(request.itemLabel(), request.itemLabelEn(), request.sortOrder(), request.tagType(), request.cssClass(), request.remark(), currentUsername());
        sysDictItemMapper.updateById(item);
        evict(item.getDictCode());
        return SysDictItemResponse.from(item);
    }

    @Transactional
    public SysDictItemResponse changeItemStatus(Long id, String status) {
        SysDictItem item = requireItem(id);
        item.changeStatus(parseStatus(status), currentUsername());
        sysDictItemMapper.updateById(item);
        evict(item.getDictCode());
        return SysDictItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        SysDictItem item = requireItem(id);
        if (item.isSystem()) {
            throw new BusinessException("system-seeded dict item cannot be deleted, disable it instead: " + item.getItemValue());
        }
        sysDictItemMapper.deleteById(id);
        evict(item.getDictCode());
    }

    /** Admin listing: every status, unlike {@link #getItems} which is ENABLED-only and cached. */
    @Transactional(readOnly = true)
    public List<SysDictItemResponse> listItemsForAdmin(String dictCode) {
        return sysDictItemMapper.selectList(Wrappers.lambdaQuery(SysDictItem.class)
                        .eq(SysDictItem::getDictCode, dictCode)
                        .orderByAsc(SysDictItem::getSortOrder))
                .stream()
                .map(SysDictItemResponse::from)
                .sorted(Comparator.comparingInt(SysDictItemResponse::sortOrder))
                .toList();
    }

    private SysDictItem requireItem(Long id) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("dict item not found: " + id);
        }
        return item;
    }

    private CommonStatus parseStatus(String status) {
        try {
            return CommonStatus.valueOf(status);
        } catch (IllegalArgumentException invalid) {
            throw new BusinessException("invalid status: " + status);
        }
    }

    private String currentUsername() {
        CurrentUser currentUser = CurrentUserContext.get();
        return currentUser != null ? currentUser.username() : "system";
    }
}
