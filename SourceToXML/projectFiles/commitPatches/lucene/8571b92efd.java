From 8571b92efd55e61e923d8a671c1d4b68f2e0d2d7 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sun, 8 Mar 2015 20:21:22 +0000
Subject: [PATCH] SOLR-7073: renamed PluginRegistry to PluginBag

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1665076 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/core/CoreContainer.java    |  4 ++--
 .../org/apache/solr/core/MemClassLoader.java   | 11 +++++------
 .../{PluginRegistry.java => PluginBag.java}    |  8 +++-----
 .../org/apache/solr/core/RequestHandlers.java  |  6 +++---
 .../java/org/apache/solr/core/SolrConfig.java  |  6 +-----
 .../java/org/apache/solr/core/SolrCore.java    | 18 +++++++++---------
 .../solr/handler/RequestHandlerBase.java       |  5 ++---
 .../test/org/apache/solr/OutputWriterTest.java |  8 ++++----
 .../apache/solr/core/RequestHandlersTest.java  |  2 +-
 9 files changed, 30 insertions(+), 38 deletions(-)
 rename solr/core/src/java/org/apache/solr/core/{PluginRegistry.java => PluginBag.java} (98%)

diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index 580849ae478..11e693deb8d 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -107,13 +107,13 @@ public class CoreContainer {
   public static final String COLLECTIONS_HANDLER_PATH = "/admin/collections";
   public static final String INFO_HANDLER_PATH = "/admin/info";
 
  private PluginRegistry<SolrRequestHandler> containerHandlers = new PluginRegistry<>(SolrRequestHandler.class, null);
  private PluginBag<SolrRequestHandler> containerHandlers = new PluginBag<>(SolrRequestHandler.class, null);
 
   public SolrRequestHandler getRequestHandler(String path) {
     return RequestHandlerBase.getRequestHandler(path, containerHandlers);
   }
 
  public PluginRegistry<SolrRequestHandler> getRequestHandlers() {
  public PluginBag<SolrRequestHandler> getRequestHandlers() {
     return this.containerHandlers;
   }
 
diff --git a/solr/core/src/java/org/apache/solr/core/MemClassLoader.java b/solr/core/src/java/org/apache/solr/core/MemClassLoader.java
index 6c28dafeeb4..2e6eb170a29 100644
-- a/solr/core/src/java/org/apache/solr/core/MemClassLoader.java
++ b/solr/core/src/java/org/apache/solr/core/MemClassLoader.java
@@ -29,7 +29,6 @@ import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicReference;
 
 import org.apache.lucene.analysis.util.ResourceLoader;
@@ -42,11 +41,11 @@ public class MemClassLoader extends ClassLoader implements AutoCloseable, Resour
   static final Logger log =  LoggerFactory.getLogger(MemClassLoader.class);
   private boolean allJarsLoaded = false;
   private final SolrResourceLoader parentLoader;
  private List<PluginRegistry.RuntimeLib> libs = new ArrayList<>();
  private List<PluginBag.RuntimeLib> libs = new ArrayList<>();
   private Map<String, Class> classCache = new HashMap<>();
 
 
  public MemClassLoader(List<PluginRegistry.RuntimeLib> libs, SolrResourceLoader resourceLoader) {
  public MemClassLoader(List<PluginBag.RuntimeLib> libs, SolrResourceLoader resourceLoader) {
     this.parentLoader = resourceLoader;
     this.libs = libs;
   }
@@ -55,7 +54,7 @@ public class MemClassLoader extends ClassLoader implements AutoCloseable, Resour
   public synchronized void loadJars() {
     if (allJarsLoaded) return;
 
    for (PluginRegistry.RuntimeLib lib : libs) {
    for (PluginBag.RuntimeLib lib : libs) {
       try {
         lib.loadJar();
       } catch (Exception exception) {
@@ -113,7 +112,7 @@ public class MemClassLoader extends ClassLoader implements AutoCloseable, Resour
 
     String path = name.replace('.', '/').concat(".class");
     ByteBuffer buf = null;
    for (PluginRegistry.RuntimeLib lib : libs) {
    for (PluginBag.RuntimeLib lib : libs) {
       try {
         buf = lib.getFileContent(path);
         if (buf != null) {
@@ -130,7 +129,7 @@ public class MemClassLoader extends ClassLoader implements AutoCloseable, Resour
 
   @Override
   public void close() throws Exception {
    for (PluginRegistry.RuntimeLib lib : libs) {
    for (PluginBag.RuntimeLib lib : libs) {
       try {
         lib.close();
       } catch (Exception e) {
diff --git a/solr/core/src/java/org/apache/solr/core/PluginRegistry.java b/solr/core/src/java/org/apache/solr/core/PluginBag.java
similarity index 98%
rename from solr/core/src/java/org/apache/solr/core/PluginRegistry.java
rename to solr/core/src/java/org/apache/solr/core/PluginBag.java
index 850d44e26ee..059df44ca93 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginRegistry.java
++ b/solr/core/src/java/org/apache/solr/core/PluginBag.java
@@ -39,13 +39,11 @@ import org.apache.solr.util.plugin.SolrCoreAware;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import static java.util.Collections.singletonList;

 /**
  * This manages the lifecycle of a set of plugin of the same type .
  */
public class PluginRegistry<T> implements AutoCloseable {
  public static Logger log = LoggerFactory.getLogger(PluginRegistry.class);
public class PluginBag<T> implements AutoCloseable {
  public static Logger log = LoggerFactory.getLogger(PluginBag.class);
 
   private Map<String, PluginHolder<T>> registry = new HashMap<>();
   private Map<String, PluginHolder<T>> immutableRegistry = Collections.unmodifiableMap(registry);
@@ -54,7 +52,7 @@ public class PluginRegistry<T> implements AutoCloseable {
   private SolrCore core;
   private SolrConfig.SolrPluginInfo meta;
 
  public PluginRegistry(Class<T> klass, SolrCore core) {
  public PluginBag(Class<T> klass, SolrCore core) {
     this.core = core;
     this.klass = klass;
     meta = SolrConfig.classVsSolrPluginInfo.get(klass.getName());
diff --git a/solr/core/src/java/org/apache/solr/core/RequestHandlers.java b/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
index 5bd12093bb1..44fa89d5e0d 100644
-- a/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
++ b/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
@@ -35,7 +35,7 @@ public final class RequestHandlers {
 
   protected final SolrCore core;
 
  final PluginRegistry<SolrRequestHandler> handlers;
  final PluginBag<SolrRequestHandler> handlers;
 
   /**
    * Trim the trailing '/' if it's there, and convert null to empty string.
@@ -57,7 +57,7 @@ public final class RequestHandlers {
   
   public RequestHandlers(SolrCore core) {
       this.core = core;
    handlers =  new PluginRegistry<>(SolrRequestHandler.class, core);
    handlers =  new PluginBag<>(SolrRequestHandler.class, core);
   }
 
   /**
@@ -88,7 +88,7 @@ public final class RequestHandlers {
   /**
    * Returns an unmodifiable Map containing the registered handlers
    */
  public PluginRegistry<SolrRequestHandler> getRequestHandlers() {
  public PluginBag<SolrRequestHandler> getRequestHandlers() {
     return handlers;
   }
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrConfig.java b/solr/core/src/java/org/apache/solr/core/SolrConfig.java
index 408856a347d..04586358024 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrConfig.java
++ b/solr/core/src/java/org/apache/solr/core/SolrConfig.java
@@ -26,7 +26,6 @@ import org.apache.solr.cloud.ZkSolrResourceLoader;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.handler.component.SearchComponent;
 import org.apache.solr.request.SolrRequestHandler;
 import org.apache.solr.response.QueryResponseWriter;
@@ -76,14 +75,11 @@ import java.util.Set;
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
@@ -311,7 +307,7 @@ public class SolrConfig extends Config implements MapSerializable{
       // and even then -- only if there is a single SpellCheckComponent
       // because of queryConverter.setIndexAnalyzer
       .add(new SolrPluginInfo(QueryConverter.class, "queryConverter", REQUIRE_NAME, REQUIRE_CLASS))
      .add(new SolrPluginInfo(PluginRegistry.RuntimeLib.class, "runtimeLib", REQUIRE_NAME, MULTI_OK))
      .add(new SolrPluginInfo(PluginBag.RuntimeLib.class, "runtimeLib", REQUIRE_NAME, MULTI_OK))
       // this is hackish, since it picks up all SolrEventListeners,
       // regardless of when/how/why they are used (or even if they are
       // declared outside of the appropriate context) but there's no nice
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index 2ae596fd107..e1ae0ba87ae 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -175,7 +175,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
 
   private final long startTime;
   private final RequestHandlers reqHandlers;
  private final PluginRegistry<SearchComponent> searchComponents = new PluginRegistry<>(SearchComponent.class, this);
  private final PluginBag<SearchComponent> searchComponents = new PluginBag<>(SearchComponent.class, this);
   private final Map<String,UpdateRequestProcessorChain> updateProcessorChains;
   private final Map<String, SolrInfoMBean> infoRegistry;
   private IndexDeletionPolicyWrapper solrDelPolicy;
@@ -788,7 +788,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
         directoryFactory = solrCoreState.getDirectoryFactory();
         this.isReloaded = true;
       }
      memClassLoader = new MemClassLoader(PluginRegistry.RuntimeLib.getLibObjects(this, solrConfig.getPluginInfos(PluginRegistry.RuntimeLib.class.getName())), getResourceLoader());
      memClassLoader = new MemClassLoader(PluginBag.RuntimeLib.getLibObjects(this, solrConfig.getPluginInfos(PluginBag.RuntimeLib.class.getName())), getResourceLoader());
       initIndex(prev != null);
 
       initWriters();
@@ -1249,7 +1249,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   /**
    * Returns an unmodifiable Map containing the registered handlers
    */
  public PluginRegistry<SolrRequestHandler> getRequestHandlers() {
  public PluginBag<SolrRequestHandler> getRequestHandlers() {
     return reqHandlers.handlers;
   }
 
@@ -1305,7 +1305,7 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
    * Accessor for all the Search Components
    * @return An unmodifiable Map of Search Components
    */
  public PluginRegistry<SearchComponent> getSearchComponents() {
  public PluginBag<SearchComponent> getSearchComponents() {
     return searchComponents;
   }
 
@@ -2050,11 +2050,11 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     SolrException.log(log,null,e);
   }
 
  public PluginRegistry<QueryResponseWriter> getResponseWriters() {
  public PluginBag<QueryResponseWriter> getResponseWriters() {
     return responseWriters;
   }
 
  private final PluginRegistry<QueryResponseWriter> responseWriters = new PluginRegistry<>(QueryResponseWriter.class, this);
  private final PluginBag<QueryResponseWriter> responseWriters = new PluginBag<>(QueryResponseWriter.class, this);
   public static final Map<String ,QueryResponseWriter> DEFAULT_RESPONSE_WRITERS ;
   static{
     HashMap<String, QueryResponseWriter> m= new HashMap<>();
@@ -2119,15 +2119,15 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
   }
 
 
  private final PluginRegistry<QParserPlugin> qParserPlugins = new PluginRegistry<>(QParserPlugin.class, this);
  private final PluginBag<QParserPlugin> qParserPlugins = new PluginBag<>(QParserPlugin.class, this);
 
   public QParserPlugin getQueryPlugin(String parserName) {
     return qParserPlugins.get(parserName);
   }
 
  private final PluginRegistry<ValueSourceParser> valueSourceParsers = new PluginRegistry<>(ValueSourceParser.class, this);
  private final PluginBag<ValueSourceParser> valueSourceParsers = new PluginBag<>(ValueSourceParser.class, this);
 
  private final PluginRegistry<TransformerFactory> transformerFactories = new PluginRegistry<>(TransformerFactory.class, this);
  private final PluginBag<TransformerFactory> transformerFactories = new PluginBag<>(TransformerFactory.class, this);
 
   <T> Map<String, T> createInstances(Map<String, Class<? extends T>> map) {
     Map<String, T> result = new LinkedHashMap<>();
diff --git a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
index a1391dbc113..c79cea69035 100644
-- a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
++ b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
@@ -22,7 +22,7 @@ import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.PluginRegistry;
import org.apache.solr.core.PluginBag;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrInfoMBean;
 import org.apache.solr.request.SolrQueryRequest;
@@ -35,7 +35,6 @@ import org.apache.solr.util.stats.Timer;
 import org.apache.solr.util.stats.TimerContext;
 
 import java.net.URL;
import java.util.Map;
 import java.util.concurrent.atomic.AtomicLong;
 
 import static org.apache.solr.core.RequestParams.USEPARAM;
@@ -214,7 +213,7 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
    *
    * This function is thread safe.
    */
  public static SolrRequestHandler getRequestHandler(String handlerName, PluginRegistry<SolrRequestHandler> reqHandlers) {
  public static SolrRequestHandler getRequestHandler(String handlerName, PluginBag<SolrRequestHandler> reqHandlers) {
     if(handlerName == null) return null;
     SolrRequestHandler handler = reqHandlers.get(handlerName);
     int idx = 0;
diff --git a/solr/core/src/test/org/apache/solr/OutputWriterTest.java b/solr/core/src/test/org/apache/solr/OutputWriterTest.java
index f58b6cafbeb..26023758481 100644
-- a/solr/core/src/test/org/apache/solr/OutputWriterTest.java
++ b/solr/core/src/test/org/apache/solr/OutputWriterTest.java
@@ -22,7 +22,7 @@ import java.io.Writer;
 
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginRegistry;
import org.apache.solr.core.PluginBag;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.QueryResponseWriter;
 import org.apache.solr.response.SolrQueryResponse;
@@ -91,12 +91,12 @@ public class OutputWriterTest extends SolrTestCaseJ4 {
     }
 
     public void testLazy() {
        PluginRegistry.PluginHolder<QueryResponseWriter> qrw = h.getCore().getResponseWriters().getRegistry().get("useless");
        assertTrue("Should be a lazy class", qrw instanceof PluginRegistry.LazyPluginHolder);
        PluginBag.PluginHolder<QueryResponseWriter> qrw = h.getCore().getResponseWriters().getRegistry().get("useless");
        assertTrue("Should be a lazy class", qrw instanceof PluginBag.LazyPluginHolder);
 
         qrw = h.getCore().getResponseWriters().getRegistry().get("xml");
         assertTrue("Should not be a lazy class", qrw.isLoaded());
        assertTrue("Should not be a lazy class", qrw.getClass() == PluginRegistry.PluginHolder.class);
        assertTrue("Should not be a lazy class", qrw.getClass() == PluginBag.PluginHolder.class);
 
     }
     
diff --git a/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java b/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
index 815aa6a479a..6d855dde1b0 100644
-- a/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
++ b/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
@@ -48,7 +48,7 @@ public class RequestHandlersTest extends SolrTestCaseJ4 {
   @Test
   public void testLazyLoading() {
     SolrCore core = h.getCore();
    PluginRegistry.PluginHolder<SolrRequestHandler> handler = core.getRequestHandlers().getRegistry().get("lazy");
    PluginBag.PluginHolder<SolrRequestHandler> handler = core.getRequestHandlers().getRegistry().get("lazy");
     assertFalse(handler.isLoaded());
     
     assertU(adoc("id", "42",
- 
2.19.1.windows.1

