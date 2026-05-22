package com.project.rxparser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ApiResponse<T>(
	    boolean success, 
	    String message, 
	    T data
) {}

