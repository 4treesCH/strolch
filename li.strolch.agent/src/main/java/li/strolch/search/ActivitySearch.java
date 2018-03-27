package li.strolch.search;

import li.strolch.model.activity.Activity;

public class ActivitySearch extends StrolchSearch<Activity> {

	private SearchNavigator<Activity> navigator;

	@Override
	protected SearchNavigator<Activity> getNavigator() {
		return this.navigator;
	}

	@Override
	public ActivitySearch types(String... types) {
		this.navigator = tx -> tx.streamActivities(types);
		return this;
	}
}
