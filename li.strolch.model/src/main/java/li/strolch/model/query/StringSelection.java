/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
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
package li.strolch.model.query;

import ch.eitchnet.utils.StringMatchMode;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class StringSelection {

	private StringMatchMode matchMode;
	private String[] values;

	public StringSelection() {
		//
	}

	/**
	 * @param matchMode
	 * @param values
	 */
	public StringSelection(StringMatchMode matchMode, String[] values) {
		this.matchMode = matchMode;
		this.values = values;
	}

	/**
	 * @return the matchMode
	 */
	public StringMatchMode getMatchMode() {
		return this.matchMode;
	}

	/**
	 * @param matchMode
	 *            the matchMode to set
	 */
	public void setMatchMode(StringMatchMode matchMode) {
		this.matchMode = matchMode;
	}

	/**
	 * @return the values
	 */
	public String[] getValues() {
		return this.values;
	}

	/**
	 * @param values
	 *            the values to set
	 */
	public void setValues(String... values) {
		this.values = values;
	}

	/**
	 * @return
	 */
	public boolean isWildCard() {
		return this.values == null || this.values.length == 0;
	}

	public boolean matches(String value) {
		for (String sel : this.values) {
			if (this.matchMode.matches(value, sel))
				return true;
		}
		return false;
	}
}