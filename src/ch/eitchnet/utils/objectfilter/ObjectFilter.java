/*
 * Copyright (c) 2012
 * 
 * Michael Gatto <michael@gatto.ch>
 * 
 */

/*
 * This file is part of ch.eitchnet.java.utils
 *
 * Privilege is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privilege is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Privilege.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package ch.eitchnet.utils.objectfilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class implements a filter where modifications to an object are collected, and only the most recent action and
 * version of the object is returned.
 * <p>
 * In its current implementation, any instance of an object may "live" in the cache registered only under one single
 * key.
 * </p>
 * <p>
 * The rules of the cache are defined as follows. The top row represents the state of the cache for an object, given in
 * version O1. Here, "N/A" symbolizes that the object is not yet present in the cache or, if it is a result of an
 * operation, that it is removed from the cache. Each other row symbolizes the next action that is performed on an
 * object, with the cell containing the "final" action for this object with the version of the object to be retained.
 * Err! symbolize incorrect sequences of events that cause an exception.
 * </p>
 * <table border="1">
 * <tr>
 * <td>Action \ State in Cache</td>
 * <td>N/A</td>
 * <td>Add(01)</td>
 * <td>Update(O1)</td>
 * <td>Remove(O1)</td>
 * </tr>
 * <tr>
 * <td>Add (O2)</td>
 * <td>Add(O2)</td>
 * <td>Err!</td>
 * <td>Err!</td>
 * <td>Update(O2)</td>
 * </tr>
 * <tr>
 * <td>Update (O2)</td>
 * <td>Update(O2)</td>
 * <td>Add(O2)</td>
 * <td>Update(O2)</td>
 * <td>Err!</td>
 * </tr>
 * <tr>
 * <td>Remove (O2)</td>
 * <td>Remove(O2)</td>
 * <td>N/A</td>
 * <td>Remove(O2)</td>
 * <td>Err!</td>
 * </tr>
 * </table>
 * 
 * @author Michael Gatto <michael@gatto.ch> (initial version)
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 * @param <T>
 */
public class ObjectFilter<T extends ITransactionObject> {
	
	// XXX think about removing the generic T, as there is no sense in it

	private final static Logger logger = Logger.getLogger(ObjectFilter.class);

	private HashMap<Long, ObjectCache<T>> cache = new HashMap<Long, ObjectCache<T>>();
	private HashSet<String> keySet = new HashSet<String>();

	private static long id = ITransactionObject.UNSET;

	/**
	 * Register, under the given key, the addition of the given object.
	 * <p>
	 * This is the single point where the updating logic is applied for the cache in case of addition. The logic is:
	 * <table border="1" >
	 * <tr>
	 * <td>Action\State in Cache</td>
	 * <td>N/A</td>
	 * <td>Add(01)</td>
	 * <td>Update(O1)</td>
	 * <td>Remove(O1)</td>
	 * </tr>
	 * <tr>
	 * <td>Add (O2)</td>
	 * <td>Add(O2)</td>
	 * <td>Err!</td>
	 * <td>Err!</td>
	 * <td>Update(O2)</td>
	 * </tr>
	 * </table>
	 * 
	 * @param key
	 *            the key to register the object with
	 * @param objectToAdd
	 *            The object for which addition shall be registered.
	 */
	public void add(String key, T objectToAdd) {

		if (logger.isDebugEnabled())
			logger.debug("add object " + objectToAdd + " with key " + key);

		// add the key to the set
		keySet.add(key);

		// BEWARE: you fix a bug here, be sure to update BOTH tables on the logic.
		long id = objectToAdd.getTransactionID();
		if (id == ITransactionObject.UNSET) {
			// The ID of the object has not been set, so it has not been in the cache during this
			// run. Hence, we create an ID and add it to the cache.
			id = dispenseID();
			objectToAdd.setTransactionID(id);
			ObjectCache<T> cacheObj = new ObjectCache<T>(key, objectToAdd, Operation.ADD);
			cache.put(id, cacheObj);
		} else {
			ObjectCache<T> cached = cache.get(Long.valueOf(objectToAdd.getTransactionID()));
			if (cached == null) {
				// The object got an ID during this run, but was not added to the cache.
				// Hence, we add it now, with the current operation.
				ObjectCache<T> cacheObj = new ObjectCache<T>(key, objectToAdd, Operation.ADD);
				cache.put(id, cacheObj);
			} else {
				String existingKey = cached.getKey();
				if (!existingKey.equals(key)) {
					throw new RuntimeException(
							"Invalid key provided for object with transaction ID "
									+ Long.toString(id)
									+ " and operation "
									+ Operation.ADD.toString()
									+ ":  existing key is "
									+ existingKey
									+ ", new key is "
									+ key
									+ ". Object may be present in the same filter instance only once, registered using one key only. Object:"
									+ objectToAdd.toString());
				}
				// The object is in cache: update the version as required, keeping in mind that most
				// of the cases here will be mistakes...
				Operation op = cached.getOperation();
				switch (op) {
				case ADD:
					throw new RuntimeException("Stale State exception. Invalid + after +");
				case MODIFY:
					throw new RuntimeException("Stale State exception. Invalid + after +=");
				case REMOVE:
					cached.setObject(objectToAdd);
					cached.setOperation(Operation.MODIFY);
					break;
				} // switch
			}// else of object not in cache
		}// else of ID not set
	}

	/**
	 * Register, under the given key, the update of the given object. * </p>
	 * <table border="1">
	 * <tr>
	 * <td>Action \ State in Cache</td>
	 * <td>N/A</td>
	 * <td>Add(01)</td>
	 * <td>Update(O1)</td>
	 * <td>Remove(O1)</td>
	 * </tr>
	 * <tr>
	 * <td>Update (O2)</td>
	 * <td>Update(O2)</td>
	 * <td>Add(O2)</td>
	 * <td>Update(O2)</td>
	 * <td>Err!</td>
	 * </tr>
	 * </table>
	 * 
	 * @param key
	 *            the key to register the object with
	 * @param objectToUpdate
	 *            The object for which update shall be registered.
	 */
	public void update(String key, T objectToUpdate) {

		if (logger.isDebugEnabled())
			logger.debug("update object " + objectToUpdate + " with key " + key);

		// add the key to the keyset
		keySet.add(key);
		// BEWARE: you fix a bug here, be sure to update BOTH tables on the logic.

		long id = objectToUpdate.getTransactionID();
		if (id == ITransactionObject.UNSET) {
			id = dispenseID();
			objectToUpdate.setTransactionID(id);
			ObjectCache<T> cacheObj = new ObjectCache<T>(key, objectToUpdate, Operation.MODIFY);
			cache.put(id, cacheObj);
		} else {
			ObjectCache<T> cached = cache.get(Long.valueOf(objectToUpdate.getTransactionID()));
			if (cached == null) {
				// The object got an ID during this run, but was not added to this cache.
				// Hence, we add it now, with the current operation.
				ObjectCache<T> cacheObj = new ObjectCache<T>(key, objectToUpdate, Operation.MODIFY);
				cache.put(id, cacheObj);
			} else {
				String existingKey = cached.getKey();
				if (!existingKey.equals(key)) {

					throw new RuntimeException(
							"Invalid key provided for object with transaction ID "
									+ Long.toString(id)
									+ " and operation "
									+ Operation.MODIFY.toString()
									+ ":  existing key is "
									+ existingKey
									+ ", new key is "
									+ key
									+ ". Object may be present in the same filter instance only once, registered using one key only. Object:"
									+ objectToUpdate.toString());
				}
				// The object is in cache: update the version as required.
				Operation op = cached.getOperation();
				switch (op) {
				case ADD:
					cached.setObject(objectToUpdate);
					break;
				case MODIFY:
					cached.setObject(objectToUpdate);
					break;
				case REMOVE:
					throw new RuntimeException("Stale State exception: Invalid += after -");
				} // switch
			}// else of object not in cache
		}// else of ID not set
	}

	/**
	 * Register, under the given key, the removal of the given object. * </p>
	 * <table border="1">
	 * <tr>
	 * <td>Action \ State in Cache</td>
	 * <td>N/A</td>
	 * <td>Add(01)</td>
	 * <td>Update(O1)</td>
	 * <td>Remove(O1)</td>
	 * </tr>
	 * <tr>
	 * <td>Remove (O2)</td>
	 * <td>Remove(O2)</td>
	 * <td>N/A</td>
	 * <td>Remove(O2)</td>
	 * <td>Err!</td>
	 * </tr>
	 * </table>
	 * 
	 * @param key
	 *            the key to register the object with
	 * @param objectToRemove
	 *            The object for which removal shall be registered.
	 */
	public void remove(String key, T objectToRemove) {

		if (logger.isDebugEnabled())
			logger.debug("remove object " + objectToRemove + " with key " + key);

		// add the key to the keyset
		keySet.add(key);
		// BEWARE: you fix a bug here, be sure to update BOTH tables on the logic.
		long id = objectToRemove.getTransactionID();
		if (id == ITransactionObject.UNSET) {
			id = dispenseID();
			objectToRemove.setTransactionID(id);
			ObjectCache<T> cacheObj = new ObjectCache<T>(key, objectToRemove, Operation.REMOVE);
			cache.put(id, cacheObj);
		} else {
			ObjectCache<T> cached = cache.get(Long.valueOf(id));
			if (cached == null) {
				// The object got an ID during this run, but was not added to this cache.
				// Hence, we add it now, with the current operation.
				ObjectCache<T> cacheObj = new ObjectCache<T>(key, objectToRemove, Operation.REMOVE);
				cache.put(id, cacheObj);
			} else {
				String existingKey = cached.getKey();
				if (!existingKey.equals(key)) {
					throw new RuntimeException(
							"Invalid key provided for object with transaction ID "
									+ Long.toString(id)
									+ " and operation "
									+ Operation.REMOVE.toString()
									+ ":  existing key is "
									+ existingKey
									+ ", new key is "
									+ key
									+ ". Object may be present in the same filter instance only once, registered using one key only. Object:"
									+ objectToRemove.toString());
				}
				// The object is in cache: update the version as required.
				Operation op = cached.getOperation();
				switch (op) {
				case ADD:
					// this is a case where we're removing the object from the cache, since we are
					// removing it now and it was added previously.
					cache.remove(Long.valueOf(id));
					break;
				case MODIFY:
					cached.setObject(objectToRemove);
					cached.setOperation(Operation.REMOVE);
					break;
				case REMOVE:
					throw new RuntimeException("Stale State exception. Invalid - after -");
				} // switch
			}
		}
	}

	/**
	 * Register, under the given key, the addition of the given list of objects.
	 * 
	 * @param key
	 *            the key to register the objects with
	 * @param addedObjects
	 *            The objects for which addition shall be registered.
	 */
	public void addAll(String key, Collection<T> addedObjects) {
		for (T addObj : addedObjects) {
			add(key, addObj);
		}
	}

	/**
	 * Register, under the given key, the update of the given list of objects.
	 * 
	 * @param key
	 *            the key to register the objects with
	 * @param updatedObjects
	 *            The objects for which update shall be registered.
	 */
	public void updateAll(String key, Collection<T> updatedObjects) {
		for (T update : updatedObjects) {
			update(key, update);
		}
	}

	/**
	 * Register, under the given key, the removal of the given list of objects.
	 * 
	 * @param key
	 *            the key to register the objects with
	 * @param removedObjects
	 *            The objects for which removal shall be registered.
	 */
	public void removeAll(String key, Collection<T> removedObjects) {
		for (T removed : removedObjects) {
			remove(key, removed);
		}
	}

	/**
	 * Register the addition of the given object. Since no key is provided, the class name is used as a key.
	 * 
	 * @param object
	 *            The object that shall be registered for addition
	 */
	public void add(T object) {
		add(object.getClass().getName(), object);
	}

	/**
	 * Register the update of the given object. Since no key is provided, the class name is used as a key.
	 * 
	 * @param object
	 *            The object that shall be registered for updating
	 */
	public void update(T object) {
		update(object.getClass().getName(), object);
	}

	/**
	 * Register the removal of the given object. Since no key is provided, the class name is used as a key.
	 * 
	 * @param object
	 *            The object that shall be registered for removal
	 */
	public void remove(T object) {
		remove(object.getClass().getName(), object);
	}

	/**
	 * Register the addition of all objects in the list. Since no key is provided, the class name of each object is used
	 * as a key.
	 * 
	 * @param objects
	 *            The objects that shall be registered for addition
	 */
	public void addAll(List<T> objects) {
		for (T addedObj : objects) {
			add(addedObj.getClass().getName(), addedObj);
		}
	}

	/**
	 * Register the updating of all objects in the list. Since no key is provided, the class name of each object is used
	 * as a key.
	 * 
	 * @param updateObjects
	 *            The objects that shall be registered for updating
	 */
	public void updateAll(List<T> updateObjects) {
		for (T update : updateObjects) {
			update(update.getClass().getName(), update);
		}
	}

	/**
	 * Register the removal of all objects in the list. Since no key is provided, the class name of each object is used
	 * as a key.
	 * 
	 * @param removedObjects
	 *            The objects that shall be registered for removal
	 */
	public void removeAll(List<T> removedObjects) {
		for (T removed : removedObjects) {
			remove(removed.getClass().getName(), removed);
		}
	}

	/**
	 * Get all objects that were registered under the given key and that have as a resulting final action an addition.
	 * 
	 * @param key
	 *            The registration key of the objects to match
	 * @return The list of all objects registered under the given key and that need to be added.
	 */
	public List<T> getAdded(String key) {
		List<T> addedObjects = new LinkedList<T>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getOperation() == Operation.ADD && (objectCache.getKey().equals(key))) {
				addedObjects.add(objectCache.getObject());
			}
		}
		return addedObjects;
	}

	/**
	 * Get all objects that were registered under the given key and that have as a resulting final action an addition.
	 * 
	 * @param clazz
	 *            The class type of the object to be retrieved, that acts as an additional filter criterion.
	 * @param key
	 *            The registration key of the objects to match
	 * @return The list of all objects registered under the given key and that need to be added.
	 */
	public <V extends T> List<V> getAdded(Class<V> clazz, String key) {
		List<V> addedObjects = new LinkedList<V>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getOperation() == Operation.ADD && (objectCache.getKey().equals(key))) {
				if (objectCache.getObject().getClass() == clazz) {
					@SuppressWarnings("unchecked")
					V object = (V) objectCache.getObject();
					addedObjects.add(object);
				}
			}
		}
		return addedObjects;
	}

	/**
	 * Get all objects that were registered under the given key and that have as a resulting final action an update.
	 * 
	 * @param key
	 *            registration key of the objects to match
	 * @return The list of all objects registered under the given key and that need to be updated.
	 */
	public List<T> getUpdated(String key) {
		List<T> updatedObjects = new LinkedList<T>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getOperation() == Operation.MODIFY && (objectCache.getKey().equals(key))) {
				updatedObjects.add(objectCache.getObject());
			}
		}
		return updatedObjects;
	}

	/**
	 * Get all objects that were registered under the given key and that have as a resulting final action an update.
	 * 
	 * @param key
	 *            registration key of the objects to match
	 * @return The list of all objects registered under the given key and that need to be updated.
	 */
	public <V extends T> List<V> getUpdated(Class<V> clazz, String key) {
		List<V> updatedObjects = new LinkedList<V>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getOperation() == Operation.MODIFY && (objectCache.getKey().equals(key))) {
				if (objectCache.getObject().getClass() == clazz) {
					@SuppressWarnings("unchecked")
					V object = (V) objectCache.getObject();
					updatedObjects.add(object);
				}
			}
		}
		return updatedObjects;
	}

	/**
	 * Get all objects that were registered under the given key that have as a resulting final action their removal.
	 * 
	 * @param key
	 *            The registration key of the objects to match
	 * @return The list of object registered under the given key that have, as a final action, removal.
	 */
	public List<T> getRemoved(String key) {
		List<T> removedObjects = new LinkedList<T>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getOperation() == Operation.REMOVE && (objectCache.getKey().equals(key))) {
				removedObjects.add(objectCache.getObject());
			}
		}
		return removedObjects;
	}

	/**
	 * Get all objects that were registered under the given key that have as a resulting final action their removal.
	 * 
	 * @param key
	 *            The registration key of the objects to match
	 * @return The list of object registered under the given key that have, as a final action, removal.
	 */
	public <V extends T> List<V> getRemoved(Class<V> clazz, String key) {
		List<V> removedObjects = new LinkedList<V>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getOperation() == Operation.REMOVE && (objectCache.getKey().equals(key))) {
				if (objectCache.getObject().getClass() == clazz) {
					@SuppressWarnings("unchecked")
					V object = (V) objectCache.getObject();
					removedObjects.add(object);
				}
			}
		}
		return removedObjects;
	}

	/**
	 * Get all the objects that were processed in this transaction, that were registered under the given key. No action
	 * is associated to the object.
	 * 
	 * @param key
	 *            The registration key for which the objects shall be retrieved
	 * @return The list of objects matching the given key.
	 */
	public List<T> getAll(String key) {
		List<T> allObjects = new LinkedList<T>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getKey().equals(key)) {
				allObjects.add(objectCache.getObject());
			}
		}
		return allObjects;
	}

	/**
	 * Get all the objects that were processed in this transaction, that were registered under the given key. No action
	 * is associated to the object.
	 * 
	 * @param key
	 *            The registration key for which the objects shall be retrieved
	 * @return The list of objects matching the given key.
	 */
	public List<ObjectCache<T>> getCache(String key) {
		List<ObjectCache<T>> allCache = new LinkedList<ObjectCache<T>>();
		Collection<ObjectCache<T>> allObjs = cache.values();
		for (ObjectCache<T> objectCache : allObjs) {
			if (objectCache.getKey().equals(key)) {
				allCache.add(objectCache);
			}
		}
		return allCache;
	}

	/**
	 * Return the set of keys that are currently present in the object filter.
	 * 
	 * @return The set containing the keys of that have been added to the filter.
	 */
	public Set<String> keySet() {
		return keySet;
	}

	/**
	 * Clear the cache.
	 */
	public void clearCache() {
		cache.clear();
		keySet.clear();
	}

	/**
	 * @return get a unique transaction ID
	 */
	public synchronized long dispenseID() {
		id++;
		if (id == Long.MAX_VALUE) {
			logger.error("Rolling IDs of objectFilter back to 1. Hope this is fine.");
			id = 1;
		}
		return id;
	}
}
