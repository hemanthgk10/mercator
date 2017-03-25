/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.mercator.vmware;

import java.util.Map;

import org.lendingclub.mercator.core.ScannerBuilder;

import com.google.common.collect.Maps;

public class VMWareScannerBuilder extends ScannerBuilder<VMWareScanner> {

	Map<String, String> props = Maps.newConcurrentMap();

	public static final String URL_PROPERTY="vmware.url";
	public static final String USERNAME_PROPERTY="vmware.username";
	public static final String PASSWORD_PROPERTY="vmware.password";
	public static final String VALIDATE_CERTS_PROPERTY="vmware.validateCerts";
	
	public VMWareScannerBuilder withUrl(String url) {

		setProperty(URL_PROPERTY, url);
		return this;
	}

	public VMWareScannerBuilder withUsername(String username) {

		setProperty(USERNAME_PROPERTY, username);
		return this;
	}

	public VMWareScannerBuilder withPassword(String password) {

		setProperty(PASSWORD_PROPERTY, password);
		return this;
	}

	private void setProperty(String key, String val) {
		if (val == null) {
			props.remove(key);
		} else {
			props.put(key, val);
		}
	}

	@Override
	public VMWareScanner build() {

		return new VMWareScanner(this, props);

	}

}
