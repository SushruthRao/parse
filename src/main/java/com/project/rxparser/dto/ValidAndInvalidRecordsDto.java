package com.project.rxparser.dto;

import java.util.List;

public record ValidAndInvalidRecordsDto(
        List<IndexedRecord> validRecords,
        List<String> invalidRecords
) {}
