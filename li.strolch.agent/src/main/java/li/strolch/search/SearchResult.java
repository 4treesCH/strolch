package li.strolch.search;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import li.strolch.utils.collections.Paging;

public class SearchResult<T> {

	protected Stream<T> stream;

	public SearchResult(Stream<T> stream) {
		this.stream = stream;
	}

	public Stream<T> asStream() {
		return this.stream;
	}

	public <U> SearchResult<U> map(Function<T, U> mapper) {
		return new SearchResult<>(this.stream.map(mapper));
	}

	public SearchResult<T> filter(Predicate<T> predicate) {
		return new SearchResult<>(this.stream.filter(predicate));
	}

	public SearchResult<T> orderBy(Comparator<? super T> comparator) {
		this.stream = this.stream.sorted(comparator);
		return this;
	}

	public List<T> toList() {
		return this.stream.collect(Collectors.toList());
	}

	public Set<T> toSet() {
		return this.stream.collect(Collectors.toSet());
	}

	public <U, V> Map<U, V> toMap(Function<T, U> keyMapper, Function<T, V> valueMapper) {
		return this.stream.collect(Collectors.toMap(keyMapper, valueMapper));
	}

	public Paging<T> toPaging(int offset, int limit) {
		return Paging.asPage(this.stream.collect(Collectors.toList()), offset, limit);
	}

	public void forEach(Consumer<T> consumer) {
		this.stream.forEach(consumer);
	}
}
