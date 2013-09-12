package org.infinispan.loaders.mapdb.configuration;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

/**
 * 
 * @author <a href="mailto:rtsang@redhat.com">Ray Tsang</a>
 * 
 */
public class MapDBStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<MapDBStoreConfiguration, MapDBStoreConfigurationBuilder> {
   protected String location = "Infinispan-MapDBStore";
   protected boolean compression = false;
   protected int expiryQueueSize = 10000;

   public MapDBStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
      super(builder);
   }
   
   public MapDBStoreConfigurationBuilder location(String location) {
      this.location = location;
      return self();
   }
   
   public MapDBStoreConfigurationBuilder expiryQueueSize(int expiryQueueSize) {
      this.expiryQueueSize = expiryQueueSize;
      return self();
   }
   
   public MapDBStoreConfigurationBuilder compression(boolean compression) {
      this.compression = compression;
      return self();
   }

   @Override
   public void validate() {
      // how do you validate required attributes?
      super.validate();
   }

   @Override
   public MapDBStoreConfiguration create() {
      return new MapDBStoreConfiguration(purgeOnStartup, fetchPersistentState, ignoreModifications, async.create(),
            singletonStore.create(), preload, shared, properties,
            location, compression, expiryQueueSize);
            
   }

   @Override
   public Builder<?> read(MapDBStoreConfiguration template) {
      location = template.location();
      compression = template.compression();
      expiryQueueSize = template.expiryQueueSize();
      
      // AbstractStore-specific configuration
      fetchPersistentState = template.fetchPersistentState();
      ignoreModifications = template.ignoreModifications();
      properties = template.properties();
      purgeOnStartup = template.purgeOnStartup();
      this.async.read(template.async());
      this.singletonStore.read(template.singletonStore());

      return self();
   }

   @Override
   public MapDBStoreConfigurationBuilder self() {
      return this;
   }

}
