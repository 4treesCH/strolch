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
package li.strolch.persistence.postgresql;

import java.sql.Connection;
import java.util.Date;
import java.util.Set;

import li.strolch.model.StrolchElement;
import li.strolch.persistence.api.ModificationResult;
import li.strolch.persistence.api.OrderDao;
import li.strolch.persistence.api.ResourceDao;
import li.strolch.persistence.api.StrolchPersistenceException;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.persistence.api.TransactionCloseStrategy;
import li.strolch.persistence.api.TransactionResult;
import li.strolch.persistence.api.TransactionState;
import li.strolch.runtime.observer.ObserverHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.eitchnet.utils.helper.StringHelper;

public class PostgreSqlStrolchTransaction implements StrolchTransaction {

	private static final Logger logger = LoggerFactory.getLogger(PostgreSqlStrolchTransaction.class);
	private PostgreSqlPersistenceHandler persistenceHandler;
	private String realm;

	private TransactionCloseStrategy closeStrategy;
	private ObserverHandler observerHandler;
	private boolean suppressUpdates;

	private PostgresqlDao<?> orderDao;
	private PostgresqlDao<?> resourceDao;
	private Connection connection;
	private long startTime;
	private Date startTimeDate;
	private TransactionResult txResult;
	private boolean open;

	public PostgreSqlStrolchTransaction(String realm, PostgreSqlPersistenceHandler persistenceHandler) {
		this.startTime = System.nanoTime();
		this.startTimeDate = new Date();
		this.realm = realm;
		this.persistenceHandler = persistenceHandler;
		this.suppressUpdates = false;
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

	@Override
	public void setCloseStrategy(TransactionCloseStrategy closeStrategy) {
		this.closeStrategy = closeStrategy;
	}

	@Override
	public void autoCloseableCommit() {

		if (logger.isDebugEnabled()) {
			logger.info("Committing TX..."); //$NON-NLS-1$
		}

		long start = System.nanoTime();
		this.txResult = new TransactionResult();

		try {
			if (this.orderDao != null) {
				this.orderDao.commit(this.txResult);
			}

			if (this.resourceDao != null) {
				this.resourceDao.commit(this.txResult);
			}

			this.txResult.setState(TransactionState.COMMITTED);

		} catch (Exception e) {
			this.txResult.setState(TransactionState.FAILED);

			long end = System.nanoTime();
			long txDuration = end - this.startTime;
			long closeDuration = end - start;
			StringBuilder sb = new StringBuilder();
			sb.append("TX has failed after "); //$NON-NLS-1$
			sb.append(StringHelper.formatNanoDuration(txDuration));
			sb.append(" with close operation taking "); //$NON-NLS-1$
			sb.append(StringHelper.formatNanoDuration(closeDuration));
			logger.info(sb.toString());

			throw e;
		}

		long end = System.nanoTime();
		long txDuration = end - this.startTime;
		long closeDuration = end - start;

		this.txResult.setStartTime(this.startTimeDate);
		this.txResult.setTxDuration(txDuration);
		this.txResult.setCloseDuration(closeDuration);
		this.txResult.setRealm(this.realm);

		StringBuilder sb = new StringBuilder();
		sb.append("TX was completed after "); //$NON-NLS-1$
		sb.append(StringHelper.formatNanoDuration(txDuration));
		sb.append(" with close operation taking "); //$NON-NLS-1$
		sb.append(StringHelper.formatNanoDuration(closeDuration));
		logger.info(sb.toString());

		if (!this.suppressUpdates && this.observerHandler != null) {

			Set<String> keys = this.txResult.getKeys();
			for (String key : keys) {
				ModificationResult modificationResult = this.txResult.getModificationResult(key);

				this.observerHandler.add(key, modificationResult.<StrolchElement> getCreated());
				this.observerHandler.update(key, modificationResult.<StrolchElement> getUpdated());
				this.observerHandler.remove(key, modificationResult.<StrolchElement> getDeleted());
			}
		}
	}

	@Override
	public void autoCloseableRollback() {
		long start = System.nanoTime();

		long end = System.nanoTime();
		long txDuration = end - this.startTime;
		long closeDuration = end - start;

		this.txResult = new TransactionResult();
		this.txResult.setState(TransactionState.ROLLED_BACK);
		this.txResult.setStartTime(this.startTimeDate);
		this.txResult.setTxDuration(txDuration);
		this.txResult.setCloseDuration(closeDuration);
		this.txResult.setRealm(this.realm);
	}

	@Override
	public void close() throws StrolchPersistenceException {
		this.closeStrategy.close(this);
	}

	@Override
	public boolean isOpen() {
		return this.open;
	}

	OrderDao getOrderDao(PostgreSqlStrolchTransaction tx) {
		if (this.orderDao == null)
			this.orderDao = new PostgreSqlOrderDao(tx);
		return (OrderDao) this.orderDao;
	}

	ResourceDao getResourceDao(PostgreSqlStrolchTransaction tx) {
		if (this.resourceDao == null)
			this.resourceDao = new PostgreSqlResourceDao(tx);
		return (ResourceDao) this.resourceDao;
	}

	/**
	 * @return
	 */
	Connection getConnection() {
		if (this.connection == null) {
			this.connection = this.persistenceHandler.getConnection(this.realm);
		}
		return this.connection;
	}
}
