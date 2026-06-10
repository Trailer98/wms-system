package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("skus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku {

    private Long id;
    private String code;
    private String name;
    private String unit;
    private String category;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    public Sku(String code, String name, String unit, String category) {
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.category = category;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
