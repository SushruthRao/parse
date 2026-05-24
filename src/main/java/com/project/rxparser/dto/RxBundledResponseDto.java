package com.project.rxparser.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

//JsonInclude NON_NULL makes it so that null fields are not included
// so when bundlekey is memberId then other member fields are null and not included in final response

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RxBundledResponseDto (
		String memberId,
	    String firstname,
	    String lastname,
	    String dob,
	    List<RxInfoDto> rxInfo
) {}