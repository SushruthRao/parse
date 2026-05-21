package com.project.rxparser.dto;

public record RawJsonDataDto(
		String memberId,
	    String firstname,
	    String lastname,
	    String dob,
	    String rx,
	    String drugName,
	    String description	
) {}
