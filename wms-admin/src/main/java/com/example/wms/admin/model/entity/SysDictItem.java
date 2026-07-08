package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.CommonStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single label/style entry within a {@link SysDictType}, e.g. {@code (stock_movement_operation_type,
 * STOCK_ADJUST_DECREASE) -> "库存调整减少"}. {@code isSystem} rows are ones seeded by
 * {@code DictDataInitializer} from a live Java enum (see {@code OperationType}/{@code MovementType}) —
 * they can be relabeled/restyled/disabled freely, but never physically deleted, since the code that
 * relies on those values existing (business logic, not display) is still there regardless of what the
 * dictionary says.
 */
@TableName("sys_dict_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysDictItem {

    private Long id;
    private String dictCode;
    private String itemValue;
    private String itemLabel;
    private String itemLabelEn;
    private int sortOrder;
    private CommonStatus status = CommonStatus.ENABLED;
    private String tagType;
    private String cssClass;
    private boolean isSystem;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic(value = "0", delval = "1")
    private boolean deleted = false;

    public SysDictItem(String dictCode, String itemValue, String itemLabel, String itemLabelEn, int sortOrder,
            String tagType, String cssClass, boolean isSystem, String remark, String createdBy) {
        this.dictCode = dictCode;
        this.itemValue = itemValue;
        this.itemLabel = itemLabel;
        this.itemLabelEn = itemLabelEn;
        this.sortOrder = sortOrder;
        this.tagType = tagType;
        this.cssClass = cssClass;
        this.isSystem = isSystem;
        this.remark = remark;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String itemLabel, String itemLabelEn, int sortOrder, String tagType, String cssClass, String remark, String updatedBy) {
        this.itemLabel = itemLabel;
        this.itemLabelEn = itemLabelEn;
        this.sortOrder = sortOrder;
        this.tagType = tagType;
        this.cssClass = cssClass;
        this.remark = remark;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(CommonStatus status, String updatedBy) {
        this.status = status;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
