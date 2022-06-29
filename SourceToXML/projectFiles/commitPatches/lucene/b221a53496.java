From b221a5349610150ffbbea6d6c2493c7f32ef8834 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sat, 7 Mar 2015 05:55:35 +0000
Subject: [PATCH] SOLR-7073: Add an API to add a jar to a collection's
 classpath

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1664797 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   5 +
 .../apache/solr/cloud/RecoveryStrategy.java   |   4 -
 .../org/apache/solr/core/CoreContainer.java   |   5 +-
 ...ginsRegistry.java => ImplicitPlugins.java} |   3 +-
 .../org/apache/solr/core/JarRepository.java   |  65 +-
 .../org/apache/solr/core/MemClassLoader.java  | 180 +++++
 .../org/apache/solr/core/PluginRegistry.java  | 371 +++++++++++
 .../org/apache/solr/core/RequestHandlers.java | 442 +------------
 .../java/org/apache/solr/core/SolrConfig.java |  30 +-
 .../java/org/apache/solr/core/SolrCore.java   | 617 ++++++------------
 .../solr/handler/RequestHandlerBase.java      |   8 +-
 .../solr/handler/SolrConfigHandler.java       |  21 +-
 .../solr/handler/admin/AdminHandlers.java     |  16 +-
 .../handler/component/SearchComponent.java    |  20 +
 .../org/apache/solr/search/QParserPlugin.java |  65 +-
 .../apache/solr/util/CommandOperation.java    |   4 +-
 .../org/apache/solr/OutputWriterTest.java     |  14 +-
 .../core/BlobStoreTestRequestHandler.java     |  32 +-
 .../core/BlobStoreTestRequestHandlerV2.java   |  68 --
 .../apache/solr/core/RequestHandlersTest.java |   5 +-
 .../apache/solr/core/TestDynamicLoading.java  | 218 +++++--
 .../apache/solr/handler/TestBlobHandler.java  |  15 +-
 .../component/SpellCheckComponentTest.java    |   6 +-
 .../apache/solr/search/QueryEqualityTest.java |   6 +-
 .../solr/search/TestStandardQParsers.java     |  37 +-
 25 files changed, 1139 insertions(+), 1118 deletions(-)
 rename solr/core/src/java/org/apache/solr/core/{PluginsRegistry.java => ImplicitPlugins.java} (98%)
 create mode 100644 solr/core/src/java/org/apache/solr/core/MemClassLoader.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/PluginRegistry.java
 delete mode 100644 solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandlerV2.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index ed6bc9077ef..4eb1c8d1e1c 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -84,6 +84,9 @@ Upgrading from Solr 5.0
 * The signature of SolrDispatchFilter.createCoreContainer() has changed to take
   (String,Properties) arguments
 
* Deprecated the 'lib' option added to create-requesthandler as part of SOLR-6801 in 5.0 release.
  Please use the add-runtimelib command

 Detailed Change List
 ----------------------
 
@@ -136,6 +139,8 @@ New Features
 * SOLR-7155: All SolrClient methods now take an optional 'collection' argument
   (Alan Woodward)
 
* SOLR-7073: Support adding a jar to a collections classpath (Noble Paul)

 Bug Fixes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java b/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java
index 493e35b8c62..7e6bdf668ab 100644
-- a/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java
++ b/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java
@@ -39,7 +39,6 @@ import org.apache.solr.common.params.UpdateParams;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.CoreDescriptor;
 import org.apache.solr.core.DirectoryFactory.DirContext;
import org.apache.solr.core.RequestHandlers.LazyRequestHandlerWrapper;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.handler.ReplicationHandler;
 import org.apache.solr.request.LocalSolrQueryRequest;
@@ -146,9 +145,6 @@ public class RecoveryStrategy extends Thread implements ClosableThread {
     
     // use rep handler directly, so we can do this sync rather than async
     SolrRequestHandler handler = core.getRequestHandler(REPLICATION_HANDLER);
    if (handler instanceof LazyRequestHandlerWrapper) {
      handler = ((LazyRequestHandlerWrapper) handler).getWrappedHandler();
    }
     ReplicationHandler replicationHandler = (ReplicationHandler) handler;
     
     if (replicationHandler == null) {
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index 28ef085dea3..580849ae478 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -41,7 +41,6 @@ import org.slf4j.LoggerFactory;
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Collection;
import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
@@ -108,13 +107,13 @@ public class CoreContainer {
   public static final String COLLECTIONS_HANDLER_PATH = "/admin/collections";
   public static final String INFO_HANDLER_PATH = "/admin/info";
 
  private Map<String, SolrRequestHandler> containerHandlers = new HashMap<>();
  private PluginRegistry<SolrRequestHandler> containerHandlers = new PluginRegistry<>(SolrRequestHandler.class, null);
 
   public SolrRequestHandler getRequestHandler(String path) {
     return RequestHandlerBase.getRequestHandler(path, containerHandlers);
   }
 
  public Map<String, SolrRequestHandler> getRequestHandlers(){
  public PluginRegistry<SolrRequestHandler> getRequestHandlers() {
     return this.containerHandlers;
   }
 
diff --git a/solr/core/src/java/org/apache/solr/core/PluginsRegistry.java b/solr/core/src/java/org/apache/solr/core/ImplicitPlugins.java
similarity index 98%
rename from solr/core/src/java/org/apache/solr/core/PluginsRegistry.java
rename to solr/core/src/java/org/apache/solr/core/ImplicitPlugins.java
index 0b593bf05eb..27009923771 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginsRegistry.java
++ b/solr/core/src/java/org/apache/solr/core/ImplicitPlugins.java
@@ -45,7 +45,7 @@ import static org.apache.solr.common.cloud.ZkNodeProps.makeMap;
 import static org.apache.solr.core.PluginInfo.DEFAULTS;
 import static org.apache.solr.core.PluginInfo.INVARIANTS;
 
public class PluginsRegistry {
public class ImplicitPlugins {
 
   public static List<PluginInfo> getHandlers(SolrCore solrCore){
     List<PluginInfo> implicits = new ArrayList<>();
@@ -88,4 +88,5 @@ public class PluginsRegistry {
     Map m = makeMap("name", name, "class", clz.getName());
     return new PluginInfo(SolrRequestHandler.TYPE, m, new NamedList<>(singletonMap(DEFAULTS, new NamedList(defaults))),null);
   }
  public static final String IMPLICIT = "implicit";
 }
diff --git a/solr/core/src/java/org/apache/solr/core/JarRepository.java b/solr/core/src/java/org/apache/solr/core/JarRepository.java
index 8d0ccba9a05..dee1afadd83 100644
-- a/solr/core/src/java/org/apache/solr/core/JarRepository.java
++ b/solr/core/src/java/org/apache/solr/core/JarRepository.java
@@ -24,7 +24,6 @@ import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.ArrayList;
import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
@@ -43,7 +42,6 @@ import org.apache.solr.common.cloud.ClusterState;
 import org.apache.solr.common.cloud.DocCollection;
 import org.apache.solr.common.cloud.Replica;
 import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkCoreNodeProps;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.handler.admin.CollectionsHandler;
 import org.apache.solr.util.SimplePostTool;
@@ -76,37 +74,16 @@ public class JarRepository {
   }
 
   /**
   * Returns the contents of a jar and increments a reference count. Please return the same object to decerease the refcount
   * Returns the contents of a jar and increments a reference count. Please return the same object to decrease the refcount
    *
    * @param key it is a combination of blobname and version like blobName/version
    * @return The reference of a jar
    */
  public JarContentRef getJarIncRef(String key) throws IOException {
  public JarContentRef getJarIncRef(String key) {
     JarContent jar = jars.get(key);
     if (jar == null) {
       if (this.coreContainer.isZooKeeperAware()) {
        ZkStateReader zkStateReader = this.coreContainer.getZkController().getZkStateReader();
        ClusterState cs = zkStateReader.getClusterState();
        DocCollection coll = cs.getCollectionOrNull(CollectionsHandler.SYSTEM_COLL);
        if (coll == null) throw new SolrException(SERVICE_UNAVAILABLE, ".system collection not available");
        ArrayList<Slice> slices = new ArrayList<>(coll.getActiveSlices());
        if (slices.isEmpty()) throw new SolrException(SERVICE_UNAVAILABLE, "No active slices for .system collection");
        Collections.shuffle(slices, RANDOM); //do load balancing

        Replica replica = null;
        for (Slice slice : slices)  {
          List<Replica> replicas = new ArrayList<>(slice.getReplicasMap().values());
          Collections.shuffle(replicas, RANDOM);
          for (Replica r : replicas) {
            if (ZkStateReader.ACTIVE.equals(r.getStr(ZkStateReader.STATE_PROP))) {
              replica = r;
              break;
            }
          }
        }
        if (replica == null) {
          throw new SolrException(SERVICE_UNAVAILABLE, ".no active replica available for .system collection");
        }
        Replica replica = getSystemCollReplica();
         String url = replica.getStr(BASE_URL_PROP) + "/.system/blob/" + key + "?wt=filestream";
 
         HttpClient httpClient = coreContainer.getUpdateShardHandler().getHttpClient();
@@ -119,6 +96,12 @@ public class JarRepository {
             throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "no such blob or version available: " + key);
           }
           b = SimplePostTool.inputStreamToByteArray(entity.getEntity().getContent());
        } catch (Exception e) {
          if (e instanceof SolrException) {
            throw (SolrException) e;
          } else {
            throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "could not load : " + key, e);
          }
         } finally {
           httpGet.releaseConnection();
         }
@@ -138,6 +121,36 @@ public class JarRepository {
 
   }
 
  private Replica getSystemCollReplica() {
    ZkStateReader zkStateReader = this.coreContainer.getZkController().getZkStateReader();
    ClusterState cs = zkStateReader.getClusterState();
    DocCollection coll = cs.getCollectionOrNull(CollectionsHandler.SYSTEM_COLL);
    if (coll == null) throw new SolrException(SERVICE_UNAVAILABLE, ".system collection not available");
    ArrayList<Slice> slices = new ArrayList<>(coll.getActiveSlices());
    if (slices.isEmpty()) throw new SolrException(SERVICE_UNAVAILABLE, "No active slices for .system collection");
    Collections.shuffle(slices, RANDOM); //do load balancing

    Replica replica = null;
    for (Slice slice : slices) {
      List<Replica> replicas = new ArrayList<>(slice.getReplicasMap().values());
      Collections.shuffle(replicas, RANDOM);
      for (Replica r : replicas) {
        if (ZkStateReader.ACTIVE.equals(r.getStr(ZkStateReader.STATE_PROP))) {
          if(zkStateReader.getClusterState().getLiveNodes().contains(r.get(ZkStateReader.NODE_NAME_PROP))){
            replica = r;
            break;
          } else {
            log.info("replica {} says it is active but not a member of live nodes", r.get(ZkStateReader.NODE_NAME_PROP));
          }
        }
      }
    }
    if (replica == null) {
      throw new SolrException(SERVICE_UNAVAILABLE, ".no active replica available for .system collection");
    }
    return replica;
  }

   /**
    * This is to decrement a ref count
    *
diff --git a/solr/core/src/java/org/apache/solr/core/MemClassLoader.java b/solr/core/src/java/org/apache/solr/core/MemClassLoader.java
new file mode 100644
index 00000000000..6c28dafeeb4
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/MemClassLoader.java
@@ -0,0 +1,180 @@
package org.apache.solr.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MemClassLoader extends ClassLoader implements AutoCloseable, ResourceLoader {
  static final Logger log =  LoggerFactory.getLogger(MemClassLoader.class);
  private boolean allJarsLoaded = false;
  private final SolrResourceLoader parentLoader;
  private List<PluginRegistry.RuntimeLib> libs = new ArrayList<>();
  private Map<String, Class> classCache = new HashMap<>();


  public MemClassLoader(List<PluginRegistry.RuntimeLib> libs, SolrResourceLoader resourceLoader) {
    this.parentLoader = resourceLoader;
    this.libs = libs;
  }


  public synchronized void loadJars() {
    if (allJarsLoaded) return;

    for (PluginRegistry.RuntimeLib lib : libs) {
      try {
        lib.loadJar();
      } catch (Exception exception) {
        if (exception instanceof SolrException) throw (SolrException) exception;
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Atleast one runtimeLib could not be loaded", exception);
      }
    }
    allJarsLoaded = true;
  }


  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if(!allJarsLoaded ) loadJars();
    try {
      return parentLoader.findClass(name, Object.class);
    } catch (Exception e) {
      return loadFromRuntimeLibs(name);
    }
  }

  private synchronized  Class<?> loadFromRuntimeLibs(String name) throws ClassNotFoundException {
    Class result = classCache.get(name);
    if(result != null)
      return result;
    AtomicReference<String> jarName = new AtomicReference<>();
    ByteBuffer buf = null;
    try {
      buf = getByteBuffer(name, jarName);
    } catch (Exception e) {
      throw new ClassNotFoundException("class could not be loaded " + name, e);
    }
    if (buf == null) throw new ClassNotFoundException("Class not found :" + name);
    ProtectionDomain defaultDomain = null;
    //using the default protection domain, with no permissions
    try {
      defaultDomain = new ProtectionDomain(new CodeSource(new URL("http://localhost/.system/blob/" + jarName.get()), (Certificate[]) null),
          null);
    } catch (MalformedURLException mue) {
      throw new ClassNotFoundException("Unexpected exception ", mue);
      //should not happen
    }
    log.info("Defining_class {} from runtime jar {} ", name, jarName);

    result = defineClass(name, buf.array(), buf.arrayOffset(), buf.limit(), defaultDomain);
    classCache.put(name, result);
    return result;
  }

  private ByteBuffer getByteBuffer(String name, AtomicReference<String> jarName) throws Exception {
    if (!allJarsLoaded) {
      loadJars();

    }

    String path = name.replace('.', '/').concat(".class");
    ByteBuffer buf = null;
    for (PluginRegistry.RuntimeLib lib : libs) {
      try {
        buf = lib.getFileContent(path);
        if (buf != null) {
          jarName.set(lib.name);
          break;
        }
      } catch (Exception exp) {
        throw new ClassNotFoundException("Unable to load class :" + name, exp);
      }
    }

    return buf;
  }

  @Override
  public void close() throws Exception {
    for (PluginRegistry.RuntimeLib lib : libs) {
      try {
        lib.close();
      } catch (Exception e) {
      }
    }
  }

  @Override
  public InputStream openResource(String resource) throws IOException {
    AtomicReference<String> jarName = new AtomicReference<>();
    try {
      ByteBuffer buf = getByteBuffer(resource, jarName);
      if (buf == null) throw new IOException("Resource could not be found " + resource);
    } catch (Exception e) {
      throw new IOException("Resource could not be found " + resource, e);
    }
    return null;
  }

  @Override
  public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) {
    if(!allJarsLoaded ) loadJars();
    try {
      return findClass(cname).asSubclass(expectedType);
    } catch (Exception e) {
      if (e instanceof SolrException) {
        throw (SolrException) e;
      } else {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "error loading class " + cname, e);
      }
    }

  }

  @Override
  public <T> T newInstance(String cname, Class<T> expectedType) {
    try {
      return findClass(cname, expectedType).newInstance();
    } catch (SolrException e) {
      throw e;
    } catch (Exception e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "error instantiating class :" + cname, e);
    }
  }


}
diff --git a/solr/core/src/java/org/apache/solr/core/PluginRegistry.java b/solr/core/src/java/org/apache/solr/core/PluginRegistry.java
new file mode 100644
index 00000000000..2e33c18b690
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/PluginRegistry.java
@@ -0,0 +1,371 @@
package org.apache.solr.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.SolrException;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;

/**
 * This manages the lifecycle of a set of plugin of the same type .
 */
public class PluginRegistry<T> implements AutoCloseable {
  public static Logger log = LoggerFactory.getLogger(PluginRegistry.class);

  private Map<String, PluginHolder<T>> registry = new HashMap<>();
  private Map<String, PluginHolder<T>> immutableRegistry = Collections.unmodifiableMap(registry);
  private String def;
  private Class klass;
  private SolrCore core;
  private SolrConfig.SolrPluginInfo meta;

  public PluginRegistry(Class<T> klass, SolrCore core) {
    this.core = core;
    this.klass = klass;
    meta = SolrConfig.classVsSolrPluginInfo.get(klass.getName());
    if (meta == null) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unknown Plugin : " + klass.getName());
    }
  }

  static void initInstance(Object inst, PluginInfo info, SolrCore core) {
    if (inst instanceof PluginInfoInitialized) {
      ((PluginInfoInitialized) inst).init(info);
    } else if (inst instanceof NamedListInitializedPlugin) {
      ((NamedListInitializedPlugin) inst).init(info.initArgs);
    } else if (inst instanceof SolrRequestHandler) {
      ((SolrRequestHandler) inst).init(info.initArgs);
    }
    if (inst instanceof SearchComponent) {
      ((SearchComponent) inst).setName(info.name);
    }
    if (inst instanceof RequestHandlerBase) {
      ((RequestHandlerBase) inst).setPluginInfo(info);
    }

  }

  PluginHolder<T> createPlugin(PluginInfo info, SolrCore core) {
    if ("true".equals(String.valueOf(info.attributes.get("runtimeLib")))) {
      log.info(" {} : '{}'  created with runtimeLib=true ", meta.tag, info.name);
      return new LazyPluginHolder<>(meta, info, core, core.getMemClassLoader());
    } else if ("lazy".equals(info.attributes.get("startup")) && meta.options.contains(SolrConfig.PluginOpts.LAZY)) {
      log.info("{} : '{}' created with startup=lazy ", meta.tag, info.name);
      return new LazyPluginHolder<T>(meta, info, core, core.getResourceLoader());
    } else {
      T inst = core.createInstance(info.className, (Class<T>) meta.clazz, meta.tag, null, core.getResourceLoader());
      initInstance(inst, info, core);
      return new PluginHolder<>(info, inst);
    }
  }

  boolean alias(String src, String target) {
    PluginHolder<T> a = registry.get(src);
    if (a == null) return false;
    PluginHolder<T> b = registry.get(target);
    if (b != null) return false;
    registry.put(target, a);
    return true;
  }

  /**
   * Get a plugin by name. If the plugin is not already instantiated, it is
   * done here
   */
  public T get(String name) {
    PluginHolder<T> result = registry.get(name);
    return result == null ? null : result.get();
  }

  /**
   * Fetches a plugin by name , or the default
   *
   * @param name       name using which it is registered
   * @param useDefault Return the default , if a plugin by that name does not exist
   */
  public T get(String name, boolean useDefault) {
    T result = get(name);
    if (useDefault && result == null) return get(def);
    return result;
  }

  public Set<String> keySet() {
    return immutableRegistry.keySet();
  }

  /**
   * register a plugin by a name
   */
  public T put(String name, T plugin) {
    if (plugin == null) return null;
    PluginHolder<T> old = put(name, new PluginHolder<T>(null, plugin));
    return old == null ? null : old.get();
  }


  PluginHolder<T> put(String name, PluginHolder<T> plugin) {
    PluginHolder<T> old = registry.put(name, plugin);
    if (plugin.pluginInfo != null && plugin.pluginInfo.isDefault()) {
      setDefault(name);
    }
    if (plugin.isLoaded()) registerMBean(plugin.get(), core, name);
    return old;
  }

  void setDefault(String def) {
    if (!registry.containsKey(def)) return;
    if (this.def != null) log.warn("Multiple defaults for : " + meta.tag);
    this.def = def;
  }

  public Map<String, PluginHolder<T>> getRegistry() {
    return immutableRegistry;
  }

  public boolean contains(String name) {
    return registry.containsKey(name);
  }

  String getDefault() {
    return def;
  }

  T remove(String name) {
    PluginHolder<T> removed = registry.remove(name);
    return removed == null ? null : removed.get();
  }

  void init(Map<String, T> defaults, SolrCore solrCore) {
    init(defaults, solrCore, solrCore.getSolrConfig().getPluginInfos(klass.getName()));
  }

  /**
   * Initializes the plugins after reading the meta data from {@link org.apache.solr.core.SolrConfig}.
   *
   * @param defaults These will be registered if not explicitly specified
   */
  void init(Map<String, T> defaults, SolrCore solrCore, List<PluginInfo> infos) {
    core = solrCore;
    for (PluginInfo info : infos) {
      PluginHolder<T> o = createPlugin(info, solrCore);
      String name = info.name;
      if (meta.clazz.equals(SolrRequestHandler.class)) name = RequestHandlers.normalize(info.name);
      PluginHolder<T> old = put(name, o);
      if (old != null) log.warn("Multiple entries of {} with name {}", meta.tag, name);
    }
    for (Map.Entry<String, T> e : defaults.entrySet()) {
      if (!contains(e.getKey())) {
        put(e.getKey(), new PluginHolder<T>(null, e.getValue()));
      }
    }
  }

  /**
   * To check if a plugin by a specified name is already loaded
   */
  public boolean isLoaded(String name) {
    PluginHolder<T> result = registry.get(name);
    if (result == null) return false;
    return result.isLoaded();
  }

  private static void registerMBean(Object inst, SolrCore core, String pluginKey) {
    if (core == null) return;
    if (inst instanceof SolrInfoMBean) {
      SolrInfoMBean mBean = (SolrInfoMBean) inst;
      String name = (inst instanceof SolrRequestHandler) ? pluginKey : mBean.getName();
      core.registerInfoBean(name, mBean);
    }
  }


  /**
   * Close this registry. This will in turn call a close on all the contained plugins
   */
  @Override
  public void close() {
    for (Map.Entry<String, PluginHolder<T>> e : registry.entrySet()) {
      try {
        e.getValue().close();
      } catch (Exception exp) {
        log.error("Error closing plugin " + e.getKey() + " of type : " + meta.tag, exp);
      }
    }
  }

  /**
   * An indirect reference to a plugin. It just wraps a plugin instance.
   * subclasses may choose to lazily load the plugin
   */
  public static class PluginHolder<T> implements AutoCloseable {
    protected T inst;
    protected final PluginInfo pluginInfo;

    public PluginHolder(PluginInfo info) {
      this.pluginInfo = info;
    }

    public PluginHolder(PluginInfo info, T inst) {
      this.inst = inst;
      this.pluginInfo = info;
    }

    public T get() {
      return inst;
    }

    public boolean isLoaded() {
      return inst != null;
    }

    @Override
    public void close() throws Exception {
      if (inst != null && inst instanceof AutoCloseable) ((AutoCloseable) inst).close();

    }
  }

  /**
   * A class that loads plugins Lazily. When the get() method is invoked
   * the Plugin is initialized and returned.
   */
  public static class LazyPluginHolder<T> extends PluginHolder<T> {
    private final SolrConfig.SolrPluginInfo pluginMeta;
    protected SolrException solrException;
    private final SolrCore core;
    protected ResourceLoader resourceLoader;


    LazyPluginHolder(SolrConfig.SolrPluginInfo pluginMeta, PluginInfo pluginInfo, SolrCore core, ResourceLoader loader) {
      super(pluginInfo);
      this.pluginMeta = pluginMeta;
      this.core = core;
      this.resourceLoader = loader;
    }

    @Override
    public T get() {
      if (inst != null) return inst;
      if (solrException != null) throw solrException;
      createInst();
      registerMBean(inst, core, pluginInfo.name);
      return inst;
    }

    protected synchronized void createInst() {
      if (inst != null) return;
      log.info("Going to create a new {} with {} ", pluginMeta.tag, pluginInfo.toString());
      if (resourceLoader instanceof MemClassLoader) {
        MemClassLoader loader = (MemClassLoader) resourceLoader;
        loader.loadJars();
      }
      Class<T> clazz = (Class<T>) pluginMeta.clazz;
      inst = core.createInstance(pluginInfo.className, clazz, pluginMeta.tag, null, resourceLoader);
      initInstance(inst, pluginInfo, core);
      if (inst instanceof SolrCoreAware) {
        SolrResourceLoader.assertAwareCompatibility(SolrCoreAware.class, inst);
        ((SolrCoreAware) inst).inform(core);
      }
      if (inst instanceof ResourceLoaderAware) {
        SolrResourceLoader.assertAwareCompatibility(ResourceLoaderAware.class, inst);
        try {
          ((ResourceLoaderAware) inst).inform(core.getResourceLoader());
        } catch (IOException e) {
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "error initializing component", e);
        }
      }
    }


  }

  /**
   * This represents a Runtime Jar. A jar requires two details , name and version
   */
  public static class RuntimeLib implements PluginInfoInitialized, AutoCloseable {
    String name;
    String version;
    private JarRepository.JarContentRef jarContent;
    private final JarRepository jarRepository;

    @Override
    public void init(PluginInfo info) {
      name = info.attributes.get("name");
      Object v = info.attributes.get("version");
      if (name == null || v == null) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "runtimeLib must have name and version");
      }
      version = String.valueOf(v);
    }

    public RuntimeLib(SolrCore core) {
      jarRepository = core.getCoreDescriptor().getCoreContainer().getJarRepository();
    }


    void loadJar() {
      if (jarContent != null) return;
      synchronized (this) {
        if (jarContent != null) return;
        jarContent = jarRepository.getJarIncRef(name + "/" + version);
      }
    }

    public ByteBuffer getFileContent(String entryName) throws IOException {
      if (jarContent == null)
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "jar not available: " + name + "/" + version);
      return jarContent.jar.getFileContent(entryName);

    }

    @Override
    public void close() throws Exception {
      if (jarContent != null) jarRepository.decrementJarRefCount(jarContent);
    }

    public static List<RuntimeLib> getLibObjects(SolrCore core, List<PluginInfo> libs) {
      List<RuntimeLib> l = new ArrayList<>(libs.size());
      for (PluginInfo lib : libs) {
        RuntimeLib rtl = new RuntimeLib(core);
        rtl.init(lib);
        l.add(rtl);
      }
      return l;
    }
  }
}
diff --git a/solr/core/src/java/org/apache/solr/core/RequestHandlers.java b/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
index ba295bc128f..5bd12093bb1 100644
-- a/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
++ b/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
@@ -17,41 +17,14 @@
 
 package org.apache.solr.core;
 
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.BasicPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
 
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.StrUtils;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
 import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -61,13 +34,8 @@ public final class RequestHandlers {
   public static Logger log = LoggerFactory.getLogger(RequestHandlers.class);
 
   protected final SolrCore core;
  // Use a synchronized map - since the handlers can be changed at runtime, 
  // the map implementation should be thread safe
  private final Map<String, SolrRequestHandler> handlers =
      new ConcurrentHashMap<>() ;
  private final Map<String, SolrRequestHandler> immutableHandlers = Collections.unmodifiableMap(handlers) ;
 
  public static final boolean disableExternalLib = Boolean.parseBoolean(System.getProperty("disable.external.lib", "false"));
  final PluginRegistry<SolrRequestHandler> handlers;
 
   /**
    * Trim the trailing '/' if it's there, and convert null to empty string.
@@ -89,6 +57,7 @@ public final class RequestHandlers {
   
   public RequestHandlers(SolrCore core) {
       this.core = core;
    handlers =  new PluginRegistry<>(SolrRequestHandler.class, core);
   }
 
   /**
@@ -98,17 +67,6 @@ public final class RequestHandlers {
     return handlers.get(normalize(handlerName));
   }
 
  /**
   * @return a Map of all registered handlers of the specified type.
   */
  public <T extends SolrRequestHandler> Map<String,T> getAll(Class<T> clazz) {
    Map<String,T> result = new HashMap<>(7);
    for (Map.Entry<String,SolrRequestHandler> e : handlers.entrySet()) {
      if(clazz.isInstance(e.getValue())) result.put(e.getKey(), clazz.cast(e.getValue()));
    }
    return result;
  }

   /**
    * Handlers must be initialized before calling this function.  As soon as this is
    * called, the handler can immediately accept requests.
@@ -118,22 +76,20 @@ public final class RequestHandlers {
    * @return the previous handler at the given path or null
    */
   public SolrRequestHandler register( String handlerName, SolrRequestHandler handler ) {
    String norm = normalize( handlerName );
    String norm = normalize(handlerName);
     if (handler == null) {
       return handlers.remove(norm);
     }
    SolrRequestHandler old = handlers.put(norm, handler);
    if (0 != norm.length() && handler instanceof SolrInfoMBean) {
      core.getInfoRegistry().put(handlerName, handler);
    }
    return old;
    return handlers.put(norm, handler);
//    return register(handlerName, new PluginRegistry.PluginHolder<>(null, handler));
   }
 

   /**
    * Returns an unmodifiable Map containing the registered handlers
    */
  public Map<String,SolrRequestHandler> getRequestHandlers() {
    return immutableHandlers;
  public PluginRegistry<SolrRequestHandler> getRequestHandlers() {
    return handlers;
   }
 
 
@@ -157,66 +113,28 @@ public final class RequestHandlers {
    * Handlers will be registered and initialized in the order they appear in solrconfig.xml
    */
 
  void initHandlersFromConfig(SolrConfig config){
    List<PluginInfo> implicits = PluginsRegistry.getHandlers(core);
  void initHandlersFromConfig(SolrConfig config) {
    List<PluginInfo> implicits = ImplicitPlugins.getHandlers(core);
     // use link map so we iterate in the same order
    Map<PluginInfo,SolrRequestHandler> handlers = new LinkedHashMap<>();
     Map<String, PluginInfo> infoMap= new LinkedHashMap<>();
     //deduping implicit and explicit requesthandlers
     for (PluginInfo info : implicits) infoMap.put(info.name,info);
     for (PluginInfo info : config.getPluginInfos(SolrRequestHandler.class.getName())) infoMap.put(info.name, info);
     ArrayList<PluginInfo> infos = new ArrayList<>(infoMap.values());

    List<PluginInfo> modifiedInfos = new ArrayList<>();
     for (PluginInfo info : infos) {
      try {
        SolrRequestHandler requestHandler;
        String startup = info.attributes.get("startup");
        String lib = info.attributes.get("lib");
        if (lib != null) {
          requestHandler = new DynamicLazyRequestHandlerWrapper(core);
        } else if (startup != null) {
          if ("lazy".equals(startup)) {
            log.info("adding lazy requestHandler: " + info.className);
            requestHandler = new LazyRequestHandlerWrapper(core);
          } else {
            throw new Exception("Unknown startup value: '" + startup + "' for: " + info.className);
          }
        } else {
          requestHandler = core.createRequestHandler(info.className);
        }
        if (requestHandler instanceof RequestHandlerBase) ((RequestHandlerBase) requestHandler).setPluginInfo(info);
        
        handlers.put(info, requestHandler);
        SolrRequestHandler old = register(info.name, requestHandler);
        if (old != null) {
          log.warn("Multiple requestHandler registered to the same name: " + info.name + " ignoring: " + old.getClass().getName());
        }
        if (info.isDefault()) {
          old = register("", requestHandler);
          if (old != null) log.warn("Multiple default requestHandler registered" + " ignoring: " + old.getClass().getName());
        }
        log.info("created " + info.name + ": " + info.className);
      } catch (Exception ex) {
          throw new SolrException
            (ErrorCode.SERVER_ERROR, "RequestHandler init failure", ex);
      }
      modifiedInfos.add(applyInitParams(config, info));
     }

    // we've now registered all handlers, time to init them in the same order
    for (Map.Entry<PluginInfo,SolrRequestHandler> entry : handlers.entrySet()) {
      PluginInfo info = entry.getKey();
      SolrRequestHandler requestHandler = entry.getValue();
      info = applyInitParams(config, info);
      if (requestHandler instanceof PluginInfoInitialized) {
       ((PluginInfoInitialized) requestHandler).init(info);
      } else{
        requestHandler.init(info.initArgs);
    handlers.init(Collections.emptyMap(),core, modifiedInfos);
    handlers.alias(handlers.getDefault(), "");
    log.info("Registered paths: {}" , StrUtils.join(new ArrayList<>(handlers.keySet()) , ',' ));
    if(!handlers.alias( "/select","")){
      if(!handlers.alias( "standard","")){
        log.warn("no default request handler is registered (either '/select' or 'standard')");
       }
     }
 
    if(get("") == null) register("", get("/select"));//defacto default handler
    if(get("") == null) register("", get("standard"));//old default handler name; TODO remove?
    if(get("") == null)
      log.warn("no default request handler is registered (either '/select' or 'standard')");
   }
 
   private PluginInfo applyInitParams(SolrConfig config, PluginInfo info) {
@@ -239,328 +157,8 @@ public final class RequestHandlers {
     return info;
   }
 

  /**
   * The <code>LazyRequestHandlerWrapper</code> wraps any {@link SolrRequestHandler}.
   * Rather then instantiate and initialize the handler on startup, this wrapper waits
   * until it is actually called.  This should only be used for handlers that are
   * unlikely to be used in the normal lifecycle.
   *
   * You can enable lazy loading in solrconfig.xml using:
   *
   * <pre>
   *  &lt;requestHandler name="..." class="..." startup="lazy"&gt;
   *    ...
   *  &lt;/requestHandler&gt;
   * </pre>
   *
   * This is a private class - if there is a real need for it to be public, it could
   * move
   *
   * @since solr 1.2
   */
  public static class LazyRequestHandlerWrapper implements SolrRequestHandler, AutoCloseable, PluginInfoInitialized {
    private final SolrCore core;
    String _className;
    SolrRequestHandler _handler;
    PluginInfo _pluginInfo;

    public LazyRequestHandlerWrapper(SolrCore core) {
      this.core = core;
      _handler = null; // don't initialize
    }

    @Override
    public void init(NamedList args) {
    }

    /**
     * Wait for the first request before initializing the wrapped handler
     */
    @Override
    public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
      SolrRequestHandler handler = _handler;
      if (handler == null) {
        handler = getWrappedHandler();
      }
      handler.handleRequest(req, rsp);
    }

    public synchronized SolrRequestHandler getWrappedHandler() {
      if (_handler == null) {
        try {
          SolrRequestHandler handler = createRequestHandler();
          if (handler instanceof PluginInfoInitialized) {
            ((PluginInfoInitialized) handler).init(_pluginInfo);
          } else {
            handler.init(_pluginInfo.initArgs);
          }

          if (handler instanceof PluginInfoInitialized) {
            ((PluginInfoInitialized) handler).init(_pluginInfo);
          } else {
            handler.init(_pluginInfo.initArgs);
          }


          if (handler instanceof SolrCoreAware) {
            ((SolrCoreAware) handler).inform(core);
          }
          if (handler instanceof RequestHandlerBase) ((RequestHandlerBase) handler).setPluginInfo(_pluginInfo);
          _handler = handler;
        } catch (Exception ex) {
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "lazy loading error", ex);
        }
      }
      return _handler;
    }

    protected SolrRequestHandler createRequestHandler() {
      return core.createRequestHandler(_className);
    }

    public String getHandlerClass() {
      return _className;
    }

    //////////////////////// SolrInfoMBeans methods //////////////////////

    @Override
    public String getName() {
      return "Lazy[" + _className + "]";
    }

    @Override
    public String getDescription() {
      if (_handler == null) {
        return getName();
      }
      return _handler.getDescription();
    }

    @Override
    public String getVersion() {
      if (_handler != null) {
        return _handler.getVersion();
      }
      return null;
    }

    @Override
    public String getSource() {
      return null;
    }

    @Override
    public URL[] getDocs() {
      if (_handler == null) {
        return null;
      }
      return _handler.getDocs();
    }

    @Override
    public Category getCategory() {
      return Category.QUERYHANDLER;
    }

    @Override
    public NamedList getStatistics() {
      if (_handler != null) {
        return _handler.getStatistics();
      }
      NamedList<String> lst = new SimpleOrderedMap<>();
      lst.add("note", "not initialized yet");
      return lst;
    }

    @Override
    public void close() throws Exception {
      if (_handler == null) return;
      if (_handler instanceof AutoCloseable && !(_handler instanceof DynamicLazyRequestHandlerWrapper)) {
        ((AutoCloseable) _handler).close();
      }
    }

    @Override
    public void init(PluginInfo info) {
      _pluginInfo = info;
      _className = info.className;
    }
  }

  public static class DynamicLazyRequestHandlerWrapper extends LazyRequestHandlerWrapper {
    private String lib;
    private String key;
    private String version;
    private CoreContainer coreContainer;
    private SolrResourceLoader solrResourceLoader;
    private MemClassLoader classLoader;
    private boolean _closed = false;
    boolean unrecoverable = false;
    String errMsg = null;
    private Exception exception;


    public DynamicLazyRequestHandlerWrapper(SolrCore core) {
      super(core);
      this.coreContainer = core.getCoreDescriptor().getCoreContainer();
      this.solrResourceLoader = core.getResourceLoader();

    }

    @Override
    public void init(PluginInfo info) {
      super.init(info);
      this.lib = _pluginInfo.attributes.get("lib");

      if (disableExternalLib) {
        errMsg = "ERROR external library loading is disabled";
        unrecoverable = true;
        _handler = this;
        log.error(errMsg);
        return;
      }

      if (_pluginInfo.attributes.get("version") == null) {
        errMsg = "ERROR 'lib' attribute must be accompanied with version also";
        unrecoverable = true;
        _handler = this;
        log.error(errMsg);
        return;
      }
      version = String.valueOf(_pluginInfo.attributes.get("version"));
      classLoader = new MemClassLoader(this);
    }

    @Override
    public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
      if (unrecoverable) {
        rsp.add("error", errMsg);
        if (exception != null) rsp.setException(exception);
        return;
      }
      try {
        classLoader.checkJarAvailable();
      } catch (SolrException e) {
        rsp.add("error", "Jar could not be loaded");
        rsp.setException(e);
        return;
      } catch (IOException e) {
        unrecoverable = true;
        errMsg = "Could not load jar";
        exception = e;
        handleRequest(req, rsp);
        return;
      }

      super.handleRequest(req, rsp);
    }

    @Override
    protected SolrRequestHandler createRequestHandler() {
      try {
        Class clazz = classLoader.findClass(_className);
        Constructor<?>[] cons = clazz.getConstructors();
        for (Constructor<?> con : cons) {
          Class<?>[] types = con.getParameterTypes();
          if (types.length == 1 && types[0] == SolrCore.class) {
            return SolrRequestHandler.class.cast(con.newInstance(this));
          }
        }
        return (SolrRequestHandler) clazz.newInstance();
      } catch (Exception e) {
        unrecoverable = true;
        errMsg = MessageFormat.format("class {0} could not be loaded ", _className);
        this.exception = e;
        return this;

      }

    }

    @Override
    public void close() throws Exception {
      super.close();
      if (_closed) return;
      if (classLoader != null) classLoader.releaseJar();
      _closed = true;
    }
  }


  public static class MemClassLoader extends ClassLoader {
    private JarRepository.JarContentRef jarContent;
    private final DynamicLazyRequestHandlerWrapper handlerWrapper;

    public MemClassLoader(DynamicLazyRequestHandlerWrapper handlerWrapper) {
      super(handlerWrapper.solrResourceLoader.classLoader);
      this.handlerWrapper = handlerWrapper;

    }

    boolean checkJarAvailable() throws IOException {
      if (jarContent != null) return true;

      try {
        synchronized (this) {
          jarContent = handlerWrapper.coreContainer.getJarRepository().getJarIncRef(handlerWrapper.lib + "/" + handlerWrapper.version);
          return true;
        }
      } catch (SolrException se) {
        throw se;
      }

    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      try {
        return super.findClass(name);
      } catch (ClassNotFoundException e) {
        String path = name.replace('.', '/').concat(".class");
        ByteBuffer buf = null;
        try {
          if (jarContent == null) checkJarAvailable();
          buf = jarContent.jar.getFileContent(path);
          if (buf == null) throw new ClassNotFoundException("class not found in loaded jar" + name);
        } catch (IOException e1) {
          throw new ClassNotFoundException("class not found " + name, e1);

        }

        ProtectionDomain defaultDomain = null;

        //using the default protection domain, with no permissions
        try {
          defaultDomain = new ProtectionDomain(new CodeSource(new URL("http://localhost/.system/blob/" + handlerWrapper.lib), (Certificate[]) null),
              null);
        } catch (MalformedURLException e1) {
          //should not happen
        }
        return defineClass(name, buf.array(), buf.arrayOffset(), buf.limit(), defaultDomain);
      }
    }


    private void releaseJar() {
      handlerWrapper.coreContainer.getJarRepository().decrementJarRefCount(jarContent);
    }

  }

   public void close() {
    for (Map.Entry<String, SolrRequestHandler> e : handlers.entrySet()) {
      if (e.getValue() instanceof AutoCloseable) {
        try {
          ((AutoCloseable) e.getValue()).close();
        } catch (Exception exp) {
          log.error("Error closing requestHandler " + e.getKey(), exp);
        }
      }

    }

    handlers.close();
   }
 }
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrConfig.java b/solr/core/src/java/org/apache/solr/core/SolrConfig.java
index 736b13321ee..408856a347d 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrConfig.java
++ b/solr/core/src/java/org/apache/solr/core/SolrConfig.java
@@ -26,6 +26,7 @@ import org.apache.solr.cloud.ZkSolrResourceLoader;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.handler.component.SearchComponent;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.response.QueryResponseWriter;
@@ -45,8 +46,6 @@ import org.apache.solr.update.processor.UpdateRequestProcessorChain;
 import org.apache.solr.util.DOMUtil;
 import org.apache.solr.util.FileUtils;
 import org.apache.solr.util.RegexFileFilter;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
 import org.noggit.JSONParser;
 import org.noggit.ObjectBuilder;
 import org.slf4j.Logger;
@@ -77,10 +76,14 @@ import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
import static java.util.Collections.unmodifiableMap;
import static org.apache.solr.common.params.CoreAdminParams.NAME;
import static org.apache.solr.core.SolrConfig.PluginOpts.LAZY;
 import static org.apache.solr.core.SolrConfig.PluginOpts.MULTI_OK;
 import static org.apache.solr.core.SolrConfig.PluginOpts.NOOP;
 import static org.apache.solr.core.SolrConfig.PluginOpts.REQUIRE_CLASS;
 import static org.apache.solr.core.SolrConfig.PluginOpts.REQUIRE_NAME;
import static org.apache.solr.schema.FieldType.CLASS_NAME;
 
 
 /**
@@ -99,6 +102,7 @@ public class SolrConfig extends Config implements MapSerializable{
     MULTI_OK, 
     REQUIRE_NAME,
     REQUIRE_CLASS,
    LAZY,
     // EnumSet.of and/or EnumSet.copyOf(Collection) are anoying
     // because of type determination
     NOOP
@@ -296,9 +300,9 @@ public class SolrConfig extends Config implements MapSerializable{
   }
 
   public static final  List<SolrPluginInfo> plugins = ImmutableList.<SolrPluginInfo>builder()
      .add(new SolrPluginInfo(SolrRequestHandler.class, SolrRequestHandler.TYPE, REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK))
      .add(new SolrPluginInfo(SolrRequestHandler.class, SolrRequestHandler.TYPE, REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK, LAZY))
       .add(new SolrPluginInfo(QParserPlugin.class, "queryParser", REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK))
      .add(new SolrPluginInfo(QueryResponseWriter.class, "queryResponseWriter", REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK))
      .add(new SolrPluginInfo(QueryResponseWriter.class, "queryResponseWriter", REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK, LAZY))
       .add(new SolrPluginInfo(ValueSourceParser.class, "valueSourceParser", REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK))
       .add(new SolrPluginInfo(TransformerFactory.class, "transformer", REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK))
       .add(new SolrPluginInfo(SearchComponent.class, "searchComponent", REQUIRE_NAME, REQUIRE_CLASS, MULTI_OK))
@@ -307,6 +311,7 @@ public class SolrConfig extends Config implements MapSerializable{
       // and even then -- only if there is a single SpellCheckComponent
       // because of queryConverter.setIndexAnalyzer
       .add(new SolrPluginInfo(QueryConverter.class, "queryConverter", REQUIRE_NAME, REQUIRE_CLASS))
      .add(new SolrPluginInfo(PluginRegistry.RuntimeLib.class, "runtimeLib", REQUIRE_NAME, MULTI_OK))
       // this is hackish, since it picks up all SolrEventListeners,
       // regardless of when/how/why they are used (or even if they are
       // declared outside of the appropriate context) but there's no nice
@@ -323,10 +328,11 @@ public class SolrConfig extends Config implements MapSerializable{
       .add(new SolrPluginInfo(InitParams.class, InitParams.TYPE, MULTI_OK))
       .add(new SolrPluginInfo(StatsCache.class, "statsCache", REQUIRE_CLASS))
       .build();
  private static final Map<String, SolrPluginInfo> clsVsInfo = new HashMap<>();

  public static final Map<String, SolrPluginInfo> classVsSolrPluginInfo;
   static {
    for (SolrPluginInfo plugin : plugins) clsVsInfo.put(plugin.clazz.getName(), plugin);
    Map<String, SolrPluginInfo> map = new HashMap<>();
    for (SolrPluginInfo plugin : plugins) map.put(plugin.clazz.getName(), plugin);
    classVsSolrPluginInfo = Collections.unmodifiableMap(map);
   }
 
   public static class SolrPluginInfo{
@@ -634,7 +640,7 @@ public class SolrConfig extends Config implements MapSerializable{
    */
   public List<PluginInfo> getPluginInfos(String type) {
     List<PluginInfo> result = pluginStore.get(type);
    SolrPluginInfo info = clsVsInfo.get(type);
    SolrPluginInfo info = classVsSolrPluginInfo.get(type);
     if (info != null && info.options.contains(REQUIRE_NAME)) {
       Map<String, Map> infos = overlay.getNamedPlugins(info.tag);
       if (!infos.isEmpty()) {
@@ -664,14 +670,14 @@ public class SolrConfig extends Config implements MapSerializable{
   private void initLibs() {
     NodeList nodes = (NodeList) evaluate("lib", XPathConstants.NODESET);
     if (nodes == null || nodes.getLength() == 0) return;
    

     log.info("Adding specified lib dirs to ClassLoader");
     SolrResourceLoader loader = getResourceLoader();
    

     try {
       for (int i = 0; i < nodes.getLength(); i++) {
         Node node = nodes.item(i);
        

         String baseDir = DOMUtil.getAttr(node, "dir");
         String path = DOMUtil.getAttr(node, "path");
         if (null != baseDir) {
@@ -696,7 +702,7 @@ public class SolrConfig extends Config implements MapSerializable{
       loader.reloadLuceneSPI();
     }
   }
  

   public int getMultipartUploadLimitKB() {
     return multipartUploadLimitKB;
   }
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index 1ffbcc2ad31..2ae596fd107 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -17,6 +17,7 @@
 
 package org.apache.solr.core;
 
import javax.xml.parsers.ParserConfigurationException;
 import java.io.Closeable;
 import java.io.File;
 import java.io.FileNotFoundException;
@@ -24,7 +25,6 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
import java.io.Writer;
 import java.lang.reflect.Constructor;
 import java.net.URL;
 import java.nio.charset.StandardCharsets;
@@ -56,9 +56,8 @@ import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.locks.ReentrantLock;
 
import javax.xml.parsers.ParserConfigurationException;

 import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.util.ResourceLoader;
 import org.apache.lucene.codecs.Codec;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexDeletionPolicy;
@@ -87,15 +86,8 @@ import org.apache.solr.handler.IndexFetcher;
 import org.apache.solr.handler.ReplicationHandler;
 import org.apache.solr.handler.RequestHandlerBase;
 import org.apache.solr.handler.admin.ShowFileRequestHandler;
import org.apache.solr.handler.component.DebugComponent;
import org.apache.solr.handler.component.ExpandComponent;
import org.apache.solr.handler.component.FacetComponent;
 import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.handler.component.MoreLikeThisComponent;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.handler.component.RealTimeGetComponent;
 import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.StatsComponent;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.response.BinaryResponseWriter;
@@ -154,14 +146,14 @@ import org.xml.sax.SAXException;
  *
  */
 public final class SolrCore implements SolrInfoMBean, Closeable {
  public static final String version="1.0";  
  public static final String version="1.0";
 
   // These should *only* be used for debugging or monitoring purposes
   public static final AtomicLong numOpens = new AtomicLong();
   public static final AtomicLong numCloses = new AtomicLong();
   public static Map<SolrCore,Exception> openHandles = Collections.synchronizedMap(new IdentityHashMap<SolrCore, Exception>());
 
  

   public static final Logger log = LoggerFactory.getLogger(SolrCore.class);
   public static final Logger requestLog = LoggerFactory.getLogger(SolrCore.class.getName() + ".Request");
 
@@ -180,29 +172,30 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   private final String ulogDir;
   private final UpdateHandler updateHandler;
   private final SolrCoreState solrCoreState;
  

   private final long startTime;
   private final RequestHandlers reqHandlers;
  private final Map<String,SearchComponent> searchComponents;
  private final PluginRegistry<SearchComponent> searchComponents = new PluginRegistry<>(SearchComponent.class, this);
   private final Map<String,UpdateRequestProcessorChain> updateProcessorChains;
   private final Map<String, SolrInfoMBean> infoRegistry;
   private IndexDeletionPolicyWrapper solrDelPolicy;
   private DirectoryFactory directoryFactory;
   private IndexReaderFactory indexReaderFactory;
   private final Codec codec;
  private final MemClassLoader memClassLoader;
 
   private final List<Runnable> confListeners = new CopyOnWriteArrayList<>();
  

   private final ReentrantLock ruleExpiryLock;
  

   public long getStartTime() { return startTime; }
  

   private RestManager restManager;
  

   public RestManager getRestManager() {
     return restManager;
   }
  

   static int boolean_query_max_clause_count = Integer.MIN_VALUE;
   // only change the BooleanQuery maxClauseCount once for ALL cores...
   void booleanQueryMaxClauseCount()  {
@@ -215,7 +208,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
       }
     }
   }
    

   /**
    * The SolrResourceLoader used to load all resources for this core.
    * @since solr 1.3
@@ -238,7 +231,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   public SolrConfig getSolrConfig() {
     return solrConfig;
   }
  

   /**
    * Gets the schema resource name used by this core instance.
    * @since solr 1.3
@@ -248,15 +241,15 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   }
 
   /** @return the latest snapshot of the schema used by this core instance. */
  public IndexSchema getLatestSchema() { 
  public IndexSchema getLatestSchema() {
     return schema;
   }
  

   /** Sets the latest schema snapshot to be used by this core instance. */
   public void setLatestSchema(IndexSchema replacementSchema) {
     schema = replacementSchema;
   }
  

   public String getDataDir() {
     return dataDir;
   }
@@ -300,12 +293,12 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         final InputStream is = new PropertiesInputStream(input);
         try {
           p.load(new InputStreamReader(is, StandardCharsets.UTF_8));
          

           String s = p.getProperty("index");
           if (s != null && s.trim().length() > 0) {
               result = dataDir + s;
           }
          

         } catch (Exception e) {
           log.error("Unable to load " + IndexFetcher.INDEX_PROPERTIES, e);
         } finally {
@@ -331,15 +324,15 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   }
   private String lastNewIndexDir; // for debugging purposes only... access not synchronized, but that's ok
 
  

   public DirectoryFactory getDirectoryFactory() {
     return directoryFactory;
   }
  

   public IndexReaderFactory getIndexReaderFactory() {
     return indexReaderFactory;
   }
  

   @Override
   public String getName() {
     return name;
@@ -372,13 +365,13 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
      PluginInfo info = solrConfig.getPluginInfo(IndexDeletionPolicy.class.getName());
      IndexDeletionPolicy delPolicy = null;
      if(info != null){
       delPolicy = createInstance(info.className,IndexDeletionPolicy.class,"Deletion Policy for SOLR");
       delPolicy = createInstance(info.className, IndexDeletionPolicy.class, "Deletion Policy for SOLR", this, getResourceLoader());
        if (delPolicy instanceof NamedListInitializedPlugin) {
          ((NamedListInitializedPlugin) delPolicy).init(info.initArgs);
        }
      } else {
        delPolicy = new SolrDeletionPolicy();
     }     
     }
      solrDelPolicy = new IndexDeletionPolicyWrapper(delPolicy);
    }
 
@@ -406,7 +399,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    * NOTE: this function is not thread safe.  However, it is safe to call within the
    * <code>inform( SolrCore core )</code> function for <code>SolrCoreAware</code> classes.
    * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
   * 
   *
    * @see SolrCoreAware
    */
   public void registerFirstSearcherListener( SolrEventListener listener )
@@ -418,7 +411,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    * NOTE: this function is not thread safe.  However, it is safe to call within the
    * <code>inform( SolrCore core )</code> function for <code>SolrCoreAware</code> classes.
    * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
   * 
   *
    * @see SolrCoreAware
    */
   public void registerNewSearcherListener( SolrEventListener listener )
@@ -430,7 +423,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    * NOTE: this function is not thread safe.  However, it is safe to call within the
    * <code>inform( SolrCore core )</code> function for <code>SolrCoreAware</code> classes.
    * Outside <code>inform</code>, this could potentially throw a ConcurrentModificationException
   * 
   *
    * @see SolrCoreAware
    */
   public QueryResponseWriter registerResponseWriter( String name, QueryResponseWriter responseWriter ){
@@ -439,7 +432,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
   public SolrCore reload(ConfigSet coreConfig) throws IOException,
       ParserConfigurationException, SAXException {
    

     solrCoreState.increfSolrCoreState();
     SolrCore currentCore;
     boolean indexDirChange = !getNewIndexDir().equals(getIndexDir());
@@ -449,17 +442,17 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     } else {
       currentCore = this;
     }
    

     SolrCore core = new SolrCore(getName(), getDataDir(), coreConfig.getSolrConfig(),
         coreConfig.getIndexSchema(), coreDescriptor, updateHandler, this.solrDelPolicy, currentCore);
     core.solrDelPolicy = this.solrDelPolicy;
    

 
     // we open a new indexwriter to pick up the latest config
     core.getUpdateHandler().getSolrCoreState().newIndexWriter(core, false);
    

     core.getSearcher(true, false, null, true);
    

     return core;
   }
 
@@ -487,15 +480,15 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
       indexReaderFactory.init(info.initArgs);
     } else {
       indexReaderFactory = new StandardIndexReaderFactory();
    } 
    }
     this.indexReaderFactory = indexReaderFactory;
   }
  

   // protect via synchronized(SolrCore.class)
   private static Set<String> dirs = new HashSet<>();
 
   void initIndex(boolean reload) throws IOException {
 

       String indexDir = getNewIndexDir();
       boolean indexExists = getDirectoryFactory().exists(indexDir);
       boolean firstTime;
@@ -507,7 +500,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
       initIndexReaderFactory();
 
       if (indexExists && firstTime && !reload) {
        

         Directory dir = directoryFactory.get(indexDir, DirContext.DEFAULT,
             getSolrConfig().indexConfig.lockType);
         try {
@@ -517,7 +510,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
                   logid
                       + "WARNING: Solr index directory '{}' is locked.  Unlocking...",
                   indexDir);
              dir.makeLock(IndexWriter.WRITE_LOCK_NAME).close();              
              dir.makeLock(IndexWriter.WRITE_LOCK_NAME).close();
             } else {
               log.error(logid
                   + "Solr index directory '{}' is locked.  Throwing exception",
@@ -525,7 +518,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
               throw new LockObtainFailedException(
                   "Index locked for write for core " + name);
             }
            

           }
         } finally {
           directoryFactory.release(dir);
@@ -537,26 +530,15 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         log.warn(logid+"Solr index directory '" + new File(indexDir) + "' doesn't exist."
                 + " Creating new index...");
 
        SolrIndexWriter writer = SolrIndexWriter.create(this, "SolrCore.initIndex", indexDir, getDirectoryFactory(), true, 
        SolrIndexWriter writer = SolrIndexWriter.create(this, "SolrCore.initIndex", indexDir, getDirectoryFactory(), true,
                                                         getLatestSchema(), solrConfig.indexConfig, solrDelPolicy, codec);
         writer.close();
       }
 
 
  }
 
  /** Creates an instance by trying a constructor that accepts a SolrCore before
   *  trying the default (no arg) constructor.
   *@param className the instance class to create
   *@param cast the class or interface that the instance should extend or implement
   *@param msg a message helping compose the exception error if any occurs.
   *@return the desired instance
   *@throws SolrException if the object could not be instantiated
   */
  private <T> T createInstance(String className, Class<T> cast, String msg) {
    return createInstance(className,cast,msg, this);
   }
 

   /**
    * Creates an instance by trying a constructor that accepts a SolrCore before
    * trying the default (no arg) constructor.
@@ -568,11 +550,11 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    * @return the desired instance
    * @throws SolrException if the object could not be instantiated
    */
  public static <T> T createInstance(String className, Class<T> cast, String msg, SolrCore core) {
  public static <T> T createInstance(String className, Class<T> cast, String msg, SolrCore core, ResourceLoader resourceLoader) {
     Class<? extends T> clazz = null;
     if (msg == null) msg = "SolrCore Object";
     try {
      clazz = core.getResourceLoader().findClass(className, cast);
      clazz = resourceLoader.findClass(className, cast);
       //most of the classes do not have constructors which takes SolrCore argument. It is recommended to obtain SolrCore by implementing SolrCoreAware.
       // So invariably always it will cause a  NoSuchMethodException. So iterate though the list of available constructors
       Constructor<?>[] cons = clazz.getConstructors();
@@ -582,7 +564,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
           return cast.cast(con.newInstance(core));
         }
       }
      return core.getResourceLoader().newInstance(className, cast);//use the empty constructor
      return resourceLoader.newInstance(className, cast);//use the empty constructor
     } catch (SolrException e) {
       throw e;
     } catch (Exception e) {
@@ -596,7 +578,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
       throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error Instantiating " + msg + ", " + className + " failed to instantiate " + cast.getName(), e);
     }
   }
  

   private UpdateHandler createReloadedUpdateHandler(String className, String msg, UpdateHandler updateHandler) {
     Class<? extends UpdateHandler> clazz = null;
     if (msg == null) msg = "SolrCore Object";
@@ -609,7 +591,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
           Class<?>[] types = con.getParameterTypes();
           if(types.length == 2 && types[0] == SolrCore.class && types[1] == UpdateHandler.class){
             return UpdateHandler.class.cast(con.newInstance(this, updateHandler));
          } 
          }
         }
         throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,"Error Instantiating "+msg+", "+className+ " could not find proper constructor for " + UpdateHandler.class.getName());
     } catch (SolrException e) {
@@ -628,7 +610,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
   public <T extends Object> T createInitInstance(PluginInfo info,Class<T> cast, String msg, String defClassName){
     if(info == null) return null;
    T o = createInstance(info.className == null ? defClassName : info.className,cast, msg);
    T o = createInstance(info.className == null ? defClassName : info.className ,cast, msg,this, getResourceLoader());
     if (o instanceof PluginInfoInitialized) {
       ((PluginInfoInitialized) o).init(info);
     } else if (o instanceof NamedListInitializedPlugin) {
@@ -640,26 +622,14 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     return o;
   }
 
  public SolrEventListener createEventListener(String className) {
    return createInstance(className, SolrEventListener.class, "Event Listener");
  }

  public SolrRequestHandler createRequestHandler(String className) {
    return createInstance(className, SolrRequestHandler.class, "Request Handler");
  }

   private UpdateHandler createUpdateHandler(String className) {
    return createInstance(className, UpdateHandler.class, "Update Handler");
    return createInstance(className, UpdateHandler.class, "Update Handler", this, getResourceLoader());
   }
  

   private UpdateHandler createUpdateHandler(String className, UpdateHandler updateHandler) {
     return createReloadedUpdateHandler(className, "Update Handler", updateHandler);
   }
 
  private QueryResponseWriter createQueryResponseWriter(String className) {
    return createInstance(className, QueryResponseWriter.class, "Query Response Writer");
  }
  
   /**
    * Creates a new core and register it in the list of cores.
    * If a core with the same name already exists, it will be stopped and replaced by this one.
@@ -697,11 +667,11 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     this.updateHandler = null;
     this.isReloaded = true;
     this.reqHandlers = null;
    this.searchComponents = null;
     this.updateProcessorChains = null;
     this.infoRegistry = null;
     this.codec = null;
     this.ruleExpiryLock = null;
    this.memClassLoader = null;
 
     solrCoreState = null;
   }
@@ -719,7 +689,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     this.setName( name );
     resourceLoader = config.getResourceLoader();
     this.solrConfig = config;
    

     if (updateHandler == null) {
       initDirectoryFactory();
     }
@@ -784,7 +754,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
       schema = IndexSchemaFactory.buildIndexSchema(IndexSchema.DEFAULT_SCHEMA_FILE, config);
     }
     this.schema = schema;
    final SimilarityFactory similarityFactory = schema.getSimilarityFactory(); 
    final SimilarityFactory similarityFactory = schema.getSimilarityFactory();
     if (similarityFactory instanceof SolrCoreAware) {
       // Similarity needs SolrCore before inform() is called on all registered SolrCoreAware listeners below
       ((SolrCoreAware)similarityFactory).inform(this);
@@ -796,21 +766,21 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     this.slowQueryThresholdMillis = config.slowQueryThresholdMillis;
 
     booleanQueryMaxClauseCount();
  

     final CountDownLatch latch = new CountDownLatch(1);
 
     try {
      

       initListeners();
      

       if (delPolicy == null) {
         initDeletionPolicy();
       } else {
         this.solrDelPolicy = delPolicy;
       }
      

       this.codec = initCodec(solrConfig, schema);
      

       if (updateHandler == null) {
         solrCoreState = new DefaultSolrCoreState(getDirectoryFactory());
       } else {
@@ -818,17 +788,15 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         directoryFactory = solrCoreState.getDirectoryFactory();
         this.isReloaded = true;
       }
      
      memClassLoader = new MemClassLoader(PluginRegistry.RuntimeLib.getLibObjects(this, solrConfig.getPluginInfos(PluginRegistry.RuntimeLib.class.getName())), getResourceLoader());
       initIndex(prev != null);
      

       initWriters();
      initQParsers();
      initValueSourceParsers();
      initTransformerFactories();
      
      this.searchComponents = Collections
          .unmodifiableMap(loadSearchComponents());
      
      qParserPlugins.init(createInstances(QParserPlugin.standardPlugins), this);
      valueSourceParsers.init(ValueSourceParser.standardValueSourceParsers, this);
      transformerFactories.init(TransformerFactory.defaultFactories, this);
      loadSearchComponents();

       // Processors initialized before the handlers
       updateProcessorChains = loadUpdateProcessorChains();
       reqHandlers = new RequestHandlers(this);
@@ -836,9 +804,9 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
       // Handle things that should eventually go away
       initDeprecatedSupport();
      

       statsCache = initStatsCache();
      

       // cause the executor to stall so firstSearcher events won't fire
       // until after inform() has been called for all components.
       // searchExecutor must be single-threaded for this to work
@@ -849,7 +817,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
           return null;
         }
       });
      

       // use the (old) writer to open the first searcher
       RefCounted<IndexWriter> iwRef = null;
       if (prev != null) {
@@ -867,9 +835,9 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
           };
         }
       }
      

       String updateHandlerClass = solrConfig.getUpdateHandlerInfo().className;
      

       if (updateHandler == null) {
         this.updateHandler = createUpdateHandler(updateHandlerClass == null ? DirectUpdateHandler2.class
             .getName() : updateHandlerClass);
@@ -886,10 +854,10 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         newReaderCreator = null;
         if (iwRef != null) iwRef.decref();
       }
      

       // Initialize the RestManager
       restManager = initRestManager();
            

       // Finally tell anyone who wants to know
       resourceLoader.inform(resourceLoader);
       resourceLoader.inform(this); // last call before the latch is released.
@@ -899,7 +867,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
       if (e instanceof OutOfMemoryError) {
         throw (OutOfMemoryError)e;
       }
      

       try {
        this.close();
       } catch (Throwable t) {
@@ -908,8 +876,8 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         }
         log.error("Error while closing", t);
       }
      
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, 

      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                               e.getMessage(), e);
     } finally {
       // allow firstSearcher events to fire and make sure it is released
@@ -917,7 +885,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     }
 
     infoRegistry.put("core", this);
    

     // register any SolrInfoMBeans SolrResourceLoader initialized
     //
     // this must happen after the latch is released, because a JMX server impl may
@@ -925,7 +893,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     // and a SolrCoreAware MBean may have properties that depend on getting a Searcher
     // from the core.
     resourceLoader.inform(infoRegistry);
    

     CoreContainer cc = cd.getCoreContainer();
 
     if (cc != null && cc.isZooKeeperAware()) {
@@ -951,7 +919,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     ruleExpiryLock = new ReentrantLock();
     registerConfListener();
   }
    

   private Codec initCodec(SolrConfig solrConfig, final IndexSchema schema) {
     final PluginInfo info = solrConfig.getPluginInfo(CodecFactory.class.getName());
     final CodecFactory factory;
@@ -986,7 +954,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     }
     return factory.getCodec();
   }
  

   private StatsCache initStatsCache() {
     final StatsCache cache;
     PluginInfo pluginInfo = solrConfig.getPluginInfo(StatsCache.class.getName());
@@ -1016,7 +984,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     UpdateRequestProcessorChain def = initPlugins(map,UpdateRequestProcessorChain.class, UpdateRequestProcessorChain.class.getName());
     if(def == null){
       def = map.get(null);
    } 
    }
     if (def == null) {
       log.info("no updateRequestProcessorChain defined as default, creating implicit default");
       // construct the default chain
@@ -1031,14 +999,14 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     map.put("", def);
     return map;
   }
   

   public SolrCoreState getSolrCoreState() {
     return solrCoreState;
  }  
  }
 
   /**
    * @return an update processor registered to the given name.  Throw an exception if this chain is undefined
   */    
   */
   public UpdateRequestProcessorChain getUpdateProcessingChain( final String name )
   {
     UpdateRequestProcessorChain chain = updateProcessorChains.get( name );
@@ -1048,7 +1016,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     }
     return chain;
   }
  

   // this core current usage count
   private final AtomicInteger refCount = new AtomicInteger(1);
 
@@ -1056,7 +1024,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   public void open() {
     refCount.incrementAndGet();
   }
  

   /**
    * Close all resources allocated by the core if it is no longer in use...
    * <ul>
@@ -1065,7 +1033,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    *   <li>all CloseHooks will be notified</li>
    *   <li>All MBeans will be unregistered from MBeanServer if JMX was enabled
    *       </li>
   * </ul> 
   * </ul>
    * <p>
    * The behavior of this method is determined by the result of decrementing
    * the core's reference count (A core is created with a reference count of 1)...
@@ -1079,7 +1047,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    *       is taken.
    *   </li>
    * </ul>
   * @see #isClosed() 
   * @see #isClosed()
    */
   public void close() {
     int count = refCount.decrementAndGet();
@@ -1097,7 +1065,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
          try {
            hook.preClose( this );
          } catch (Throwable e) {
           SolrException.log(log, e);       
           SolrException.log(log, e);
            if (e instanceof Error) {
              throw (Error) e;
            }
@@ -1106,6 +1074,18 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     }
 
     if(reqHandlers != null) reqHandlers.close();
    responseWriters.close();
    searchComponents.close();
    qParserPlugins.close();
    valueSourceParsers.close();
    transformerFactories.close();

    if (memClassLoader != null) {
      try {
        memClassLoader.close();
      } catch (Exception e) {
      }
    }
 
 
     try {
@@ -1118,7 +1098,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         throw (Error) e;
       }
     }
    

     boolean coreStateClosed = false;
     try {
       if (solrCoreState != null) {
@@ -1134,7 +1114,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         throw (Error) e;
       }
     }
    

     try {
       ExecutorUtil.shutdownAndAwaitTermination(searcherExecutor);
     } catch (Throwable e) {
@@ -1168,9 +1148,9 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         throw (Error) e;
       }
     }
    

     if (coreStateClosed) {
      

       try {
         directoryFactory.close();
       } catch (Throwable e) {
@@ -1179,10 +1159,10 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
           throw (Error) e;
         }
       }
      

     }
 
    

     if( closeHooks != null ) {
        for( CloseHook hook : closeHooks ) {
          try {
@@ -1195,7 +1175,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
          }
       }
     }
    

     // For debugging 
 //    numCloses.incrementAndGet();
 //    openHandles.remove(this);
@@ -1205,12 +1185,12 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   public int getOpenCount() {
     return refCount.get();
   }
  

   /** Whether this core is closed. */
   public boolean isClosed() {
       return refCount.get() <= 0;
   }
  

   @Override
   protected void finalize() throws Throwable {
     try {
@@ -1258,113 +1238,74 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   ////////////////////////////////////////////////////////////////////////////////
 
   /**
   * Get the request handler registered to a given name.  
   * 
   * Get the request handler registered to a given name.
   *
    * This function is thread safe.
    */
   public SolrRequestHandler getRequestHandler(String handlerName) {
    return RequestHandlerBase.getRequestHandler(RequestHandlers.normalize(handlerName), reqHandlers.getRequestHandlers());
    return RequestHandlerBase.getRequestHandler(RequestHandlers.normalize(handlerName), reqHandlers.handlers);
   }
 
  /**
   * Returns an unmodifiable Map containing the registered handlers of the specified type.
   */
  public <T extends SolrRequestHandler> Map<String,T> getRequestHandlers(Class<T> clazz) {
    return reqHandlers.getAll(clazz);
  }
  
   /**
    * Returns an unmodifiable Map containing the registered handlers
    */
  public Map<String,SolrRequestHandler> getRequestHandlers() {
    return reqHandlers.getRequestHandlers();
  public PluginRegistry<SolrRequestHandler> getRequestHandlers() {
    return reqHandlers.handlers;
   }
 
 
   /**
    * Registers a handler at the specified location.  If one exists there, it will be replaced.
    * To remove a handler, register <code>null</code> at its path
   * 
   *
    * Once registered the handler can be accessed through:
    * <pre>
    *   http://${host}:${port}/${context}/${handlerName}
   * or:  
   * or:
    *   http://${host}:${port}/${context}/select?qt=${handlerName}
   * </pre>  
   * 
   * </pre>
   *
    * Handlers <em>must</em> be initialized before getting registered.  Registered
    * handlers can immediately accept requests.
   * 
   *
    * This call is thread safe.
   *  
   *
    * @return the previous <code>SolrRequestHandler</code> registered to this name <code>null</code> if none.
    */
   public SolrRequestHandler registerRequestHandler(String handlerName, SolrRequestHandler handler) {
     return reqHandlers.register(handlerName,handler);
   }
  

   /**
    * Register the default search components
    */
  private Map<String, SearchComponent> loadSearchComponents()
  private void loadSearchComponents()
   {
    Map<String, SearchComponent> components = new HashMap<>();
    initPlugins(components,SearchComponent.class);
    for (Map.Entry<String, SearchComponent> e : components.entrySet()) {
      SearchComponent c = e.getValue();
      if (c instanceof HighlightComponent) {
        HighlightComponent hl = (HighlightComponent) c;
        if(!HighlightComponent.COMPONENT_NAME.equals(e.getKey())){
          components.put(HighlightComponent.COMPONENT_NAME,hl);
    Map<String, SearchComponent> instances = createInstances(SearchComponent.standard_components);
    for (Map.Entry<String, SearchComponent> e : instances.entrySet()) e.getValue().setName(e.getKey());
    searchComponents.init(instances, this);

    for (String name : searchComponents.keySet()) {
      if (searchComponents.isLoaded(name) && searchComponents.get(name) instanceof HighlightComponent) {
        if (!HighlightComponent.COMPONENT_NAME.equals(name)) {
          searchComponents.put(HighlightComponent.COMPONENT_NAME, searchComponents.getRegistry().get(name));
         }
         break;
       }
     }
    addIfNotPresent(components,HighlightComponent.COMPONENT_NAME,HighlightComponent.class);
    addIfNotPresent(components,QueryComponent.COMPONENT_NAME,QueryComponent.class);
    addIfNotPresent(components,FacetComponent.COMPONENT_NAME,FacetComponent.class);
    addIfNotPresent(components,MoreLikeThisComponent.COMPONENT_NAME,MoreLikeThisComponent.class);
    addIfNotPresent(components,StatsComponent.COMPONENT_NAME,StatsComponent.class);
    addIfNotPresent(components,DebugComponent.COMPONENT_NAME,DebugComponent.class);
    addIfNotPresent(components,RealTimeGetComponent.COMPONENT_NAME,RealTimeGetComponent.class);
    addIfNotPresent(components,ExpandComponent.COMPONENT_NAME,ExpandComponent.class);

    return components;
   }
  private <T> void addIfNotPresent(Map<String ,T> registry, String name, Class<? extends  T> c){
    if(!registry.containsKey(name)){
      T searchComp = resourceLoader.newInstance(c.getName(), c);
      if (searchComp instanceof NamedListInitializedPlugin){
        ((NamedListInitializedPlugin)searchComp).init( new NamedList<String>() );
      }
      if(searchComp instanceof SearchComponent) {
        ((SearchComponent)searchComp).setName(name);
      }
      registry.put(name, searchComp);
      if (searchComp instanceof SolrInfoMBean){
        infoRegistry.put(((SolrInfoMBean)searchComp).getName(), (SolrInfoMBean)searchComp);
      }
    }
  }
  
   /**
    * @return a Search Component registered to a given name.  Throw an exception if the component is undefined
    */
  public SearchComponent getSearchComponent( String name )
  {
    SearchComponent component = searchComponents.get( name );
    if( component == null ) {
      throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,
          "Unknown Search Component: "+name );
    }
    return component;
  public SearchComponent getSearchComponent(String name) {
    return searchComponents.get(name);
   }
 
   /**
    * Accessor for all the Search Components
    * @return An unmodifiable Map of Search Components
    */
  public Map<String, SearchComponent> getSearchComponents() {
  public PluginRegistry<SearchComponent> getSearchComponents() {
     return searchComponents;
   }
 
@@ -1374,7 +1315,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
   /**
    * RequestHandlers need access to the updateHandler so they can all talk to the
   * same RAM indexer.  
   * same RAM indexer.
    */
   public UpdateHandler getUpdateHandler() {
     return updateHandler;
@@ -1529,7 +1470,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         DirectoryReader currentReader = newestSearcher.get().getRawReader();
 
         // SolrCore.verbose("start reopen from",previousSearcher,"writer=",writer);
        

         RefCounted<IndexWriter> writer = getUpdateHandler().getSolrCoreState()
             .getIndexWriter(null);
         try {
@@ -1582,7 +1523,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
           // so that we pick up any uncommitted changes and so we don't go backwards
           // in time on a core reload
           DirectoryReader newReader = newReaderCreator.call();
          tmp = new SolrIndexSearcher(this, newIndexDir, getLatestSchema(), 
          tmp = new SolrIndexSearcher(this, newIndexDir, getLatestSchema(),
               (realtime ? "realtime":"main"), newReader, true, !realtime, true, directoryFactory);
         } else  {
           RefCounted<IndexWriter> writer = getUpdateHandler().getSolrCoreState().getIndexWriter(this);
@@ -1626,7 +1567,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     }
 
   }
  

   /**
    * Get a {@link SolrIndexSearcher} or start the process of creating a new one.
    * <p>
@@ -1764,7 +1705,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
       // if the underlying seracher has not changed, no warming is needed
       if (newSearcher != currSearcher) {
        

         // warm the new searcher based on the current searcher.
         // should this go before the other event handlers or after?
         if (currSearcher != null) {
@@ -1783,7 +1724,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
             }
           });
         }
        

         if (currSearcher == null) {
           future = searcherExecutor.submit(new Callable() {
             @Override
@@ -1802,7 +1743,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
             }
           });
         }
        

         if (currSearcher != null) {
           future = searcherExecutor.submit(new Callable() {
             @Override
@@ -1821,7 +1762,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
             }
           });
         }
        

       }
 
 
@@ -2005,9 +1946,9 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     if (handler==null) {
       String msg = "Null Request Handler '" +
         req.getParams().get(CommonParams.QT) + "'";
      

       if (log.isWarnEnabled()) log.warn(logid + msg + ":" + req);
      

       throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, msg);
     }
 
@@ -2094,7 +2035,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     if( ep != null ) {
       EchoParamStyle echoParams = EchoParamStyle.get( ep );
       if( echoParams == null ) {
        throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"Invalid value '" + ep + "' for " + CommonParams.HEADER_ECHO_PARAMS 
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Invalid value '" + ep + "' for " + CommonParams.HEADER_ECHO_PARAMS
             + " parameter, use '" + EchoParamStyle.EXPLICIT + "' or '" + EchoParamStyle.ALL + "'" );
       }
       if( echoParams == EchoParamStyle.EXPLICIT ) {
@@ -2109,10 +2050,11 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     SolrException.log(log,null,e);
   }
 
  
  
  private QueryResponseWriter defaultResponseWriter;
  private final Map<String, QueryResponseWriter> responseWriters = new HashMap<>();
  public PluginRegistry<QueryResponseWriter> getResponseWriters() {
    return responseWriters;
  }

  private final PluginRegistry<QueryResponseWriter> responseWriters = new PluginRegistry<>(QueryResponseWriter.class, this);
   public static final Map<String ,QueryResponseWriter> DEFAULT_RESPONSE_WRITERS ;
   static{
     HashMap<String, QueryResponseWriter> m= new HashMap<>();
@@ -2147,165 +2089,60 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     };
   }
 
  public MemClassLoader getMemClassLoader() {
    return memClassLoader;
  }

   public interface RawWriter {
     public void write(OutputStream os) throws IOException ;
   }
  

   /** Configure the query response writers. There will always be a default writer; additional
    * writers may also be configured. */
   private void initWriters() {
    // use link map so we iterate in the same order
    Map<PluginInfo,QueryResponseWriter> writers = new LinkedHashMap<>();
    for (PluginInfo info : solrConfig.getPluginInfos(QueryResponseWriter.class.getName())) {
      try {
        QueryResponseWriter writer;
        String startup = info.attributes.get("startup") ;
        if( startup != null ) {
          if( "lazy".equals(startup) ) {
            log.info("adding lazy queryResponseWriter: " + info.className);
            writer = new LazyQueryResponseWriterWrapper(this, info.className, info.initArgs );
          } else {
            throw new Exception( "Unknown startup value: '"+startup+"' for: "+info.className );
          }
        } else {
          writer = createQueryResponseWriter(info.className);
        }
        writers.put(info,writer);
        QueryResponseWriter old = registerResponseWriter(info.name, writer);
        if(old != null) {
          log.warn("Multiple queryResponseWriter registered to the same name: " + info.name + " ignoring: " + old.getClass().getName());
        }
        if(info.isDefault()){
          if(defaultResponseWriter != null)
            log.warn("Multiple default queryResponseWriter registered, using: " + info.name);
          defaultResponseWriter = writer;
        }
        log.info("created "+info.name+": " + info.className);
      } catch (Exception ex) {
          SolrException e = new SolrException
            (SolrException.ErrorCode.SERVER_ERROR, "QueryResponseWriter init failure", ex);
          SolrException.log(log,null,e);
          throw e;
      }
    }

    // we've now registered all handlers, time to init them in the same order
    for (Map.Entry<PluginInfo,QueryResponseWriter> entry : writers.entrySet()) {
      PluginInfo info = entry.getKey();
      QueryResponseWriter writer = entry.getValue();
      responseWriters.put(info.name, writer);
      if (writer instanceof PluginInfoInitialized) {
        ((PluginInfoInitialized) writer).init(info);
      } else{
        writer.init(info.initArgs);
      }
    }

    NamedList emptyList = new NamedList();
    for (Map.Entry<String, QueryResponseWriter> entry : DEFAULT_RESPONSE_WRITERS.entrySet()) {
      if(responseWriters.get(entry.getKey()) == null) {
        responseWriters.put(entry.getKey(), entry.getValue());
        // call init so any logic in the default writers gets invoked
        entry.getValue().init(emptyList);
      }
    }
    
    responseWriters.init(DEFAULT_RESPONSE_WRITERS, this);
     // configure the default response writer; this one should never be null
    if (defaultResponseWriter == null) {
      defaultResponseWriter = responseWriters.get("standard");
    }

    if (responseWriters.getDefault() == null) responseWriters.setDefault("standard");
   }
  


   /** Finds a writer by name, or returns the default writer if not found. */
   public final QueryResponseWriter getQueryResponseWriter(String writerName) {
    if (writerName != null) {
        QueryResponseWriter writer = responseWriters.get(writerName);
        if (writer != null) {
            return writer;
        }
    }
    return defaultResponseWriter;
    return responseWriters.get(writerName, true);
   }
 
   /** Returns the appropriate writer for a request. If the request specifies a writer via the
    * 'wt' parameter, attempts to find that one; otherwise return the default writer.
    */
   public final QueryResponseWriter getQueryResponseWriter(SolrQueryRequest request) {
    return getQueryResponseWriter(request.getParams().get(CommonParams.WT)); 
  }

  private final Map<String, QParserPlugin> qParserPlugins = new HashMap<>();

  /** Configure the query parsers. */
  private void initQParsers() {
    initPlugins(qParserPlugins,QParserPlugin.class);
    // default parsers
    for (int i=0; i<QParserPlugin.standardPlugins.length; i+=2) {
     try {
       String name = (String)QParserPlugin.standardPlugins[i];
       if (null == qParserPlugins.get(name)) {
         Class<QParserPlugin> clazz = (Class<QParserPlugin>)QParserPlugin.standardPlugins[i+1];
         QParserPlugin plugin = clazz.newInstance();
         qParserPlugins.put(name, plugin);
         plugin.init(null);
         infoRegistry.put(name, plugin);
       }
     } catch (Exception e) {
       throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
     }
    }
    return getQueryResponseWriter(request.getParams().get(CommonParams.WT));
   }
 

  private final PluginRegistry<QParserPlugin> qParserPlugins = new PluginRegistry<>(QParserPlugin.class, this);

   public QParserPlugin getQueryPlugin(String parserName) {
    QParserPlugin plugin = qParserPlugins.get(parserName);
    if (plugin != null) return plugin;
    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown query parser '"+parserName+"'");
  }
  
  private final HashMap<String, ValueSourceParser> valueSourceParsers = new HashMap<>();
  
  /** Configure the ValueSource (function) plugins */
  private void initValueSourceParsers() {
    initPlugins(valueSourceParsers,ValueSourceParser.class);
    // default value source parsers
    for (Map.Entry<String, ValueSourceParser> entry : ValueSourceParser.standardValueSourceParsers.entrySet()) {
      try {
        String name = entry.getKey();
        if (null == valueSourceParsers.get(name)) {
          ValueSourceParser valueSourceParser = entry.getValue();
          valueSourceParsers.put(name, valueSourceParser);
          valueSourceParser.init(null);
        }
      } catch (Exception e) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
      }
    }
    return qParserPlugins.get(parserName);
   }
  
 
  private final HashMap<String, TransformerFactory> transformerFactories = new HashMap<>();
  
  /** Configure the TransformerFactory plugins */
  private void initTransformerFactories() {
    // Load any transformer factories
    initPlugins(transformerFactories,TransformerFactory.class);
    
    // Tell each transformer what its name is
    for( Map.Entry<String, TransformerFactory> entry : TransformerFactory.defaultFactories.entrySet() ) {
  private final PluginRegistry<ValueSourceParser> valueSourceParsers = new PluginRegistry<>(ValueSourceParser.class, this);

  private final PluginRegistry<TransformerFactory> transformerFactories = new PluginRegistry<>(TransformerFactory.class, this);

  <T> Map<String, T> createInstances(Map<String, Class<? extends T>> map) {
    Map<String, T> result = new LinkedHashMap<>();
    for (Map.Entry<String, Class<? extends T>> e : map.entrySet()) {
       try {
        String name = entry.getKey();
        if (null == valueSourceParsers.get(name)) {
          TransformerFactory f = entry.getValue();
          transformerFactories.put(name, f);
          // f.init(null); default ones don't need init
        }
      } catch (Exception e) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
        Object o = getResourceLoader().newInstance(e.getValue().getName(), e.getValue());
        result.put(e.getKey(), (T) o);
      } catch (Exception exp) {
        //should never happen
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unbale to instantiate class", exp);
       }
     }
    return result;
   }
  

   public TransformerFactory getTransformerFactory(String name) {
     return transformerFactories.get(name);
   }
@@ -2313,7 +2150,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   public void addTransformerFactory(String name, TransformerFactory factory){
     transformerFactories.put(name, factory);
   }
  

 
   /**
    * @param registry The map to which the instance should be added to. The key is the name attribute
@@ -2321,7 +2158,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    * @param defClassName If PluginInfo does not have a classname, use this as the classname
    * @return The default instance . The one with (default=true)
    */
  public <T> T initPlugins(Map<String ,T> registry, Class<T> type, String defClassName){
  private <T> T initPlugins(Map<String, T> registry, Class<T> type, String defClassName) {
     return initPlugins(solrConfig.getPluginInfos(type.getName()), registry, type, defClassName);
   }
 
@@ -2361,7 +2198,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   public ValueSourceParser getValueSourceParser(String parserName) {
     return valueSourceParsers.get(parserName);
   }
  

   /**
    * Manage anything that should be taken care of in case configs change
    */
@@ -2370,19 +2207,19 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     // TODO -- this should be removed in deprecation release...
     String gettable = solrConfig.get("admin/gettableFiles", null );
     if( gettable != null ) {
      log.warn( 
      log.warn(
           "solrconfig.xml uses deprecated <admin/gettableFiles>, Please "+
           "update your config to use the ShowFileRequestHandler." );
       if( getRequestHandler( "/admin/file" ) == null ) {
         NamedList<String> invariants = new NamedList<>();
        

         // Hide everything...
         Set<String> hide = new HashSet<>();
 
         for (String file : solrConfig.getResourceLoader().listConfigDir()) {
           hide.add(file.toUpperCase(Locale.ROOT));
        }    
        
        }

         // except the "gettable" list
         StringTokenizer st = new StringTokenizer( gettable );
         while( st.hasMoreTokens() ) {
@@ -2391,7 +2228,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         for( String s : hide ) {
           invariants.add( ShowFileRequestHandler.HIDDEN, s );
         }
        

         NamedList<Object> args = new NamedList<>();
         args.add( "invariants", invariants );
         ShowFileRequestHandler handler = new ShowFileRequestHandler();
@@ -2404,12 +2241,12 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
     String facetSort = solrConfig.get("//bool[@name='facet.sort']", null);
     if (facetSort != null) {
      log.warn( 
      log.warn(
           "solrconfig.xml uses deprecated <bool name='facet.sort'>. Please "+
           "update your config to use <string name='facet.sort'>.");
     }
  } 
  
  }

   /**
    * Creates and initializes a RestManager based on configuration args in solrconfig.xml.
    * RestManager provides basic storage support for managed resource data, such as to
@@ -2417,36 +2254,36 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    */
   @SuppressWarnings("unchecked")
   protected RestManager initRestManager() throws SolrException {
    
    PluginInfo restManagerPluginInfo = 

    PluginInfo restManagerPluginInfo =
         getSolrConfig().getPluginInfo(RestManager.class.getName());
        

     NamedList<String> initArgs = null;
     RestManager mgr = null;
     if (restManagerPluginInfo != null) {
       if (restManagerPluginInfo.className != null) {
         mgr = resourceLoader.newInstance(restManagerPluginInfo.className, RestManager.class);
       }
      

       if (restManagerPluginInfo.initArgs != null) {
        initArgs = (NamedList<String>)restManagerPluginInfo.initArgs;        
        initArgs = (NamedList<String>) restManagerPluginInfo.initArgs;
       }
     }
    
    if (mgr == null) 

    if (mgr == null)
       mgr = new RestManager();
    

     if (initArgs == null)
       initArgs = new NamedList<>();
                                

     String collection = coreDescriptor.getCollectionName();
    StorageIO storageIO = 
        ManagedResourceStorage.newStorageIO(collection, resourceLoader, initArgs);    
    StorageIO storageIO =
        ManagedResourceStorage.newStorageIO(collection, resourceLoader, initArgs);
     mgr.init(resourceLoader, initArgs, storageIO);
    

     return mgr;
  }  
  
  }

   public CoreDescriptor getCoreDescriptor() {
     return coreDescriptor;
   }
@@ -2516,10 +2353,10 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         lst.add("shard", shard);
       }
     }
    

     return lst;
   }
  

   public Codec getCodec() {
     return codec;
   }
@@ -2583,50 +2420,6 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     }
   }
 
  public final class LazyQueryResponseWriterWrapper implements QueryResponseWriter {
    private SolrCore _core;
    private String _className;
    private NamedList _args;
    private QueryResponseWriter _writer;

    public LazyQueryResponseWriterWrapper(SolrCore core, String className, NamedList args) {
      _core = core;
      _className = className;
      _args = args;
      _writer = null;
    }

    public synchronized QueryResponseWriter getWrappedWriter()
    {
      if( _writer == null ) {
        try {
          QueryResponseWriter writer = createQueryResponseWriter(_className);
          writer.init( _args );
          _writer = writer;
        }
        catch( Exception ex ) {
          throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, "lazy loading error", ex );
        }
      }
      return _writer;
    }


    @Override
    public void init(NamedList args) {
      // do nothing
    }

    @Override
    public void write(Writer writer, SolrQueryRequest request, SolrQueryResponse response) throws IOException {
      getWrappedWriter().write(writer, request, response);
    }

    @Override
    public String getContentType(SolrQueryRequest request, SolrQueryResponse response) {
      return getWrappedWriter().getContentType(request, response);
    }
  }
 
   /**Register to notify for any file change in the conf directory.
    * If the file change results in a core reload , then the listener
@@ -2714,6 +2507,10 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     };
   }
 
  public void registerInfoBean(String name, SolrInfoMBean solrInfoMBean) {
    infoRegistry.put(name, solrInfoMBean);
  }

   private static boolean checkStale(SolrZkClient zkClient,  String zkPath, int currentVersion)  {
     if(zkPath == null) return false;
     try {
diff --git a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
index ad76ce2011c..a1391dbc113 100644
-- a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
++ b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
@@ -22,8 +22,7 @@ import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.RequestHandlers;
import org.apache.solr.core.RequestParams;
import org.apache.solr.core.PluginRegistry;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrInfoMBean;
 import org.apache.solr.request.SolrQueryRequest;
@@ -215,7 +214,7 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
    *
    * This function is thread safe.
    */
  public static SolrRequestHandler getRequestHandler(String handlerName, Map<String, SolrRequestHandler> reqHandlers) {
  public static SolrRequestHandler getRequestHandler(String handlerName, PluginRegistry<SolrRequestHandler> reqHandlers) {
     if(handlerName == null) return null;
     SolrRequestHandler handler = reqHandlers.get(handlerName);
     int idx = 0;
@@ -226,9 +225,6 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
           String firstPart = handlerName.substring(0, idx);
           handler = reqHandlers.get(firstPart);
           if (handler == null) continue;
          if(handler instanceof RequestHandlers.LazyRequestHandlerWrapper) {
            handler = ((RequestHandlers.LazyRequestHandlerWrapper)handler).getWrappedHandler();
          }
           if (handler instanceof NestedRequestHandler) {
             return ((NestedRequestHandler) handler).getSubHandler(handlerName.substring(idx));
           }
diff --git a/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java b/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java
index 79f776187ac..c51525c4029 100644
-- a/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java
@@ -43,19 +43,14 @@ import org.apache.solr.common.util.ContentStream;
 import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.core.ConfigOverlay;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.PluginsRegistry;
import org.apache.solr.core.ImplicitPlugins;
 import org.apache.solr.core.RequestParams;
 import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.SearchComponent;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.transform.TransformerFactory;
 import org.apache.solr.schema.SchemaManager;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.ValueSourceParser;
 import org.apache.solr.util.CommandOperation;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -64,6 +59,7 @@ import static java.text.MessageFormat.format;
 import static java.util.Collections.singletonList;
 import static org.apache.solr.common.params.CoreAdminParams.NAME;
 import static org.apache.solr.core.ConfigOverlay.NOT_EDITABLE;
import static org.apache.solr.core.SolrConfig.PluginOpts.REQUIRE_CLASS;
 import static org.apache.solr.core.SolrConfig.PluginOpts.REQUIRE_NAME;
 import static org.apache.solr.schema.FieldType.CLASS_NAME;
 
@@ -152,7 +148,7 @@ public class SolrConfigHandler extends RequestHandlerBase {
       Map<String, Object> map = req.getCore().getSolrConfig().toMap();
       Map reqHandlers = (Map) map.get(SolrRequestHandler.TYPE);
       if (reqHandlers == null) map.put(SolrRequestHandler.TYPE, reqHandlers = new LinkedHashMap<>());
      List<PluginInfo> plugins = PluginsRegistry.getHandlers(req.getCore());
      List<PluginInfo> plugins = ImplicitPlugins.getHandlers(req.getCore());
       for (PluginInfo plugin : plugins) {
         if (SolrRequestHandler.TYPE.equals(plugin.type)) {
           if (!reqHandlers.containsKey(plugin.name)) {
@@ -316,7 +312,7 @@ public class SolrConfigHandler extends RequestHandlerBase {
                 if ("delete".equals(prefix)) {
                   overlay = deleteNamedComponent(op, overlay, info.tag);
                 } else {
                  overlay = updateNamedPlugin(info, op, overlay, prefix.equals("create"));
                  overlay = updateNamedPlugin(info, op, overlay, prefix.equals("create") || prefix.equals("add"));
                 }
               } else {
                 op.addError(MessageFormat.format("Unknown operation ''{0}'' ", op.name));
@@ -359,7 +355,7 @@ public class SolrConfigHandler extends RequestHandlerBase {
 
     private ConfigOverlay updateNamedPlugin(SolrConfig.SolrPluginInfo info, CommandOperation op, ConfigOverlay overlay, boolean isCeate) {
       String name = op.getStr(NAME);
      String clz = op.getStr(CLASS_NAME);
      String clz = info.options.contains(REQUIRE_CLASS) ? op.getStr(CLASS_NAME) : op.getStr(CLASS_NAME, null);
       op.getMap(PluginInfo.DEFAULTS, null);
       op.getMap(PluginInfo.INVARIANTS, null);
       op.getMap(PluginInfo.APPENDS, null);
@@ -383,10 +379,11 @@ public class SolrConfigHandler extends RequestHandlerBase {
     }
 
     private boolean verifyClass(CommandOperation op, String clz, Class expected) {
      if (op.getStr("lib", null) == null) {
      if (clz == null) return true;
      if ( !"true".equals(String.valueOf(op.getStr("runtimeLib", null)))) {
         //this is not dynamically loaded so we can verify the class right away
         try {
          SolrCore.createInstance(clz, expected, expected.getSimpleName(), req.getCore());
          req.getCore().createInitInstance(new PluginInfo(SolrRequestHandler.TYPE, op.getDataMap()), expected, clz, "");
         } catch (Exception e) {
           op.addError(e.getMessage());
           return false;
@@ -522,6 +519,6 @@ public class SolrConfigHandler extends RequestHandlerBase {
   public static final String SET = "set";
   public static final String UPDATE = "update";
   public static final String CREATE = "create";
  private static Set<String> cmdPrefixes = ImmutableSet.of(CREATE, UPDATE, "delete");
  private static Set<String> cmdPrefixes = ImmutableSet.of(CREATE, UPDATE, "delete", "add");
 
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/AdminHandlers.java b/solr/core/src/java/org/apache/solr/handler/admin/AdminHandlers.java
index 8187727bc6c..a218e35f113 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/AdminHandlers.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/AdminHandlers.java
@@ -23,6 +23,7 @@ import java.util.Map;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.response.SolrQueryResponse;
@@ -38,7 +39,7 @@ import org.slf4j.LoggerFactory;
  * the plugins registered by this class are iplicitly registered by the system
  */
 @Deprecated
public class AdminHandlers implements SolrCoreAware, SolrRequestHandler
public class AdminHandlers extends RequestHandlerBase implements SolrCoreAware
 {
   public static Logger log = LoggerFactory.getLogger(AdminHandlers.class);
   NamedList initArgs = null;
@@ -61,17 +62,12 @@ public class AdminHandlers implements SolrCoreAware, SolrRequestHandler
   public void init(NamedList args) {
     this.initArgs = args;
   }
  

   @Override
   public void inform(SolrCore core) 
   {
     String path = null;
    for( Map.Entry<String, SolrRequestHandler> entry : core.getRequestHandlers().entrySet() ) {
      if( entry.getValue() == this ) {
        path = entry.getKey();
        break;
      }
    }
    path = getPluginInfo().name;
     if( path == null ) {
       throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, 
           "The AdminHandler is not registered with the current core." );
@@ -109,9 +105,9 @@ public class AdminHandlers implements SolrCoreAware, SolrRequestHandler
     log.warn("<requestHandler name=\"/admin/\" \n class=\"solr.admin.AdminHandlers\" /> is deprecated . It is not required anymore");
   }
 
  

   @Override
  public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
  public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) {
     throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, 
         "The AdminHandler should never be called directly" );
   }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java b/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
index 6b35a3d6de8..937818e8e91 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
@@ -19,6 +19,9 @@ package org.apache.solr.handler.component;
 
 import java.io.IOException;
 import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
 
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.SolrInfoMBean;
@@ -122,4 +125,21 @@ public abstract class SearchComponent implements SolrInfoMBean, NamedListInitial
   public NamedList getStatistics() {
     return null;
   }

  public static final Map<String, Class<? extends SearchComponent>> standard_components;
  ;

  static {
    HashMap<String, Class<? extends SearchComponent>> map = new HashMap<>();
    map.put(HighlightComponent.COMPONENT_NAME, HighlightComponent.class);
    map.put(QueryComponent.COMPONENT_NAME, QueryComponent.class);
    map.put(FacetComponent.COMPONENT_NAME, FacetComponent.class);
    map.put(MoreLikeThisComponent.COMPONENT_NAME, MoreLikeThisComponent.class);
    map.put(StatsComponent.COMPONENT_NAME, StatsComponent.class);
    map.put(DebugComponent.COMPONENT_NAME, DebugComponent.class);
    map.put(RealTimeGetComponent.COMPONENT_NAME, RealTimeGetComponent.class);
    map.put(ExpandComponent.COMPONENT_NAME, ExpandComponent.class);
    standard_components = Collections.unmodifiableMap(map);
  }

 }
diff --git a/solr/core/src/java/org/apache/solr/search/QParserPlugin.java b/solr/core/src/java/org/apache/solr/search/QParserPlugin.java
index 3534769bc01..e8da6729c85 100644
-- a/solr/core/src/java/org/apache/solr/search/QParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/QParserPlugin.java
@@ -26,6 +26,9 @@ import org.apache.solr.search.mlt.MLTQParserPlugin;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
 import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
 
 public abstract class QParserPlugin implements NamedListInitializedPlugin, SolrInfoMBean {
   /** internal use - name of the default parser */
@@ -38,35 +41,39 @@ public abstract class QParserPlugin implements NamedListInitializedPlugin, SolrI
    * This result to NPE during initialization.
    * For every plugin, listed here, NAME field has to be final and static.
    */
  public static final Object[] standardPlugins = {
    LuceneQParserPlugin.NAME, LuceneQParserPlugin.class,
    OldLuceneQParserPlugin.NAME, OldLuceneQParserPlugin.class,
    FunctionQParserPlugin.NAME, FunctionQParserPlugin.class,
    PrefixQParserPlugin.NAME, PrefixQParserPlugin.class,
    BoostQParserPlugin.NAME, BoostQParserPlugin.class,
    DisMaxQParserPlugin.NAME, DisMaxQParserPlugin.class,
    ExtendedDismaxQParserPlugin.NAME, ExtendedDismaxQParserPlugin.class,
    FieldQParserPlugin.NAME, FieldQParserPlugin.class,
    RawQParserPlugin.NAME, RawQParserPlugin.class,
    TermQParserPlugin.NAME, TermQParserPlugin.class,
    TermsQParserPlugin.NAME, TermsQParserPlugin.class,
    NestedQParserPlugin.NAME, NestedQParserPlugin.class,
    FunctionRangeQParserPlugin.NAME, FunctionRangeQParserPlugin.class,
    SpatialFilterQParserPlugin.NAME, SpatialFilterQParserPlugin.class,
    SpatialBoxQParserPlugin.NAME, SpatialBoxQParserPlugin.class,
    JoinQParserPlugin.NAME, JoinQParserPlugin.class,
    SurroundQParserPlugin.NAME, SurroundQParserPlugin.class,
    SwitchQParserPlugin.NAME, SwitchQParserPlugin.class,
    MaxScoreQParserPlugin.NAME, MaxScoreQParserPlugin.class,
    BlockJoinParentQParserPlugin.NAME, BlockJoinParentQParserPlugin.class,
    BlockJoinChildQParserPlugin.NAME, BlockJoinChildQParserPlugin.class,
    CollapsingQParserPlugin.NAME, CollapsingQParserPlugin.class,
    SimpleQParserPlugin.NAME, SimpleQParserPlugin.class,
    ComplexPhraseQParserPlugin.NAME, ComplexPhraseQParserPlugin.class,
    ReRankQParserPlugin.NAME, ReRankQParserPlugin.class,
    ExportQParserPlugin.NAME, ExportQParserPlugin.class,
    MLTQParserPlugin.NAME, MLTQParserPlugin.class
  };
  public static final Map<String, Class<? extends QParserPlugin>> standardPlugins;

  static {
    HashMap<String, Class<? extends QParserPlugin>> map = new HashMap<>();
    map.put(LuceneQParserPlugin.NAME, LuceneQParserPlugin.class);
    map.put(OldLuceneQParserPlugin.NAME, OldLuceneQParserPlugin.class);
    map.put(FunctionQParserPlugin.NAME, FunctionQParserPlugin.class);
    map.put(PrefixQParserPlugin.NAME, PrefixQParserPlugin.class);
    map.put(BoostQParserPlugin.NAME, BoostQParserPlugin.class);
    map.put(DisMaxQParserPlugin.NAME, DisMaxQParserPlugin.class);
    map.put(ExtendedDismaxQParserPlugin.NAME, ExtendedDismaxQParserPlugin.class);
    map.put(FieldQParserPlugin.NAME, FieldQParserPlugin.class);
    map.put(RawQParserPlugin.NAME, RawQParserPlugin.class);
    map.put(TermQParserPlugin.NAME, TermQParserPlugin.class);
    map.put(TermsQParserPlugin.NAME, TermsQParserPlugin.class);
    map.put(NestedQParserPlugin.NAME, NestedQParserPlugin.class);
    map.put(FunctionRangeQParserPlugin.NAME, FunctionRangeQParserPlugin.class);
    map.put(SpatialFilterQParserPlugin.NAME, SpatialFilterQParserPlugin.class);
    map.put(SpatialBoxQParserPlugin.NAME, SpatialBoxQParserPlugin.class);
    map.put(JoinQParserPlugin.NAME, JoinQParserPlugin.class);
    map.put(SurroundQParserPlugin.NAME, SurroundQParserPlugin.class);
    map.put(SwitchQParserPlugin.NAME, SwitchQParserPlugin.class);
    map.put(MaxScoreQParserPlugin.NAME, MaxScoreQParserPlugin.class);
    map.put(BlockJoinParentQParserPlugin.NAME, BlockJoinParentQParserPlugin.class);
    map.put(BlockJoinChildQParserPlugin.NAME, BlockJoinChildQParserPlugin.class);
    map.put(CollapsingQParserPlugin.NAME, CollapsingQParserPlugin.class);
    map.put(SimpleQParserPlugin.NAME, SimpleQParserPlugin.class);
    map.put(ComplexPhraseQParserPlugin.NAME, ComplexPhraseQParserPlugin.class);
    map.put(ReRankQParserPlugin.NAME, ReRankQParserPlugin.class);
    map.put(ExportQParserPlugin.NAME, ExportQParserPlugin.class);
    map.put(MLTQParserPlugin.NAME, MLTQParserPlugin.class);
    standardPlugins = Collections.unmodifiableMap(map);
  }
 
   /** return a {@link QParser} */
   public abstract QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req);
diff --git a/solr/core/src/java/org/apache/solr/util/CommandOperation.java b/solr/core/src/java/org/apache/solr/util/CommandOperation.java
index 64f914d02e8..ae1e6305ecb 100644
-- a/solr/core/src/java/org/apache/solr/util/CommandOperation.java
++ b/solr/core/src/java/org/apache/solr/util/CommandOperation.java
@@ -52,8 +52,8 @@ public class CommandOperation {
       Object obj = getRootPrimitive();
       return obj == def ? null : String.valueOf(obj);
     }
    String s = (String) getMapVal(key);
    return s == null ? def : s;
    Object o = getMapVal(key);
    return o == null ? def : String.valueOf(o);
   }
 
   public Map<String, Object> getDataMap() {
diff --git a/solr/core/src/test/org/apache/solr/OutputWriterTest.java b/solr/core/src/test/org/apache/solr/OutputWriterTest.java
index cd4047585ba..f58b6cafbeb 100644
-- a/solr/core/src/test/org/apache/solr/OutputWriterTest.java
++ b/solr/core/src/test/org/apache/solr/OutputWriterTest.java
@@ -19,16 +19,13 @@ package org.apache.solr;
 
 import java.io.IOException;
 import java.io.Writer;
import java.util.List;
 
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.PluginRegistry;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.QueryResponseWriter;
 import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.XMLResponseWriter;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
@@ -94,11 +91,12 @@ public class OutputWriterTest extends SolrTestCaseJ4 {
     }
 
     public void testLazy() {
      QueryResponseWriter qrw = h.getCore().getQueryResponseWriter("useless");
      assertTrue("Should be a lazy class", qrw instanceof SolrCore.LazyQueryResponseWriterWrapper);
        PluginRegistry.PluginHolder<QueryResponseWriter> qrw = h.getCore().getResponseWriters().getRegistry().get("useless");
        assertTrue("Should be a lazy class", qrw instanceof PluginRegistry.LazyPluginHolder);
 
      qrw = h.getCore().getQueryResponseWriter("xml");
      assertTrue("Should not be a lazy class", qrw instanceof XMLResponseWriter);
        qrw = h.getCore().getResponseWriters().getRegistry().get("xml");
        assertTrue("Should not be a lazy class", qrw.isLoaded());
        assertTrue("Should not be a lazy class", qrw.getClass() == PluginRegistry.PluginHolder.class);
 
     }
     
diff --git a/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandler.java b/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandler.java
index 7583dcb09dc..e6f8f490e5f 100644
-- a/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandler.java
++ b/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandler.java
@@ -23,11 +23,41 @@ import java.io.IOException;
 import org.apache.solr.handler.DumpRequestHandler;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;

public class BlobStoreTestRequestHandler extends DumpRequestHandler implements Runnable, SolrCoreAware{

  private SolrCore core;

  private long version = 1;
  private String watchedVal = null;
 
public class BlobStoreTestRequestHandler extends DumpRequestHandler{
   @Override
   public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
     super.handleRequestBody(req, rsp);
     rsp.add("class", this.getClass().getName());
    rsp.add("x",watchedVal);
  }

  @Override
  public void run() {
    RequestParams p = core.getSolrConfig().getRequestParams();
    RequestParams.VersionedParams v = p.getParams("watched");
    if(v== null){
      watchedVal = null;
      version=-1;
      return;
    }
    if(v.getVersion() != version){
      watchedVal =  v.getMap().get("x");
    }
  }

  @Override
  public void inform(SolrCore core) {
    this.core = core;
    core.addConfListener(this);
    run();

   }
 }
diff --git a/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandlerV2.java b/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandlerV2.java
deleted file mode 100644
index 4eeefe99f84..00000000000
-- a/solr/core/src/test/org/apache/solr/core/BlobStoreTestRequestHandlerV2.java
++ /dev/null
@@ -1,68 +0,0 @@
package org.apache.solr.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;

public class BlobStoreTestRequestHandlerV2 extends BlobStoreTestRequestHandler implements Runnable, SolrCoreAware{

  private SolrCore core;

  private long version = 1;
  private String watchedVal = null;

  @Override
  public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
    super.handleRequestBody(req, rsp);
    rsp.add("class", this.getClass().getName());
    rsp.add("x",watchedVal);
  /*  try {
      Class.forName("org.apache.solr.core.BlobStoreTestRequestHandler");
    } catch (ClassNotFoundException e) {
      rsp.add("e", ClassNotFoundException.class.getSimpleName());
    }*/

  }

  @Override
  public void run() {
    RequestParams p = core.getSolrConfig().getRequestParams();
    RequestParams.VersionedParams v = p.getParams("watched");
    if(v== null){
      watchedVal = null;
      version=-1;
      return;
    }
    if(v.getVersion() != version){
       watchedVal =  v.getMap().get("x");
    }
  }

  @Override
  public void inform(SolrCore core) {
    this.core = core;
    core.addConfListener(this);
    run();

  }
}
diff --git a/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java b/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
index 9bd8dbc501d..815aa6a479a 100644
-- a/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
++ b/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
@@ -19,7 +19,6 @@ package org.apache.solr.core;
 
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.StandardRequestHandler;
 import org.apache.solr.request.SolrRequestHandler;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -49,8 +48,8 @@ public class RequestHandlersTest extends SolrTestCaseJ4 {
   @Test
   public void testLazyLoading() {
     SolrCore core = h.getCore();
    SolrRequestHandler handler = core.getRequestHandler( "lazy" );
    assertFalse( handler instanceof StandardRequestHandler ); 
    PluginRegistry.PluginHolder<SolrRequestHandler> handler = core.getRequestHandlers().getRegistry().get("lazy");
    assertFalse(handler.isLoaded());
     
     assertU(adoc("id", "42",
                  "name", "Zapp Brannigan"));
diff --git a/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java b/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java
index c3bea635fdd..b02801fdba2 100644
-- a/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java
++ b/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java
@@ -26,10 +26,13 @@ import org.apache.solr.handler.TestBlobHandler;
 import org.apache.solr.util.RESTfulServerProvider;
 import org.apache.solr.util.RestTestHarness;
 import org.apache.solr.util.SimplePostTool;
import org.junit.BeforeClass;
 import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.charset.StandardCharsets;
@@ -40,6 +43,9 @@ import java.util.Map;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipOutputStream;
 
import static java.util.Arrays.asList;
import static org.apache.solr.handler.TestSolrConfigHandlerCloud.compareValues;

 public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
   static final Logger log =  LoggerFactory.getLogger(TestDynamicLoading.class);
   private List<RestTestHarness> restTestHarnesses = new ArrayList<>();
@@ -56,6 +62,11 @@ public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
     }
   }
 
  @BeforeClass
  public static void enableRuntimeLib() throws Exception {
    System.setProperty("enable.runtime.lib", "true");
  }

   @Override
   public void distribTearDown() throws Exception {
     super.distribTearDown();
@@ -66,51 +77,56 @@ public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
 
   @Test
   public void testDynamicLoading() throws Exception {
    System.setProperty("enable.runtime.lib", "true");
     setupHarnesses();

    String blobName = "colltest";
    boolean success = false;


    HttpSolrClient randomClient = (HttpSolrClient) clients.get(random().nextInt(clients.size()));
    String baseURL = randomClient.getBaseURL();
    baseURL = baseURL.substring(0, baseURL.lastIndexOf('/'));
     String payload = "{\n" +
        "'create-requesthandler' : { 'name' : '/test1', 'class': 'org.apache.solr.core.BlobStoreTestRequestHandler' , 'lib':'test','version':'1'}\n" +
        "'add-runtimelib' : { 'name' : 'colltest' ,'version':1}\n" +
         "}";
     RestTestHarness client = restTestHarnesses.get(random().nextInt(restTestHarnesses.size()));
    TestSolrConfigHandler.runConfigCommand(client, "/config?wt=json", payload);
    TestSolrConfigHandler.testForResponseElement(client,
        null,
        "/config/overlay?wt=json",
        null,
        Arrays.asList("overlay", "runtimeLib", blobName, "version"),
        1l, 10);


    payload = "{\n" +
        "'create-requesthandler' : { 'name' : '/test1', 'class': 'org.apache.solr.core.BlobStoreTestRequestHandler' , 'runtimeLib' : true }\n" +
        "}";

    client = restTestHarnesses.get(random().nextInt(restTestHarnesses.size()));
     TestSolrConfigHandler.runConfigCommand(client,"/config?wt=json",payload);
     TestSolrConfigHandler.testForResponseElement(client,
         null,
         "/config/overlay?wt=json",
         null,
        Arrays.asList("overlay", "requestHandler", "/test1", "lib"),
        "test",10);
        Arrays.asList("overlay", "requestHandler", "/test1", "class"),
        "org.apache.solr.core.BlobStoreTestRequestHandler",10);
 
     Map map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
 
    assertNotNull(map = (Map) map.get("error"));
    assertEquals(".system collection not available", map.get("msg"));
    assertNotNull(TestBlobHandler.getAsString(map), map = (Map) map.get("error"));
    assertEquals(TestBlobHandler.getAsString(map), ".system collection not available", map.get("msg"));

 
    HttpSolrClient randomClient = (HttpSolrClient) clients.get(random().nextInt(clients.size()));
    String baseURL = randomClient.getBaseURL();
    baseURL = baseURL.substring(0, baseURL.lastIndexOf('/'));
     TestBlobHandler.createSystemCollection(new HttpSolrClient(baseURL, randomClient.getHttpClient()));
     waitForRecoveriesToFinish(".system", true);
 
     map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
 
    assertNotNull(map = (Map) map.get("error"));
    assertEquals("no such blob or version available: test/1", map.get("msg"));
    ByteBuffer jar = generateZip( TestDynamicLoading.class,BlobStoreTestRequestHandler.class);
    TestBlobHandler.postAndCheck(cloudClient, baseURL, jar,1);

    boolean success= false;
    for(int i=0;i<50;i++) {
      map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
      if(BlobStoreTestRequestHandler.class.getName().equals(map.get("class"))){
        success = true;
        break;
      }
      Thread.sleep(100);
    }
    assertTrue(new String( ZkStateReader.toJSON(map) , StandardCharsets.UTF_8), success );

    jar = generateZip( TestDynamicLoading.class,BlobStoreTestRequestHandlerV2.class);
    TestBlobHandler.postAndCheck(cloudClient, baseURL, jar,2);
 
    assertNotNull(map = (Map) map.get("error"));
    assertEquals("full output " + TestBlobHandler.getAsString(map), "no such blob or version available: colltest/1" , map.get("msg"));
     payload = " {\n" +
         "  'set' : {'watched': {" +
         "                    'x':'X val',\n" +
@@ -129,39 +145,111 @@ public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
         10);
 
 


    for(int i=0;i<100;i++) {
      map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
      if("X val".equals(map.get("x"))){
         success = true;
         break;
      }
      Thread.sleep(100);
    }
    ByteBuffer jar = null;

//     jar = persistZip("/tmp/runtimelibs.jar", TestDynamicLoading.class, RuntimeLibReqHandler.class, RuntimeLibResponseWriter.class, RuntimeLibSearchComponent.class);
//    if(true) return;

    jar = getFileContent("runtimecode/runtimelibs.jar");
    TestBlobHandler.postAndCheck(cloudClient, baseURL, blobName, jar, 1);

     payload = "{\n" +
        "'update-requesthandler' : { 'name' : '/test1', 'class': 'org.apache.solr.core.BlobStoreTestRequestHandlerV2' , 'lib':'test','version':2}\n" +
        "'create-requesthandler' : { 'name' : '/runtime', 'class': 'org.apache.solr.core.RuntimeLibReqHandler' , 'runtimeLib':true }," +
        "'create-searchcomponent' : { 'name' : 'get', 'class': 'org.apache.solr.core.RuntimeLibSearchComponent' , 'runtimeLib':true }," +
        "'create-queryResponseWriter' : { 'name' : 'json1', 'class': 'org.apache.solr.core.RuntimeLibResponseWriter' , 'runtimeLib':true }" +
         "}";
    client = restTestHarnesses.get(random().nextInt(restTestHarnesses.size()));
    TestSolrConfigHandler.runConfigCommand(client, "/config?wt=json", payload);

    Map result = TestSolrConfigHandler.testForResponseElement(client,
        null,
        "/config/overlay?wt=json",
        null,
        Arrays.asList("overlay", "requestHandler", "/runtime", "class"),
        "org.apache.solr.core.RuntimeLibReqHandler", 10);
    compareValues(result, "org.apache.solr.core.RuntimeLibResponseWriter", asList("overlay", "queryResponseWriter", "json1", "class"));
    compareValues(result, "org.apache.solr.core.RuntimeLibSearchComponent", asList("overlay", "searchComponent", "get", "class"));

    result = TestSolrConfigHandler.testForResponseElement(client,
        null,
        "/runtime?wt=json",
        null,
        Arrays.asList("class"),
        "org.apache.solr.core.RuntimeLibReqHandler", 10);
    compareValues(result, MemClassLoader.class.getName(), asList( "loader"));

    result = TestSolrConfigHandler.testForResponseElement(client,
        null,
        "/runtime?wt=json1",
        null,
        Arrays.asList("wt"),
        "org.apache.solr.core.RuntimeLibResponseWriter", 10);
    compareValues(result, MemClassLoader.class.getName(), asList( "loader"));

    result = TestSolrConfigHandler.testForResponseElement(client,
        null,
        "/get?abc=xyz",
        null,
        Arrays.asList("get"),
        "org.apache.solr.core.RuntimeLibSearchComponent", 10);
    compareValues(result, MemClassLoader.class.getName(), asList( "loader"));
 
    jar = getFileContent("runtimecode/runtimelibs_v2.jar");
    TestBlobHandler.postAndCheck(cloudClient, baseURL, blobName, jar, 2);
    payload = "{\n" +
        "'update-runtimelib' : { 'name' : 'colltest' ,'version':2}\n" +
        "}";
     client = restTestHarnesses.get(random().nextInt(restTestHarnesses.size()));
    TestSolrConfigHandler.runConfigCommand(client,"/config?wt=json",payload);
    TestSolrConfigHandler.runConfigCommand(client, "/config?wt=json", payload);
     TestSolrConfigHandler.testForResponseElement(client,
         null,
         "/config/overlay?wt=json",
         null,
        Arrays.asList("overlay", "requestHandler", "/test1", "version"),
        2l,10);
        Arrays.asList("overlay", "runtimeLib", blobName, "version"),
        2l, 10);
 
    success= false;
    for(int i=0;i<100;i++) {
      map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
      if(BlobStoreTestRequestHandlerV2.class.getName().equals(map.get("class"))) {
        success = true;
        break;
      }
      Thread.sleep(100);
    }
    result = TestSolrConfigHandler.testForResponseElement(client,
        null,
        "/get?abc=xyz",
        null,
        Arrays.asList("Version"),
        "2", 10);
 
    assertTrue("New version of class is not loaded " + new String(ZkStateReader.toJSON(map), StandardCharsets.UTF_8), success);
 
    for(int i=0;i<100;i++) {
      map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
      if("X val".equals(map.get("x"))){
         success = true;
         break;
      }
      Thread.sleep(100);
    }
    payload = " {\n" +
        "  'set' : {'watched': {" +
        "                    'x':'X val',\n" +
        "                    'y': 'Y val'}\n" +
        "             }\n" +
        "  }";

    TestSolrConfigHandler.runConfigCommand(client,"/config/params?wt=json",payload);
    TestSolrConfigHandler.testForResponseElement(
        client,
        null,
        "/config/params?wt=json",
        cloudClient,
        Arrays.asList("response", "params", "watched", "x"),
        "X val",
        10);
   result = TestSolrConfigHandler.testForResponseElement(
        client,
        null,
        "/test1?wt=json",
        cloudClient,
        Arrays.asList("x"),
        "X val",
        10);
 
     payload = " {\n" +
         "  'set' : {'watched': {" +
@@ -171,17 +259,33 @@ public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
         "  }";
 
     TestSolrConfigHandler.runConfigCommand(client,"/config/params?wt=json",payload);
    for(int i=0;i<50;i++) {
      map = TestSolrConfigHandler.getRespMap("/test1?wt=json", client);
      if("X val changed".equals(map.get("x"))){
        success = true;
        break;
      }
      Thread.sleep(100);
    }
    assertTrue("listener did not get triggered" + new String(ZkStateReader.toJSON(map), StandardCharsets.UTF_8), success);
    result = TestSolrConfigHandler.testForResponseElement(
        client,
        null,
        "/test1?wt=json",
        cloudClient,
        Arrays.asList("x"),
        "X val changed",
        10);
  }
 
  private ByteBuffer getFileContent(String f) throws IOException {
    ByteBuffer jar;
    try (FileInputStream fis = new FileInputStream(getFile(f))) {
      byte[] buf = new byte[fis.available()];
      fis.read(buf);
      jar = ByteBuffer.wrap(buf);
    }
    return jar;
  }
 
  public static  ByteBuffer persistZip(String loc, Class... classes) throws IOException {
    ByteBuffer jar = generateZip(classes);
    try (FileOutputStream fos =  new FileOutputStream(loc)){
      fos.write(jar.array(), 0, jar.limit());
      fos.flush();
    }
    return jar;
   }
 
 
diff --git a/solr/core/src/test/org/apache/solr/handler/TestBlobHandler.java b/solr/core/src/test/org/apache/solr/handler/TestBlobHandler.java
index 466e51692cc..9994b9e557d 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestBlobHandler.java
++ b/solr/core/src/test/org/apache/solr/handler/TestBlobHandler.java
@@ -86,8 +86,9 @@ public class TestBlobHandler extends AbstractFullDistribZkTestBase {
       for (int i = 0; i < bytarr.length; i++) bytarr[i]= (byte) (i % 127);
       byte[] bytarr2  = new byte[2048];
       for (int i = 0; i < bytarr2.length; i++) bytarr2[i]= (byte) (i % 127);
      postAndCheck(cloudClient, baseUrl, ByteBuffer.wrap( bytarr), 1);
      postAndCheck(cloudClient, baseUrl, ByteBuffer.wrap( bytarr2), 2);
      String blobName = "test";
      postAndCheck(cloudClient, baseUrl, blobName, ByteBuffer.wrap(bytarr), 1);
      postAndCheck(cloudClient, baseUrl, blobName, ByteBuffer.wrap(bytarr2), 2);
 
       url = baseUrl + "/.system/blob/test/1";
       map = TestSolrConfigHandlerConcurrent.getAsMap(url,cloudClient);
@@ -123,8 +124,8 @@ public class TestBlobHandler extends AbstractFullDistribZkTestBase {
     DirectUpdateHandler2.commitOnClose = true;
   }
 
  public static void postAndCheck(CloudSolrClient cloudClient, String baseUrl, ByteBuffer bytes, int count) throws Exception {
    postData(cloudClient, baseUrl, bytes);
  public static void postAndCheck(CloudSolrClient cloudClient, String baseUrl, String blobName, ByteBuffer bytes, int count) throws Exception {
    postData(cloudClient, baseUrl, blobName, bytes);
 
     String url;
     Map map = null;
@@ -132,7 +133,7 @@ public class TestBlobHandler extends AbstractFullDistribZkTestBase {
     long start = System.currentTimeMillis();
     int i=0;
     for(;i<150;i++) {//15 secs
      url = baseUrl + "/.system/blob/test";
      url = baseUrl + "/.system/blob/" + blobName;
       map = TestSolrConfigHandlerConcurrent.getAsMap(url, cloudClient);
       String numFound = String.valueOf(ConfigOverlay.getObjectByPath(map, false, Arrays.asList("response", "numFound")));
       if(!(""+count).equals(numFound)) {
@@ -171,12 +172,12 @@ public class TestBlobHandler extends AbstractFullDistribZkTestBase {
 
   }
 
  public static void postData(CloudSolrClient cloudClient, String baseUrl, ByteBuffer bytarr) throws IOException {
  public static void postData(CloudSolrClient cloudClient, String baseUrl, String blobName, ByteBuffer bytarr) throws IOException {
     HttpPost httpPost = null;
     HttpEntity entity;
     String response = null;
     try {
      httpPost = new HttpPost(baseUrl+"/.system/blob/test");
      httpPost = new HttpPost(baseUrl + "/.system/blob/" + blobName);
       httpPost.setHeader("Content-Type","application/octet-stream");
       httpPost.setEntity(new ByteArrayEntity(bytarr.array(), bytarr.arrayOffset(), bytarr.limit()));
       entity = cloudClient.getLbClient().getHttpClient().execute(httpPost).getEntity();
diff --git a/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java b/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java
index c44c1133e7d..96afee8c3d7 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/SpellCheckComponentTest.java
@@ -180,7 +180,11 @@ public class SpellCheckComponentTest extends SolrTestCaseJ4 {
     request = req("qt", "spellCheckCompRH", "q", "*:*", "spellcheck.q", "ttle",
         "spellcheck", "true", "spellcheck.dictionary", "default",
         "spellcheck.reload", "true");
    ResponseBuilder rb = new ResponseBuilder(request, new SolrQueryResponse(), new ArrayList(h.getCore().getSearchComponents().values()));
    List<SearchComponent> components = new ArrayList<>();
    for (String name : h.getCore().getSearchComponents().keySet()) {
      components.add(h.getCore().getSearchComponent(name));
    }
    ResponseBuilder rb = new ResponseBuilder(request, new SolrQueryResponse(), components);
     checker.prepare(rb);
 
     try {
diff --git a/solr/core/src/test/org/apache/solr/search/QueryEqualityTest.java b/solr/core/src/test/org/apache/solr/search/QueryEqualityTest.java
index 2487f5f4858..e87b8c7ab54 100644
-- a/solr/core/src/test/org/apache/solr/search/QueryEqualityTest.java
++ b/solr/core/src/test/org/apache/solr/search/QueryEqualityTest.java
@@ -55,11 +55,7 @@ public class QueryEqualityTest extends SolrTestCaseJ4 {
   public static void afterClassParserCoverageTest() {
 
     if ( ! doAssertParserCoverage) return;

    for (int i=0; i < QParserPlugin.standardPlugins.length; i+=2) {
      assertTrue("qparser #"+i+" name not a string", 
                 QParserPlugin.standardPlugins[i] instanceof String);
      final String name = (String)QParserPlugin.standardPlugins[i];
    for (String name : QParserPlugin.standardPlugins.keySet()) {
       assertTrue("testParserCoverage was run w/o any other method explicitly testing qparser: " + name, qParsersTested.contains(name));
     }
 
diff --git a/solr/core/src/test/org/apache/solr/search/TestStandardQParsers.java b/solr/core/src/test/org/apache/solr/search/TestStandardQParsers.java
index 5fbef6abde1..d65e09a3b9b 100644
-- a/solr/core/src/test/org/apache/solr/search/TestStandardQParsers.java
++ b/solr/core/src/test/org/apache/solr/search/TestStandardQParsers.java
@@ -24,7 +24,6 @@ import java.lang.reflect.Field;
 import java.lang.reflect.Modifier;
 import java.util.ArrayList;
 import java.util.List;
import java.util.HashMap;
 import java.util.Map;
 
 /**
@@ -48,15 +47,13 @@ public class TestStandardQParsers extends LuceneTestCase {
    */
   @Test
   public void testRegisteredName() throws Exception {
    Map<String, Class<QParserPlugin>> standardPlugins = getStandardQParsers();
    List<String> notStatic = new ArrayList<>(QParserPlugin.standardPlugins.size());
    List<String> notFinal = new ArrayList<>(QParserPlugin.standardPlugins.size());
    List<String> mismatch = new ArrayList<>(QParserPlugin.standardPlugins.size());
 
    List<String> notStatic = new ArrayList<>(standardPlugins.size());
    List<String> notFinal = new ArrayList<>(standardPlugins.size());
    List<String> mismatch = new ArrayList<>(standardPlugins.size());
 
    for (Map.Entry<String,Class<QParserPlugin>> pair : standardPlugins.entrySet()) {
    for (Map.Entry<String, Class<? extends QParserPlugin>> pair : QParserPlugin.standardPlugins.entrySet()) {
       String regName = pair.getKey();
      Class<QParserPlugin> clazz = pair.getValue();
      Class<? extends QParserPlugin> clazz = pair.getValue();
 
       Field nameField = clazz.getField(FIELD_NAME);
       int modifiers = nameField.getModifiers();
@@ -79,30 +76,8 @@ public class TestStandardQParsers extends LuceneTestCase {
 
     assertTrue("DEFAULT_QTYPE is not in the standard set of registered names: " + 
                QParserPlugin.DEFAULT_QTYPE,
               standardPlugins.keySet().contains(QParserPlugin.DEFAULT_QTYPE));
        QParserPlugin.standardPlugins.keySet().contains(QParserPlugin.DEFAULT_QTYPE));
 
   }
 
  /**
   * Get standard query parsers registered by default.
   *
   * @see org.apache.solr.search.QParserPlugin#standardPlugins
   * @return Map of classes extending QParserPlugin keyed by the registered name
   */
  private Map<String,Class<QParserPlugin>> getStandardQParsers() {
    Object[] standardPluginsValue = QParserPlugin.standardPlugins;

    Map<String, Class<QParserPlugin>> standardPlugins 
      = new HashMap<>(standardPluginsValue.length / 2);

    for (int i = 0; i < standardPluginsValue.length; i += 2) {
      @SuppressWarnings("unchecked")
      String registeredName = (String) standardPluginsValue[i];
      @SuppressWarnings("unchecked")
      Class<QParserPlugin> clazz = (Class<QParserPlugin>) standardPluginsValue[i + 1];
      standardPlugins.put(registeredName, clazz);
    }
    return standardPlugins;
  }

 }
- 
2.19.1.windows.1

