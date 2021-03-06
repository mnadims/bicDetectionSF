From e7294aab77dcd0501576c0639bc644ca7a0015c2 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Mon, 14 Nov 2011 10:27:51 +0000
Subject: [PATCH] SOLR-2382 Support pluggable caching implementations

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1201659 13f79535-47bb-0310-9956-ffa450edef68
--
 .../handler/dataimport/CachePropertyUtil.java |  32 +++
 .../dataimport/CachedSqlEntityProcessor.java  |  56 +---
 .../solr/handler/dataimport/DIHCache.java     |  91 +++++++
 .../handler/dataimport/DIHCacheSupport.java   | 251 ++++++++++++++++++
 .../solr/handler/dataimport/DIHWriter.java    |  12 +
 .../handler/dataimport/DIHWriterBase.java     |  28 ++
 .../solr/handler/dataimport/DataConfig.java   |   2 +
 .../solr/handler/dataimport/DocBuilder.java   |  38 ++-
 .../dataimport/EntityProcessorBase.java       | 225 ++++------------
 .../solr/handler/dataimport/SolrWriter.java   |   4 +-
 .../dataimport/SortedMapBackedCache.java      | 198 ++++++++++++++
 .../handler/dataimport/ThreadedContext.java   |   8 +-
 .../solr/conf/dataimport-cache-ephemeral.xml  |  32 +++
 .../dih/solr/conf/dataimport-schema.xml       |  17 +-
 .../dataimport/AbstractDIHCacheTestCase.java  | 217 +++++++++++++++
 .../TestCachedSqlEntityProcessor.java         |   2 +-
 .../dataimport/TestEphemeralCache.java        |  68 +++++
 .../dataimport/TestSortedMapBackedCache.java  | 142 ++++++++++
 18 files changed, 1177 insertions(+), 246 deletions(-)
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachePropertyUtil.java
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCacheSupport.java
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriterBase.java
 create mode 100644 solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SortedMapBackedCache.java
 create mode 100644 solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-cache-ephemeral.xml
 create mode 100644 solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDIHCacheTestCase.java
 create mode 100644 solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestEphemeralCache.java
 create mode 100644 solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSortedMapBackedCache.java

diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachePropertyUtil.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachePropertyUtil.java
new file mode 100644
index 00000000000..bae10757b2d
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachePropertyUtil.java
@@ -0,0 +1,32 @@
package org.apache.solr.handler.dataimport;

public class CachePropertyUtil {
  public static String getAttributeValueAsString(Context context, String attr) {
    Object o = context.getSessionAttribute(attr, Context.SCOPE_ENTITY);
    if (o == null) {
      o = context.getResolvedEntityAttribute(attr);
    }
    if (o == null && context.getRequestParameters() != null) {
      o = context.getRequestParameters().get(attr);
    }
    if (o == null) {
      return null;
    }
    return o.toString();
  }
  
  public static Object getAttributeValue(Context context, String attr) {
    Object o = context.getSessionAttribute(attr, Context.SCOPE_ENTITY);
    if (o == null) {
      o = context.getResolvedEntityAttribute(attr);
    }
    if (o == null && context.getRequestParameters() != null) {
      o = context.getRequestParameters().get(attr);
    }
    if (o == null) {
      return null;
    }
    return o;
  }
  
}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachedSqlEntityProcessor.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachedSqlEntityProcessor.java
index a2ef3b97dd7..b67a68c40cb 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachedSqlEntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/CachedSqlEntityProcessor.java
@@ -16,66 +16,26 @@
  */
 package org.apache.solr.handler.dataimport;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

 /**
  * This class enables caching of data obtained from the DB to avoid too many sql
  * queries
  * <p/>
  * <p>
  * Refer to <a
 * href="http://wiki.apache.org/solr/DataImportHandler">http://wiki.apache.org/solr/DataImportHandler</a>
 * for more details.
 * href="http://wiki.apache.org/solr/DataImportHandler">http://wiki.apache
 * .org/solr/DataImportHandler</a> for more details.
  * </p>
  * <p/>
  * <b>This API is experimental and subject to change</b>
 *
 * 
  * @since solr 1.3
 * @deprecated - Use SqlEntityProcessor with cacheImpl parameter.
  */
@Deprecated
 public class CachedSqlEntityProcessor extends SqlEntityProcessor {
  private boolean isFirst;

  @Override
  @SuppressWarnings("unchecked")
  public void init(Context context) {
    super.init(context);
    super.cacheInit();
    isFirst = true;
  }

  @Override
  public Map<String, Object> nextRow() {
    if (dataSourceRowCache != null)
      return getFromRowCacheTransformed();
    if (!isFirst)
      return null;
    String query = context.replaceTokens(context.getEntityAttribute("query"));
    isFirst = false;
    if (simpleCache != null) {
      return getSimpleCacheData(query);
    } else {
      return getIdCacheData(query);
    @Override
    protected void initCache(Context context) {
      cacheSupport = new DIHCacheSupport(context, "SortedMapBackedCache");
     }
 
  }

  @Override
  protected List<Map<String, Object>> getAllNonCachedRows() {
    List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    String q = getQuery();
    initQuery(context.replaceTokens(q));
    if (rowIterator == null)
      return rows;
    while (rowIterator.hasNext()) {
      Map<String, Object> arow = rowIterator.next();
      if (arow == null) {
        break;
      } else {
        rows.add(arow);
      }
    }
    return rows;
  }
 }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java
new file mode 100644
index 00000000000..049e503330c
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCache.java
@@ -0,0 +1,91 @@
package org.apache.solr.handler.dataimport;

import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * A cache that allows a DIH entity's data to persist locally prior being joined
 * to other data and/or indexed.
 * </p>
 * 
 * @solr.experimental
 */
public interface DIHCache extends Iterable<Map<String,Object>> {
  
  /**
   * <p>
   * Opens the cache using the specified properties. The {@link Context}
   * includes any parameters needed by the cache impl. This must be called
   * before any read/write operations are permitted.
   * <p>
   */
  public void open(Context context);
  
  /**
   * <p>
   * Releases resources used by this cache, if possible. The cache is flushed
   * but not destroyed.
   * </p>
   */
  public void close();
  
  /**
   * <p>
   * Persists any pending data to the cache
   * </p>
   */
  public void flush();
  
  /**
   * <p>
   * Closes the cache, if open. Then removes all data, possibly removing the
   * cache entirely from persistent storage.
   * </p>
   */
  public void destroy();
  
  /**
   * <p>
   * Adds a document. If a document already exists with the same key, both
   * documents will exist in the cache, as the cache allows duplicate keys. To
   * update a key's documents, first call delete(Object key).
   * </p>
   * 
   * @param rec
   */
  public void add(Map<String,Object> rec);
  
  /**
   * <p>
   * Returns an iterator, allowing callers to iterate through the entire cache
   * in key, then insertion, order.
   * </p>
   */
  public Iterator<Map<String,Object>> iterator();
  
  /**
   * <p>
   * Returns an iterator, allowing callers to iterate through all documents that
   * match the given key in insertion order.
   * </p>
   */
  public Iterator<Map<String,Object>> iterator(Object key);
  
  /**
   * <p>
   * Delete all documents associated with the given key
   * </p>
   * 
   * @param key
   */
  public void delete(Object key);
  
  /**
   * <p>
   * Delete all data from the cache,leaving the empty cache intact.
   * </p>
   */
  public void deleteAll();
  
}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCacheSupport.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCacheSupport.java
new file mode 100644
index 00000000000..c042bfa86ac
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHCacheSupport.java
@@ -0,0 +1,251 @@
package org.apache.solr.handler.dataimport;

import static org.apache.solr.handler.dataimport.DataImportHandlerException.wrapAndThrow;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DIHCacheSupport {
  private static final Logger log = LoggerFactory
      .getLogger(DIHCacheSupport.class);
  private String cacheVariableName;
  private String cacheImplName;
  private Map<String,DIHCache> queryVsCache = new HashMap<String,DIHCache>();
  private Map<String,Iterator<Map<String,Object>>> queryVsCacheIterator;
  private Iterator<Map<String,Object>> dataSourceRowCache;
  private boolean cacheDoKeyLookup;
  
  public DIHCacheSupport(Context context, String cacheImplName) {
    this.cacheImplName = cacheImplName;
    
    String where = context.getEntityAttribute("where");
    String cacheKey = context
        .getEntityAttribute(DIHCacheSupport.CACHE_PRIMARY_KEY);
    String lookupKey = context
        .getEntityAttribute(DIHCacheSupport.CACHE_FOREIGN_KEY);
    if (cacheKey != null && lookupKey == null) {
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE,
          "'cacheKey' is specified for the entity "
              + context.getEntityAttribute("name")
              + " but 'cacheLookup' is missing");
      
    }
    if (where == null && cacheKey == null) {
      cacheDoKeyLookup = false;
    } else {
      if (where != null) {
        String[] splits = where.split("=");
        cacheVariableName = splits[1].trim();
      } else {
        cacheVariableName = lookupKey;
      }
      cacheDoKeyLookup = true;
    }
    context.setSessionAttribute(DIHCacheSupport.CACHE_PRIMARY_KEY, cacheKey,
        Context.SCOPE_ENTITY);
    context.setSessionAttribute(DIHCacheSupport.CACHE_FOREIGN_KEY, lookupKey,
        Context.SCOPE_ENTITY);
    context.setSessionAttribute(DIHCacheSupport.CACHE_DELETE_PRIOR_DATA,
        "true", Context.SCOPE_ENTITY);
    context.setSessionAttribute(DIHCacheSupport.CACHE_READ_ONLY, "false",
        Context.SCOPE_ENTITY);
  }
  
  private DIHCache instantiateCache(Context context) {
    DIHCache cache = null;
    try {
      @SuppressWarnings("unchecked")
      Class<DIHCache> cacheClass = DocBuilder.loadClass(cacheImplName, context
          .getSolrCore());
      Constructor<DIHCache> constr = cacheClass.getConstructor();
      cache = constr.newInstance();
      cache.open(context);
    } catch (Exception e) {
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE,
          "Unable to load Cache implementation:" + cacheImplName, e);
    }
    return cache;
  }
  
  public void initNewParent(Context context) {
    queryVsCacheIterator = new HashMap<String,Iterator<Map<String,Object>>>();
    for (Map.Entry<String,DIHCache> entry : queryVsCache.entrySet()) {
      queryVsCacheIterator.put(entry.getKey(), entry.getValue().iterator());
    }
  }
  
  public void destroyAll() {
    if (queryVsCache != null) {
      for (DIHCache cache : queryVsCache.values()) {
        cache.destroy();
      }
    }
    queryVsCache = null;
    dataSourceRowCache = null;
    cacheVariableName = null;
  }
  
  /**
   * <p>
   * Get all the rows from the datasource for the given query and cache them
   * </p>
   */
  public void populateCache(String query,
      Iterator<Map<String,Object>> rowIterator) {
    Map<String,Object> aRow = null;
    DIHCache cache = queryVsCache.get(query);
    while ((aRow = getNextFromCache(query, rowIterator)) != null) {
      cache.add(aRow);
    }
  }
  
  private Map<String,Object> getNextFromCache(String query,
      Iterator<Map<String,Object>> rowIterator) {
    try {
      if (rowIterator == null) return null;
      if (rowIterator.hasNext()) return rowIterator.next();
      return null;
    } catch (Exception e) {
      SolrException.log(log, "getNextFromCache() failed for query '" + query
          + "'", e);
      wrapAndThrow(DataImportHandlerException.WARN, e);
      return null;
    }
  }
  
  public Map<String,Object> getCacheData(Context context, String query,
      Iterator<Map<String,Object>> rowIterator) {
    if (cacheDoKeyLookup) {
      return getIdCacheData(context, query, rowIterator);
    } else {
      return getSimpleCacheData(context, query, rowIterator);
    }
  }
  
  /**
   * If the where clause is present the cache is sql Vs Map of key Vs List of
   * Rows.
   * 
   * @param query
   *          the query string for which cached data is to be returned
   * 
   * @return the cached row corresponding to the given query after all variables
   *         have been resolved
   */
  protected Map<String,Object> getIdCacheData(Context context, String query,
      Iterator<Map<String,Object>> rowIterator) {
    Object key = context.resolve(cacheVariableName);
    if (key == null) {
      throw new DataImportHandlerException(DataImportHandlerException.WARN,
          "The cache lookup value : " + cacheVariableName
              + " is resolved to be null in the entity :"
              + context.getEntityAttribute("name"));
      
    }
    DIHCache cache = queryVsCache.get(query);
    if (cache == null) {
      cache = instantiateCache(context);
      queryVsCache.put(query, cache);
      populateCache(query, rowIterator);
    }
    if (dataSourceRowCache == null) {
      dataSourceRowCache = cache.iterator(key);
    }
    if (dataSourceRowCache == null) {
      return null;
    }
    return getFromRowCacheTransformed();
  }
  
  /**
   * If where clause is not present the cache is a Map of query vs List of Rows.
   * 
   * @param query
   *          string for which cached row is to be returned
   * 
   * @return the cached row corresponding to the given query
   */
  protected Map<String,Object> getSimpleCacheData(Context context,
      String query, Iterator<Map<String,Object>> rowIterator) {
    DIHCache cache = queryVsCache.get(query);
    if (cache == null) {
      cache = instantiateCache(context);
      queryVsCache.put(query, cache);
      populateCache(query, rowIterator);
      queryVsCacheIterator.put(query, cache.iterator());
    }
    if (dataSourceRowCache == null || !dataSourceRowCache.hasNext()) {
      dataSourceRowCache = null;
      Iterator<Map<String,Object>> cacheIter = queryVsCacheIterator.get(query);
      if (cacheIter.hasNext()) {
        List<Map<String,Object>> dsrcl = new ArrayList<Map<String,Object>>(1);
        dsrcl.add(cacheIter.next());
        dataSourceRowCache = dsrcl.iterator();
      }
    }
    if (dataSourceRowCache == null) {
      return null;
    }
    return getFromRowCacheTransformed();
  }
  
  protected Map<String,Object> getFromRowCacheTransformed() {
    if (dataSourceRowCache == null || !dataSourceRowCache.hasNext()) {
      dataSourceRowCache = null;
      return null;
    }
    Map<String,Object> r = dataSourceRowCache.next();
    return r;
  }
  
  /**
   * <p>
   * Specify the class for the cache implementation
   * </p>
   */
  public static final String CACHE_IMPL = "cacheImpl";

  /**
   * <p>
   * If the cache supports persistent data, set to "true" to delete any prior
   * persisted data before running the entity.
   * </p>
   */
  
  public static final String CACHE_DELETE_PRIOR_DATA = "cacheDeletePriorData";
  /**
   * <p>
   * Specify the Foreign Key from the parent entity to join on. Use if the cache
   * is on a child entity.
   * </p>
   */
  public static final String CACHE_FOREIGN_KEY = "cacheLookup";



  /**
   * <p>
   * Specify the Primary Key field from this Entity to map the input records
   * with
   * </p>
   */
  public static final String CACHE_PRIMARY_KEY = "cachePk";
  /**
   * <p>
   * If true, a pre-existing cache is re-opened for read-only access.
   * </p>
   */
  public static final String CACHE_READ_ONLY = "cacheReadOnly";



  
}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriter.java
index 563c07be66e..9140730aaef 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriter.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriter.java
@@ -15,6 +15,9 @@ package org.apache.solr.handler.dataimport;
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import java.util.Map;
import java.util.Set;

 import org.apache.solr.common.SolrInputDocument;
 
 /**
@@ -90,4 +93,13 @@ public interface DIHWriter {
 	 */
 	public void init(Context context) ;
 
	
	/**
	 * <p>
	 *  Specify the keys to be modified by a delta update (required by writers that can store duplicate keys)
	 * </p>
	 * @param deltaKeys
	 */
	public void setDeltaKeys(Set<Map<String, Object>> deltaKeys) ;

 }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriterBase.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriterBase.java
new file mode 100644
index 00000000000..a11df0d6feb
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DIHWriterBase.java
@@ -0,0 +1,28 @@
package org.apache.solr.handler.dataimport;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class DIHWriterBase implements DIHWriter {
  protected String keyFieldName;
  protected Set<Object> deltaKeys = null;
  
  @Override
  public void setDeltaKeys(Set<Map<String,Object>> passedInDeltaKeys) {
    deltaKeys = new HashSet<Object>();
    for (Map<String,Object> aMap : passedInDeltaKeys) {
      if (aMap.size() > 0) {
        Object key = null;
        if (keyFieldName != null) {
          key = aMap.get(keyFieldName);
        } else {
          key = aMap.entrySet().iterator().next();
        }
        if (key != null) {
          deltaKeys.add(key);
        }
      }
    }
  }
}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataConfig.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataConfig.java
index ab958860acb..2b0e7bade33 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataConfig.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataConfig.java
@@ -109,6 +109,8 @@ public class DataConfig {
     public DataSource dataSrc;
 
     public Map<String, List<Field>> colNameVsField = new HashMap<String, List<Field>>();
    
    public boolean initalized = false;
 
     public Entity() {
     }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
index 77f41c850e6..1badb6ba4c4 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
@@ -336,6 +336,7 @@ public class DocBuilder {
       // Make sure that documents are not re-created
     }
     deletedKeys = null;
    writer.setDeltaKeys(allPks);
 
     statusMessages.put("Total Changed Documents", allPks.size());
     VariableResolverImpl vri = getVariableResolver();
@@ -428,7 +429,7 @@ public class DocBuilder {
       for (int i = 0; i < threads; i++) {
         entityProcessorWrapper.add(new ThreadedEntityProcessorWrapper(entityProcessor, DocBuilder.this, this, getVariableResolver()));
       }
      context = new ThreadedContext(this, DocBuilder.this);
      context = new ThreadedContext(this, DocBuilder.this, getVariableResolver());
     }
 
 
@@ -557,7 +558,6 @@ public class DocBuilder {
           }
         }
       } finally {
        epw.destroy();
         currentEntityProcWrapper.remove();
         Context.CURRENT_CONTEXT.remove();
       }
@@ -590,10 +590,35 @@ public class DocBuilder {
     }
   }
 
  private void resetEntity(DataConfig.Entity entity) {
    entity.initalized = false;
    if (entity.entities != null) {
      for (DataConfig.Entity child : entity.entities) {
        resetEntity(child);
      }
    }
  }
  
  private void buildDocument(VariableResolverImpl vr, DocWrapper doc,
      Map<String,Object> pk, DataConfig.Entity entity, boolean isRoot,
      ContextImpl parentCtx) {
    List<EntityProcessorWrapper> entitiesToDestroy = new ArrayList<EntityProcessorWrapper>();
    try {
      buildDocument(vr, doc, pk, entity, isRoot, parentCtx, entitiesToDestroy);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      for (EntityProcessorWrapper entityWrapper : entitiesToDestroy) {
        entityWrapper.destroy();
      }
      resetEntity(entity);
    }
  }

   @SuppressWarnings("unchecked")
   private void buildDocument(VariableResolverImpl vr, DocWrapper doc,
                              Map<String, Object> pk, DataConfig.Entity entity, boolean isRoot,
                             ContextImpl parentCtx) {
                             ContextImpl parentCtx, List<EntityProcessorWrapper> entitiesToDestroy) {
 
     EntityProcessorWrapper entityProcessor = getEntityProcessor(entity);
 
@@ -602,6 +627,10 @@ public class DocBuilder {
             session, parentCtx, this);
     entityProcessor.init(ctx);
     Context.CURRENT_CONTEXT.set(ctx);
    if (!entity.initalized) {
      entitiesToDestroy.add(entityProcessor);
      entity.initalized = true;
    }
     
     if (requestParameters.start > 0) {
       getDebugLogger().log(DIHLogLevels.DISABLE_LOGGING, null, null);
@@ -666,7 +695,7 @@ public class DocBuilder {
             vr.addNamespace(entity.name, arow);
             for (DataConfig.Entity child : entity.entities) {
               buildDocument(vr, doc,
                  child.isDocRoot ? pk : null, child, false, ctx);
                  child.isDocRoot ? pk : null, child, false, ctx, entitiesToDestroy);
             }
             vr.removeNamespace(entity.name);
           }
@@ -729,7 +758,6 @@ public class DocBuilder {
       if (verboseDebug) {
         getDebugLogger().log(DIHLogLevels.END_ENTITY, null, null);
       }
      entityProcessor.destroy();
     }
   }
 
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorBase.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorBase.java
index 9aaa5374841..46cfe57c276 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorBase.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorBase.java
@@ -17,6 +17,7 @@
 package org.apache.solr.handler.dataimport;
 
 import org.apache.solr.common.SolrException;

 import static org.apache.solr.handler.dataimport.DataImportHandlerException.*;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -42,21 +43,25 @@ public class EntityProcessorBase extends EntityProcessor {
 
   protected Iterator<Map<String, Object>> rowIterator;
 
  protected List<Transformer> transformers;

  protected String query;

  protected String onError = ABORT;
  protected String query;  
  
  protected String onError = ABORT;  
  
  protected DIHCacheSupport cacheSupport = null;
 
 
   @Override
   public void init(Context context) {
    rowIterator = null;
     this.context = context;
     if (isFirstInit) {
       firstInit(context);
     }
    query = null;
    if(cacheSupport!=null) {
      rowIterator = null;
      query = null;
      cacheSupport.initNewParent(context);
    }   
    
   }
 
   /**first time init call. do one-time operations here
@@ -65,29 +70,20 @@ public class EntityProcessorBase extends EntityProcessor {
     entityName = context.getEntityAttribute("name");
     String s = context.getEntityAttribute(ON_ERROR);
     if (s != null) onError = s;
    initCache(context);
     isFirstInit = false;
   }
 
    protected void initCache(Context context) {
        String cacheImplName = context
            .getResolvedEntityAttribute(DIHCacheSupport.CACHE_IMPL);
 
  protected Map<String, Object> getNext() {
    try {
      if (rowIterator == null)
        return null;
      if (rowIterator.hasNext())
        return rowIterator.next();
      query = null;
      rowIterator = null;
      return null;
    } catch (Exception e) {
      SolrException.log(log, "getNext() failed for query '" + query + "'", e);
      query = null;
      rowIterator = null;
      wrapAndThrow(DataImportHandlerException.WARN, e);
      return null;
        if (cacheImplName != null ) {
          cacheSupport = new DIHCacheSupport(context, cacheImplName);
        }
     }
  }
 
  @Override
    @Override
   public Map<String, Object> nextModifiedRowKey() {
     return null;
   }
@@ -113,165 +109,40 @@ public class EntityProcessorBase extends EntityProcessor {
   public Map<String, Object> nextRow() {
     return null;// do not do anything
   }


  @Override
  public void destroy() {
    /*no op*/
  }

  /**
   * Only used by cache implementations
   */
  protected String cachePk;

  /**
   * Only used by cache implementations
   */
  protected String cacheVariableName;

  /**
   * Only used by cache implementations
   */
  protected Map<String, List<Map<String, Object>>> simpleCache;

  /**
   * Only used by cache implementations
   */
  protected Map<String, Map<Object, List<Map<String, Object>>>> cacheWithWhereClause;

  protected List<Map<String, Object>> dataSourceRowCache;

  /**
   * Only used by cache implementations
   */
  protected void cacheInit() {
    if (simpleCache != null || cacheWithWhereClause != null)
      return;
    String where = context.getEntityAttribute("where");

    String cacheKey = context.getEntityAttribute(CACHE_KEY);
    String lookupKey = context.getEntityAttribute(CACHE_LOOKUP);
    if(cacheKey != null && lookupKey == null){
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE,
              "'cacheKey' is specified for the entity "+ entityName+" but 'cacheLookup' is missing" );

    }
    if (where == null && cacheKey == null) {
      simpleCache = new HashMap<String, List<Map<String, Object>>>();
    } else {
      if (where != null) {
        String[] splits = where.split("=");
        cachePk = splits[0];
        cacheVariableName = splits[1].trim();
      } else {
        cachePk = cacheKey;
        cacheVariableName = lookupKey;
      }
      cacheWithWhereClause = new HashMap<String, Map<Object, List<Map<String, Object>>>>();
    }
  }

  /**
   * If the where clause is present the cache is sql Vs Map of key Vs List of Rows. Only used by cache implementations.
   *
   * @param query the query string for which cached data is to be returned
   *
   * @return the cached row corresponding to the given query after all variables have been resolved
   */
  protected Map<String, Object> getIdCacheData(String query) {
    Map<Object, List<Map<String, Object>>> rowIdVsRows = cacheWithWhereClause
            .get(query);
    List<Map<String, Object>> rows = null;
    Object key = context.resolve(cacheVariableName);
    if (key == null) {
      throw new DataImportHandlerException(DataImportHandlerException.WARN,
              "The cache lookup value : " + cacheVariableName + " is resolved to be null in the entity :" +
                      context.getEntityAttribute("name"));

    }
    if (rowIdVsRows != null) {
      rows = rowIdVsRows.get(key);
      if (rows == null)
  
  protected Map<String, Object> getNext() {
    if(cacheSupport==null) {
      try {
        if (rowIterator == null)
          return null;
        if (rowIterator.hasNext())
          return rowIterator.next();
        query = null;
        rowIterator = null;
         return null;
      dataSourceRowCache = new ArrayList<Map<String, Object>>(rows);
      return getFromRowCacheTransformed();
    } else {
      rows = getAllNonCachedRows();
      if (rows.isEmpty()) {
      } catch (Exception e) {
        SolrException.log(log, "getNext() failed for query '" + query + "'", e);
        query = null;
        rowIterator = null;
        wrapAndThrow(DataImportHandlerException.WARN, e);
         return null;
      } else {
        rowIdVsRows = new HashMap<Object, List<Map<String, Object>>>();
        for (Map<String, Object> row : rows) {
          Object k = row.get(cachePk);
          if (k == null) {
            throw new DataImportHandlerException(DataImportHandlerException.WARN,
                    "No value available for the cache key : " + cachePk + " in the entity : " +
                            context.getEntityAttribute("name"));
          }
          if (!k.getClass().equals(key.getClass())) {
            throw new DataImportHandlerException(DataImportHandlerException.WARN,
                    "The key in the cache type : " + k.getClass().getName() +
                            "is not same as the lookup value type " + key.getClass().getName() + " in the entity " +
                            context.getEntityAttribute("name"));
          }
          if (rowIdVsRows.get(k) == null)
            rowIdVsRows.put(k, new ArrayList<Map<String, Object>>());
          rowIdVsRows.get(k).add(row);
        }
        cacheWithWhereClause.put(query, rowIdVsRows);
        if (!rowIdVsRows.containsKey(key))
          return null;
        dataSourceRowCache = new ArrayList<Map<String, Object>>(rowIdVsRows.get(key));
        if (dataSourceRowCache.isEmpty()) {
          dataSourceRowCache = null;
          return null;
        }
        return getFromRowCacheTransformed();
       }
    }
    } else  {
      return cacheSupport.getCacheData(context, query, rowIterator);
    }      
   }
 
  /**
   * <p> Get all the rows from the the datasource for the given query. Only used by cache implementations. </p> This
   * <b>must</b> be implemented by sub-classes which intend to provide a cached implementation
   *
   * @return the list of all rows fetched from the datasource.
   */
  protected List<Map<String, Object>> getAllNonCachedRows() {
    return Collections.EMPTY_LIST;
  }
 
  /**
   * If where clause is not present the cache is a Map of query vs List of Rows. Only used by cache implementations.
   *
   * @param query string for which cached row is to be returned
   *
   * @return the cached row corresponding to the given query
   */
  protected Map<String, Object> getSimpleCacheData(String query) {
    List<Map<String, Object>> rows = simpleCache.get(query);
    if (rows != null) {
      dataSourceRowCache = new ArrayList<Map<String, Object>>(rows);
      return getFromRowCacheTransformed();
    } else {
      rows = getAllNonCachedRows();
      if (rows.isEmpty()) {
        return null;
      } else {
        dataSourceRowCache = new ArrayList<Map<String, Object>>(rows);
        simpleCache.put(query, rows);
        return getFromRowCacheTransformed();
      }
    }
  @Override
  public void destroy() {
  	query = null;
  	if(cacheSupport!=null){
  	  cacheSupport.destroyAll();
  	}
  	cacheSupport = null;
   }
 
  protected Map<String, Object> getFromRowCacheTransformed() {
    Map<String, Object> r = dataSourceRowCache.remove(0);
    if (dataSourceRowCache.isEmpty())
      dataSourceRowCache = null;
    return r;
  }
  
 
   public static final String TRANSFORMER = "transformer";
 
@@ -287,8 +158,4 @@ public class EntityProcessorBase extends EntityProcessor {
 
   public static final String SKIP_DOC = "$skipDoc";
 
  public static final String CACHE_KEY = "cacheKey";
  
  public static final String CACHE_LOOKUP = "cacheLookup";

 }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
index 8944a4cd988..a37de1780b8 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
@@ -37,7 +37,7 @@ import java.util.Set;
  *
  * @since solr 1.3
  */
public class SolrWriter implements DIHWriter {
public class SolrWriter extends DIHWriterBase implements DIHWriter {
   private static final Logger log = LoggerFactory.getLogger(SolrWriter.class);
 
   static final String LAST_INDEX_KEY = "last_index_time";
@@ -159,5 +159,5 @@ public class SolrWriter implements DIHWriter {
 	@Override
 	public void init(Context context) {
 		/* NO-OP */		
	}
	}	
 }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SortedMapBackedCache.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SortedMapBackedCache.java
new file mode 100644
index 00000000000..be66208c233
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SortedMapBackedCache.java
@@ -0,0 +1,198 @@
package org.apache.solr.handler.dataimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class SortedMapBackedCache implements DIHCache {
  private SortedMap<Object,List<Map<String,Object>>> theMap = null;
  private boolean isOpen = false;
  private boolean isReadOnly = false;
  String primaryKeyName = null;
  
  @SuppressWarnings("unchecked")
  @Override
  public void add(Map<String,Object> rec) {
    checkOpen(true);
    checkReadOnly();
    
    if (rec == null || rec.size() == 0) {
      return;
    }
    
    if (primaryKeyName == null) {
      primaryKeyName = rec.keySet().iterator().next();
    }
    
    Object pk = rec.get(primaryKeyName);
    if (pk instanceof Collection<?>) {
      Collection<Object> c = (Collection<Object>) pk;
      if (c.size() != 1) {
        throw new RuntimeException(
            "The primary key must have exactly 1 element.");
      }
      pk = c.iterator().next();
    }
    List<Map<String,Object>> thisKeysRecs = theMap.get(pk);
    if (thisKeysRecs == null) {
      thisKeysRecs = new ArrayList<Map<String,Object>>();
      theMap.put(pk, thisKeysRecs);
    }
    thisKeysRecs.add(rec);
  }
  
  private void checkOpen(boolean shouldItBe) {
    if (!isOpen && shouldItBe) {
      throw new IllegalStateException(
          "Must call open() before using this cache.");
    }
    if (isOpen && !shouldItBe) {
      throw new IllegalStateException("The cache is already open.");
    }
  }
  
  private void checkReadOnly() {
    if (isReadOnly) {
      throw new IllegalStateException("Cache is read-only.");
    }
  }
  
  @Override
  public void close() {
    isOpen = false;
  }
  
  @Override
  public void delete(Object key) {
    checkOpen(true);
    checkReadOnly();
    theMap.remove(key);
  }
  
  @Override
  public void deleteAll() {
    deleteAll(false);
  }
  
  private void deleteAll(boolean readOnlyOk) {
    if (!readOnlyOk) {
      checkReadOnly();
    }
    if (theMap != null) {
      theMap.clear();
    }
  }
  
  @Override
  public void destroy() {
    deleteAll(true);
    theMap = null;
    isOpen = false;
  }
  
  @Override
  public void flush() {
    checkOpen(true);
    checkReadOnly();
  }
  
  @Override
  public Iterator<Map<String,Object>> iterator(Object key) {
    checkOpen(true);
    List<Map<String,Object>> val = theMap.get(key);
    if (val == null) {
      return null;
    }
    return val.iterator();
  }
  
  @Override
  public Iterator<Map<String,Object>> iterator() {
    return new Iterator<Map<String, Object>>() {
        private Iterator<Map.Entry<Object,List<Map<String,Object>>>> theMapIter;
        private List<Map<String,Object>> currentKeyResult = null;
        private Iterator<Map<String,Object>> currentKeyResultIter = null;

        {
            theMapIter = theMap.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
          if (currentKeyResultIter != null) {
            if (currentKeyResultIter.hasNext()) {
              return true;
            } else {
              currentKeyResult = null;
              currentKeyResultIter = null;
            }
          }

          Map.Entry<Object,List<Map<String,Object>>> next = null;
          if (theMapIter.hasNext()) {
            next = theMapIter.next();
            currentKeyResult = next.getValue();
            currentKeyResultIter = currentKeyResult.iterator();
            if (currentKeyResultIter.hasNext()) {
              return true;
            }
          }
          return false;
        }

        @Override
        public Map<String,Object> next() {
          if (currentKeyResultIter != null) {
            if (currentKeyResultIter.hasNext()) {
              return currentKeyResultIter.next();
            } else {
              currentKeyResult = null;
              currentKeyResultIter = null;
            }
          }

          Map.Entry<Object,List<Map<String,Object>>> next = null;
          if (theMapIter.hasNext()) {
            next = theMapIter.next();
            currentKeyResult = next.getValue();
            currentKeyResultIter = currentKeyResult.iterator();
            if (currentKeyResultIter.hasNext()) {
              return currentKeyResultIter.next();
            }
          }
          return null;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
    };
  }

    @Override
  public void open(Context context) {
    checkOpen(false);
    isOpen = true;
    if (theMap == null) {
      theMap = new TreeMap<Object,List<Map<String,Object>>>();
    }
    
    String pkName = CachePropertyUtil.getAttributeValueAsString(context,
        DIHCacheSupport.CACHE_PRIMARY_KEY);
    if (pkName != null) {
      primaryKeyName = pkName;
    }
    isReadOnly = false;
    String readOnlyStr = CachePropertyUtil.getAttributeValueAsString(context,
        DIHCacheSupport.CACHE_READ_ONLY);
    if ("true".equalsIgnoreCase(readOnlyStr)) {
      isReadOnly = true;
    }
  }
  
}
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ThreadedContext.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ThreadedContext.java
index 0386e76f1a6..2d4fe703a38 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ThreadedContext.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ThreadedContext.java
@@ -24,17 +24,19 @@ package org.apache.solr.handler.dataimport;
  */
 public class ThreadedContext extends ContextImpl{
   private DocBuilder.EntityRunner entityRunner;
  private VariableResolverImpl resolver;
   private boolean limitedContext = false;
 
  public ThreadedContext(DocBuilder.EntityRunner entityRunner, DocBuilder docBuilder) {
  public ThreadedContext(DocBuilder.EntityRunner entityRunner, DocBuilder docBuilder, VariableResolverImpl resolver) {
     super(entityRunner.entity,
            null,//to be fetched realtime
    				resolver,
             null,
             null,
             docBuilder.session,
             null,
             docBuilder);
     this.entityRunner = entityRunner;
    this.resolver = resolver;
   }
 
   @Override
@@ -45,7 +47,7 @@ public class ThreadedContext extends ContextImpl{
 
   @Override
   public Context getParentContext() {
    ThreadedContext ctx = new ThreadedContext(entityRunner.parent, docBuilder);
  	ThreadedContext ctx = new ThreadedContext(entityRunner.parent, docBuilder, resolver);
     ctx.limitedContext =  true;
     return ctx;
   }
diff --git a/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-cache-ephemeral.xml b/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-cache-ephemeral.xml
new file mode 100644
index 00000000000..a8ef928959f
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-cache-ephemeral.xml
@@ -0,0 +1,32 @@
<dataConfig>
	<dataSource type="MockDataSource" />
	<document>
		<entity 
			name="PARENT"
			processor="SqlEntityProcessor"
			cacheName="PARENT"
			cachePk="id"			
			query="SELECT * FROM PARENT"				
		>
			<entity
				name="CHILD_1"
				processor="SqlEntityProcessor"
				cacheImpl="SortedMapBackedCache"
				cacheName="CHILD"
				cachePk="id"
				cacheLookup="PARENT.id"
				fieldNames="id,         child1a_mult_s, child1b_s"
				fieldTypes="BIGDECIMAL, STRING,         STRING"
				query="SELECT * FROM CHILD_1"				
			/>
			<entity
				name="CHILD_2"
				processor="SqlEntityProcessor"
				cacheImpl="SortedMapBackedCache"
				cachePk="id"
				cacheLookup="PARENT.id"
				query="SELECT * FROM CHILD_2"				
			/>
		</entity>
	</document>
</dataConfig>
diff --git a/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-schema.xml b/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-schema.xml
index 65a855f9c4f..f71dd3ddcd9 100644
-- a/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-schema.xml
++ b/solr/contrib/dataimporthandler/src/test-files/dih/solr/conf/dataimport-schema.xml
@@ -274,14 +274,15 @@
         EXAMPLE:  name="*_i" will match any field ending in _i (like myid_i, z_i)
         Longer patterns will be matched first.  if equal size patterns
         both match, the first appearing in the schema will be used.  -->
   <dynamicField name="*_i"  type="sint"    indexed="true"  stored="true"/>
   <dynamicField name="*_s"  type="string"  indexed="true"  stored="true"/>
   <dynamicField name="*_l"  type="slong"   indexed="true"  stored="true"/>
   <dynamicField name="*_t"  type="text"    indexed="true"  stored="true"/>
   <dynamicField name="*_b"  type="boolean" indexed="true"  stored="true"/>
   <dynamicField name="*_f"  type="sfloat"  indexed="true"  stored="true"/>
   <dynamicField name="*_d"  type="sdouble" indexed="true"  stored="true"/>
   <dynamicField name="*_dt" type="date"    indexed="true"  stored="true"/>
   <dynamicField name="*_i"       type="sint"    indexed="true"  stored="true"/>
   <dynamicField name="*_s"       type="string"  indexed="true"  stored="true"/>
   <dynamicField name="*_mult_s"  type="string"  indexed="true"  stored="true"   multiValued="true"/>
   <dynamicField name="*_l"       type="slong"   indexed="true"  stored="true"/>
   <dynamicField name="*_t"       type="text"    indexed="true"  stored="true"/>
   <dynamicField name="*_b"       type="boolean" indexed="true"  stored="true"/>
   <dynamicField name="*_f"       type="sfloat"  indexed="true"  stored="true"/>
   <dynamicField name="*_d"       type="sdouble" indexed="true"  stored="true"/>
   <dynamicField name="*_dt"      type="date"    indexed="true"  stored="true"/>
 
    <dynamicField name="random*" type="random" />
 
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDIHCacheTestCase.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDIHCacheTestCase.java
new file mode 100644
index 00000000000..9a1cf997dae
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDIHCacheTestCase.java
@@ -0,0 +1,217 @@
package org.apache.solr.handler.dataimport;

import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.serial.SerialClob;

import org.apache.solr.handler.dataimport.AbstractDataImportHandlerTestCase.TestContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class AbstractDIHCacheTestCase {	
	protected static final Date Feb21_2011 = new Date(1298268000000l);
	protected final String[] fieldTypes = { "INTEGER", "BIGDECIMAL", "STRING", "STRING",   "FLOAT",   "DATE",   "CLOB" };
	protected final String[] fieldNames = { "a_id",    "PI",         "letter", "examples", "a_float", "a_date", "DESCRIPTION" };
	protected List<ControlData> data = new ArrayList<ControlData>();
	protected Clob APPLE = null;
	
	@Before
	public void setup() {
		try {
			APPLE = new SerialClob(new String("Apples grow on trees and they are good to eat.").toCharArray());
		} catch (SQLException sqe) {
			Assert.fail("Could not Set up Test");
		}

		// The first row needs to have all non-null fields,
		// otherwise we would have to always send the fieldTypes & fieldNames as CacheProperties when building.
		data = new ArrayList<ControlData>();
		data.add(new ControlData(new Object[] { new Integer(1), new BigDecimal(Math.PI), "A", "Apple", new Float(1.11), Feb21_2011, APPLE }));
		data.add(new ControlData(new Object[] { new Integer(2), new BigDecimal(Math.PI), "B", "Ball", new Float(2.22), Feb21_2011, null }));
		data.add(new ControlData(new Object[] { new Integer(4), new BigDecimal(Math.PI), "D", "Dog", new Float(4.44), Feb21_2011, null }));
		data.add(new ControlData(new Object[] { new Integer(3), new BigDecimal(Math.PI), "C", "Cookie", new Float(3.33), Feb21_2011, null }));
		data.add(new ControlData(new Object[] { new Integer(4), new BigDecimal(Math.PI), "D", "Daisy", new Float(4.44), Feb21_2011, null }));
		data.add(new ControlData(new Object[] { new Integer(4), new BigDecimal(Math.PI), "D", "Drawing", new Float(4.44), Feb21_2011, null }));
		data.add(new ControlData(new Object[] { new Integer(5), new BigDecimal(Math.PI), "E",
				Arrays.asList(new String[] { "Eggplant", "Ear", "Elephant", "Engine" }), new Float(5.55), Feb21_2011, null }));
	}

	@After
	public void teardown() {
		APPLE = null;
		data = null;
	}
	
	//A limitation of this test class is that the primary key needs to be the first one in the list.
	//DIHCaches, however, can handle any field being the primary key.
	class ControlData implements Comparable<ControlData>, Iterable<Object> {
		Object[] data;

		ControlData(Object[] data) {
			this.data = data;
		}

		@SuppressWarnings("unchecked")
		public int compareTo(ControlData cd) {
			Comparable c1 = (Comparable) data[0];
			Comparable c2 = (Comparable) cd.data[0];
			return c1.compareTo(c2);
		}

		public Iterator<Object> iterator() {
			return Arrays.asList(data).iterator();
		}
	}
	
	protected void loadData(DIHCache cache, List<ControlData> theData, String[] theFieldNames, boolean keepOrdered) {
		for (ControlData cd : theData) {
			cache.add(controlDataToMap(cd, theFieldNames, keepOrdered));
		}
	}

	protected List<ControlData> extractDataInKeyOrder(DIHCache cache, String[] theFieldNames) {
		List<Object[]> data = new ArrayList<Object[]>();
		Iterator<Map<String, Object>> cacheIter = cache.iterator();
		while (cacheIter.hasNext()) {
			data.add(mapToObjectArray(cacheIter.next(), theFieldNames));
		}
		return listToControlData(data);
	}

	//This method assumes that the Primary Keys are integers and that the first id=1.  
	//It will look for id's sequentially until one is skipped, then will stop.
	protected List<ControlData> extractDataByKeyLookup(DIHCache cache, String[] theFieldNames) {
		int recId = 1;
		List<Object[]> data = new ArrayList<Object[]>();
		while (true) {
			Iterator<Map<String, Object>> listORecs = cache.iterator(recId);
			if (listORecs == null) {
				break;
			}

			while(listORecs.hasNext()) {
				data.add(mapToObjectArray(listORecs.next(), theFieldNames));
			}
			recId++;
		}
		return listToControlData(data);
	}

	protected List<ControlData> listToControlData(List<Object[]> data) {
		List<ControlData> returnData = new ArrayList<ControlData>(data.size());
		for (int i = 0; i < data.size(); i++) {
			returnData.add(new ControlData(data.get(i)));
		}
		return returnData;
	}

	protected Object[] mapToObjectArray(Map<String, Object> rec, String[] theFieldNames) {
		Object[] oos = new Object[theFieldNames.length];
		for (int i = 0; i < theFieldNames.length; i++) {
			oos[i] = rec.get(theFieldNames[i]);
		}
		return oos;
	}

	protected void compareData(List<ControlData> theControl, List<ControlData> test) {
		// The test data should come back primarily in Key order and secondarily in insertion order.
		List<ControlData> control = new ArrayList<ControlData>(theControl);
		Collections.sort(control);

		StringBuilder errors = new StringBuilder();
		if (test.size() != control.size()) {
			errors.append("-Returned data has " + test.size() + " records.  expected: " + control.size() + "\n");
		}
		for (int i = 0; i < control.size() && i < test.size(); i++) {
			Object[] controlRec = control.get(i).data;
			Object[] testRec = test.get(i).data;
			if (testRec.length != controlRec.length) {
				errors.append("-Record indexAt=" + i + " has " + testRec.length + " data elements.  extpected: " + controlRec.length + "\n");
			}
			for (int j = 0; j < controlRec.length && j < testRec.length; j++) {
				Object controlObj = controlRec[j];
				Object testObj = testRec[j];
				if (controlObj == null && testObj != null) {
					errors.append("-Record indexAt=" + i + ", Data Element indexAt=" + j + " is not NULL as expected.\n");
				} else if (controlObj != null && testObj == null) {
					errors.append("-Record indexAt=" + i + ", Data Element indexAt=" + j + " is NULL.  Expected: " + controlObj + " (class="
							+ controlObj.getClass().getName() + ")\n");
				} else if (controlObj != null && testObj != null && controlObj instanceof Clob) {
					String controlString = clobToString((Clob) controlObj);
					String testString = clobToString((Clob) testObj);
					if (!controlString.equals(testString)) {
						errors.append("-Record indexAt=" + i + ", Data Element indexAt=" + j + " has: " + testString + " (class=Clob) ... Expected: " + controlString
								+ " (class=Clob)\n");
					}
				} else if (controlObj != null && !controlObj.equals(testObj)) {
					errors.append("-Record indexAt=" + i + ", Data Element indexAt=" + j + " has: " + testObj + " (class=" + testObj.getClass().getName()
							+ ") ... Expected: " + controlObj + " (class=" + controlObj.getClass().getName() + ")\n");
				}
			}
		}
		if (errors.length() > 0) {
			Assert.fail(errors.toString());
		}
	}

	protected Map<String, Object> controlDataToMap(ControlData cd, String[] theFieldNames, boolean keepOrdered) {
		Map<String, Object> rec = null;
		if (keepOrdered) {
			rec = new LinkedHashMap<String, Object>();
		} else {
			rec = new HashMap<String, Object>();
		}
		for (int i = 0; i < cd.data.length; i++) {
			String fieldName = theFieldNames[i];
			Object data = cd.data[i];
			rec.put(fieldName, data);
		}
		return rec;
	}

	protected String stringArrayToCommaDelimitedList(String[] strs) {
		StringBuilder sb = new StringBuilder();
		for (String a : strs) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(a);
		}
		return sb.toString();
	}

	protected String clobToString(Clob cl) {
		StringBuilder sb = new StringBuilder();
		try {
			Reader in = cl.getCharacterStream();
			char[] cbuf = new char[1024];
			int numGot = -1;
			while ((numGot = in.read(cbuf)) != -1) {
				sb.append(String.valueOf(cbuf, 0, numGot));
			}
		} catch (Exception e) {
			Assert.fail(e.toString());
		}
		return sb.toString();
	}
	
	public static Context getContext(final Map<String, String> entityAttrs) {
		VariableResolverImpl resolver = new VariableResolverImpl();
    final Context delegate = new ContextImpl(null, resolver, null, null, new HashMap<String, Object>(), null, null);
    return new TestContext(entityAttrs, delegate, null, true);
  }
	
}
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestCachedSqlEntityProcessor.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestCachedSqlEntityProcessor.java
index 89c79a64a5a..e945a15eb32 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestCachedSqlEntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestCachedSqlEntityProcessor.java
@@ -190,7 +190,7 @@ public class TestCachedSqlEntityProcessor extends AbstractDataImportHandlerTestC
     fields.add(createMap("column", "desc"));
     String q = "select * from x";
     Map<String, String> entityAttrs = createMap(
            "query", q, EntityProcessorBase.CACHE_KEY,"id", EntityProcessorBase.CACHE_LOOKUP ,"x.id");
        "query", q, DIHCacheSupport.CACHE_PRIMARY_KEY,"id", DIHCacheSupport.CACHE_FOREIGN_KEY ,"x.id");
     MockDataSource ds = new MockDataSource();
     VariableResolverImpl vr = new VariableResolverImpl();
     Map xNamespace = createMap("id", 0);
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestEphemeralCache.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestEphemeralCache.java
new file mode 100644
index 00000000000..9645236e338
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestEphemeralCache.java
@@ -0,0 +1,68 @@
package org.apache.solr.handler.dataimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;

public class TestEphemeralCache extends AbstractDataImportHandlerTestCase {

	@BeforeClass
	public static void beforeClass() throws Exception {
		initCore("dataimport-solrconfig.xml", "dataimport-schema.xml");
	}

	public void testEphemeralCache() throws Exception {
		List parentRows = new ArrayList();
		parentRows.add(createMap("id", new BigDecimal("1"), "parent_s", "one"));
		parentRows.add(createMap("id", new BigDecimal("2"), "parent_s", "two"));
		parentRows.add(createMap("id", new BigDecimal("3"), "parent_s", "three"));
		parentRows.add(createMap("id", new BigDecimal("4"), "parent_s", "four"));
		parentRows.add(createMap("id", new BigDecimal("5"), "parent_s", "five"));

		List child1Rows = new ArrayList();
    child1Rows.add(createMap("id", new BigDecimal("6"), "child1a_mult_s", "this is the number six."));
    child1Rows.add(createMap("id", new BigDecimal("5"), "child1a_mult_s", "this is the number five."));
    child1Rows.add(createMap("id", new BigDecimal("6"), "child1a_mult_s", "let's sing a song of six."));
    child1Rows.add(createMap("id", new BigDecimal("3"), "child1a_mult_s", "three"));
    child1Rows.add(createMap("id", new BigDecimal("3"), "child1a_mult_s", "III"));
    child1Rows.add(createMap("id", new BigDecimal("3"), "child1a_mult_s", "3"));
    child1Rows.add(createMap("id", new BigDecimal("3"), "child1a_mult_s", "|||"));
    child1Rows.add(createMap("id", new BigDecimal("1"), "child1a_mult_s", "one"));
    child1Rows.add(createMap("id", new BigDecimal("1"), "child1a_mult_s", "uno"));
    child1Rows.add(createMap("id", new BigDecimal("2"), "child1b_s", "CHILD1B", "child1a_mult_s", "this is the number two."));

    List child2Rows = new ArrayList();
    child2Rows.add(createMap("id", new BigDecimal("6"), "child2a_mult_s", "Child 2 says, 'this is the number six.'"));
    child2Rows.add(createMap("id", new BigDecimal("5"), "child2a_mult_s", "Child 2 says, 'this is the number five.'"));
    child2Rows.add(createMap("id", new BigDecimal("6"), "child2a_mult_s", "Child 2 says, 'let's sing a song of six.'"));
    child2Rows.add(createMap("id", new BigDecimal("3"), "child2a_mult_s", "Child 2 says, 'three'"));
    child2Rows.add(createMap("id", new BigDecimal("3"), "child2a_mult_s", "Child 2 says, 'III'"));
    child2Rows.add(createMap("id", new BigDecimal("3"), "child2b_s", "CHILD2B", "child2a_mult_s", "Child 2 says, '3'"));
    child2Rows.add(createMap("id", new BigDecimal("3"), "child2a_mult_s", "Child 2 says, '|||'"));
    child2Rows.add(createMap("id", new BigDecimal("1"), "child2a_mult_s", "Child 2 says, 'one'"));
    child2Rows.add(createMap("id", new BigDecimal("1"), "child2a_mult_s", "Child 2 says, 'uno'"));
    child2Rows.add(createMap("id", new BigDecimal("2"), "child2a_mult_s", "Child 2 says, 'this is the number two.'"));

    MockDataSource.setIterator("SELECT * FROM PARENT", parentRows.iterator());
    MockDataSource.setIterator("SELECT * FROM CHILD_1", child1Rows.iterator());
    MockDataSource.setIterator("SELECT * FROM CHILD_2", child2Rows.iterator());

    runFullImport(loadDataConfig("dataimport-cache-ephemeral.xml"));

    assertQ(req("*:*"),                                       "//*[@numFound='5']");
    assertQ(req("id:1"),                                      "//*[@numFound='1']");
    assertQ(req("id:6"),                                      "//*[@numFound='0']");
    assertQ(req("parent_s:four"),                             "//*[@numFound='1']");
    assertQ(req("child1a_mult_s:this\\ is\\ the\\ numbe*"),   "//*[@numFound='2']");
    assertQ(req("child2a_mult_s:Child\\ 2\\ say*"),           "//*[@numFound='4']");
    assertQ(req("child1b_s:CHILD1B"),                         "//*[@numFound='1']");
    assertQ(req("child2b_s:CHILD2B"),                         "//*[@numFound='1']");
    assertQ(req("child1a_mult_s:one"),                        "//*[@numFound='1']");
    assertQ(req("child1a_mult_s:uno"),                        "//*[@numFound='1']");
    assertQ(req("child1a_mult_s:(uno OR one)"),               "//*[@numFound='1']");

	}

}
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSortedMapBackedCache.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSortedMapBackedCache.java
new file mode 100644
index 00000000000..5f21663bf42
-- /dev/null
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSortedMapBackedCache.java
@@ -0,0 +1,142 @@
package org.apache.solr.handler.dataimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSortedMapBackedCache extends AbstractDIHCacheTestCase {
	
	public static Logger log = LoggerFactory.getLogger(TestSortedMapBackedCache.class);
	
	@Test
	public void testCacheWithKeyLookup() {
		DIHCache cache = null;
		try {
			cache = new SortedMapBackedCache();
			cache.open(getContext(new HashMap<String,String>()));
			loadData(cache, data, fieldNames, true);
			List<ControlData> testData = extractDataByKeyLookup(cache, fieldNames);
			compareData(data, testData);
		} catch (Exception e) {
			log.warn("Exception thrown: " + e.toString());
			Assert.fail();
		} finally {
			try {
				cache.destroy();
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testCacheWithOrderedLookup() {
		DIHCache cache = null;
		try {
			cache = new SortedMapBackedCache();
			cache.open(getContext(new HashMap<String,String>()));
			loadData(cache, data, fieldNames, true);
			List<ControlData> testData = extractDataInKeyOrder(cache, fieldNames);
			compareData(data, testData);
		} catch (Exception e) {
			log.warn("Exception thrown: " + e.toString());
			Assert.fail();
		} finally {
			try {
				cache.destroy();
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testCacheReopensWithUpdate() {
		DIHCache cache = null;
		try {			
			Map<String, String> cacheProps = new HashMap<String, String>();
			cacheProps.put(DIHCacheSupport.CACHE_PRIMARY_KEY, "a_id");
			
			cache = new SortedMapBackedCache();
			cache.open(getContext(cacheProps));
			// We can let the data hit the cache with the fields out of order because
			// we've identified the pk up-front.
			loadData(cache, data, fieldNames, false);

			// Close the cache.
			cache.close();

			List<ControlData> newControlData = new ArrayList<ControlData>();
			Object[] newIdEqualsThree = null;
			int j = 0;
			for (int i = 0; i < data.size(); i++) {
				// We'll be deleting a_id=1 so remove it from the control data.
				if (data.get(i).data[0].equals(new Integer(1))) {
					continue;
				}

				// We'll be changing "Cookie" to "Carrot" in a_id=3 so change it in the control data.
				if (data.get(i).data[0].equals(new Integer(3))) {
					newIdEqualsThree = new Object[data.get(i).data.length];
					System.arraycopy(data.get(i).data, 0, newIdEqualsThree, 0, newIdEqualsThree.length);
					newIdEqualsThree[3] = "Carrot";
					newControlData.add(new ControlData(newIdEqualsThree));
				}
				// Everything else can just be copied over.
				else {
					newControlData.add(data.get(i));
				}

				j++;
			}

			// These new rows of data will get added to the cache, so add them to the control data too.
			Object[] newDataRow1 = new Object[] { new Integer(99), new BigDecimal(Math.PI), "Z", "Zebra", new Float(99.99), Feb21_2011, null };
			Object[] newDataRow2 = new Object[] { new Integer(2), new BigDecimal(Math.PI), "B", "Ballerina", new Float(2.22), Feb21_2011, null };

			newControlData.add(new ControlData(newDataRow1));
			newControlData.add(new ControlData(newDataRow2));

			// Re-open the cache
			cache.open(getContext(new HashMap<String,String>()));

			// Delete a_id=1 from the cache.
			cache.delete(new Integer(1));

			// Because the cache allows duplicates, the only way to update is to
			// delete first then add.
			cache.delete(new Integer(3));
			cache.add(controlDataToMap(new ControlData(newIdEqualsThree), fieldNames, false));

			// Add this row with a new Primary key.
			cache.add(controlDataToMap(new ControlData(newDataRow1), fieldNames, false));

			// Add this row, creating two records in the cache with a_id=2.
			cache.add(controlDataToMap(new ControlData(newDataRow2), fieldNames, false));

			// Read the cache back and compare to the newControlData
			List<ControlData> testData = extractDataInKeyOrder(cache, fieldNames);
			compareData(newControlData, testData);

			// Now try reading the cache read-only.
			cache.close();
			cache.open(getContext(new HashMap<String,String>()));
			testData = extractDataInKeyOrder(cache, fieldNames);
			compareData(newControlData, testData);

		} catch (Exception e) {
			log.warn("Exception thrown: " + e.toString());
			Assert.fail();
		} finally {
			try {
				cache.destroy();
			} catch (Exception ex) {
			}
		}
	}
}
- 
2.19.1.windows.1

