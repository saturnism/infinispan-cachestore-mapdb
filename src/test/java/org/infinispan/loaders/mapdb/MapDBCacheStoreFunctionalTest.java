package org.infinispan.loaders.mapdb;

import java.io.File;

import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.loaders.mapdb.configuration.MapDBStoreConfigurationBuilder;
import org.infinispan.persistence.BaseCacheStoreFunctionalTest;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public abstract class MapDBCacheStoreFunctionalTest extends BaseCacheStoreFunctionalTest {
   protected String tmpDirectory;
   
   @BeforeClass
   protected void setUpTempDir() {
      tmpDirectory = TestingUtil.tmpDirectory(this);
   }

   @AfterClass(alwaysRun = true)
   protected void clearTempDir() {
      TestingUtil.recursiveFileRemove(tmpDirectory);
      new File(tmpDirectory).mkdirs();
   }
   
   MapDBStoreConfigurationBuilder createStoreBuilder(PersistenceConfigurationBuilder loaders) {
      return loaders.addStore(MapDBStoreConfigurationBuilder.class).location(tmpDirectory);
   }
}
