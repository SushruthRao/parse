package com.project.rxparser.globalexceptionhandler;

import java.io.IOException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.project.rxparser.dto.ApiResponse;
import com.project.rxparser.exception.InvalidBundleKeyException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidBundleKeyException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalidBundleKey(InvalidBundleKeyException exception) {

		ApiResponse<?> errorResponse = new ApiResponse<>(false, exception.getMessage(), null);
		log.info("[GlobalExceptionHandler] Invalid bundle key exception : {} ", exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(JacksonException.class)
	public ResponseEntity<ApiResponse<?>> handleJacksonException(JacksonException exception) {

		ApiResponse<?> errorResponse = new ApiResponse<>(false, "JSON Parsing error", null);
		log.info("[GlobalExceptionHandler] Jackson JSON parsing exception : {} ", exception.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<ApiResponse<?>> handleIOException(IOException exception) {

		ApiResponse<?> errorResponse = new ApiResponse<>(false, "Internal server error", null);
		log.info("[GlobalExceptionHandler] IOException occurred : {} ", exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}
	

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<?>> handle(DataIntegrityViolationException exception) {

		ApiResponse<?> errorResponse = new ApiResponse<>(false, "Internal server error", null);
		log.info("[GlobalExceptionHandler] DataIntegrityViolationException occurred : {} ", exception.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}


	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception exception) {

		ApiResponse<?> errorResponse = new ApiResponse<>(false, exception.getMessage(), null);
		log.info("[GlobalExceptionHandler] General Exception occured : {} ", exception.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

}
