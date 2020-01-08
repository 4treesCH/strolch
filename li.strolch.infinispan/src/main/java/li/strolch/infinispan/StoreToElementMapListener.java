/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
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
package li.strolch.infinispan;

import li.strolch.agent.api.ActivityMap;
import li.strolch.agent.api.OrderMap;
import li.strolch.agent.api.ResourceMap;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.activity.Activity;
import li.strolch.model.xml.StrolchElementListener;
import li.strolch.persistence.api.StrolchTransaction;

public class StoreToElementMapListener implements StrolchElementListener {

	private StrolchTransaction tx;
	private ResourceMap resourceMap;
	private OrderMap orderMap;
	private ActivityMap activityMap;

	public StoreToElementMapListener(StrolchTransaction tx, ResourceMap resourceMap, OrderMap orderMap,
			ActivityMap activityMap) {
		this.tx = tx;
		this.resourceMap = resourceMap;
		this.orderMap = orderMap;
		this.activityMap = activityMap;
	}

	@Override
	public void notifyResource(Resource resource) {
		this.resourceMap.add(this.tx, resource);
	}

	@Override
	public void notifyOrder(Order order) {
		this.orderMap.add(this.tx, order);
	}

	@Override
	public void notifyActivity(Activity activity) {
		this.activityMap.add(this.tx, activity);
	}
}