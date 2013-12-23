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
package li.strolch.persistence.postgresql.dao.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import li.strolch.persistence.api.StrolchPersistenceHandler;
import li.strolch.persistence.postgresql.DbSchemaVersionCheck;
import li.strolch.testbase.runtime.RuntimeMock;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public abstract class AbstractDaoImplTest extends RuntimeMock {

	private static final String DB_URL = "jdbc:postgresql://localhost/testdb"; //$NON-NLS-1$
	private static final String DB_USERNAME = "testuser"; //$NON-NLS-1$
	private static final String DB_PASSWORD = "test"; //$NON-NLS-1$
	private static final String RUNTIME_PATH = "target/strolchRuntime/"; //$NON-NLS-1$
	private static final String DB_STORE_PATH_DIR = "dbStore"; //$NON-NLS-1$
	private static final String CONFIG_SRC = "src/test/resources/runtime/config"; //$NON-NLS-1$
	protected static StrolchPersistenceHandler persistenceHandler;

	@BeforeClass
	public static void beforeClass() throws SQLException {

		dropSchema();

		File rootPath = new File(RUNTIME_PATH);
		File configSrc = new File(CONFIG_SRC);
		RuntimeMock.mockRuntime(rootPath, configSrc);
		new File(rootPath, DB_STORE_PATH_DIR).mkdir();
		RuntimeMock.startContainer(rootPath);

		// initialize the component configuration
		persistenceHandler = getContainer().getComponent(StrolchPersistenceHandler.class);
	}

	private static void dropSchema() throws SQLException {
		String dbVersion = DbSchemaVersionCheck.getExpectedDbVersion();
		String sql = DbSchemaVersionCheck.getSql(dbVersion, "drop"); //$NON-NLS-1$
		try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
			connection.prepareStatement(sql).execute();
		}
	}

	@AfterClass
	public static void afterClass() {
		RuntimeMock.destroyRuntime();
	}
}
