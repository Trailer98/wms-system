package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    private Long id;
    private String code;
    private String name;
    private String contactName;
    private String contactPhone;
    private String address;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    public Customer(String code, String name, String contactName, String contactPhone, String address) {
        this.code = code;
        this.name = name;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.address = address;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
