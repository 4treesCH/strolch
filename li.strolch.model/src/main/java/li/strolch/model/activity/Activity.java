/*
 * Copyright 2013 Martin Smock <smock.martin@gmail.com>
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
package li.strolch.model.activity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import li.strolch.exception.StrolchException;
import li.strolch.model.GroupedParameterizedElement;
import li.strolch.model.Locator;
import li.strolch.model.Locator.LocatorBuilder;
import li.strolch.model.State;
import li.strolch.model.StrolchElement;
import li.strolch.model.StrolchRootElement;
import li.strolch.model.Tags;
import li.strolch.model.visitor.StrolchRootElementVisitor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parameterized object grouping a collection of {@link Activity} and
 * {@link Action} objects defining the process to be scheduled
 * 
 * @author Martin Smock <martin.smock@bluewin.ch>
 */
public class Activity extends GroupedParameterizedElement implements IActivityElement, StrolchRootElement {

	private static final long serialVersionUID = 1L;

	protected Activity parent;

	public Activity(String id, String name, String type) {
		super(id, name, type);
	}

	// use a LinkedHashMap since we will iterate elements in the order added and
	// lookup elements by ID
	protected Map<String, IActivityElement> elements = new LinkedHashMap<String, IActivityElement>();

	/**
	 * add an activity element to the <code>LinkedHashMap</code> of
	 * <code>IActivityElements</code>
	 * 
	 * @param activityElement
	 * @return the element added
	 */
	public IActivityElement addElement(IActivityElement activityElement) {
		String id = activityElement.getId();
		if (id == null)
			throw new StrolchException("Cannot add IActivityElement without id.");
		else if (elements.containsKey(id))
			throw new StrolchException("Activiy " + getLocator() + " already contains an activity element with id = " + id);
		else {
			activityElement.setParent(this);
			return elements.put(activityElement.getId(), activityElement);
		}
	}

	/**
	 * get <code>IActivityElement</code> by id
	 * 
	 * @param id
	 *            the id of the <code>IActivityElement</code>
	 * @return IActivityElement
	 */
	public IActivityElement getElement(String id) {
		return elements.get(id);
	}

	/**
	 * @return get the <code>LinkedHashMap</code> of
	 *         <code>IActivityElements</code>
	 */
	public Map<String, IActivityElement> getElements() {
		return elements;
	}

	/**
	 * @return the iterator for entries, which include the id as key and the
	 *         {@link IActivityElement} as value
	 */
	public Iterator<Entry<String, IActivityElement>> elementIterator() {
		return elements.entrySet().iterator();
	}

	public Long getStart() {
		Long start = Long.MAX_VALUE;
		Iterator<Entry<String, IActivityElement>> elementIterator = elementIterator();
		while (elementIterator.hasNext()) {
			IActivityElement action = elementIterator.next().getValue();
			start = Math.min(start, action.getStart());
		}
		return start;
	}

	public Long getEnd() {
		Long end = 0L;
		Iterator<Entry<String, IActivityElement>> elementIterator = elementIterator();
		while (elementIterator.hasNext()) {
			IActivityElement action = elementIterator.next().getValue();
			end = Math.max(end, action.getEnd());
		}
		return end;
	}

	public State getState() {
		State state = State.PLANNED;
		Iterator<Entry<String, IActivityElement>> elementIterator = elementIterator();
		while (elementIterator.hasNext()) {
			IActivityElement child = elementIterator.next().getValue();
			State childState = child.getState();
			if (childState.ordinal() < state.ordinal()) {
				state = childState;
			}
		}
		return state;
	}

	@Override
	public Locator getLocator() {
		LocatorBuilder lb = new LocatorBuilder();
		fillLocator(lb);
		return lb.build();
	}

	@Override
	protected void fillLocator(LocatorBuilder locatorBuilder) {
		locatorBuilder.append(Tags.ACTIVITY).append(getType()).append(getId());
	}

	@Override
	public Element toDom(Document doc) {
		Element element = doc.createElement(Tags.ACTIVITY);
		fillElement(element);
		Iterator<Entry<String, IActivityElement>> elementIterator = elementIterator();
		while (elementIterator.hasNext()) {
			IActivityElement activityElement = elementIterator.next().getValue();
			element.appendChild(activityElement.toDom(doc)); 
		}
		return element;
	}

	@Override
	public StrolchElement getParent() {
		return parent;
	}

	@Override
	public StrolchRootElement getRootElement() {
		return (parent == null) ? this : parent.getRootElement();
	}

	@Override
	public boolean isRootElement() {
		return (parent == null);
	}

	@Override
	public StrolchElement getClone() {
		Activity clone = new Activity(id, name, type);
		Iterator<Entry<String, IActivityElement>> elementIterator = elementIterator();
		while (elementIterator.hasNext()) {
			Entry<String, IActivityElement> next = elementIterator.next();
			clone.elements.put(next.getKey(), (IActivityElement) next.getValue().getClone());
		}
		return clone;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Activity [id=");
		builder.append(this.id);
		builder.append(", name=");
		builder.append(this.name);
		builder.append(", type=");
		builder.append(this.type);
		builder.append(", state=");
		builder.append(this.getState());
		builder.append(", start=");
		builder.append(this.getStart());
		builder.append(", end=");
		builder.append(this.getEnd());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public <T> T accept(StrolchRootElementVisitor<T> visitor) {
		throw new StrolchException("not implemented yet");
	}

	@Override
	public void setParent(Activity activity) {
		this.parent = activity;
	}

}
