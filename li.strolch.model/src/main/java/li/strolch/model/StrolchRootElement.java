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
package li.strolch.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import li.strolch.model.activity.Activity;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.model.visitor.StrolchElementVisitor;
import li.strolch.model.visitor.StrolchRootElementVisitor;
import li.strolch.model.xml.StrolchElementToXmlStringVisitor;

/**
 * Root element for all top level {@link StrolchElement}. These are elements which have no parent, e.g. {@link Resource
 * Resources} and {@link Order Orders}. Every root element has a version, so that versions can be kept of an object
 *
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public interface StrolchRootElement extends StrolchElement, PolicyContainer, ParameterBagContainer {

	/**
	 * Returns the object type
	 *
	 * @return the object type
	 */
	String getObjectType();

	/**
	 * Set the type of this {@link StrolchRootElement}. Not that this method should only be called for new elements, not
	 * if this element has already been persisted!
	 *
	 * @param type
	 * 		the new type
	 */
	void setType(String type);

	/**
	 * Returns true if this {@link StrolchRootElement} has a version set
	 *
	 * @return true if this {@link StrolchRootElement} has a version set
	 */
	boolean hasVersion();

	/**
	 * Returns the current version of this object, or null if no version is set
	 *
	 * @return the current version of this object, or null if no version is set
	 */
	Version getVersion();

	/**
	 * <p>
	 * Sets the version of this object
	 * </p>
	 *
	 * @param version
	 * 		the version to set
	 *
	 * @throws IllegalArgumentException
	 * 		if the given version's locator is not equal to the current element's locator
	 */
	void setVersion(Version version) throws IllegalArgumentException;

	/**
	 * Return a clone of this {@link StrolchElement}
	 *
	 * @return a clone of this {@link StrolchElement}
	 */
	@Override
	StrolchRootElement getClone();

	/**
	 * Return a clone of this {@link StrolchElement}
	 *
	 * @return a clone of this {@link StrolchElement}
	 */
	StrolchRootElement getClone(boolean withVersion);

	/**
	 * Visitor pattern accept method. Takes a {@link StrolchRootElementVisitor} to visit this element
	 *
	 * @param visitor
	 * 		the visitor
	 *
	 * @return the result of the visitation
	 */
	<T> T accept(StrolchElementVisitor<T> visitor);

	/**
	 * Formats this {@link StrolchRootElement} as an XML string
	 *
	 * @return the formatted XML string
	 */
	default String toXmlString() {
		return accept(new StrolchElementToXmlStringVisitor().withoutDocument());
	}

	/**
	 * Formats this element as a JSON string
	 *
	 * @return the formatted JSON string
	 */
	default String toJsonString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.accept(new StrolchRootElementToJsonVisitor()));
	}

	/**
	 * Formats this element as a flat JSON string, i.e. all parameter bags are removed and parameters are on the root
	 * element of the JSON object
	 *
	 * @return the formatted JSON string
	 */
	default String toFlatJsonString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.accept(new StrolchRootElementToJsonVisitor().flat()));
	}

	/**
	 * Formats this element to a {@link JsonObject}
	 *
	 * @return the {@link JsonObject}
	 */
	default JsonObject toJsonObject() {
		return this.accept(new StrolchRootElementToJsonVisitor());
	}

	/**
	 * Formats this element to a flat {@link JsonObject}
	 *
	 * @return the {@link JsonObject}
	 */
	default JsonObject toFlatJsonObject() {
		return this.accept(new StrolchRootElementToJsonVisitor().flat());
	}

	/**
	 * Casts this element to a Resource
	 *
	 * @return Casts this element to a Resource
	 */
	default Resource asResource() {
		return (Resource) this;
	}

	/**
	 * Casts this element to a Order
	 *
	 * @return Casts this element to a Order
	 */
	default Order asOrder() {
		return (Order) this;
	}

	/**
	 * Casts this element to a Activity
	 *
	 * @return Casts this element to a Activity
	 */
	default Activity asActivity() {
		return (Activity) this;
	}
}
