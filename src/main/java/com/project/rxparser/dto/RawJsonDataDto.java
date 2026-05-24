package com.project.rxparser.dto;

public record RawJsonDataDto(
		String memberId,
	    String firstname,
	    String lastname,
	    String dob,
	    String rx,
	    String drugName,
	    String description	
) {
	@Override
    public String toString() {
        return String.format(
            "{ memberId=%s, firstname=%s, lastname=%s, dob=%s, rx=%s, drugName=%s, description=%s }",
            memberId, firstname, lastname, dob, rx, drugName, description
        );
    }
}
