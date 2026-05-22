package com.project.rxparser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
	    boolean success, 
	    String message, 
	    T data
) {}

