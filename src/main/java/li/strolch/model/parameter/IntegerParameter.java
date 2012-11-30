/*
 * Copyright (c) 2012, Robert von Burg
 *
 * All rights reserved.
 *
 * This file is part of li.strolch.model.
 *
 *  li.strolch.model is free software: you can redistribute 
 *  it and/or modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  li.strolch.model is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with li.strolch.model.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package li.strolch.model.parameter;

import li.strolch.exception.StrolchException;

import org.dom4j.Element;

import ch.eitchnet.utils.helper.StringHelper;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public class IntegerParameter extends AbstractParameter<Integer> {

	public static final String TYPE = "Integer";
	private static final long serialVersionUID = 0L;

	private Integer value = Integer.MAX_VALUE;

	/**
	 * Empty constructor
	 */
	public IntegerParameter() {
		//
	}

	/**
	 * Default constructor
	 * 
	 * @param id
	 * @param name
	 * @param value
	 */
	public IntegerParameter(String id, String name, Integer value) {
		super(id, name);
		setValue(value);
	}

	/**
	 * DOM Constructor
	 * 
	 * @param element
	 */
	public IntegerParameter(Element element) {
		super.fromDom(element);

		String valueS = element.attributeValue("Value");
		if (StringHelper.isEmpty(valueS)) {
			throw new StrolchException("No value defined for " + this.id);
		}

		setValue(Integer.valueOf(valueS));
	}

	@Override
	public String getType() {
		return IntegerParameter.TYPE;
	}

	@Override
	public String getValueAsString() {
		return Integer.toString(this.value);
	}

	@Override
	public Integer getValue() {
		return this.value;
	}

	@Override
	public void setValue(Integer value) {
		validateValue(value);
		this.value = value;
	}

	@Override
	public Parameter<Integer> getClone() {
		IntegerParameter clone = new IntegerParameter();

		super.fillClone(clone);

		clone.setValue(this.value);

		return clone;
	}
}
