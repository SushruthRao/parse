package com.project.rxparser.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
// DTO Which is sent as a success response to the POST request in RxController
// JsonInclude NON_NULL makes it so that null fields are not included
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RxBundledResponseDto (
		String memberId,
	    String firstname,
	    String lastname,
	    String dob,
	    List<RxInfoDto> rxInfo
) {}
