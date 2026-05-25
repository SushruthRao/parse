package com.project.rxparser.service;

import com.project.rxparser.config.RxBatchConfiguration;
import com.project.rxparser.dto.IndexedRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RxBatchSaverService {

    private final RxBatchTransactionService batchTransactionService;
    private final RxBatchConfiguration batchConfiguration;

    public RxBatchSaverService(RxBatchTransactionService batchTransactionService,
                               RxBatchConfiguration batchConfiguration) {
        this.batchTransactionService = batchTransactionService;
        this.batchConfiguration = batchConfiguration;
    }

    public List<String> batchInsert(List<IndexedRecord> records) {

        int batchSize = batchConfiguration.getBatchSize();

        Map<String, List<IndexedRecord>> groupedByMemberId = records.stream()
                .collect(Collectors.groupingBy(
                        indexed -> indexed.data().memberId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Map.Entry<String, List<IndexedRecord>>> entries = new ArrayList<>(groupedByMemberId.entrySet());
        List<String> failedRecords = new ArrayList<>();
        int total = entries.size();

        log.info("[batchInsert] Starting — {} unique members, batch size: {}", total, batchSize);

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Map.Entry<String, List<IndexedRecord>>> batch = entries.subList(i, end);

            log.info("[batchInsert] Processing members {}-{} of {}", i + 1, end, total);
            
            List<String> batchFailures = batchTransactionService.saveBatch(batch);
            failedRecords.addAll(batchFailures);

            if (batchFailures.isEmpty()) {
                log.info("[batchInsert] Batch {}-{} committed successfully", i + 1, end);
            } else {
                log.warn("[batchInsert] Batch {}-{} done with {} failure(s): {}",
                        i + 1, end, batchFailures.size(), batchFailures);
            }
        }

        log.info("[batchInsert] Complete — {}/{} members failed", failedRecords.size(), total);
        return failedRecords;
    }
}