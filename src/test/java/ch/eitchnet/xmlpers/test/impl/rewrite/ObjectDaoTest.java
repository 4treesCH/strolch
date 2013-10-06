/*
 * Copyright (c) 2012, Robert von Burg
 *
 * All rights reserved.
 *
 * This file is part of the XXX.
 *
 *  XXX is free software: you can redistribute 
 *  it and/or modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  XXX is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XXX.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package ch.eitchnet.xmlpers.test.impl.rewrite;

import static ch.eitchnet.xmlpers.test.model.ModelBuilder.RES_ID;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.RES_TYPE;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.assertResource;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.assertResourceUpdated;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.createResource;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.updateResource;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.eitchnet.utils.helper.FileHelper;
import ch.eitchnet.xmlpers.api.PersistenceContext;
import ch.eitchnet.xmlpers.api.XmlIoMode;
import ch.eitchnet.xmlpers.api.XmlPersistenceConstants;
import ch.eitchnet.xmlpers.test.impl.TestConstants;
import ch.eitchnet.xmlpers.test.model.Resource;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public class ObjectDaoTest {

	private static final String BASEPATH = "target/dbTest/rewrite"; //$NON-NLS-1$

	private PersistenceContextFactory ctxFactory;
	private XmlPersistenceManager persistenceManager;
	private PersistenceTransaction tx;

	@BeforeClass
	public static void beforeClass() {

		File basePath = new File(BASEPATH);
		if (basePath.exists()) {
			if (!FileHelper.deleteFile(basePath, true)) {
				throw new RuntimeException("Faile to delete base path " + BASEPATH); //$NON-NLS-1$
			}
		}

		if (!basePath.mkdirs()) {
			throw new RuntimeException("Failed to create base path " + BASEPATH); //$NON-NLS-1$
		}

		new File(BASEPATH + "/sax").mkdir(); //$NON-NLS-1$
		new File(BASEPATH + "/dom").mkdir(); //$NON-NLS-1$
	}

	@After
	public void tearDown() {
		if (this.tx != null && this.tx.isOpen()) {
			this.tx.rollback();
		}
	}

	@Test
	public void testSaxObjectDao() {
		this.ctxFactory = new TestPersistenceContextFactory();
		Properties properties = new Properties();
		properties.setProperty(XmlPersistenceConstants.PROP_BASEPATH, BASEPATH + "/sax"); //$NON-NLS-1$
		this.persistenceManager = XmlPersistenceManagerLoader.load(properties);

		testCrud(XmlIoMode.SAX);
	}

	@Test
	public void testDomObjectDao() {
		this.ctxFactory = new TestPersistenceContextFactory();
		Properties properties = new Properties();
		properties.setProperty(XmlPersistenceConstants.PROP_BASEPATH, BASEPATH + "/dom"); //$NON-NLS-1$
		this.persistenceManager = XmlPersistenceManagerLoader.load(properties);

		testCrud(XmlIoMode.DOM);
	}

	private PersistenceTransaction freshTx(XmlIoMode ioMode) {
		if (this.tx != null && this.tx.isOpen())
			this.tx.rollback();

		this.tx = this.persistenceManager.openTx();
		this.tx.setIoMode(ioMode);

		return this.tx;
	}

	private void testCrud(XmlIoMode ioMode) {

		ObjectDao objectDao;

		// create new resource
		Resource resource = createResource();
		objectDao = freshTx(ioMode).getObjectDao();
		objectDao.add(resource);
		this.tx.commit(this.ctxFactory);

		// read resource
		PersistenceContext<Resource> ctx = this.ctxFactory.createCtx(this.tx, TestConstants.TYPE_RES, RES_TYPE, RES_ID);
		objectDao = freshTx(ioMode).getObjectDao();
		resource = objectDao.queryById(ctx);
		assertResource(resource);

		// modify resource
		updateResource(resource);
		objectDao = freshTx(ioMode).getObjectDao();
		objectDao.update(resource);
		this.tx.commit(this.ctxFactory);

		// read modified resource
		objectDao = freshTx(ioMode).getObjectDao();
		resource = objectDao.queryById(ctx);
		assertResourceUpdated(resource);
		this.tx.commit(this.ctxFactory);

		// delete resource
		objectDao = freshTx(ioMode).getObjectDao();
		objectDao.remove(resource);
		this.tx.commit(this.ctxFactory);

		// fail to read
		objectDao = freshTx(ioMode).getObjectDao();
		resource = objectDao.queryById(ctx);
		assertNull(resource);

		// and create again
		resource = createResource();
		assertResource(resource);
		objectDao = freshTx(ioMode).getObjectDao();
		objectDao.add(resource);
		this.tx.commit(this.ctxFactory);
	}
}
