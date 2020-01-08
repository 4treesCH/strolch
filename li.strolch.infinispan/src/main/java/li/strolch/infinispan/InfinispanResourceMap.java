package li.strolch.infinispan;

import static java.util.stream.Collectors.toSet;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import li.strolch.agent.impl.TransientResourceMap;
import li.strolch.exception.StrolchElementNotFoundException;
import li.strolch.exception.StrolchException;
import li.strolch.model.Resource;
import li.strolch.model.Version;
import li.strolch.model.query.ResourceQuery;
import li.strolch.persistence.api.StrolchPersistenceException;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.utils.ObjectHelper;
import org.infinispan.commons.api.BasicCache;

public class InfinispanResourceMap extends TransientResourceMap {

	private BasicCache<String, Resource> cache;

	public InfinispanResourceMap(BasicCache<String, Resource> cache) {
		this.cache = cache;
	}

	@Override
	public <U> List<U> doQuery(StrolchTransaction tx, ResourceQuery<U> query) {
		throw new UnsupportedOperationException("Performing querying not supported!");
	}

	@Override
	public boolean hasType(StrolchTransaction tx, String type) {
		return this.cache.keySet().stream()
				.anyMatch(elementKey -> elementKey.substring(0, elementKey.charAt('_')).equals(type));
	}

	@Override
	public boolean hasElement(StrolchTransaction tx, String type, String id) {
		return this.cache.containsKey(type + "_" + id);
	}

	@Override
	public long querySize(StrolchTransaction tx) {
		return this.cache.size();
	}

	@Override
	public long querySize(StrolchTransaction tx, String type) {
		return this.cache.keySet().stream()
				.filter(elementKey -> elementKey.substring(0, elementKey.charAt('_')).equals(type)).count();
	}

	@Override
	public Resource getBy(StrolchTransaction tx, String type, String id, boolean assertExists) throws StrolchException {

		Resource t = this.cache.get(type + "_" + id);
		if (assertExists && t == null) {
			String msg = "The element with type \"{0}\" and id \"{1}\" does not exist!"; //$NON-NLS-1$
			throw new StrolchElementNotFoundException(MessageFormat.format(msg, type, id));
		}

		if (t == null)
			return null;

		return t.getClone(true);
	}

	@Override
	public List<Resource> getAllElements(StrolchTransaction tx) {
		return new ArrayList<>(this.cache.values());
	}

	@Override
	public List<Resource> getElementsBy(StrolchTransaction tx, String type) {
		return null;
	}

	@Override
	public Stream<Resource> stream(StrolchTransaction tx, String... types) {
		return this.cache.values().stream().filter(element -> ObjectHelper.isIn(element.getType(), types, false));
	}

	@Override
	public Set<String> getTypes(StrolchTransaction tx) {
		return this.cache.keySet().stream().map(e -> e.substring(0, e.charAt('_'))).collect(toSet());
	}

	@Override
	public Set<String> getAllKeys(StrolchTransaction tx) {
		return this.cache.keySet().stream().map(e -> e.substring(e.charAt('_') + 1)).collect(toSet());
	}

	@Override
	public Set<String> getKeysBy(StrolchTransaction tx, String type) {
		return this.cache.keySet().stream().filter(e -> e.substring(0, e.charAt('_')).equals(type))
				.map(e -> e.substring(e.charAt('_') + 1)).collect(toSet());
	}

	@Override
	public void add(StrolchTransaction tx, Resource element) throws StrolchPersistenceException {
		internalAdd(tx, element);
	}

	@Override
	public void addAll(StrolchTransaction tx, List<Resource> elements) throws StrolchPersistenceException {
		for (Resource element : elements) {
			internalAdd(tx, element);
		}
	}

	@Override
	protected void internalAdd(StrolchTransaction tx, Resource element) {
		Version.updateVersionFor(element, 0, tx.getCertificate().getUsername(), false);

		// make read only
		element.setReadOnly();

		// assert no object already exists with this id
		if (this.cache.containsKey(element.getType() + "_" + element.getId())) {
			String msg = "An element already exists with the id \"{0}\". Elements of the same class must always have a unique id, regardless of their type!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, element.getId());
			throw new StrolchPersistenceException(msg);
		}

		this.cache.put(element.getType() + "_" + element.getId(), element);
	}

	@Override
	public void update(StrolchTransaction tx, Resource element) throws StrolchPersistenceException {
		internalUpdate(tx, element);
	}

	@Override
	public void updateAll(StrolchTransaction tx, List<Resource> elements) throws StrolchPersistenceException {
		for (Resource element : elements) {
			internalUpdate(tx, element);
		}
	}

	@Override
	protected void internalUpdate(StrolchTransaction tx, Resource element) {
		Version.updateVersionFor(element, 0, tx.getCertificate().getUsername(), false);

		// make read only
		element.setReadOnly();

		// assert no object already exists with this id
		if (!this.cache.containsKey(element.getType() + "_" + element.getId())) {
			String msg = "The element does not yet exist with the type \"{0}\" and id \"{1}\". Use add() for new objects!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, element.getType(), element.getId());
			throw new StrolchPersistenceException(msg);
		}

		this.cache.put(element.getType() + "_" + element.getId(), element);
	}

	@Override
	public void remove(StrolchTransaction tx, Resource element) throws StrolchPersistenceException {
		this.cache.remove(element.getType() + "_" + element.getId());
	}

	@Override
	public void removeAll(StrolchTransaction tx, List<Resource> elements) throws StrolchPersistenceException {
		for (Resource element : elements) {
			this.cache.remove(element.getType() + "_" + element.getId());
		}
	}

	@Override
	public long removeAll(StrolchTransaction tx) {
		int size = this.cache.size();
		this.cache.clear();
		return size;
	}

	@Override
	public long removeAllBy(StrolchTransaction tx, String type) {
		Set<String> elementKeys = this.cache.keySet().stream().filter(e -> e.substring(0, e.charAt('_')).equals(type))
				.collect(toSet());
		int size = elementKeys.size();
		for (String key : elementKeys) {
			this.cache.remove(key);
		}
		return size;
	}

	@Override
	public Resource getBy(StrolchTransaction tx, String type, String id, int version) {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public Resource getBy(StrolchTransaction tx, String type, String id, int version, boolean assertExists)
			throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public List<Resource> getVersionsFor(StrolchTransaction tx, String type, String id) {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public int getLatestVersionFor(StrolchTransaction tx, String type, String id) {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public void undoVersion(StrolchTransaction tx, Resource element) throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public Resource revertToVersion(StrolchTransaction tx, Resource element) throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public Resource revertToVersion(StrolchTransaction tx, String type, String id, int version)
			throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}
}
