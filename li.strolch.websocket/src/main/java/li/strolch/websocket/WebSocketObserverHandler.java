package li.strolch.websocket;

import static li.strolch.rest.StrolchRestfulConstants.DATA;
import static li.strolch.rest.StrolchRestfulConstants.MSG;
import static li.strolch.utils.helper.StringHelper.DASH;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import li.strolch.agent.api.Observer;
import li.strolch.agent.api.ObserverHandler;
import li.strolch.model.StrolchRootElement;
import li.strolch.model.Tags;
import li.strolch.utils.collections.MapOfLists;
import li.strolch.utils.collections.MapOfSets;

public class WebSocketObserverHandler implements Observer {

	private ObserverHandler observerHandler;
	private WebSocketClient client;

	private MapOfSets<String, String> observedTypes;
	private Map<String, Boolean> asFlat;

	public WebSocketObserverHandler(ObserverHandler observerHandler, WebSocketClient client) {
		this.observerHandler = observerHandler;
		this.client = client;
		this.observedTypes = new MapOfSets<>();
		this.asFlat = new HashMap<>(1);
	}

	public void register(String objectType, String type, boolean flat) {
		if (!this.observedTypes.containsSet(objectType)) {
			this.observerHandler.registerObserver(objectType, this);
		}
		this.observedTypes.addElement(objectType, type);
		this.asFlat.put(type, flat);
	}

	public void unregister(String objectType, String type) {
		this.observedTypes.removeElement(objectType, type);
		if (!this.observedTypes.containsSet(objectType)) {
			this.observerHandler.unregisterObserver(objectType, this);
		}
	}

	public void unregisterAll() {
		this.observedTypes.keySet().forEach(key -> this.observerHandler.unregisterObserver(key, this));
	}

	@Override
	public void add(String key, List<StrolchRootElement> elements) {
		handleUpdate("ObserverAdd", key, elements);
	}

	@Override
	public void update(String key, List<StrolchRootElement> elements) {
		handleUpdate("ObserverUpdate", key, elements);
	}

	@Override
	public void remove(String key, List<StrolchRootElement> elements) {
		handleUpdate("ObserverRemove", key, elements);
	}

	private void handleUpdate(String updateType, String key, List<StrolchRootElement> elements) {
		Set<String> observedTypesSet = this.observedTypes.getSet(key);
		if (observedTypesSet == null)
			return;

		MapOfLists<String, JsonObject> data = elements.stream().filter(e -> observedTypesSet.contains(e.getType()))
				.map(e -> {
					if (this.asFlat.get(e.getType()))
						return e.toFlatJsonObject();
					else
						return e.toJsonObject();
				}).collect(MapOfLists::new, (mol, e) -> mol.addElement(e.get(Tags.Json.TYPE).getAsString(), e),
						MapOfLists::addAll);
		if (data.isEmpty())
			return;

		data.keySet().forEach(type -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty(MSG, DASH);
			jsonObject.addProperty(Tags.Json.MSG_TYPE, updateType);
			jsonObject.addProperty(Tags.Json.OBJECT_TYPE, key);
			jsonObject.addProperty(Tags.Json.TYPE, type);
			JsonArray elementsJ = new JsonArray();
			data.getList(type).forEach(elementsJ::add);
			jsonObject.add(DATA, elementsJ);
			this.client.sendMessage(jsonObject.toString());
		});
	}
}
