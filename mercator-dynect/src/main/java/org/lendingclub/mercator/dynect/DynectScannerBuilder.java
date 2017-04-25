package org.lendingclub.mercator.dynect;

import org.lendingclub.mercator.core.ScannerBuilder;

public class DynectScannerBuilder extends ScannerBuilder<DynectScanner>{
	
	private String companyName;
	private String username;
	private String password;

	public String getCompanyName() {
		return companyName;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public DynectScannerBuilder withProperties(String company, String user, String password) {
		this.companyName = company;
		this.username = user;
		this.password = password;
		return this;
	}

	@Override
	public DynectScanner build() {
		return new DynectScanner(this);
	}

}
