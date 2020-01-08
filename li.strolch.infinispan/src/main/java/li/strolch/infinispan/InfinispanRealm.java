package li.strolch.infinispan;

import static li.strolch.agent.impl.DefaultRealmHandler.PREFIX_DATA_STORE_FILE;
import static li.strolch.runtime.StrolchConstants.makeRealmKey;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import li.strolch.agent.api.*;
import li.strolch.agent.impl.DataStoreMode;
import li.strolch.agent.impl.InternalStrolchRealm;
import li.strolch.agent.impl.TransientAuditTrail;
import li.strolch.agent.impl.TransientTransaction;
import li.strolch.model.ModelStatistics;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.Tags;
import li.strolch.model.activity.Activity;
import li.strolch.model.xml.XmlModelSaxFileReader;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.runtime.configuration.ComponentConfiguration;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;

public class InfinispanRealm extends InternalStrolchRealm {

	private static final String PROP_ALLOW_DATA_INIT_EMPTY_CLUSTER = "allowDataInitOnEmptyCluster";

	private ComponentConfiguration configuration;
	//private DefaultCacheManager cacheManager;
	private RemoteCacheManager cacheManager;
	private RemoteCache<String, Resource> resourceCache;
	private RemoteCache<ElementKey, Order> orderCache;
	private RemoteCache<ElementKey, Activity> activityCache;

	private InfinispanResourceMap resourceMap;
	private InfinispanOrderMap orderMap;
	private InfinispanActivityMap activityMap;
	private AuditTrail auditTrail;

	public InfinispanRealm(String realm) {
		super(realm);
	}

	@Override
	public void initialize(ComponentContainer container, ComponentConfiguration configuration) {
		super.initialize(container, configuration);
		this.configuration = configuration;

		ConfigurationBuilder cb = new ConfigurationBuilder();

		Map<String, String> saslProperties = new HashMap<>();
		saslProperties.put("infinispan.remote.auth-realm", "default");
		saslProperties.put("infinispan.remote.auth-server-name", "infinispan");
		saslProperties.put("infinispan.remote.auth-username", "eSusjy6G91e");
		saslProperties.put("infinispan.remote.auth-password", "NvM1B3QLvo");

		cb.marshaller(new ProtoStreamMarshaller()) //
				.statistics() //
				.enable() //
				.jmxDomain("org.infinispan") //

				.addServers("172.17.0.3:11222") //

				.security() //
				.authentication() //
				.saslMechanism("DIGEST-MD5") //
				.serverName("infinispan") //
				.username("eSusjy6G91") //
				.password("NvM1B3QLvo") //

				.saslQop(SaslQop.AUTH) //
				.saslProperties(saslProperties);

		this.cacheManager = new RemoteCacheManager(cb.build());
		this.cacheManager.start();

//		try {
//			this.cacheManager = new DefaultCacheManager(
//					configuration.getConfigFile("infinispanConfigFile", null).toURI().toURL(), false);
//		} catch (Exception e) {
//			throw new IllegalStateException("Failed to read infinispan config", e);
//		}
//		//this.cacheManager = new DefaultCacheManager(global.build());

//		ConfigurationBuilder config = new ConfigurationBuilder();
//		config //
//				.persistence().addClusterLoader() //
//				.expiration().lifespan(-1) //
//				.clustering().cacheMode(CacheMode.DIST_SYNC);
//
//		long start = System.currentTimeMillis();
//		while (this.cacheManager.getClusterSize() < 2) {
//			logger.info("Waiting for cluster to form...");
//			try {
//				Thread.sleep(1000L);
//			} catch (InterruptedException e) {
//				logger.error("Interrupted!");
//				return;
//			}
//			if (System.currentTimeMillis() - start > 60000L)
//				throw new IllegalStateException("Cluster with at least 2 nodes didn't form in 60 seconds!");
//		}
//
//		this.cacheManager.defineConfiguration(Tags.STROLCH_MODEL, config.build());
//		this.cacheManager.defineConfiguration(Tags.RESOURCE, config.build());
//		this.cacheManager.defineConfiguration(Tags.ORDER, config.build());
//		this.cacheManager.defineConfiguration(Tags.ACTIVITY, config.build());

		ClassWhiteList whiteList = new ClassWhiteList();
		whiteList.addClasses("li.strolch.infinispan.ElementKey");
		whiteList.addRegexps("li.strolch.model.*");
		this.cacheManager.getConfiguration().marshaller().initialize(whiteList);
		this.cacheManager.getMarshallerRegistry().registerMarshaller(new JavaSerializationMarshaller(whiteList));

		RemoteCacheManagerAdmin administration = this.cacheManager.administration();
		administration.getOrCreateCache(Tags.STROLCH_MODEL, getXmlCacheConfig(Tags.STROLCH_MODEL));
		administration.getOrCreateCache(Tags.RESOURCE, getXmlCacheConfig(Tags.RESOURCE));
		administration.getOrCreateCache(Tags.ORDER, getXmlCacheConfig(Tags.ORDER));
		administration.getOrCreateCache(Tags.ACTIVITY, getXmlCacheConfig(Tags.ACTIVITY));

		this.resourceCache = this.cacheManager.getCache(Tags.RESOURCE);
		this.orderCache = this.cacheManager.getCache(Tags.ORDER);
		this.activityCache = this.cacheManager.getCache(Tags.ACTIVITY);

		this.resourceMap = new InfinispanResourceMap(resourceCache);
		this.orderMap = new InfinispanOrderMap(orderCache);
		this.activityMap = new InfinispanActivityMap(activityCache);
		this.auditTrail = new TransientAuditTrail();
	}

	private XMLStringConfiguration getXmlCacheConfig(String cacheName) {
		return new XMLStringConfiguration(new StringBuilder() //
				.append("<infinispan xmlns=\"urn:infinispan:config:10.1\">") //
				.append("  <cache-container name=\"default\">") //
				.append("    <serialization marshaller=\"org.infinispan.commons.marshall.JavaSerializationMarshaller\">") //
				.append("      <white-list>") //
				.append("        <class>li.strolch.infinispan.ElementKey</class>") //
				.append("        <regex>li.strolch.model.*</regex>") //
				.append("        <regex>li.strolch.parameter.*</regex>") //
				.append("        <regex>li.strolch.activity.*</regex>") //
				.append("        <regex>li.strolch.timedstate.*</regex>") //
				.append("        <regex>li.strolch.timevalue.*</regex>") //
				.append("        <regex>li.strolch.timevalue.impl.*</regex>") //
				.append("        <regex>li.strolch.policy.*</regex>") //
				.append("        <regex>li.strolch.audit.*</regex>") //
				.append("      </white-list>") //
				.append("    </serialization>") //
				.append("    <global-state>") //
				.append("      <persistent-location ") //
				.append("          path=\"/opt/infinispan/server/data/persistence/" + cacheName + "\" />") //
				.append("    </global-state>") //
				.append("    <distributed-cache name=\"" + cacheName + "\">") //
				.append("      <persistence passivation=\"false\">") //
				.append("        <file-store ") //
				.append("            shared=\"false\" ") //
				.append("            fetch-state=\"true\" ") //
				.append("            path=\"/opt/infinispan/server/data/persistence/" + cacheName + "\"") //
				.append("          />") //
				.append("      </persistence>") //
				.append("    </distributed-cache>") //
				.append("  </cache-container>") //
				.append("</infinispan>") //

				.toString());
	}

	@Override
	public void start(PrivilegeContext privilegeContext) {

		BasicCache<String, Object> modelCache = this.cacheManager.getCache(Tags.STROLCH_MODEL);
		if (modelCache.isEmpty() || !modelCache.containsKey("DataLoaded")
				|| modelCache.get("DataLoaded") != Boolean.TRUE) {
			boolean allowDataInitOnSchemaCreate = configuration
					.getBoolean(PROP_ALLOW_DATA_INIT_EMPTY_CLUSTER, Boolean.FALSE);
			if (!allowDataInitOnSchemaCreate) {
				logger.warn(
						"Cluster is empty, but no data init allowed! Set property " + PROP_ALLOW_DATA_INIT_EMPTY_CLUSTER
								+ "=true to allow data init!");
			} else {

				logger.info("Cluster is empty, loading default model into cluster!");

				String dataStoreKey = makeRealmKey(getRealm(), PREFIX_DATA_STORE_FILE);
				File dataStoreF = configuration.getDataFile(dataStoreKey, null, true);

				try {
					container.runAsAgent(ctx -> {
						ModelStatistics statistics;
						try (StrolchTransaction tx = openTx(ctx.getCertificate(), getClass().getSimpleName(), false)) {

							StoreToElementMapListener listener = new StoreToElementMapListener(tx, this.resourceMap,
									this.orderMap, this.activityMap);
							XmlModelSaxFileReader handler = new XmlModelSaxFileReader(listener, dataStoreF, true);
							handler.parseFile();
							statistics = handler.getStatistics();
							tx.commitOnClose();
						}
						logger.info(MessageFormat
								.format("Realm {0} initialization statistics: {1}", getRealm(), statistics));
					});
				} catch (Exception e) {
					throw new IllegalStateException(
							"Failed to load model into cluster from file " + dataStoreF.getAbsolutePath(), e);
				}

				modelCache.put("DataLoaded", true);
			}
		}

		super.start(privilegeContext);
	}

	@Override
	public void destroy() {
		if (this.resourceCache != null)
			this.resourceCache.stop();
		if (this.orderCache != null)
			this.orderCache.stop();
		if (this.activityCache != null)
			this.activityCache.stop();

		if (this.cacheManager != null)
			this.cacheManager.stop();
	}

	@Override
	public ResourceMap getResourceMap() {
		return this.resourceMap;
	}

	@Override
	public OrderMap getOrderMap() {
		return this.orderMap;
	}

	@Override
	public ActivityMap getActivityMap() {
		return this.activityMap;
	}

	@Override
	public AuditTrail getAuditTrail() {
		return this.auditTrail;
	}

	@Override
	public DataStoreMode getMode() {
		return DataStoreMode.TRANSIENT;
	}

	@Override
	public StrolchTransaction openTx(Certificate certificate, Class<?> clazz, boolean readOnly) {
		return new TransientTransaction(this.container, this, certificate, clazz.getName(), readOnly);
	}

	@Override
	public StrolchTransaction openTx(Certificate certificate, String action, boolean readOnly) {
		return new TransientTransaction(this.container, this, certificate, action, readOnly);
	}
}
