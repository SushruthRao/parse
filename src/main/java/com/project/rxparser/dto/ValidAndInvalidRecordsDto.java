package com.project.rxparser.dto;

import java.util.List;

public record ValidAndInvalidRecordsDto(
        List<RawJsonDataDto> validRecords,
        List<String> invalidRecords
) {}
