/*
 * Copyright (c) 2012, Robert von Burg
 *
 * All rights reserved.
 *
 * This file is part of the XXX.
 *
 *  XXX is free software: you can redistribute 
 *  it and/or modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  XXX is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XXX.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package ch.eitchnet.xmlpers.test.impl.rewrite;

import javax.xml.stream.XMLStreamException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.eitchnet.xmlpers.api.SaxParser;
import ch.eitchnet.xmlpers.impl.XmlPersistenceStreamWriter;
import ch.eitchnet.xmlpers.test.model.Parameter;
import ch.eitchnet.xmlpers.test.model.Resource;

class ResourceSaxParser extends DefaultHandler implements SaxParser<Resource> {

	private Resource resource;

	@Override
	public Resource getObject() {
		return this.resource;
	}

	@Override
	public void setObject(Resource object) {
		this.resource = object;
	}

	@Override
	public DefaultHandler getDefaultHandler() {
		return this;
	}

	@Override
	public void write(XmlPersistenceStreamWriter writer) throws XMLStreamException {

		writer.writeElement("Resource");
		writer.writeAttribute("id", this.resource.getId());
		writer.writeAttribute("name", this.resource.getName());
		writer.writeAttribute("type", this.resource.getType());
		for (String paramId : this.resource.getParameterKeySet()) {
			Parameter param = this.resource.getParameterBy(paramId);
			writer.writeEmptyElement("Parameter");
			writer.writeAttribute("id", param.getId());
			writer.writeAttribute("name", param.getName());
			writer.writeAttribute("type", param.getType());
			writer.writeAttribute("value", param.getValue());
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		switch (qName) {
		case "Resource":
			String id = attributes.getValue("id");
			String name = attributes.getValue("name");
			String type = attributes.getValue("type");
			Resource resource = new Resource(id, name, type);
			this.resource = resource;
			break;
		case "Parameter":
			id = attributes.getValue("id");
			name = attributes.getValue("name");
			type = attributes.getValue("type");
			String value = attributes.getValue("value");
			Parameter param = new Parameter(id, name, type, value);
			this.resource.addParameter(param);
			break;
		default:
			throw new IllegalArgumentException("The element '" + qName + "' is unhandled!");
		}
	}
}