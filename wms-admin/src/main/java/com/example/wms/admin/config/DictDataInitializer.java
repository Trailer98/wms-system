package com.example.wms.admin.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.admin.model.entity.SysDictItem;
import com.example.wms.admin.model.entity.SysDictType;
import com.example.wms.admin.model.mapper.SysDictItemMapper;
import com.example.wms.admin.model.mapper.SysDictTypeMapper;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Idempotently seeds the two stock-movement display dictionaries from the live {@link OperationType}
 * and {@link MovementType} enums, on every startup. This is display data only (label/sort/tag color):
 * the enums themselves remain the sole source of truth for what values {@code stock_movements.type}/
 * {@code operation_type} can actually hold and what the backend does with them — see
 * {@code SysDictService}'s class doc for why the dictionary is never allowed to drive business logic.
 * <p>
 * {@code isSystem=true} on every seeded row: these came from code, not an admin typing them in, so
 * {@code SysDictService.deleteItem} refuses to physically remove them (an operator can still relabel,
 * restyle or disable one). Runs as a separate, independent {@link ApplicationRunner} bean rather than
 * folding into {@link DataInitializer} — dictionary seeding has nothing to do with RBAC seeding, and
 * Spring Boot runs every {@code ApplicationRunner} bean automatically regardless of how many there are.
 */
@Component
@Order(20)
public class DictDataInitializer implements ApplicationRunner {

    private static final String OPERATION_TYPE_DICT_CODE = "stock_movement_operation_type";
    private static final String BIZ_TYPE_DICT_CODE = "stock_movement_biz_type";
    private static final String SEED_OPERATOR = "system";

    private record TypeSeed(String code, String name, String remark, int sort) {
    }

    private record ItemSeed(String value, String label, int sort, String tagType) {
    }

    private static final List<TypeSeed> TYPE_SEEDS = List.of(
            new TypeSeed(OPERATION_TYPE_DICT_CODE, "库存流水操作类型", "对应 Java 枚举 OperationType，取值由后端业务逻辑决定，本字典只负责展示文案/排序/标签样式", 10),
            new TypeSeed(BIZ_TYPE_DICT_CODE, "库存流水业务类型", "对应 Java 枚举 MovementType（前端历史上称之为“业务类型”），取值由后端业务逻辑决定", 20)
    );

    // Order/tagType chosen for readability in the stock-movement list; purely cosmetic, edit freely
    // via the dictionary admin page — none of it feeds back into OperationType itself.
    private static final List<ItemSeed> OPERATION_TYPE_SEEDS = List.of(
            new ItemSeed(OperationType.INBOUND_RECEIVE.getCode(), OperationType.INBOUND_RECEIVE.getLabel(), 10, "success"),
            new ItemSeed(OperationType.OUTBOUND_LOCK.getCode(), OperationType.OUTBOUND_LOCK.getLabel(), 20, "warning"),
            new ItemSeed(OperationType.OUTBOUND_CANCEL_UNLOCK.getCode(), OperationType.OUTBOUND_CANCEL_UNLOCK.getLabel(), 30, "info"),
            new ItemSeed(OperationType.OUTBOUND_SHIP.getCode(), OperationType.OUTBOUND_SHIP.getLabel(), 40, "success"),
            new ItemSeed(OperationType.STOCK_ADJUST_INCREASE.getCode(), OperationType.STOCK_ADJUST_INCREASE.getLabel(), 50, "success"),
            new ItemSeed(OperationType.STOCK_ADJUST_DECREASE.getCode(), OperationType.STOCK_ADJUST_DECREASE.getLabel(), 60, "danger"),
            new ItemSeed(OperationType.STOCK_COUNT_PROFIT.getCode(), OperationType.STOCK_COUNT_PROFIT.getLabel(), 70, "success"),
            new ItemSeed(OperationType.STOCK_COUNT_LOSS.getCode(), OperationType.STOCK_COUNT_LOSS.getLabel(), 80, "danger"),
            new ItemSeed(OperationType.STOCK_FREEZE.getCode(), OperationType.STOCK_FREEZE.getLabel(), 90, "warning"),
            new ItemSeed(OperationType.STOCK_UNFREEZE.getCode(), OperationType.STOCK_UNFREEZE.getLabel(), 100, "info"),
            new ItemSeed(OperationType.TRANSFER_OUT.getCode(), OperationType.TRANSFER_OUT.getLabel(), 110, "warning"),
            new ItemSeed(OperationType.TRANSFER_IN.getCode(), OperationType.TRANSFER_IN.getLabel(), 120, "success"),
            new ItemSeed(OperationType.TRANSFER_TO_EXCEPTION_OUT.getCode(), OperationType.TRANSFER_TO_EXCEPTION_OUT.getLabel(), 130, "warning"),
            new ItemSeed(OperationType.TRANSFER_TO_EXCEPTION_IN.getCode(), OperationType.TRANSFER_TO_EXCEPTION_IN.getLabel(), 140, "danger"),
            new ItemSeed(OperationType.RESTORE_FROM_EXCEPTION_OUT.getCode(), OperationType.RESTORE_FROM_EXCEPTION_OUT.getLabel(), 150, "warning"),
            new ItemSeed(OperationType.RESTORE_FROM_EXCEPTION_IN.getCode(), OperationType.RESTORE_FROM_EXCEPTION_IN.getLabel(), 160, "success"),
            new ItemSeed(OperationType.UNKNOWN.getCode(), OperationType.UNKNOWN.getLabel(), 999, "info")
    );

    private static final List<ItemSeed> BIZ_TYPE_SEEDS = List.of(
            new ItemSeed(MovementType.INBOUND.getCode(), MovementType.INBOUND.getLabel(), 10, "success"),
            new ItemSeed(MovementType.OUTBOUND.getCode(), MovementType.OUTBOUND.getLabel(), 20, "warning"),
            new ItemSeed(MovementType.ADJUSTMENT.getCode(), MovementType.ADJUSTMENT.getLabel(), 30, ""),
            new ItemSeed(MovementType.LOCK.getCode(), MovementType.LOCK.getLabel(), 40, "warning"),
            new ItemSeed(MovementType.UNLOCK.getCode(), MovementType.UNLOCK.getLabel(), 50, "info"),
            new ItemSeed(MovementType.COUNT.getCode(), MovementType.COUNT.getLabel(), 60, "")
    );

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;

    public DictDataInitializer(SysDictTypeMapper sysDictTypeMapper, SysDictItemMapper sysDictItemMapper) {
        this.sysDictTypeMapper = sysDictTypeMapper;
        this.sysDictItemMapper = sysDictItemMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (TypeSeed seed : TYPE_SEEDS) {
            ensureType(seed);
        }
        for (ItemSeed seed : OPERATION_TYPE_SEEDS) {
            ensureItem(OPERATION_TYPE_DICT_CODE, seed);
        }
        for (ItemSeed seed : BIZ_TYPE_SEEDS) {
            ensureItem(BIZ_TYPE_DICT_CODE, seed);
        }
    }

    private void ensureType(TypeSeed seed) {
        boolean exists = sysDictTypeMapper.selectCount(Wrappers.lambdaQuery(SysDictType.class)
                .eq(SysDictType::getDictCode, seed.code())) > 0;
        if (!exists) {
            sysDictTypeMapper.insert(new SysDictType(seed.code(), seed.name(), seed.remark(), seed.sort(), SEED_OPERATOR));
        }
    }

    private void ensureItem(String dictCode, ItemSeed seed) {
        boolean exists = sysDictItemMapper.selectCount(Wrappers.lambdaQuery(SysDictItem.class)
                .eq(SysDictItem::getDictCode, dictCode)
                .eq(SysDictItem::getItemValue, seed.value())) > 0;
        if (!exists) {
            sysDictItemMapper.insert(new SysDictItem(dictCode, seed.value(), seed.label(), null, seed.sort(),
                    seed.tagType(), null, true, "seeded from " + dictCode + " enum", SEED_OPERATOR));
        }
    }
}
