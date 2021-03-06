From 9ba13dea312afdad1fb7d288976062bc23583d69 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sun, 12 Apr 2015 13:24:23 +0000
Subject: [PATCH] SOLR-7380: SearchHandler should not try to load runtime
 components in inform()

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1673007 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  2 +
 .../java/org/apache/solr/core/PluginBag.java  | 12 ++++
 .../solr/handler/component/SearchHandler.java | 62 ++++++++++++++-----
 3 files changed, 60 insertions(+), 16 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index c4d76fc5576..7ea72018653 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -106,6 +106,8 @@ Bug Fixes
 
 * SOLR-7369: AngularJS UI insufficient URLDecoding in cloud/tree view (janhoy)
 
* SOLR-7380: SearchHandler should not try to load runtime components in inform() (Noble Paul)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/PluginBag.java b/solr/core/src/java/org/apache/solr/core/PluginBag.java
index 31fcd7924ed..9c550074005 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginBag.java
++ b/solr/core/src/java/org/apache/solr/core/PluginBag.java
@@ -21,8 +21,10 @@ package org.apache.solr.core;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.ArrayList;
import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
@@ -99,6 +101,16 @@ public class PluginBag<T> implements AutoCloseable {
 
   }
 
  /**
   * Check if any of the mentioned names are missing. If yes, return the Set of missing names
   */
  public Set<String> checkContains(Collection<String> names) {
    if (names == null || names.isEmpty()) return Collections.EMPTY_SET;
    HashSet<String> result = new HashSet<>();
    for (String s : names) if (!this.registry.containsKey(s)) result.add(s);
    return result;
  }

   PluginHolder<T> createPlugin(PluginInfo info) {
     if ("true".equals(String.valueOf(info.attributes.get("runtimeLib")))) {
       log.info(" {} : '{}'  created with runtimeLib=true ", meta.getCleanTag(), info.name);
diff --git a/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java b/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java
index b2b0579e91c..2c1346a52a5 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java
@@ -20,8 +20,10 @@ package org.apache.solr.handler.component;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.ArrayList;
import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
import java.util.Set;
 
 import org.apache.lucene.index.ExitableDirectoryReader;
 import org.apache.lucene.util.Version;
@@ -33,6 +35,7 @@ import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.ShardParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.core.CloseHook;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
@@ -71,6 +74,7 @@ public class SearchHandler extends RequestHandlerBase implements SolrCoreAware ,
   protected List<SearchComponent> components = null;
   private ShardHandlerFactory shardHandlerFactory ;
   private PluginInfo shfInfo;
  private SolrCore core;
 
   protected List<String> getDefaultComponents()
   {
@@ -106,6 +110,38 @@ public class SearchHandler extends RequestHandlerBase implements SolrCoreAware ,
   @SuppressWarnings("unchecked")
   public void inform(SolrCore core)
   {
    this.core = core;
    Set<String> missing = new HashSet<>();
    List<String> c = (List<String>) initArgs.get(INIT_COMPONENTS);
    missing.addAll(core.getSearchComponents().checkContains(c));
    List<String> first = (List<String>) initArgs.get(INIT_FIRST_COMPONENTS);
    missing.addAll(core.getSearchComponents().checkContains(first));
    List<String> last = (List<String>) initArgs.get(INIT_LAST_COMPONENTS);
    missing.addAll(core.getSearchComponents().checkContains(last));
    if (!missing.isEmpty()) throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
        "Missing SearchComponents named : " + missing);
    if (c != null && (first != null || last != null)) throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
        "First/Last components only valid if you do not declare 'components'");

    if (shfInfo == null) {
      shardHandlerFactory = core.getCoreDescriptor().getCoreContainer().getShardHandlerFactory();
    } else {
      shardHandlerFactory = core.createInitInstance(shfInfo, ShardHandlerFactory.class, null, null);
      core.addCloseHook(new CloseHook() {
        @Override
        public void preClose(SolrCore core) {
          shardHandlerFactory.close();
        }

        @Override
        public void postClose(SolrCore core) {
        }
      });
    }

  }

  private void initComponents() {
     Object declaredComponents = initArgs.get(INIT_COMPONENTS);
     List<String> first = (List<String>) initArgs.get(INIT_FIRST_COMPONENTS);
     List<String> last  = (List<String>) initArgs.get(INIT_LAST_COMPONENTS);
@@ -136,7 +172,7 @@ public class SearchHandler extends RequestHandlerBase implements SolrCoreAware ,
     }
 
     // Build the component list
    components = new ArrayList<>( list.size() );
    List<SearchComponent> components = new ArrayList<>(list.size());
     DebugComponent dbgCmp = null;
     for(String c : list){
       SearchComponent comp = core.getSearchComponent( c );
@@ -151,30 +187,24 @@ public class SearchHandler extends RequestHandlerBase implements SolrCoreAware ,
       components.add(dbgCmp);
       log.debug("Adding  debug component:" + dbgCmp);
     }
    if(shfInfo ==null) {
      shardHandlerFactory = core.getCoreDescriptor().getCoreContainer().getShardHandlerFactory();
    } else {
      shardHandlerFactory = core.createInitInstance(shfInfo, ShardHandlerFactory.class, null, null);
      core.addCloseHook(new CloseHook() {
        @Override
        public void preClose(SolrCore core) {
          shardHandlerFactory.close();
        }
        @Override
        public void postClose(SolrCore core) {
        }
      });
    }

    this.components = components;
   }
 
   public List<SearchComponent> getComponents() {
    if (components == null) {
      synchronized (this) {
        if (components == null) {
          initComponents();
        }
      }
    }
     return components;
   }
 
   @Override
   public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception
   {
    if (components == null) getComponents();
     ResponseBuilder rb = new ResponseBuilder(req, rsp, components);
     if (rb.requestInfo != null) {
       rb.requestInfo.setResponseBuilder(rb);
- 
2.19.1.windows.1

