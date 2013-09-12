package org.infinispan.loaders.mapdb;

import java.io.File;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.loaders.mapdb.configuration.MapDBStoreConfiguration;
import org.infinispan.loaders.mapdb.configuration.MapDBStoreConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.BaseCacheStoreTest;
import org.infinispan.persistence.CacheLoaderException;
import org.infinispan.persistence.DummyLoaderContext;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "loaders.mapdb.MapDBCacheStoreTest")
public class MapDBCacheStoreTest extends BaseCacheStoreTest {
   private EmbeddedCacheManager cacheManager;
   private MapDBStore fcs;
   private String tmpDirectory;
   
   protected MapDBStoreConfiguration createCacheStoreConfig(PersistenceConfigurationBuilder lcb) {
      cacheManager = TestCacheManagerFactory.createCacheManager(CacheMode.LOCAL, false);
      MapDBStoreConfigurationBuilder cfg = new MapDBStoreConfigurationBuilder(lcb);
      cfg.location(tmpDirectory);
      return cfg.create();
   }
   
   @BeforeClass
   protected void setUpTempDir() {
      tmpDirectory = TestingUtil.tmpDirectory(this);
   }

   @AfterClass(alwaysRun = true)
   protected void clearTempDir() {
      TestingUtil.recursiveFileRemove(tmpDirectory);
      new File(tmpDirectory).mkdirs();
   }
   
   @Override
   protected StreamingMarshaller getMarshaller() {
      return cacheManager.getCache().getAdvancedCache().getComponentRegistry().getCacheMarshaller();
   }

   @AfterMethod
   @Override
   public void tearDown() throws CacheLoaderException {
      super.tearDown();
      TestingUtil.killCacheManagers(cacheManager);
   }
   
   @Override
   protected AdvancedLoadWriteStore createStore() throws Exception {
      clearTempDir();
      fcs = new MapDBStore();
      ConfigurationBuilder cb = new ConfigurationBuilder();
      MapDBStoreConfiguration cfg = createCacheStoreConfig(cb.persistence());
      fcs.init(new DummyLoaderContext(cfg, getCache(), getMarshaller()));
      fcs.start();
      return fcs;
   }
}
