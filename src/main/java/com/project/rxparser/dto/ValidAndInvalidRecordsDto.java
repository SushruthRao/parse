package com.project.rxparser.dto;

import java.util.List;
import java.util.Map;

public record ValidAndInvalidRecordsDto(
        List<RawJsonDataDto> validRecords,
        List<String> invalidRecords
) {}
