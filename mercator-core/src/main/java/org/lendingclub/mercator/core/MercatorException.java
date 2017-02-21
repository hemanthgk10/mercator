package org.lendingclub.mercator.core;

public class MercatorException extends RuntimeException {

	public MercatorException() {
		
	}

	public MercatorException(String message) {
		super(message);
		
	}

	public MercatorException(Throwable cause) {
		super(cause);
		
	}

	public MercatorException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public MercatorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	
	}

}
