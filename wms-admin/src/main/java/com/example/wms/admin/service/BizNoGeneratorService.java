package com.example.wms.admin.service;

import com.example.wms.admin.model.entity.BizSequence;
import com.example.wms.admin.model.mapper.BizSequenceMapper;
import com.example.wms.common.enums.BizNoType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates system business numbers in {@code <prefix><yyyyMMdd><4+ digit sequence>} form (e.g.
 * {@code IN202607080001}), one independent daily sequence per {@link BizNoType}. Deliberately not
 * {@code select max(...) + 1}: that pattern is not concurrency-safe under the isolation levels this
 * project already relies on elsewhere (see {@code InventoryMapper}'s guarded UPDATEs). Instead this
 * locks the one {@code biz_sequence} row for today's date + type with {@code SELECT ... FOR UPDATE},
 * so concurrent callers serialize on that single row instead of racing on a MAX() read.
 * <p>
 * {@code generate} is {@code @Transactional} with the default REQUIRED propagation, so calling it
 * from within an already-open transaction (e.g. {@code InboundOrderService.create()}) folds the
 * sequence increment into that same transaction: if the caller's save fails and rolls back, the
 * increment rolls back with it. That can leave a gap in the sequence (a number that was minted but
 * never used) — which is fine, gaps are allowed — but it can never hand out the same number twice.
 */
@Service
public class BizNoGeneratorService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MIN_SEQUENCE_DIGITS = 4;

    private final BizSequenceMapper bizSequenceMapper;

    public BizNoGeneratorService(BizSequenceMapper bizSequenceMapper) {
        this.bizSequenceMapper = bizSequenceMapper;
    }

    @Transactional
    public String generate(BizNoType type) {
        String seqDate = LocalDate.now().format(DATE_FORMAT);
        String bizType = type.name();

        BizSequence sequence = bizSequenceMapper.selectForUpdate(bizType, seqDate);
        long value;
        if (sequence == null) {
            BizSequence created = new BizSequence(bizType, seqDate, type.getPrefix());
            try {
                bizSequenceMapper.insert(created);
                value = created.getCurrentValue();
            } catch (DuplicateKeyException raceLostToConcurrentFirstOfDay) {
                sequence = bizSequenceMapper.selectForUpdate(bizType, seqDate);
                sequence.increment();
                bizSequenceMapper.updateById(sequence);
                value = sequence.getCurrentValue();
            }
        } else {
            sequence.increment();
            bizSequenceMapper.updateById(sequence);
            value = sequence.getCurrentValue();
        }

        String sequencePart = String.valueOf(value);
        if (sequencePart.length() < MIN_SEQUENCE_DIGITS) {
            sequencePart = "0".repeat(MIN_SEQUENCE_DIGITS - sequencePart.length()) + sequencePart;
        }
        return type.getPrefix() + seqDate + sequencePart;
    }
}
