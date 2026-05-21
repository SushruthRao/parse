package com.project.rxparser.exception;

public class InvalidFileException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidFileException(String exceptionMessage) {
        super(exceptionMessage);
    }

}
