/*
 * Copyright 2015 Robert von Burg <eitch@eitchnet.ch>
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
package li.strolch.model.json;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import li.strolch.exception.StrolchException;
import li.strolch.model.AbstractStrolchElement;
import li.strolch.model.GroupedParameterizedElement;
import li.strolch.model.Order;
import li.strolch.model.ParameterBag;
import li.strolch.model.ParameterizedElement;
import li.strolch.model.Resource;
import li.strolch.model.State;
import li.strolch.model.StrolchRootElement;
import li.strolch.model.StrolchValueType;
import li.strolch.model.Tags;
import li.strolch.model.Version;
import li.strolch.model.activity.Action;
import li.strolch.model.activity.Activity;
import li.strolch.model.activity.TimeOrdering;
import li.strolch.model.parameter.Parameter;
import li.strolch.model.policy.PolicyDef;
import li.strolch.model.policy.PolicyDefs;
import li.strolch.model.timedstate.StrolchTimedState;
import li.strolch.model.timevalue.IValue;
import li.strolch.model.timevalue.impl.ValueChange;
import li.strolch.utils.dbc.DBC;
import li.strolch.utils.helper.StringHelper;
import li.strolch.utils.iso8601.ISO8601FormatFactory;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class StrolchElementFromJsonVisitor {

	public void fillElement(JsonObject jsonObject, Order order) {
		fillElement(jsonObject, (GroupedParameterizedElement) order);

		parseVersion(order, jsonObject);

		// policies
		PolicyDefs defs = parsePolicies(jsonObject);
		if (defs.hasPolicyDefs())
			order.setPolicyDefs(defs);

		// attributes
		if (jsonObject.has(Tags.Json.DATE)) {
			String date = jsonObject.get(Tags.Json.DATE).getAsString();
			order.setDate(ISO8601FormatFactory.getInstance().getDateFormat().parse(date));
		} else {
			order.setDate(ISO8601FormatFactory.getInstance().getDateFormat().parse(StringHelper.DASH)); //$NON-NLS-1$
		}

		if (jsonObject.has(Tags.Json.STATE)) {
			order.setState(State.parse(jsonObject.get(Tags.Json.STATE).getAsString()));
		} else {
			order.setState(State.CREATED);
		}
	}

	public void fillElement(JsonObject jsonObject, Resource resource) {
		fillElement(jsonObject, (GroupedParameterizedElement) resource);

		parseVersion(resource, jsonObject);

		// policies
		PolicyDefs defs = parsePolicies(jsonObject);
		if (defs.hasPolicyDefs())
			resource.setPolicyDefs(defs);

		// time states
		if (!jsonObject.has(Tags.Json.TIMED_STATES))
			return;

		JsonObject timedStatesJ = jsonObject.getAsJsonObject(Tags.Json.TIMED_STATES);
		Set<Entry<String, JsonElement>> entrySet = timedStatesJ.entrySet();
		for (Entry<String, JsonElement> entry : entrySet) {

			String stateId = entry.getKey();
			JsonObject timeStateJ = entry.getValue().getAsJsonObject();

			// evaluate type of TimedState
			String typeS = timeStateJ.get(Tags.Json.TYPE).getAsString();
			DBC.PRE.assertNotEmpty("Type must be set on TimedState for resource with id " + resource.getId(), typeS);
			StrolchValueType valueType = StrolchValueType.parse(typeS);
			StrolchTimedState<? extends IValue<?>> timedState = valueType.timedStateInstance();

			// attributes
			fillElement(timeStateJ, (AbstractStrolchElement) timedState);

			// consistency of JSON key and object.id
			if (!stateId.equals(timedState.getId())) {
				String msg = "Check the values of the jsonElement: {0} JsonObject key not same as object.id!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, timeStateJ);
				throw new StrolchException(msg);
			}

			// further attributes
			if (timeStateJ.has(Tags.Json.INTERPRETATION))
				timedState.setInterpretation(timeStateJ.get(Tags.Json.INTERPRETATION).getAsString());
			if (timeStateJ.has(Tags.Json.UOM))
				timedState.setUom(timeStateJ.get(Tags.Json.UOM).getAsString());
			if (timeStateJ.has(Tags.Json.INDEX))
				timedState.setIndex(timeStateJ.get(Tags.Json.INDEX).getAsInt());
			if (timeStateJ.has(Tags.Json.HIDDEN))
				timedState.setHidden(timeStateJ.get(Tags.Json.HIDDEN).getAsBoolean());

			resource.addTimedState(timedState);

			if (!timeStateJ.has(Tags.Json.VALUES))
				continue;

			JsonArray valuesJ = timeStateJ.getAsJsonArray(Tags.Json.VALUES);
			valuesJ.forEach(e -> {

				JsonObject timeValueJ = e.getAsJsonObject();

				String timeS = timeValueJ.get(Tags.Json.TIME).getAsString();
				long time = ISO8601FormatFactory.getInstance().parseDate(timeS).getTime();

				String valueS = timeValueJ.get(Tags.Json.VALUE).getAsString();
				timedState.setStateFromStringAt(time, valueS);
			});
		}
	}

	public void fillElement(JsonObject jsonObject, Activity activity) {
		fillElement(jsonObject, (GroupedParameterizedElement) activity);

		if (!jsonObject.has(Tags.Json.TIME_ORDERING))
			throw new StrolchException("TimeOrdering not set on " + activity.getLocator());
		String timeOrderingS = jsonObject.get(Tags.Json.TIME_ORDERING).getAsString();
		TimeOrdering timeOrdering = TimeOrdering.parse(timeOrderingS);
		activity.setTimeOrdering(timeOrdering);

		parseVersion(activity, jsonObject);

		// policies
		PolicyDefs defs = parsePolicies(jsonObject);
		if (defs.hasPolicyDefs())
			activity.setPolicyDefs(defs);

		if (!jsonObject.has(Tags.Json.ELEMENTS))
			return;

		JsonArray elementsJsonArray = jsonObject.getAsJsonArray(Tags.Json.ELEMENTS);
		elementsJsonArray.forEach(e -> {

			JsonObject elementJsonObject = e.getAsJsonObject();
			String objectType = elementJsonObject.get(Tags.Json.OBJECT_TYPE).getAsString();

			switch (objectType) {
			case Tags.Json.ACTIVITY:
				Activity childActivity = new Activity();
				fillElement(elementJsonObject, childActivity);
				activity.addElement(childActivity);
				break;

			case Tags.Json.ACTION:
				Action childAction = new Action();
				fillElement(elementJsonObject, childAction);
				activity.addElement(childAction);
				break;

			default:
				String msg = "Check the values of the jsonObject: {0} unknown object Type {1} !"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, elementJsonObject, objectType);
				throw new StrolchException(msg);
			}
		});
	}

	protected void fillElement(JsonObject jsonObject, AbstractStrolchElement strolchElement) {
		if (jsonObject.has(Tags.Json.ID) && jsonObject.has(Tags.Json.NAME)) {
			strolchElement.setId(jsonObject.get(Tags.Json.ID).getAsString());
			strolchElement.setName(jsonObject.get(Tags.Json.NAME).getAsString());
		} else {
			String msg = "Check the values of the jsonObject: {0} either id or name attribute is null!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, jsonObject);
			throw new StrolchException(msg);
		}
	}

	protected void fillElement(JsonObject jsonObject, GroupedParameterizedElement groupedParameterizedElement) {
		fillElement(jsonObject, (AbstractStrolchElement) groupedParameterizedElement);

		if (jsonObject.has(Tags.Json.TYPE)) {
			groupedParameterizedElement.setType(jsonObject.get(Tags.Json.TYPE).getAsString());
		} else {
			String msg = "Check the values of the jsonObject: {0} type attribute is null!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, jsonObject);
			throw new StrolchException(msg);
		}

		// add all the parameter bags
		if (!jsonObject.has(Tags.Json.PARAMETER_BAGS))
			return;

		JsonObject bagsJsonObject = jsonObject.getAsJsonObject(Tags.Json.PARAMETER_BAGS);

		Set<Entry<String, JsonElement>> bags = bagsJsonObject.entrySet();
		for (Entry<String, JsonElement> entry : bags) {
			String bagId = entry.getKey();
			JsonElement jsonElement = entry.getValue();
			if (!jsonElement.isJsonObject()) {
				String msg = "Check the values of the jsonElement: {0} it is not a JsonObject!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, jsonElement);
				throw new StrolchException(msg);
			}

			JsonObject bagJsonObject = jsonElement.getAsJsonObject();
			ParameterBag bag = new ParameterBag();
			fillElement(bagJsonObject, bag);
			if (!bagId.equals(bag.getId())) {
				String msg = "Check the values of the jsonElement: {0} JsonObject key not same as object.id!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, jsonElement);
				throw new StrolchException(msg);
			}

			groupedParameterizedElement.addParameterBag(bag);
		}
	}

	protected void fillElement(JsonObject jsonObject, ParameterizedElement parameterizedElement) {
		fillElement(jsonObject, (AbstractStrolchElement) parameterizedElement);

		if (jsonObject.has(Tags.Json.TYPE)) {
			parameterizedElement.setType(jsonObject.get(Tags.Json.TYPE).getAsString());
		} else {
			String msg = "Check the values of the jsonObject: {0} type attribute is null!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, jsonObject);
			throw new StrolchException(msg);
		}

		// add all the parameters
		if (!jsonObject.has(Tags.Json.PARAMETERS))
			return;

		JsonObject parametersJsonObject = jsonObject.getAsJsonObject(Tags.Json.PARAMETERS);
		Set<Entry<String, JsonElement>> parameters = parametersJsonObject.entrySet();
		for (Entry<String, JsonElement> entry : parameters) {
			String paramId = entry.getKey();
			JsonElement jsonElement = entry.getValue();
			if (!jsonElement.isJsonObject()) {
				String msg = "Check the values of the jsonElement: {0} it is not a JsonObject!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, jsonElement);
				throw new StrolchException(msg);
			}

			JsonObject paramJsonObject = jsonElement.getAsJsonObject();
			String paramtype = paramJsonObject.get(Tags.Json.TYPE).getAsString();

			StrolchValueType paramValueType = StrolchValueType.parse(paramtype);
			Parameter<?> parameter = paramValueType.parameterInstance();
			fillElement(paramJsonObject, parameter);
			if (!paramId.equals(parameter.getId())) {
				String msg = "Check the values of the jsonElement: {0} JsonObject key not same as object.id!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, jsonElement);
				throw new StrolchException(msg);
			}

			parameterizedElement.addParameter(parameter);
		}
	}

	protected void fillElement(JsonObject jsonObject, Parameter<?> param) {
		fillElement(jsonObject, (AbstractStrolchElement) param);

		if (jsonObject.has(Tags.Json.INTERPRETATION))
			param.setInterpretation(jsonObject.get(Tags.Json.INTERPRETATION).getAsString());
		if (jsonObject.has(Tags.Json.UOM))
			param.setUom(jsonObject.get(Tags.Json.UOM).getAsString());
		if (jsonObject.has(Tags.Json.INDEX))
			param.setIndex(jsonObject.get(Tags.Json.INDEX).getAsInt());
		if (jsonObject.has(Tags.Json.HIDDEN))
			param.setHidden(jsonObject.get(Tags.Json.HIDDEN).getAsBoolean());

		String value = jsonObject.get(Tags.Json.VALUE).getAsString();
		param.setValueFromString(value);
	}

	protected void fillElement(JsonObject jsonObject, Action action) {
		fillElement(jsonObject, (GroupedParameterizedElement) action);

		// attributes
		if (jsonObject.has(Tags.Json.RESOURCE_ID))
			action.setResourceId(jsonObject.get(Tags.Json.RESOURCE_ID).getAsString());
		if (jsonObject.has(Tags.Json.RESOURCE_TYPE))
			action.setResourceType(jsonObject.get(Tags.Json.RESOURCE_TYPE).getAsString());
		if (jsonObject.has(Tags.Json.STATE))
			action.setState(State.parse(jsonObject.get(Tags.Json.STATE).getAsString()));

		// policies
		PolicyDefs defs = parsePolicies(jsonObject);
		if (defs.hasPolicyDefs())
			action.setPolicyDefs(defs);

		// value changes
		if (!jsonObject.has(Tags.Json.VALUE_CHANGES))
			return;

		JsonArray valueChangesJ = jsonObject.getAsJsonArray(Tags.Json.VALUE_CHANGES);
		valueChangesJ.forEach(e -> {
			try {
				JsonObject valueChangeJ = e.getAsJsonObject();

				String stateId = valueChangeJ.get(Tags.Json.STATE_ID).getAsString();
				String timeS = valueChangeJ.get(Tags.Json.TIME).getAsString();
				String valueS = valueChangeJ.get(Tags.Json.VALUE).getAsString();
				String typeS = valueChangeJ.get(Tags.Json.TYPE).getAsString();

				StrolchValueType type = StrolchValueType.parse(typeS);
				IValue<?> value = type.valueInstance(valueS);

				long time = ISO8601FormatFactory.getInstance().getDateFormat().parse(timeS).getTime();
				ValueChange<IValue<?>> valueChange = new ValueChange<>(time, value, stateId);

				action.addChange(valueChange);

			} catch (Exception e1) {
				String msg = "Check the values of the jsonElement: {0} Fields invalid!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, e);
				throw new StrolchException(msg, e1);
			}
		});
	}

	protected PolicyDefs parsePolicies(JsonObject jsonObject) {

		PolicyDefs policyDefs = new PolicyDefs();

		if (!jsonObject.has(Tags.Json.POLICIES))
			return policyDefs;

		JsonObject policiesJsonObject = jsonObject.getAsJsonObject(Tags.Json.POLICIES);

		Set<Entry<String, JsonElement>> entrySet = policiesJsonObject.entrySet();
		for (Entry<String, JsonElement> entry : entrySet) {

			String type = entry.getKey();
			String value = entry.getValue().getAsString();

			policyDefs.addOrUpdate(PolicyDef.valueOf(type, value));
		}

		return policyDefs;
	}

	protected void parseVersion(StrolchRootElement rootElement, JsonObject jsonObject) {

		if (!jsonObject.has(Tags.Json.VERSION))
			return;

		JsonObject versionJ = jsonObject.getAsJsonObject(Tags.Json.VERSION);

		int v = versionJ.get(Tags.Json.VERSION).getAsInt();
		String createdBy = versionJ.get(Tags.Json.CREATED_BY).getAsString();
		String createdAtS = versionJ.get(Tags.Json.CREATED_AT).getAsString();
		Date createdAt = ISO8601FormatFactory.getInstance().parseDate(createdAtS);
		boolean deleted = versionJ.get(Tags.Json.DELETED).getAsBoolean();

		Version version = new Version(rootElement.getLocator(), v, createdBy, createdAt, deleted);
		rootElement.setVersion(version);
	}
}
