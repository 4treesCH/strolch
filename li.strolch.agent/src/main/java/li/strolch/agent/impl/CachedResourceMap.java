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
package li.strolch.agent.impl;

import static li.strolch.model.StrolchModelConstants.INTERPRETATION_RESOURCE_REF;

import java.util.List;

import li.strolch.agent.api.ResourceMap;
import li.strolch.agent.api.StrolchRealm;
import li.strolch.model.Resource;
import li.strolch.model.parameter.Parameter;
import li.strolch.model.query.ResourceQuery;
import li.strolch.persistence.api.ResourceDao;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.runtime.query.inmemory.InMemoryQuery;
import li.strolch.runtime.query.inmemory.InMemoryResourceQueryVisitor;

public class CachedResourceMap extends CachedElementMap<Resource> implements ResourceMap {

	public CachedResourceMap(StrolchRealm realm) {
		super(realm);
	}

	@Override
	protected void assertIsRefParam(Parameter<?> refP) {
		ElementMapHelpers.assertIsRefParam(INTERPRETATION_RESOURCE_REF, refP);
	}

	@Override
	protected ResourceDao getDbDao(StrolchTransaction tx) {
		return tx.getPersistenceHandler().getResourceDao(tx);
	}

	@Override
	public <U> List<U> doQuery(StrolchTransaction tx, ResourceQuery<U> resourceQuery) {
		InMemoryResourceQueryVisitor visitor = new InMemoryResourceQueryVisitor();
		InMemoryQuery<Resource, U> query = visitor.visit(resourceQuery);
		return query.doQuery(tx, this);
	}
}
