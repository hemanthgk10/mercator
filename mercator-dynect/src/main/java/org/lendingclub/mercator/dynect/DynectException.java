package org.lendingclub.mercator.dynect;

import org.lendingclub.mercator.core.MercatorException;

public class DynectException extends MercatorException {
	
	public DynectException (String message, Throwable cause) {
		super(message, cause);
	}
	
	public DynectException(String message) {
		super(message);
	}
}
