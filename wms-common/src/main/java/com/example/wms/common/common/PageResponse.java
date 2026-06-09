package com.example.wms.common.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PageResponse<T>(
        long pageNum,
        long pageSize,
        long total,
        List<T> records
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords()
        );
    }

    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> converter) {
        return new PageResponse<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(converter).collect(Collectors.toList())
        );
    }
}
