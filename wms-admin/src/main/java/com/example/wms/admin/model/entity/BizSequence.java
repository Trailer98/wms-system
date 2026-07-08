package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("biz_sequence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BizSequence {

    private Long id;
    private String bizType;
    private String seqDate;
    private String prefix;
    private long currentValue;
    private String remark;
    private Instant createdAt;
    private Instant updatedAt;

    public BizSequence(String bizType, String seqDate, String prefix) {
        this.bizType = bizType;
        this.seqDate = seqDate;
        this.prefix = prefix;
        this.currentValue = 1;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void increment() {
        this.currentValue += 1;
        this.updatedAt = Instant.now();
    }
}
