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
package li.strolch.persistence.xml;

import java.util.Set;

import li.strolch.agent.impl.StrolchRealm;
import li.strolch.model.StrolchElement;
import li.strolch.persistence.api.AbstractTransaction;
import li.strolch.persistence.api.OrderDao;
import li.strolch.persistence.api.ResourceDao;
import li.strolch.persistence.api.StrolchPersistenceException;
import li.strolch.persistence.api.TransactionCloseStrategy;
import li.strolch.runtime.observer.ObserverHandler;
import ch.eitchnet.xmlpers.api.ModificationResult;
import ch.eitchnet.xmlpers.api.PersistenceTransaction;
import ch.eitchnet.xmlpers.api.TransactionResult;

public class XmlStrolchTransaction extends AbstractTransaction {

	private ObserverHandler observerHandler;
	private boolean suppressUpdates;
	private PersistenceTransaction tx;
	private TransactionCloseStrategy closeStrategy;

	public XmlStrolchTransaction(StrolchRealm realm, PersistenceTransaction tx) {
		super(realm);
		this.suppressUpdates = false;
		this.tx = tx;
		this.closeStrategy = TransactionCloseStrategy.COMMIT;
	}

	/**
	 * @param observerHandler
	 *            the observerHandler to set
	 */
	public void setObserverHandler(ObserverHandler observerHandler) {
		this.observerHandler = observerHandler;
	}

	/**
	 * @param suppressUpdates
	 *            the suppressUpdates to set
	 */
	public void setSuppressUpdates(boolean suppressUpdates) {
		this.suppressUpdates = suppressUpdates;
	}

	/**
	 * @return the suppressUpdates
	 */
	public boolean isSuppressUpdates() {
		return this.suppressUpdates;
	}

	PersistenceTransaction getTx() {
		return this.tx;
	}

	@Override
	public void setCloseStrategy(TransactionCloseStrategy closeStrategy) {
		this.closeStrategy = closeStrategy;
	}

	@Override
	public void autoCloseableCommit() {

		TransactionResult txResult = new TransactionResult();
		if (!this.suppressUpdates && this.observerHandler != null) {
			this.tx.setTransactionResult(txResult);
		}

		this.tx.autoCloseableCommit();

		if (!this.suppressUpdates && this.observerHandler != null) {

			Set<String> keys = txResult.getKeys();
			for (String key : keys) {
				ModificationResult modificationResult = txResult.getModificationResult(key);

				this.observerHandler.add(key, modificationResult.<StrolchElement> getCreated());
				this.observerHandler.update(key, modificationResult.<StrolchElement> getUpdated());
				this.observerHandler.remove(key, modificationResult.<StrolchElement> getDeleted());
			}
		}
	}

	@Override
	public void autoCloseableRollback() {
		this.tx.autoCloseableRollback();
	}

	@Override
	public void close() throws StrolchPersistenceException {
		this.closeStrategy.close(this);
	}

	@Override
	public boolean isOpen() {
		return this.tx.isOpen();
	}

	@Override
	public OrderDao getOrderDao() {
		return new XmlOrderDao(this);
	}

	@Override
	public ResourceDao getResourceDao() {
		return new XmlResourceDao(this);
	}
}
