package li.strolch.infinispan;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.impl.DefaultRealmHandler;
import li.strolch.agent.impl.InternalStrolchRealm;
import li.strolch.runtime.configuration.ComponentConfiguration;

public class InfinispanRealmHandler extends DefaultRealmHandler {

	public InfinispanRealmHandler(ComponentContainer container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void setup(ComponentConfiguration configuration) {
		super.setup(configuration);
	}

	@Override
	protected InternalStrolchRealm buildRealm(String realmName, String realmMode) {
		if(!realmMode.equals("Infinispan")) {
			return super.buildRealm(realmName, realmMode);
		}

		return new InfinispanRealm(realmName);
	}
}
