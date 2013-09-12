package org.infinispan.loaders.mapdb.logging;

import org.jboss.logging.MessageLogger;

/**
 * Log abstraction for the MapDB cache store. For this module, message ids ranging from 25001 to
 * 26000 inclusively have been reserved.
 * 
 * @author Ray Tsang
 * @since 6.0
 */
@MessageLogger(projectCode = "ISPN")
public interface Log extends org.infinispan.util.logging.Log {
}
