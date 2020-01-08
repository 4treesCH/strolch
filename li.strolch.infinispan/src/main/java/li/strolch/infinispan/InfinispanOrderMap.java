package li.strolch.infinispan;

import static java.util.stream.Collectors.toSet;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import li.strolch.agent.impl.TransientOrderMap;
import li.strolch.exception.StrolchElementNotFoundException;
import li.strolch.exception.StrolchException;
import li.strolch.model.Order;
import li.strolch.model.Version;
import li.strolch.model.query.OrderQuery;
import li.strolch.persistence.api.StrolchPersistenceException;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.utils.ObjectHelper;
import org.infinispan.commons.api.BasicCache;

public class InfinispanOrderMap extends TransientOrderMap {

	private BasicCache<ElementKey, Order> cache;

	public InfinispanOrderMap(BasicCache<ElementKey, Order> cache) {
		this.cache = cache;
	}

	@Override
	public <U> List<U> doQuery(StrolchTransaction tx, OrderQuery<U> query) {
		throw new UnsupportedOperationException("Performing querying not supported!");
	}

	@Override
	public boolean hasType(StrolchTransaction tx, String type) {
		return this.cache.keySet().stream().anyMatch(elementKey -> elementKey.getType().equals(type));
	}

	@Override
	public boolean hasElement(StrolchTransaction tx, String type, String id) {
		return this.cache.containsKey(new ElementKey(type, id));
	}

	@Override
	public long querySize(StrolchTransaction tx) {
		return this.cache.size();
	}

	@Override
	public long querySize(StrolchTransaction tx, String type) {
		return this.cache.keySet().stream().filter(elementKey -> elementKey.getType().equals(type)).count();
	}

	@Override
	public Order getBy(StrolchTransaction tx, String type, String id, boolean assertExists) throws StrolchException {

		Order t = this.cache.get(new ElementKey(type, id));
		if (assertExists && t == null) {
			String msg = "The element with type \"{0}\" and id \"{1}\" does not exist!"; //$NON-NLS-1$
			throw new StrolchElementNotFoundException(MessageFormat.format(msg, type, id));
		}

		if (t == null)
			return null;

		return t.getClone(true);
	}

	@Override
	public List<Order> getAllElements(StrolchTransaction tx) {
		return new ArrayList<>(this.cache.values());
	}

	@Override
	public List<Order> getElementsBy(StrolchTransaction tx, String type) {
		return null;
	}

	@Override
	public Stream<Order> stream(StrolchTransaction tx, String... types) {
		return this.cache.values().stream().filter(element -> ObjectHelper.isIn(element.getType(), types, false));
	}

	@Override
	public Set<String> getTypes(StrolchTransaction tx) {
		return this.cache.keySet().stream().map(ElementKey::getType).collect(toSet());
	}

	@Override
	public Set<String> getAllKeys(StrolchTransaction tx) {
		return this.cache.keySet().stream().map(ElementKey::getId).collect(toSet());
	}

	@Override
	public Set<String> getKeysBy(StrolchTransaction tx, String type) {
		return this.cache.keySet().stream().filter(e -> e.getType().equals(type)).map(ElementKey::getId)
				.collect(toSet());
	}

	@Override
	public void add(StrolchTransaction tx, Order element) throws StrolchPersistenceException {
		internalAdd(tx, element);
	}

	@Override
	public void addAll(StrolchTransaction tx, List<Order> elements) throws StrolchPersistenceException {
		for (Order element : elements) {
			internalAdd(tx, element);
		}
	}

	@Override
	protected void internalAdd(StrolchTransaction tx, Order element) {
		Version.updateVersionFor(element, 0, tx.getCertificate().getUsername(), false);

		// make read only
		element.setReadOnly();

		// assert no object already exists with this id
		if (this.cache.containsKey(new ElementKey(element.getType(), element.getId()))) {
			String msg = "An element already exists with the id \"{0}\". Elements of the same class must always have a unique id, regardless of their type!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, element.getId());
			throw new StrolchPersistenceException(msg);
		}

		this.cache.put(new ElementKey(element.getType(), element.getId()), element);
	}

	@Override
	public void update(StrolchTransaction tx, Order element) throws StrolchPersistenceException {
		internalUpdate(tx, element);
	}

	@Override
	public void updateAll(StrolchTransaction tx, List<Order> elements) throws StrolchPersistenceException {
		for (Order element : elements) {
			internalUpdate(tx, element);
		}
	}

	@Override
	protected void internalUpdate(StrolchTransaction tx, Order element) {
		Version.updateVersionFor(element, 0, tx.getCertificate().getUsername(), false);

		// make read only
		element.setReadOnly();

		// assert no object already exists with this id
		if (!this.cache.containsKey(new ElementKey(element.getType(), element.getId()))) {
			String msg = "The element does not yet exist with the type \"{0}\" and id \"{1}\". Use add() for new objects!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, element.getType(), element.getId());
			throw new StrolchPersistenceException(msg);
		}

		this.cache.put(new ElementKey(element.getType(), element.getId()), element);
	}

	@Override
	public void remove(StrolchTransaction tx, Order element) throws StrolchPersistenceException {
		this.cache.remove(new ElementKey(element.getType(), element.getId()));
	}

	@Override
	public void removeAll(StrolchTransaction tx, List<Order> elements) throws StrolchPersistenceException {
		for (Order element : elements) {
			this.cache.remove(new ElementKey(element.getType(), element.getId()));
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
		Set<ElementKey> elementKeys = this.cache.keySet().stream().filter(e -> e.getType().equals(type))
				.collect(toSet());
		int size = elementKeys.size();
		for (ElementKey key : elementKeys) {
			this.cache.remove(key);
		}
		return size;
	}

	@Override
	public Order getBy(StrolchTransaction tx, String type, String id, int version) {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public Order getBy(StrolchTransaction tx, String type, String id, int version, boolean assertExists)
			throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public List<Order> getVersionsFor(StrolchTransaction tx, String type, String id) {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public int getLatestVersionFor(StrolchTransaction tx, String type, String id) {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public void undoVersion(StrolchTransaction tx, Order element) throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public Order revertToVersion(StrolchTransaction tx, Order element) throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}

	@Override
	public Order revertToVersion(StrolchTransaction tx, String type, String id, int version) throws StrolchException {
		throw new UnsupportedOperationException("Versioning unsupported!");
	}
}
