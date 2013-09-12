package org.infinispan.loaders.mapdb.configuration;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.infinispan.commons.util.StringPropertyReplacer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.configuration.parsing.Namespace;
import org.infinispan.configuration.parsing.Namespaces;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.configuration.parsing.Parser60;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;

/**
 * 
 * @author <a href="mailto:rtsang@redhat.com">Ray Tsang</a>
 * 
 */
@Namespaces({ @Namespace(uri = "urn:infinispan:config:mapdbStore:6.0", root = "mapdbStore"), @Namespace(root = "mapdbStore") })
public class MapDBStoreConfigurationParser60 implements ConfigurationParser {

   public MapDBStoreConfigurationParser60() {
   }

   @Override
   public void readElement(XMLExtendedStreamReader reader, ConfigurationBuilderHolder holder) throws XMLStreamException {
      ConfigurationBuilder builder = holder.getCurrentConfigurationBuilder();
      Element element = Element.forName(reader.getLocalName());
      switch (element) {
      case MAPDB_STORE: {
         parseOffheapCacheStore(reader, builder.persistence().addStore(MapDBStoreConfigurationBuilder.class));
         break;
      }
      default: {
         throw ParseUtils.unexpectedElement(reader);
      }
      }
   }

   private void parseOffheapCacheStore(XMLExtendedStreamReader reader, MapDBStoreConfigurationBuilder builder) throws XMLStreamException {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
         ParseUtils.requireNoNamespaceAttribute(reader, i);
         String attributeValue = reader.getAttributeValue(i);
         String value = StringPropertyReplacer.replaceProperties(attributeValue);
         String attrName = reader.getAttributeLocalName(i);
         Attribute attribute = Attribute.forName(attrName);

         switch (attribute) {
         case EXPIRY_QUEUE_SIZE: {
            builder.expiryQueueSize(Integer.valueOf(value));
         }
         case COMPRESSION: {
            builder.compression(Boolean.valueOf(value));
            break;
         }
         case LOCATION: {
            builder.location(value);
            break;
         }
         default: {
            Parser60.parseCommonStoreAttributes(reader, builder, attrName, attributeValue, i);
         }
         }
      }

      if (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         ParseUtils.unexpectedElement(reader);
      }
   }
}
