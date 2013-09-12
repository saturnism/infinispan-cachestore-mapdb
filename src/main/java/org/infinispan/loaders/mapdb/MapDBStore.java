package org.infinispan.loaders.mapdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;

import org.infinispan.Cache;
import org.infinispan.loaders.mapdb.configuration.MapDBStoreConfiguration;
import org.infinispan.loaders.mapdb.logging.Log;
import org.infinispan.marshall.core.MarshalledValue;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.persistence.CacheLoaderException;
import org.infinispan.persistence.PersistenceUtil;
import org.infinispan.persistence.TaskContextImpl;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshalledEntry;
import org.infinispan.util.logging.LogFactory;
import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * A cache store that can store entries off-heap in direct memory.
 * This implementation uses MapDB's Direct Memory DB.
 * 
 * At most one offheap store can be used by one cache.
 * 
 * @author <a href="mailto:rtsang@redhat.com">Ray Tsang</a>
 * @since 6.0
 */
@SuppressWarnings("rawtypes")
public class MapDBStore implements AdvancedLoadWriteStore {
   private static final Log log = LogFactory.getLog(MapDBStore.class, Log.class);
   
   private static final String STORE_DB_NAME = "store";
   private static final String EXPIRY_DB_NAME = "expiry";
   
   private MapDBStoreConfiguration configuration;
   private Cache cache;
   private InitializationContext ctx;
   
   private DB db;
   private Map<Object, byte[]> store;
   private Map<Long, byte[]> expired;
   private BlockingQueue<ExpiryEntry> expiryEntryQueue;

   @Override
   public void init(InitializationContext ctx) {
      this.configuration = ctx.getConfiguration();
      this.cache = ctx.getCache();
      this.ctx = ctx;
   }
   
   
   @Override
   public void start() {
      expiryEntryQueue = new LinkedBlockingQueue<ExpiryEntry>(configuration.expiryQueueSize());
      File location = new File(configuration.location());
      location.mkdirs();
      File dbFile = new File(location, "mapdb");
      DBMaker<?> dbMaker = DBMaker.newFileDB(dbFile)
            .transactionDisable()
            .asyncWriteDisable();
      
      if (configuration.compression())
         dbMaker.compressionEnable();
      
      this.db = dbMaker.make();
      
      
      this.store = db.createHashMap(STORE_DB_NAME)
            .makeOrGet();
      this.expired = db.createHashMap(EXPIRY_DB_NAME)
            .makeOrGet();
   }

   @Override
   public void stop() {
      System.out.print("closing");
      db.close();
   }
   
   @Override
   public void clear() {
      store.clear();
   }
   
   @Override
   public MarshalledEntry load(Object key) {
      try {
         MarshalledEntry me = (MarshalledEntry) unmarshall(store.get(key));
         if (me == null) return null;
   
         InternalMetadata meta = me.getMetadata();
         if (meta != null && meta.isExpired(ctx.getTimeService().wallClockTime())) {
            return null;
         }
         return me;
      } catch (Exception e) {
         throw new CacheLoaderException(e);
      }
   }
   
   @Override
   public boolean contains(Object key) {
      try {
         return load(key) != null;
      } catch (Exception e) {
         throw new CacheLoaderException(e);
      }
   }

   @Override
   public void write(MarshalledEntry entry) {
      try {
         // Marshall entry first, otherwise, compact is meaningless...
         byte[] value = marshall(entry);
         
         // Make sure we are working with the same key reference
         Object key = entry.getKey();
         if (key instanceof MarshalledValue) {
            // MapDB doesn't like ExpandableMarshalledValueByteStream
            // See ISPN-3493
            
            // make a deep copy of the key so that it doesn't get serialized by other threads
            key = unmarshall(marshall(key));
            
            // compact it to keep only the object instance and remove MarshalledValueByteStream
            ((MarshalledValue) key).compact(false, true);
         }
         
         store.put(key, value);
         InternalMetadata meta = entry.getMetadata();
         if (meta != null && meta.expiryTime() > -1) {
            addNewExpiry(entry);
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt(); // Restore interruption status
      } catch (IOException e) {
         throw new CacheLoaderException(e);
      } catch (ClassNotFoundException e) {
         throw new CacheLoaderException(e);
      }
   }

   @Override
   public boolean delete(Object key) {
      try {
         return store.remove(key) != null;
      } catch (Exception e) {
         throw new CacheLoaderException(e);
      }
   }

   @Override
   public void process(KeyFilter filter, CacheLoaderTask task, Executor executor, boolean fetchValue,
         boolean fetchMetadata) {
      int batchSize = 100;
      ExecutorCompletionService ecs = new ExecutorCompletionService(executor);
      int tasks = 0;
      final TaskContext taskContext = new TaskContextImpl();

      List<Map.Entry<Object, byte[]>> entries = new ArrayList<Map.Entry<Object, byte[]>>(batchSize);
      
      try {
         for (Map.Entry<Object, byte[]> entry : store.entrySet()) {
            entries.add(entry);
            if (entries.size() == batchSize) {
               final List<Map.Entry<Object, byte[]>> batch = entries;
               entries = new ArrayList<Map.Entry<Object, byte[]>>(batchSize);
               submitProcessTask(task, filter, ecs, taskContext, batch);
               tasks++;
            }
         }
         if (!entries.isEmpty()) {
            submitProcessTask(task, filter,ecs, taskContext, entries);
            tasks++;
         }

         PersistenceUtil.waitForAllTasksToComplete(ecs, tasks);
      } catch (Exception e) {
         throw new CacheLoaderException(e);
      }
   }
   
   @SuppressWarnings("unchecked")
   private void submitProcessTask(final CacheLoaderTask cacheLoaderTask, final KeyFilter filter, ExecutorCompletionService ecs,
                                  final TaskContext taskContext, final List<Map.Entry<Object, byte[]>> batch) {
      ecs.submit(new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            for (Map.Entry<Object, byte[]> entry : batch) {
               if (taskContext.isStopped())
                  break;
               Object key = entry.getKey();
               if (filter == null || filter.shouldLoadKey(key))
                  cacheLoaderTask.processEntry((MarshalledEntry) unmarshall(entry.getValue()), taskContext);
            }
            return null;
         }
      });
   }

   @Override
   public int size() {
      return store.size();
   }

   @SuppressWarnings("unchecked")
   @Override
   public void purge(Executor executor, PurgeListener purgeListener) {
      try {
         // Drain queue and update expiry tree
         List<ExpiryEntry> entries = new ArrayList<ExpiryEntry>();
         expiryEntryQueue.drainTo(entries);
         for (ExpiryEntry entry : entries) {
            final byte[] keyBytes = marshall(entry.key);
            final byte[] existingBytes = expired.get(entry.expiry);

            if (existingBytes != null) {
               // in the case of collision make the key a List ...
               final Object existing = unmarshall(existingBytes);
               if (existing instanceof List) {
                  ((List<Object>) existing).add(entry.key);
                  expired.put(entry.expiry, marshall(existing));
               } else {
                  List<Object> al = new ArrayList<Object>(2);
                  al.add(existing);
                  al.add(entry.key);
                  expired.put(entry.expiry, marshall(al));
               }
            } else {
               expired.put(entry.expiry, keyBytes);
            }
         }

         List<Long> times = new ArrayList<Long>();
         List<Object> keys = new ArrayList<Object>();
         
         try {
            for (Map.Entry<Long, byte[]> entry : expired.entrySet()) {
               Long time = entry.getKey();
               if (time > System.currentTimeMillis())
                  break;
               times.add(time);
               Object key = unmarshall(entry.getValue());
               if (key instanceof List)
                  keys.addAll((List<?>) key);
               else
                  keys.add(key);
            }

            for (Long time : times) {
               expired.remove(time);
            }

            if (!keys.isEmpty())
               log.debugf("purge (up to) %d entries", keys.size());
            int count = 0;
            
            for (Object key : keys) {
               byte[] b = store.get(key);
               if (b == null)
                  continue;
               MarshalledEntry me = (MarshalledEntry) ctx.getMarshaller().objectFromByteBuffer(b);
               if (me.getMetadata() != null && me.getMetadata().isExpired(ctx.getTimeService().wallClockTime())) {
                  // somewhat inefficient to FIND then REMOVE...
                  store.remove(key);
                  count++;
               }
            }
            if (count != 0)
               log.debugf("purged %d entries", count);
         } catch (Exception e) {
            throw new CacheLoaderException(e);
         }
      } catch (CacheLoaderException e) {
         throw e;
      } catch (Exception e) {
         throw new CacheLoaderException(e);
      }
   }

   private void addNewExpiry(MarshalledEntry entry) throws IOException {
      long expiry = entry.getMetadata().expiryTime();
      long maxIdle = entry.getMetadata().maxIdle();
      if (maxIdle > 0) {
         // Coding getExpiryTime() for transient entries has the risk of
         // being a moving target
         // which could lead to unexpected results, hence, InternalCacheEntry
         // calls are required
         expiry = maxIdle + System.currentTimeMillis();
      }
      Long at = expiry;
      Object key = entry.getKey();

      try {
         expiryEntryQueue.put(new ExpiryEntry(at, key));
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt(); // Restore interruption status
      }
   }

   private static final class ExpiryEntry {
      private final Long expiry;
      private final Object key;

      private ExpiryEntry(long expiry, Object key) {
         this.expiry = expiry;
         this.key = key;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((key == null) ? 0 : key.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         ExpiryEntry other = (ExpiryEntry) obj;
         if (key == null) {
            if (other.key != null)
               return false;
         } else if (!key.equals(other.key))
            return false;
         return true;
      }
   }
   
   private byte[] marshall(Object entry) throws IOException, InterruptedException {
      return ctx.getMarshaller().objectToByteBuffer(entry);
   }

   private Object unmarshall(byte[] bytes) throws IOException, ClassNotFoundException {
      if (bytes == null)
         return null;

      return ctx.getMarshaller().objectFromByteBuffer(bytes);
   }

}
