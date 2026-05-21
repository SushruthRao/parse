package com.project.rxparser.dto;
import java.util.List;
// DTO Which is is sent as a success response to the POST request in RxController
public record RxBundledResponseDto (
		String memberId,
	    String firstname,
	    String lastname,
	    String dob,
	    List<RxInfoDto> rxInfo
) {}
