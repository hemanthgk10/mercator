package org.lendingclub.mercator.core;

public abstract class ScannerBuilder<T> {

	Projector projector;
	boolean failOnError = false;
	
	public void setProjector(Projector p) {
		this.projector = p;
	}
	public Projector getProjector() {
		return projector;
	}
	public <X extends ScannerBuilder<T>> X withFailOnError(boolean b) {
		this.failOnError = b;
		return (X) this;
	}	
	public boolean isFailOnError() {
		return failOnError;
	}
	public abstract T build();
}
