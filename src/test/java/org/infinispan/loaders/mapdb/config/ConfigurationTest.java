package org.infinispan.loaders.mapdb.config;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.mapdb.configuration.MapDBStoreConfiguration;
import org.infinispan.loaders.mapdb.configuration.MapDBStoreConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 *
 * @author <a href="mailto:rtsang@redhat.com">Ray Tsang</a>
 *
 */
@Test(groups = "unit", testName = "loaders.offheap.configuration.ConfigurationTest")
public class ConfigurationTest extends AbstractInfinispanTest {
   private String tmpDirectory;
   
   @BeforeTest
   protected void setUpTempDir() {
      tmpDirectory = TestingUtil.tmpDirectory(this);
   }
   
   @AfterTest(alwaysRun = true)
   protected void clearTempDir() {
      TestingUtil.recursiveFileRemove(tmpDirectory);
   }
   
   public void testConfigBuilder() {
      GlobalConfiguration globalConfig = new GlobalConfigurationBuilder().globalJmxStatistics().transport().defaultTransport().build();

      Configuration cacheConfig = new ConfigurationBuilder().persistence().addStore(MapDBStoreConfigurationBuilder.class)
            .location(tmpDirectory)
            .compression(true)
            .build();

      StoreConfiguration cacheLoaderConfig = cacheConfig.persistence().stores().get(0);
      assertTrue(cacheLoaderConfig instanceof MapDBStoreConfiguration);
      MapDBStoreConfiguration config = (MapDBStoreConfiguration) cacheLoaderConfig;
      assertEquals(true, config.compression());
      
      EmbeddedCacheManager cacheManager = new DefaultCacheManager(globalConfig);

      cacheManager.defineConfiguration("testCache", cacheConfig);

      cacheManager.start();
      Cache<String, String> cache = cacheManager.getCache("testCache");

      cache.put("hello", "there");
      cache.stop();
      cacheManager.stop();
   }

   public void testXmlConfig60() throws IOException {
      EmbeddedCacheManager cacheManager = new DefaultCacheManager("config/mapdb-config-60.xml");

      Cache<String, String> cache = cacheManager.getCache("testCache");

      cache.put("hello", "there 60 xml");
      cache.stop();
      cacheManager.stop();
   }

}
