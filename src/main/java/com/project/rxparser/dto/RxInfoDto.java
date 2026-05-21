package com.project.rxparser.dto;

// DTO which is used as a field in RxUploadResponseDTO

public record RxInfoDto(
	    String rx,
	    String drugName,
	    String description
) {}
