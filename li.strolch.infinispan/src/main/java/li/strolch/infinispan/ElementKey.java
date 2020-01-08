package li.strolch.infinispan;

import java.io.Serializable;
import java.util.Objects;


public class ElementKey implements Serializable {
	private final String type;
	private final String id;

	public ElementKey(String type, String id) {
		this.type = type;
		this.id = id;
	}

	public String getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ElementKey that = (ElementKey) o;

		if (!Objects.equals(type, that.type))
			return false;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + (id != null ? id.hashCode() : 0);
		return result;
	}
}
