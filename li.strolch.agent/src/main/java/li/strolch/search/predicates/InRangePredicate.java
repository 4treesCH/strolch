package li.strolch.search.predicates;

import java.util.Date;

import li.strolch.search.SearchPredicate;
import li.strolch.search.ValueCoercer;
import li.strolch.utils.collections.DateRange;

public class InRangePredicate implements SearchPredicate {
	private final DateRange range;

	public InRangePredicate(DateRange range) {
		this.range = range;
	}

	@Override
	public boolean matches(Object left) {
		return range.contains((Date) left);
	}

	@Override
	public SearchPredicate coerce(ValueCoercer coercer) {
		// nothing to coerce
		return this;
	}
}