package li.strolch.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.testbase.runtime.RuntimeMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InfinispanTest {

	private static final String SRC_PATH = "src/test/resources/runtime-InfinispanTest";
	private static final String TARGET_PATH = "target/" + InfinispanTest.class;

	private static RuntimeMock runtimeMock;
	private static Certificate certificate;

	@BeforeClass
	public static void beforeClass() {
		runtimeMock = new RuntimeMock().mockRuntime(TARGET_PATH, SRC_PATH);
		runtimeMock.startContainer();
		certificate = runtimeMock.loginTest();
	}

	@AfterClass
	public static void afterClass() {

		if (certificate != null)
			runtimeMock.logout(certificate);

		if (runtimeMock != null)
			runtimeMock.destroyRuntime();
	}

	@Test
	public void shouldLoadIntoRealm() throws InterruptedException {

		Thread.sleep(60000L);

		try (StrolchTransaction tx = runtimeMock.openUserTx(certificate, true)) {

			Resource ball = tx.getResourceBy("Ball", "yellowBall", true);
			assertTrue(ball.hasParameter("parameters", "color"));
			assertEquals("yellow", ball.getParameter("parameters", "color", true).getValue());
		}
	}
}
