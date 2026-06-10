package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("warehouses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warehouse {

    private Long id;
    private String code;
    private String name;
    private String address;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    public Warehouse(String code, String name, String address) {
        this.code = code;
        this.name = name;
        this.address = address;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
