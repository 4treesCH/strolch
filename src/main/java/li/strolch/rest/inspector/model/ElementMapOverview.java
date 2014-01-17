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
package li.strolch.rest.inspector.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "ElementMap")
public class ElementMapOverview {

	@XmlAttribute(name = "elementMapName")
	private String elementMapName;
	@XmlAttribute(name = "size")
	private long size;
	@XmlElement(name = "types", type = TypeOverview.class)
	private List<TypeOverview> typeOverviews;

	public ElementMapOverview() {
		// no-arg constructor for JAXB
	}

	/**
	 * @param elementMapName
	 * @param typeOverviews
	 */
	public ElementMapOverview(String elementMapName, List<TypeOverview> typeOverviews) {
		this.elementMapName = elementMapName;
		this.typeOverviews = typeOverviews;
		this.size = this.typeOverviews.size();
	}

	/**
	 * @return the elementMapName
	 */
	public String getElementMapName() {
		return this.elementMapName;
	}

	/**
	 * @param elementMapName
	 *            the elementMapName to set
	 */
	public void setElementMapName(String elementMapName) {
		this.elementMapName = elementMapName;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return this.size;
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @return the typeOverviews
	 */
	public List<TypeOverview> getTypeOverviews() {
		return this.typeOverviews;
	}

	/**
	 * @param typeOverviews
	 *            the typeOverviews to set
	 */
	public void setTypeOverviews(List<TypeOverview> typeOverviews) {
		this.typeOverviews = typeOverviews;
	}
}
