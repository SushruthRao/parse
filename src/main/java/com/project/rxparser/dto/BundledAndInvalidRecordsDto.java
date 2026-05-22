package com.project.rxparser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record BundledAndInvalidRecordsDto(
        List<RxBundledResponseDto> bundledData,
        List<String> invalidRecords
) {
}
