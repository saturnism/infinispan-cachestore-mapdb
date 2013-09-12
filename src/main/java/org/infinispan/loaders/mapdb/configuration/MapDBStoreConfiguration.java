package org.infinispan.loaders.mapdb.configuration;

import java.util.Properties;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.loaders.mapdb.MapDBStore;

/**
 * 
 * @author <a href="mailto:rtsang@redhat.com">Ray Tsang</a>
 * 
 */
@ConfigurationFor(MapDBStore.class)
@BuiltBy(MapDBStoreConfigurationBuilder.class)
public class MapDBStoreConfiguration extends AbstractStoreConfiguration {
   final private String location;
   final private boolean compression;
   final private int expiryQueueSize;
   
   public MapDBStoreConfiguration(boolean purgeOnStartup, boolean fetchPersistentState, boolean ignoreModifications, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore, boolean preload, boolean shared, Properties properties,
         String location, boolean compression, int expiryQueueSize) {
      super(purgeOnStartup, fetchPersistentState, ignoreModifications, async, singletonStore, preload, shared, properties);
      this.location = location;
      this.compression = compression;
      this.expiryQueueSize = expiryQueueSize;
   }
   
   public String location() {
      return location;
   }
   
   public boolean compression() {
      return compression;
   }

   public int expiryQueueSize() {
      return expiryQueueSize;
   }
}
