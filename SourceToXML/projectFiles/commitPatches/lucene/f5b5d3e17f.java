From f5b5d3e17f6ca8b649ef1364726726a0b92d9921 Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Mon, 17 Dec 2012 00:38:03 +0000
Subject: [PATCH] SOLR-4204: Make SolrCloud tests more friendly to FreeBSD
 blackhole 2 environments.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1422728 13f79535-47bb-0310-9956-ffa450edef68
--
 .../TestSolrEntityProcessorEndToEnd.java      |  2 +
 .../solrj/embedded/JettySolrRunner.java       |  4 +-
 .../apache/solr/cloud/RecoveryStrategy.java   |  4 +-
 .../org/apache/solr/cloud/ZkController.java   | 47 +++++++------
 .../org/apache/solr/core/CoreContainer.java   | 68 +++++++++++-------
 .../java/org/apache/solr/core/PluginInfo.java |  2 +-
 .../handler/admin/CollectionsHandler.java     |  2 +
 .../component/HttpShardHandlerFactory.java    |  8 ++-
 .../solr/update/SolrCmdDistributor.java       | 25 +++----
 .../org/apache/solr/update/UpdateLog.java     |  3 +-
 .../solr/update/UpdateShardHandler.java       | 69 +++++++++++++++++++
 .../processor/DistributedUpdateProcessor.java |  2 +-
 solr/core/src/test-files/solr/solr.xml        | 10 ++-
 .../solr/cloud/BasicDistributedZk2Test.java   | 38 +++-------
 .../solr/cloud/BasicDistributedZkTest.java    | 26 +++++--
 .../cloud/ChaosMonkeyNothingIsSafeTest.java   |  2 +
 .../cloud/FullSolrCloudDistribCmdsTest.java   |  3 +-
 .../org/apache/solr/cloud/SyncSliceTest.java  | 31 +++------
 .../apache/solr/cloud/ZkControllerTest.java   | 14 ++--
 .../solr/handler/TestReplicationHandler.java  |  2 +
 .../solr/update/SolrCmdDistributorTest.java   | 22 ++----
 .../impl/ConcurrentUpdateSolrServer.java      | 12 ++++
 .../solr/BaseDistributedSearchTestCase.java   |  3 +-
 .../java/org/apache/solr/SolrTestCaseJ4.java  |  2 +-
 .../cloud/AbstractFullDistribZkTestBase.java  | 31 ++-------
 .../org/apache/solr/util/TestHarness.java     |  1 +
 26 files changed, 251 insertions(+), 182 deletions(-)
 create mode 100644 solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java

diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java
index 082a74c9312..15a21c771ea 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java
@@ -267,6 +267,8 @@ public class TestSolrEntityProcessorEndToEnd extends AbstractDataImportHandlerTe
     }
     
     HttpSolrServer solrServer = new HttpSolrServer(getSourceUrl());
    solrServer.setConnectionTimeout(15000);
    solrServer.setSoTimeout(30000);
     solrServer.add(sidl);
     solrServer.commit(true, true);
   }
diff --git a/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java b/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java
index 56e995db00b..7260326ef31 100644
-- a/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java
++ b/solr/core/src/java/org/apache/solr/client/solrj/embedded/JettySolrRunner.java
@@ -176,9 +176,7 @@ public class JettySolrRunner {
       if (threadPool != null) {
         threadPool.setMaxThreads(10000);
         threadPool.setMaxIdleTimeMs(5000);
        if (!stopAtShutdown) {
          threadPool.setMaxStopTimeMs(100);
        }
        threadPool.setMaxStopTimeMs(30000);
       }
       
       server.setConnectors(new Connector[] {connector});
diff --git a/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java b/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java
index 14775a8dc1d..3f5eec7698b 100644
-- a/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java
++ b/solr/core/src/java/org/apache/solr/cloud/RecoveryStrategy.java
@@ -175,7 +175,7 @@ public class RecoveryStrategy extends Thread implements ClosableThread {
   private void commitOnLeader(String leaderUrl) throws SolrServerException, IOException {
     HttpSolrServer server = new HttpSolrServer(leaderUrl);
     server.setConnectionTimeout(30000);
    server.setSoTimeout(30000);
    server.setSoTimeout(60000);
     UpdateRequest ureq = new UpdateRequest();
     ureq.setParams(new ModifiableSolrParams());
     ureq.getParams().set(DistributedUpdateProcessor.COMMIT_END_POINT, true);
@@ -190,7 +190,7 @@ public class RecoveryStrategy extends Thread implements ClosableThread {
       IOException {
     HttpSolrServer server = new HttpSolrServer(leaderBaseUrl);
     server.setConnectionTimeout(45000);
    server.setSoTimeout(45000);
    server.setSoTimeout(120000);
     WaitForState prepCmd = new WaitForState();
     prepCmd.setCoreName(leaderCoreName);
     prepCmd.setNodeName(zkController.getNodeName());
diff --git a/solr/core/src/java/org/apache/solr/cloud/ZkController.java b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
index a9f6a7a0778..3eedaa35e30 100644
-- a/solr/core/src/java/org/apache/solr/cloud/ZkController.java
++ b/solr/core/src/java/org/apache/solr/cloud/ZkController.java
@@ -21,8 +21,8 @@ import java.io.File;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.InetAddress;
import java.net.URLEncoder;
 import java.net.NetworkInterface;
import java.net.URLEncoder;
 import java.util.Collections;
 import java.util.Enumeration;
 import java.util.HashMap;
@@ -31,9 +31,6 @@ import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
@@ -55,16 +52,14 @@ import org.apache.solr.common.cloud.ZkNodeProps;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.cloud.ZooKeeperException;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ExecutorUtil;

 import org.apache.solr.core.Config;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.CoreDescriptor;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.handler.component.ShardHandler;
 import org.apache.solr.update.UpdateLog;
import org.apache.solr.update.UpdateShardHandler;
 import org.apache.solr.util.DOMUtil;
import org.apache.solr.util.DefaultSolrThreadFactory;
 import org.apache.zookeeper.CreateMode;
 import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.KeeperException.NoNodeException;
@@ -104,10 +99,6 @@ public final class ZkController {
   public final static String COLLECTION_PARAM_PREFIX="collection.";
   public final static String CONFIGNAME_PROP="configName";
 
  private ThreadPoolExecutor cmdDistribExecutor = new ThreadPoolExecutor(
      0, Integer.MAX_VALUE, 5, TimeUnit.SECONDS,
      new SynchronousQueue<Runnable>(), new DefaultSolrThreadFactory(
          "cmdDistribExecutor"));
   
   private final Map<String, ElectionContext> electionContexts = Collections.synchronizedMap(new HashMap<String, ElectionContext>());
   
@@ -141,9 +132,11 @@ public final class ZkController {
   private int clientTimeout;
 
   private volatile boolean isClosed;
  
  private UpdateShardHandler updateShardHandler;
 
   public ZkController(final CoreContainer cc, String zkServerAddress, int zkClientTimeout, int zkClientConnectTimeout, String localHost, String locaHostPort,
      String localHostContext, String leaderVoteWait, final CurrentCoreDescriptorProvider registerOnReconnect) throws InterruptedException,
      String localHostContext, String leaderVoteWait, int distribUpdateConnTimeout, int distribUpdateSoTimeout, final CurrentCoreDescriptorProvider registerOnReconnect) throws InterruptedException,
       TimeoutException, IOException {
     if (cc == null) throw new IllegalArgumentException("CoreContainer cannot be null.");
     this.cc = cc;
@@ -159,6 +152,7 @@ public final class ZkController {
       localHostContext = localHostContext.substring(0,localHostContext.length()-1);
     }
     
    updateShardHandler = new UpdateShardHandler(distribUpdateConnTimeout, distribUpdateSoTimeout);
     
     this.zkServerAddress = zkServerAddress;
     this.localHostPort = locaHostPort;
@@ -315,14 +309,6 @@ public final class ZkController {
   public void close() {
     this.isClosed = true;
     
    if (cmdDistribExecutor != null) {
      try {
        ExecutorUtil.shutdownNowAndAwaitTermination(cmdDistribExecutor);
      } catch (Throwable e) {
        SolrException.log(log, e);
      }
    }
    
     for (ElectionContext context : electionContexts.values()) {
       try {
         context.close();
@@ -336,7 +322,20 @@ public final class ZkController {
     } catch(Throwable t) {
       log.error("Error closing overseer", t);
     }
    zkClient.close();
    
    try {
      zkClient.close();;
    } catch(Throwable t) {
      log.error("Error closing zkClient", t);
    } 
    
    if (updateShardHandler != null) {
      try {
        updateShardHandler.close();
      } catch(Throwable t) {
        log.error("Error closing updateShardHandler", t);
      }
    }
   }
 
   /**
@@ -1178,7 +1177,7 @@ public final class ZkController {
       HttpSolrServer server = null;
       server = new HttpSolrServer(leaderBaseUrl);
       server.setConnectionTimeout(45000);
      server.setSoTimeout(45000);
      server.setSoTimeout(120000);
       WaitForState prepCmd = new WaitForState();
       prepCmd.setCoreName(leaderCoreName);
       prepCmd.setNodeName(getNodeName());
@@ -1293,8 +1292,8 @@ public final class ZkController {
   }
 
   // may return null if not in zk mode
  public ThreadPoolExecutor getCmdDistribExecutor() {
    return cmdDistribExecutor;
  public UpdateShardHandler getUpdateShardHandler() {
    return updateShardHandler;
   }
 
   /**
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index 321616fd6cd..aef07cf61ad 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -159,6 +159,8 @@ public class CoreContainer
   private String zkHost;
   private Map<SolrCore,String> coreToOrigName = new ConcurrentHashMap<SolrCore,String>();
   private String leaderVoteWait = LEADER_VOTE_WAIT;
  private int distribUpdateConnTimeout = 0;
  private int distribUpdateSoTimeout = 0;
   protected int swappableCacheSize = Integer.MAX_VALUE; // Use as a flag too, if swappableCacheSize set in solr.xml this will be changed
   private int coreLoadThreads;
   
@@ -250,18 +252,21 @@ public class CoreContainer
               "A chroot was specified in ZkHost but the znode doesn't exist. ");
         }
         
        zkController = new ZkController(this, zookeeperHost, zkClientTimeout, zkClientConnectTimeout, host, hostPort, hostContext, leaderVoteWait, new CurrentCoreDescriptorProvider() {
          
          @Override
          public List<CoreDescriptor> getCurrentDescriptors() {
            List<CoreDescriptor> descriptors = new ArrayList<CoreDescriptor>(getCoreNames().size());
            for (SolrCore core : getCores()) {
              descriptors.add(core.getCoreDescriptor());
            }
            return descriptors;
          }
        });        

        zkController = new ZkController(this, zookeeperHost, zkClientTimeout,
            zkClientConnectTimeout, host, hostPort, hostContext,
            leaderVoteWait, distribUpdateConnTimeout, distribUpdateSoTimeout,
            new CurrentCoreDescriptorProvider() {
              
              @Override
              public List<CoreDescriptor> getCurrentDescriptors() {
                List<CoreDescriptor> descriptors = new ArrayList<CoreDescriptor>(
                    getCoreNames().size());
                for (SolrCore core : getCores()) {
                  descriptors.add(core.getCoreDescriptor());
                }
                return descriptors;
              }
            });
         
         
         if (zkRun != null && zkServer.getServers().size() > 1 && confDir == null && boostrapConf == false) {
@@ -428,6 +433,8 @@ public class CoreContainer
     // now.
     cfg.substituteProperties();
     
    initShardHandler(cfg);
    
     allocateLazyCores(cfg);
     
     // Initialize Logging
@@ -487,6 +494,9 @@ public class CoreContainer
     zkClientTimeout = cfg.getInt("solr/cores/@zkClientTimeout",
         DEFAULT_ZK_CLIENT_TIMEOUT);
     
    distribUpdateConnTimeout = cfg.getInt("solr/cores/@distribUpdateConnTimeout", 0);
    distribUpdateSoTimeout = cfg.getInt("solr/cores/@distribUpdateSoTimeout", 0);
    
     hostPort = cfg.get("solr/cores/@hostPort", DEFAULT_HOST_PORT);
     
     hostContext = cfg.get("solr/cores/@hostContext", DEFAULT_HOST_CONTEXT);
@@ -689,6 +699,27 @@ public class CoreContainer
     }
   }
 
  protected void initShardHandler(Config cfg) {
    PluginInfo info = null;
    if (cfg != null) {
      Node shfn = cfg.getNode("solr/cores/shardHandlerFactory", false);
  
      if (shfn != null) {
        info = new PluginInfo(shfn, "shardHandlerFactory", false, true);
      } else {
        Map m = new HashMap();
        m.put("class",HttpShardHandlerFactory.class.getName());
        info = new PluginInfo("shardHandlerFactory", m, null, Collections.<PluginInfo>emptyList());
      }
    }

    HttpShardHandlerFactory fac = new HttpShardHandlerFactory();
    if (info != null) {
      fac.init(info);
    }
    shardHandlerFactory = fac;
  }

   private Document copyDoc(Document document) throws TransformerException {
     TransformerFactory tfactory = TransformerFactory.newInstance();
     Transformer tx   = tfactory.newTransformer();
@@ -1568,18 +1599,7 @@ public class CoreContainer
 
   /** The default ShardHandlerFactory used to communicate with other solr instances */
   public ShardHandlerFactory getShardHandlerFactory() {
    synchronized (this) {
      if (shardHandlerFactory == null) {
        Map m = new HashMap();
        m.put("class",HttpShardHandlerFactory.class.getName());
        PluginInfo info = new PluginInfo("shardHandlerFactory", m,null,Collections.<PluginInfo>emptyList());

        HttpShardHandlerFactory fac = new HttpShardHandlerFactory();
        fac.init(info);
        shardHandlerFactory = fac;
      }
      return shardHandlerFactory;
    }
    return shardHandlerFactory;
   }
   
   private SolrConfig getSolrConfigFromZk(String zkConfigName, String solrConfigFileName,
diff --git a/solr/core/src/java/org/apache/solr/core/PluginInfo.java b/solr/core/src/java/org/apache/solr/core/PluginInfo.java
index c6b6e3b1cac..2ecb617ad7d 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginInfo.java
++ b/solr/core/src/java/org/apache/solr/core/PluginInfo.java
@@ -74,7 +74,7 @@ public class PluginInfo {
     if (type != null) sb.append("type = " + type + ",");
     if (name != null) sb.append("name = " + name + ",");
     if (className != null) sb.append("class = " + className + ",");
    if (initArgs.size() > 0) sb.append("args = " + initArgs);
    if (initArgs != null && initArgs.size() > 0) sb.append("args = " + initArgs);
     sb.append("}");
     return sb.toString();
   }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
index a4391a87fd2..1919c090775 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CollectionsHandler.java
@@ -150,6 +150,8 @@ public class CollectionsHandler extends RequestHandlerBase {
     ZkCoreNodeProps nodeProps = new ZkCoreNodeProps(leaderProps);
     
     HttpSolrServer server = new HttpSolrServer(nodeProps.getBaseUrl());
    server.setConnectionTimeout(15000);
    server.setSoTimeout(30000);
     RequestSyncShard reqSyncShard = new CoreAdminRequest.RequestSyncShard();
     reqSyncShard.setCollection(collection);
     reqSyncShard.setShard(shard);
diff --git a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
index ba847a88128..6e5be383f88 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
++ b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
@@ -156,19 +156,21 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements Plug
   @Override
   public void close() {
     try {
      defaultClient.getConnectionManager().shutdown();
      ExecutorUtil.shutdownNowAndAwaitTermination(commExecutor);
     } catch (Throwable e) {
       SolrException.log(log, e);
     }
    
     try {
      loadbalancer.shutdown();
      defaultClient.getConnectionManager().shutdown();
     } catch (Throwable e) {
       SolrException.log(log, e);
     }
     try {
      ExecutorUtil.shutdownNowAndAwaitTermination(commExecutor);
      loadbalancer.shutdown();
     } catch (Throwable e) {
       SolrException.log(log, e);
     }

   }
 }
diff --git a/solr/core/src/java/org/apache/solr/update/SolrCmdDistributor.java b/solr/core/src/java/org/apache/solr/update/SolrCmdDistributor.java
index 759802e8831..7d1c68b6721 100644
-- a/solr/core/src/java/org/apache/solr/update/SolrCmdDistributor.java
++ b/solr/core/src/java/org/apache/solr/update/SolrCmdDistributor.java
@@ -30,11 +30,8 @@ import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorCompletionService;
 import java.util.concurrent.Future;
 import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
 
import org.apache.http.client.HttpClient;
 import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
 import org.apache.solr.client.solrj.impl.HttpSolrServer;
 import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
 import org.apache.solr.client.solrj.request.UpdateRequestExt;
@@ -50,19 +47,11 @@ import org.slf4j.LoggerFactory;
 
 
 public class SolrCmdDistributor {
  private static final int MAX_RETRIES_ON_FORWARD = 10;
  private static final int MAX_RETRIES_ON_FORWARD = 15;
   public static Logger log = LoggerFactory.getLogger(SolrCmdDistributor.class);
 
  static final HttpClient client;
   static AdjustableSemaphore semaphore = new AdjustableSemaphore(8);
   
  static {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 500);
    params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 16);
    client = HttpClientUtil.createClient(params);
  }
  
   CompletionService<Request> completionService;
   Set<Future<Request>> pending;
   
@@ -73,6 +62,7 @@ public class SolrCmdDistributor {
   
   private final Map<Node,List<AddRequest>> adds = new HashMap<Node,List<AddRequest>>();
   private final Map<Node,List<DeleteRequest>> deletes = new HashMap<Node,List<DeleteRequest>>();
  private UpdateShardHandler updateShardHandler;
   
   class AddRequest {
     AddUpdateCommand cmd;
@@ -88,14 +78,15 @@ public class SolrCmdDistributor {
     public boolean abortCheck();
   }
   
  public SolrCmdDistributor(int numHosts, ThreadPoolExecutor executor) {
  public SolrCmdDistributor(int numHosts, UpdateShardHandler updateShardHandler) {
     int maxPermits = Math.max(16, numHosts * 16);
     // limits how many tasks can actually execute at once
     if (maxPermits != semaphore.getMaxPermits()) {
       semaphore.setMaxPermits(maxPermits);
     }

    completionService = new ExecutorCompletionService<Request>(executor);
    
    this.updateShardHandler = updateShardHandler;
    completionService = new ExecutorCompletionService<Request>(updateShardHandler.getCmdDistribExecutor());
     pending = new HashSet<Future<Request>>();
   }
   
@@ -329,7 +320,7 @@ public class SolrCmdDistributor {
           }
   
           HttpSolrServer server = new HttpSolrServer(fullUrl,
              client);
              updateShardHandler.getHttpClient());
           
           if (Thread.currentThread().isInterrupted()) {
             clonedRequest.rspCode = 503;
@@ -363,7 +354,7 @@ public class SolrCmdDistributor {
       pending.add(completionService.submit(task));
     } catch (RejectedExecutionException e) {
       semaphore.release();
      throw e;
      throw new SolrException(ErrorCode.SERVICE_UNAVAILABLE, "Shutting down", e);
     }
     
   }
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateLog.java b/solr/core/src/java/org/apache/solr/update/UpdateLog.java
index 8e2b562ad02..ffa8f8ce19c 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateLog.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateLog.java
@@ -23,6 +23,7 @@ import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
@@ -819,7 +820,7 @@ public class UpdateLog implements PluginInfoInitialized {
   public void close(boolean committed, boolean deleteOnClose) {
     synchronized (this) {
       try {
        recoveryExecutor.shutdownNow();
        ExecutorUtil.shutdownNowAndAwaitTermination(recoveryExecutor);
       } catch (Exception e) {
         SolrException.log(log, e);
       }
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
new file mode 100644
index 00000000000..c3d1499c8d1
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
@@ -0,0 +1,69 @@
package org.apache.solr.update;

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

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.util.DefaultSolrThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateShardHandler {
  
  private static Logger log = LoggerFactory.getLogger(UpdateShardHandler.class);
  
  private ThreadPoolExecutor cmdDistribExecutor = new ThreadPoolExecutor(0,
      Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
      new DefaultSolrThreadFactory("cmdDistribExecutor"));
  
  private final HttpClient client;

  public UpdateShardHandler(int distribUpdateConnTimeout, int distribUpdateSoTimeout) {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 500);
    params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 16);
    params.set(HttpClientUtil.PROP_SO_TIMEOUT, distribUpdateConnTimeout);
    params.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, distribUpdateSoTimeout);
    client = HttpClientUtil.createClient(params);
  }
  
  
  public HttpClient getHttpClient() {
    return client;
  }
  
  public ThreadPoolExecutor getCmdDistribExecutor() {
    return cmdDistribExecutor;
  }

  public void close() {
    try {
      ExecutorUtil.shutdownNowAndAwaitTermination(cmdDistribExecutor);
    } catch (Throwable e) {
      SolrException.log(log, e);
    }
    client.getConnectionManager().shutdown();
  }
}
diff --git a/solr/core/src/java/org/apache/solr/update/processor/DistributedUpdateProcessor.java b/solr/core/src/java/org/apache/solr/update/processor/DistributedUpdateProcessor.java
index 44cfbbfefca..f48ceefe38f 100644
-- a/solr/core/src/java/org/apache/solr/update/processor/DistributedUpdateProcessor.java
++ b/solr/core/src/java/org/apache/solr/update/processor/DistributedUpdateProcessor.java
@@ -166,7 +166,7 @@ public class DistributedUpdateProcessor extends UpdateRequestProcessor {
     zkController = req.getCore().getCoreDescriptor().getCoreContainer().getZkController();
     if (zkEnabled) {
       numNodes =  zkController.getZkStateReader().getClusterState().getLiveNodes().size();
      cmdDistrib = new SolrCmdDistributor(numNodes, coreDesc.getCoreContainer().getZkController().getCmdDistribExecutor());
      cmdDistrib = new SolrCmdDistributor(numNodes, coreDesc.getCoreContainer().getZkController().getUpdateShardHandler());
     }
     //this.rsp = reqInfo != null ? reqInfo.getRsp() : null;
 
diff --git a/solr/core/src/test-files/solr/solr.xml b/solr/core/src/test-files/solr/solr.xml
index c146f6af167..0123502db7d 100644
-- a/solr/core/src/test-files/solr/solr.xml
++ b/solr/core/src/test-files/solr/solr.xml
@@ -29,7 +29,15 @@
     If 'null' (or absent), cores will not be manageable via request handler
   -->
   <cores adminPath="/admin/cores" defaultCoreName="collection1" host="127.0.0.1" hostPort="${hostPort:8983}" 
         hostContext="${hostContext:solr}" zkClientTimeout="8000" numShards="${numShards:3}" shareSchema="${shareSchema:false}">
         hostContext="${hostContext:solr}" zkClientTimeout="30000" numShards="${numShards:3}" shareSchema="${shareSchema:false}" 
         distribUpdateConnTimeout="${distribUpdateConnTimeout:15000}" distribUpdateSoTimeout="${distribUpdateSoTimeout:30000}">
     <core name="collection1" instanceDir="collection1" shard="${shard:}" collection="${collection:collection1}" config="${solrconfig:solrconfig.xml}" schema="${schema:schema.xml}"/>
    <shardHandlerFactory name="shardHandlerFactory" class="HttpShardHandlerFactory">
      <int name="socketTimeout">${socketTimeout:30000}</int>
      <int name="connTimeout">${connTimeout:15000}</int>
    </shardHandlerFactory>
   </cores>
  
    

 </solr>
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
index 943e2799d6b..fba1c467b42 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
@@ -200,26 +200,6 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
     jetties.addAll(shardToJetty.get(SHARD2));
     jetties.remove(deadShard);
     
    // wait till live nodes drops by 1
    int liveNodes = cloudClient.getZkStateReader().getZkClient().getChildren(ZkStateReader.LIVE_NODES_ZKNODE, null, true).size();
    int tries = 50;
    while(oldLiveNodes == liveNodes) {
      Thread.sleep(100);
      if (tries-- == 0) {
        fail("We expected a node to drop...");
      }
      liveNodes = cloudClient.getZkStateReader().getZkClient().getChildren(ZkStateReader.LIVE_NODES_ZKNODE, null, true).size();
    }
    assertEquals(4, liveNodes);

    int cnt = 0;
    for (CloudJettyRunner cjetty : jetties) {
      waitToSeeNotLive(((SolrDispatchFilter) cjetty.jetty.getDispatchFilter()
          .getFilter()).getCores().getZkController().getZkStateReader(),
          deadShard, cnt++);
    }
    waitToSeeNotLive(cloudClient.getZkStateReader(), deadShard);

     // ensure shard is dead
     try {
       index_specific(deadShard.client.solrClient, id, 999, i1, 107, t1,
@@ -269,7 +249,14 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
     UpdateRequest ureq = new UpdateRequest();
     ureq.add(doc);
     // ureq.setParam("update.chain", DISTRIB_UPDATE_CHAIN);
    ureq.process(cloudClient);
    
    try {
      ureq.process(cloudClient);
    } catch(SolrServerException e){
      // try again
      Thread.sleep(500);
      ureq.process(cloudClient);
    }
     
     commit();
     
@@ -319,14 +306,7 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
     
     // recover over 100 docs so we do more than just peer sync (replicate recovery)
     chaosMonkey.stopJetty(deadShard);
    
    for (CloudJettyRunner cjetty : jetties) {
      waitToSeeNotLive(((SolrDispatchFilter) cjetty.jetty.getDispatchFilter()
          .getFilter()).getCores().getZkController().getZkStateReader(),
          deadShard);
    }
    waitToSeeNotLive(cloudClient.getZkStateReader(), deadShard);
    

     for (int i = 0; i < 226; i++) {
       doc = new SolrInputDocument();
       doc.addField("id", 2000 + i);
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index 814138ca550..c34de311adb 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -375,7 +375,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     SolrServer client = clients.get(0);
     String url1 = getBaseUrl(client);
     HttpSolrServer server = new HttpSolrServer(url1);
    
    server.setConnectionTimeout(15000);
    server.setSoTimeout(15000);
     server.request(createCmd);
     
     createCmd = new Create();
@@ -437,6 +438,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     SolrServer client = clients.get(0);
     String url1 = getBaseUrl(client);
     HttpSolrServer server = new HttpSolrServer(url1);
    server.setConnectionTimeout(15000);
    server.setSoTimeout(30000);
     
     Create createCmd = new Create();
     createCmd.setCoreName("unloadcollection1");
@@ -627,7 +630,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     SolrServer client = clients.get(2);
     String url3 = getBaseUrl(client);
     final HttpSolrServer server = new HttpSolrServer(url3);
    
    server.setConnectionTimeout(15000);
    server.setSoTimeout(60000);
     ThreadPoolExecutor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
         5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
         new DefaultSolrThreadFactory("testExecutor"));
@@ -667,7 +671,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     SolrServer client = clients.get(0);
     String url3 = getBaseUrl(client);
     final HttpSolrServer server = new HttpSolrServer(url3);
    
    server.setConnectionTimeout(15000);
    server.setSoTimeout(30000);
     ThreadPoolExecutor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
         5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
         new DefaultSolrThreadFactory("testExecutor"));
@@ -791,7 +796,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
       String url = getUrlFromZk(collection);
 
       HttpSolrServer collectionClient = new HttpSolrServer(url);
      
      collectionClient.setConnectionTimeout(15000);
      collectionClient.setSoTimeout(30000);
       // poll for a second - it can take a moment before we are ready to serve
       waitForNon403or404or503(collectionClient);
     }
@@ -1291,6 +1297,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
   private Long getNumCommits(HttpSolrServer solrServer) throws
       SolrServerException, IOException {
     HttpSolrServer server = new HttpSolrServer(solrServer.getBaseURL());
    server.setConnectionTimeout(15000);
    server.setSoTimeout(30000);
     ModifiableSolrParams params = new ModifiableSolrParams();
     params.set("qt", "/admin/mbeans?key=updateHandler&stats=true");
     // use generic request to avoid extra processing of queries
@@ -1483,7 +1491,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
         HttpSolrServer server;
         try {
           server = new HttpSolrServer(baseUrl);
          
          server.setConnectionTimeout(15000);
          server.setSoTimeout(30000);
           Create createCmd = new Create();
           createCmd.setRoles("none");
           createCmd.setCoreName(collection + num);
@@ -1649,7 +1658,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
           HttpSolrServer server;
           try {
             server = new HttpSolrServer(baseUrl);
            
            server.setConnectionTimeout(15000);
            server.setSoTimeout(30000);
             Create createCmd = new Create();
             createCmd.setCoreName(collection);
             createCmd.setDataDir(dataDir.getAbsolutePath() + File.separator
@@ -1679,7 +1689,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     try {
       // setup the server...
       HttpSolrServer s = new HttpSolrServer(baseUrl + "/" + collection);
      s.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      s.setSoTimeout(30000);
       s.setDefaultMaxConnectionsPerHost(100);
       s.setMaxTotalConnections(100);
       return s;
@@ -1696,6 +1706,8 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
         try {
           commondCloudSolrServer = new CloudSolrServer(zkServer.getZkAddress());
           commondCloudSolrServer.setDefaultCollection(DEFAULT_COLLECTION);
          commondCloudSolrServer.getLbServer().setConnectionTimeout(15000);
          commondCloudSolrServer.getLbServer().setSoTimeout(30000);
           commondCloudSolrServer.connect();
         } catch (MalformedURLException e) {
           throw new RuntimeException(e);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
index d9698d6bde6..21c9d196169 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ChaosMonkeyNothingIsSafeTest.java
@@ -204,6 +204,8 @@ public class ChaosMonkeyNothingIsSafeTest extends AbstractFullDistribZkTestBase
       setName("FullThrottleStopableIndexingThread");
       setDaemon(true);
       this.clients = clients;
      HttpClientUtil.setConnectionTimeout(httpClient, 15000);
      HttpClientUtil.setSoTimeout(httpClient, 15000);
       suss = new ConcurrentUpdateSolrServer(
           ((HttpSolrServer) clients.get(0)).getBaseURL(), httpClient, 8,
           2) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/FullSolrCloudDistribCmdsTest.java b/solr/core/src/test/org/apache/solr/cloud/FullSolrCloudDistribCmdsTest.java
index 22898f63593..8ed0fa23f36 100644
-- a/solr/core/src/test/org/apache/solr/cloud/FullSolrCloudDistribCmdsTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/FullSolrCloudDistribCmdsTest.java
@@ -238,7 +238,8 @@ public class FullSolrCloudDistribCmdsTest extends AbstractFullDistribZkTestBase
   private void testIndexingWithSuss() throws Exception {
     ConcurrentUpdateSolrServer suss = new ConcurrentUpdateSolrServer(
         ((HttpSolrServer) clients.get(0)).getBaseURL(), 3, 1);
    
    suss.setConnectionTimeout(15000);
    suss.setSoTimeout(30000);
     for (int i=100; i<150; i++) {
       index_specific(suss, id, i);      
     }
diff --git a/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java b/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java
index fa6be408485..f51d86d0ead 100644
-- a/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/SyncSliceTest.java
@@ -33,7 +33,6 @@ import org.apache.solr.client.solrj.request.UpdateRequest;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.params.CollectionParams.CollectionAction;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.servlet.SolrDispatchFilter;
 import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.Before;
@@ -130,6 +129,8 @@ public class SyncSliceTest extends AbstractFullDistribZkTestBase {
     baseUrl = baseUrl.substring(0, baseUrl.length() - "collection1".length());
     
     HttpSolrServer baseServer = new HttpSolrServer(baseUrl);
    baseServer.setConnectionTimeout(15000);
    baseServer.setSoTimeout(30000);
     baseServer.request(request);
     
     waitForThingsToLevelOut(15);
@@ -157,12 +158,11 @@ public class SyncSliceTest extends AbstractFullDistribZkTestBase {
     
     chaosMonkey.killJetty(leaderJetty);
 
    // we are careful to make sure the downed node is no longer in the state,
    // because on some systems (especially freebsd w/ blackhole enabled), trying
    // to talk to a downed node causes grief
    waitToSeeDownInClusterState(leaderJetty, jetties);

    waitForThingsToLevelOut(45);
    Thread.sleep(2000);
    
    waitForThingsToLevelOut(90);
    
    Thread.sleep(1000);
     
     checkShardConsistency(false, true);
     
@@ -220,9 +220,11 @@ public class SyncSliceTest extends AbstractFullDistribZkTestBase {
     // kill the current leader
     chaosMonkey.killJetty(leaderJetty);
     
    waitToSeeDownInClusterState(leaderJetty, jetties);
    Thread.sleep(3000);
    
    waitForThingsToLevelOut(90);
     
    Thread.sleep(4000);
    Thread.sleep(2000);
     
     waitForRecoveriesToFinish(false);
 
@@ -251,17 +253,6 @@ public class SyncSliceTest extends AbstractFullDistribZkTestBase {
     skipServers.add(cjetty.url + "/");
     return skipServers;
   }

  private void waitToSeeDownInClusterState(CloudJettyRunner leaderJetty,
      Set<CloudJettyRunner> jetties) throws InterruptedException {

    for (CloudJettyRunner cjetty : jetties) {
      waitToSeeNotLive(((SolrDispatchFilter) cjetty.jetty.getDispatchFilter()
          .getFilter()).getCores().getZkController().getZkStateReader(),
          leaderJetty);
    }
    waitToSeeNotLive(cloudClient.getZkStateReader(), leaderJetty);
  }
   
   protected void indexDoc(List<String> skipServers, Object... fields) throws IOException,
       SolrServerException {
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
index a0409b0c589..a661fd48ec6 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
@@ -137,7 +137,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
       cc = getCoreContainer();
       
       ZkController zkController = new ZkController(cc, server.getZkAddress(), TIMEOUT, 10000,
          "127.0.0.1", "8983", "solr", "0", new CurrentCoreDescriptorProvider() {
          "127.0.0.1", "8983", "solr", "0", 10000, 10000, new CurrentCoreDescriptorProvider() {
             
             @Override
             public List<CoreDescriptor> getCurrentDescriptors() {
@@ -177,7 +177,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
       cc = getCoreContainer();
       
       zkController = new ZkController(cc, server.getZkAddress(),
          TIMEOUT, 10000, "127.0.0.1", "8983", "solr", "0", new CurrentCoreDescriptorProvider() {
          TIMEOUT, 10000, "127.0.0.1", "8983", "solr", "0", 10000, 10000, new CurrentCoreDescriptorProvider() {
             
             @Override
             public List<CoreDescriptor> getCurrentDescriptors() {
@@ -198,7 +198,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
       }
       testFinished = true;
     } finally {
      if (!testFinished) {
      if (!testFinished & zkController != null) {
         zkController.getZkClient().printLayoutToStdOut();
       }
       
@@ -214,7 +214,13 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
   }
 
   private CoreContainer getCoreContainer() {
    return new CoreContainer(TEMP_DIR.getAbsolutePath());
    CoreContainer cc = new CoreContainer(TEMP_DIR.getAbsolutePath()) {
      {
        initShardHandler(null);
      }
    };
    
    return cc;
   }
 
   @Override
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
index 2f335877f19..3d654334188 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
@@ -138,6 +138,8 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
       // setup the server...
       String url = "http://127.0.0.1:" + port + context;
       HttpSolrServer s = new HttpSolrServer(url);
      s.setConnectionTimeout(15000);
      s.setSoTimeout(30000);
       s.setDefaultMaxConnectionsPerHost(100);
       s.setMaxTotalConnections(100);
       return s;
diff --git a/solr/core/src/test/org/apache/solr/update/SolrCmdDistributorTest.java b/solr/core/src/test/org/apache/solr/update/SolrCmdDistributorTest.java
index 41fd6d5f744..47ff0496b3e 100644
-- a/solr/core/src/test/org/apache/solr/update/SolrCmdDistributorTest.java
++ b/solr/core/src/test/org/apache/solr/update/SolrCmdDistributorTest.java
@@ -21,9 +21,6 @@ import java.io.File;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import org.apache.solr.BaseDistributedSearchTestCase;
@@ -37,7 +34,6 @@ import org.apache.solr.common.cloud.ZkCoreNodeProps;
 import org.apache.solr.common.cloud.ZkNodeProps;
 import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.SolrCore;
@@ -48,10 +44,9 @@ import org.apache.solr.update.SolrCmdDistributor.Node;
 import org.apache.solr.update.SolrCmdDistributor.Response;
 import org.apache.solr.update.SolrCmdDistributor.StdNode;
 import org.apache.solr.update.processor.DistributedUpdateProcessor;
import org.apache.solr.util.DefaultSolrThreadFactory;
 
 public class SolrCmdDistributorTest extends BaseDistributedSearchTestCase {
  private ThreadPoolExecutor executor;
  private UpdateShardHandler updateShardHandler;
   
   public SolrCmdDistributorTest() {
     fixShardCount = true;
@@ -97,7 +92,7 @@ public class SolrCmdDistributorTest extends BaseDistributedSearchTestCase {
   public void doTest() throws Exception {
     del("*:*");
     
    SolrCmdDistributor cmdDistrib = new SolrCmdDistributor(5, executor);
    SolrCmdDistributor cmdDistrib = new SolrCmdDistributor(5, updateShardHandler);
     
     ModifiableSolrParams params = new ModifiableSolrParams();
 
@@ -137,7 +132,7 @@ public class SolrCmdDistributorTest extends BaseDistributedSearchTestCase {
     nodes.add(new StdNode(new ZkCoreNodeProps(nodeProps)));
     
     // add another 2 docs to control and 3 to client
    cmdDistrib = new SolrCmdDistributor(5, executor);
    cmdDistrib = new SolrCmdDistributor(5, updateShardHandler);
     cmd.solrDoc = sdoc("id", 2);
     params = new ModifiableSolrParams();
     params.set(DistributedUpdateProcessor.COMMIT_END_POINT, true);
@@ -180,7 +175,7 @@ public class SolrCmdDistributorTest extends BaseDistributedSearchTestCase {
     
     
 
    cmdDistrib = new SolrCmdDistributor(5, executor);
    cmdDistrib = new SolrCmdDistributor(5, updateShardHandler);
     
     params = new ModifiableSolrParams();
     params.set(DistributedUpdateProcessor.COMMIT_END_POINT, true);
@@ -213,7 +208,7 @@ public class SolrCmdDistributorTest extends BaseDistributedSearchTestCase {
     
     int id = 5;
     
    cmdDistrib = new SolrCmdDistributor(5, executor);
    cmdDistrib = new SolrCmdDistributor(5, updateShardHandler);
     
     int cnt = atLeast(303);
     for (int i = 0; i < cnt; i++) {
@@ -289,15 +284,12 @@ public class SolrCmdDistributorTest extends BaseDistributedSearchTestCase {
   @Override
   public void setUp() throws Exception {
     super.setUp();
    executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5,
        TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
        new DefaultSolrThreadFactory("cmdDistribExecutor"));
    updateShardHandler = new UpdateShardHandler(10000, 10000);
   }
   
   @Override
   public void tearDown() throws Exception {
    ExecutorUtil.shutdownNowAndAwaitTermination(executor);
    executor = null;
    updateShardHandler = null;
     super.tearDown();
   }
 }
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrServer.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrServer.java
index 27f177ea58f..75519267874 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrServer.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ConcurrentUpdateSolrServer.java
@@ -372,6 +372,18 @@ public class ConcurrentUpdateSolrServer extends SolrServer {
       Thread.currentThread().interrupt();
     }
   }
  
  public void setConnectionTimeout(int timeout) {
    HttpClientUtil.setConnectionTimeout(server.getHttpClient(), timeout);
  }

  /**
   * set soTimeout (read timeout) on the underlying HttpConnectionManager. This is desirable for queries, but probably
   * not for indexing.
   */
  public void setSoTimeout(int timeout) {
    HttpClientUtil.setSoTimeout(server.getHttpClient(), timeout);
  }
 
   public void shutdownNow() {
     server.shutdown();
diff --git a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
index 54c87482f49..0ad8d061127 100644
-- a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
++ b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
@@ -367,7 +367,8 @@ public abstract class BaseDistributedSearchTestCase extends SolrTestCaseJ4 {
       // setup the server...
       String url = "http://127.0.0.1:" + port + context;
       HttpSolrServer s = new HttpSolrServer(url);
      s.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      s.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);;
      s.setSoTimeout(30000);
       s.setDefaultMaxConnectionsPerHost(100);
       s.setMaxTotalConnections(100);
       return s;
diff --git a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
index 169e986df02..97d78f54130 100755
-- a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
++ b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
@@ -67,7 +67,7 @@ import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;
 })
 public abstract class SolrTestCaseJ4 extends LuceneTestCase {
   private static String coreName = CoreContainer.DEFAULT_DEFAULT_CORE_NAME;
  public static int DEFAULT_CONNECTION_TIMEOUT = 1000;  // default socket connection timeout in ms
  public static int DEFAULT_CONNECTION_TIMEOUT = 15000;  // default socket connection timeout in ms
 
 
   @ClassRule
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
index e98efa8d783..25395dd5c37 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
@@ -219,7 +219,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
     server.getLbServer().getHttpClient().getParams()
         .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
     server.getLbServer().getHttpClient().getParams()
        .setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
        .setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
     return server;
   }
   
@@ -1157,7 +1157,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
         retry  = true;
       }
       cnt++;
      if (cnt > 4) break;
      if (cnt > 10) break;
       Thread.sleep(2000);
     } while (retry);
   }
@@ -1210,7 +1210,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
           + DEFAULT_COLLECTION;
       HttpSolrServer s = new HttpSolrServer(url);
       s.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
      s.setSoTimeout(40000);
      s.setSoTimeout(15000);
       s.setDefaultMaxConnectionsPerHost(100);
       s.setMaxTotalConnections(100);
       return s;
@@ -1218,28 +1218,5 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
       throw new RuntimeException(ex);
     }
   }
  
  protected void waitToSeeNotLive(ZkStateReader zkStateReader,
      CloudJettyRunner cjetty) throws InterruptedException {
    waitToSeeNotLive(zkStateReader, cjetty, 0);
  }
  
  protected void waitToSeeNotLive(ZkStateReader zkStateReader,
      CloudJettyRunner cjetty, int cnt) throws InterruptedException {
    int tries = 0;
    ClusterState clusterState = zkStateReader.getClusterState();
    while (clusterState.liveNodesContain(cjetty.info
        .getStr(ZkStateReader.NODE_NAME_PROP))) {
      System.out.println("scs:"
          + zkStateReader.getClusterState().getZkClusterStateVersion() + " "
          + zkStateReader.getClusterState().getLiveNodes());
      System.out.println("see live nodes:"
          + zkStateReader.getClusterState().getLiveNodes());
      if (tries++ == 30) {
        fail("Shard still reported as live in zk - " + cnt + " jetty");
      }
      Thread.sleep(1000);
      clusterState = zkStateReader.getClusterState();
    }
  }

 }
diff --git a/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java b/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
index f8554d16a46..1588524ad9f 100644
-- a/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
++ b/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
@@ -191,6 +191,7 @@ public class TestHarness {
           hostPort = System.getProperty("hostPort");
           hostContext = "solr";
           defaultCoreName = CoreContainer.DEFAULT_DEFAULT_CORE_NAME;
          initShardHandler(null);
           initZooKeeper(System.getProperty("zkHost"), 10000);
         }
       };
- 
2.19.1.windows.1

