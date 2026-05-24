package com.project.rxparser.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record BundledAndInvalidRecordsDto (
        List<RxBundledResponseDto> bundledData,
        List<String> invalidRecords
) {
	 @JsonIgnore
	 public String getStatusMessage() {
		 
	        if (invalidRecords.isEmpty()) {
	            return "File upload success";
	        }
	        String status = "File upload success, skipped " + invalidRecords.size() + " invalid records";
	        return status;
	    }
}