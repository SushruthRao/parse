package com.project.rxparser.service;

import com.project.rxparser.config.RxBatchConfiguration;
import com.project.rxparser.dto.IndexedRecord;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public RxBatchSaverService(RxBatchTransactionService batchTransactionService,
                               RxBatchConfiguration batchConfiguration,
                               EntityManager entityManager) {
        this.batchTransactionService = batchTransactionService;
        this.batchConfiguration = batchConfiguration;
        this.entityManager = entityManager;
    }

    public List<String> batchInsert(List<IndexedRecord> records, Integer size) {

        int batchSize = (size != null && size > 0) ? size : batchConfiguration.getBatchSize();

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

            int batchFailuresBefore = failedRecords.size();

            for (Map.Entry<String, List<IndexedRecord>> entry : batch) {
                String memberId = entry.getKey();
                List<IndexedRecord> indexedRecords = entry.getValue();

                try {

                    batchTransactionService.saveSingleMember(memberId, indexedRecords);
                    log.debug("[batchInsert] Member {} saved successfully", memberId);

                } catch (Exception e) {

                    entityManager.clear(); // Prevents a failed entity from corrupting the persistence context
                    indexedRecords.forEach(indexed -> failedRecords.add("Line " + indexed.lineNumber()));
                    log.warn("[batchInsert] Member {} failed, exception: {}", memberId, e.getMessage());
                }
            }

            int batchFailuresCount = failedRecords.size() - batchFailuresBefore;
            if (batchFailuresCount == 0) {
                log.info("[batchInsert] Batch {}-{} committed successfully", i + 1, end);
            } else {
                log.warn("[batchInsert] Batch {}-{} done with {} failure(s)", i + 1, end, batchFailuresCount);
            }
        }

        log.info("[batchInsert] Complete — {} lines failed out of {} total members", failedRecords.size(), total);
        return failedRecords;
    }
}