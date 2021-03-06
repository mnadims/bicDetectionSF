From d87ffa4bf82c30e9a6f0bbb6b8c0087a5c07f9d6 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Fri, 25 Nov 2016 00:27:16 +0530
Subject: [PATCH] SOLR-9784: Refactor CloudSolrClient to eliminate direct
 dependency on ZK SOLR-9512: CloudSolrClient's cluster state cache can break
 direct updates to leaders

--
 solr/CHANGES.txt                              |   6 +-
 solr/solrj/ivy.xml                            |   4 +
 .../impl/ZkClientClusterStateProvider.java    | 177 +++++++++++++++
 .../solr/common/cloud/ClusterState.java       |   7 +
 .../solr/common/cloud/ZkStateReader.java      |  10 +-
 .../solrj/impl/CloudSolrClientCacheTest.java  | 206 ++++++++++++++++++
 6 files changed, 406 insertions(+), 4 deletions(-)
 create mode 100644 solr/solrj/src/java/org/apache/solr/client/solrj/impl/ZkClientClusterStateProvider.java
 create mode 100644 solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientCacheTest.java

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 4d9e6a0c547..fe674906e99 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -196,7 +196,9 @@ Bug Fixes
 * SOLR-9729: JDBCStream improvements (Kevin Risden)
 
 * SOLR-9626: new Admin UI now also highlights matched terms in the Analysis screen. (Alexandre Rafalovitch)
  

* SOLR-9512: CloudSolrClient's cluster state cache can break direct updates to leaders (noble)

 Other Changes
 ----------------------
 
@@ -222,6 +224,8 @@ Other Changes
 * SOLR-8785: Use Dropwizard Metrics library for core metrics. The copied over code in
   org.apache.solr.util.stats has been removed. (Jeff Wartes, Kelvin Wong, Christine Poerschke, shalin)
 
* SOLR-9784: Refactor CloudSolrClient to eliminate direct dependency on ZK (noble)

 ==================  6.3.0 ==================
 
 Consult the LUCENE_CHANGES.txt file for additional, low level, changes in this release.
diff --git a/solr/solrj/ivy.xml b/solr/solrj/ivy.xml
index c9995f326c7..ae7d02beb6b 100644
-- a/solr/solrj/ivy.xml
++ b/solr/solrj/ivy.xml
@@ -40,6 +40,10 @@
     <dependency org="org.slf4j" name="jcl-over-slf4j" rev="${/org.slf4j/jcl-over-slf4j}" conf="compile"/>
 
     <dependency org="org.slf4j" name="slf4j-log4j12" rev="${/org.slf4j/slf4j-log4j12}" conf="test"/>
    <dependency org="org.easymock" name="easymock" rev="${/org.easymock/easymock}" conf="test"/>
    <dependency org="cglib" name="cglib-nodep" rev="${/cglib/cglib-nodep}" conf="test"/>
    <dependency org="org.objenesis" name="objenesis" rev="${/org.objenesis/objenesis}" conf="test"/>

 
     <exclude org="*" ext="*" matcher="regexp" type="${ivy.exclude.types}"/>
   </dependencies>
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ZkClientClusterStateProvider.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ZkClientClusterStateProvider.java
new file mode 100644
index 00000000000..5541186f8dd
-- /dev/null
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/ZkClientClusterStateProvider.java
@@ -0,0 +1,177 @@
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

package org.apache.solr.client.solrj.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.Aliases;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.cloud.ZooKeeperException;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ZkClientClusterStateProvider implements CloudSolrClient.ClusterStateProvider {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  ZkStateReader zkStateReader;
  String zkHost;
  int zkConnectTimeout = 10000;
  int zkClientTimeout = 10000;

  public ZkClientClusterStateProvider(Collection<String> zkHosts, String chroot) {
    zkHost = buildZkHostString(zkHosts,chroot);
  }

  public ZkClientClusterStateProvider(String zkHost){
    this.zkHost = zkHost;
  }

  @Override
  public ClusterState.CollectionRef getState(String collection) {
    return zkStateReader.getClusterState().getCollectionRef(collection);
  }

  @Override
  public Set<String> liveNodes() {
    return zkStateReader.getClusterState().getLiveNodes();
  }


  @Override
  public String getAlias(String collection) {
    Aliases aliases = zkStateReader.getAliases();
    return aliases.getCollectionAlias(collection);
  }

  @Override
  public Map<String, Object> getClusterProperties() {
    return zkStateReader.getClusterProperties();
  }

  @Override
  public String getCollectionName(String name) {
    Aliases aliases = zkStateReader.getAliases();
    if (aliases != null) {
      Map<String, String> collectionAliases = aliases.getCollectionAliasMap();
      if (collectionAliases != null && collectionAliases.containsKey(name)) {
        name = collectionAliases.get(name);
      }
    }
    return name;
  }
  /**
   * Download a named config from Zookeeper to a location on the filesystem
   * @param configName    the name of the config
   * @param downloadPath  the path to write config files to
   * @throws IOException  if an I/O exception occurs
   */
  public void downloadConfig(String configName, Path downloadPath) throws IOException {
    connect();
    zkStateReader.getConfigManager().downloadConfigDir(configName, downloadPath);
  }

  public void uploadConfig(Path configPath, String configName) throws IOException {
    connect();
    zkStateReader.getConfigManager().uploadConfigDir(configPath, configName);
  }

  @Override
  public void connect() {
    if (zkStateReader == null) {
      synchronized (this) {
        if (zkStateReader == null) {
          ZkStateReader zk = null;
          try {
            zk = new ZkStateReader(zkHost, zkClientTimeout, zkConnectTimeout);
            zk.createClusterStateWatchersAndUpdate();
            zkStateReader = zk;
            log.info("Cluster at {} ready", zkHost);
          } catch (InterruptedException e) {
            zk.close();
            Thread.currentThread().interrupt();
            throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
          } catch (KeeperException e) {
            zk.close();
            throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
          } catch (Exception e) {
            if (zk != null) zk.close();
            // do not wrap because clients may be relying on the underlying exception being thrown
            throw e;
          }
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (zkStateReader != null) {
      synchronized (this) {
        if (zkStateReader != null)
          zkStateReader.close();
        zkStateReader = null;
      }
    }
  }


  static String buildZkHostString(Collection<String> zkHosts, String chroot) {
    if (zkHosts == null || zkHosts.isEmpty()) {
      throw new IllegalArgumentException("Cannot create CloudSearchClient without valid ZooKeeper host; none specified!");
    }

    StringBuilder zkBuilder = new StringBuilder();
    int lastIndexValue = zkHosts.size() - 1;
    int i = 0;
    for (String zkHost : zkHosts) {
      zkBuilder.append(zkHost);
      if (i < lastIndexValue) {
        zkBuilder.append(",");
      }
      i++;
    }
    if (chroot != null) {
      if (chroot.startsWith("/")) {
        zkBuilder.append(chroot);
      } else {
        throw new IllegalArgumentException(
            "The chroot must start with a forward slash.");
      }
    }

    /* Log the constructed connection string and then initialize. */
    final String zkHostString = zkBuilder.toString();
    log.debug("Final constructed zkHost string: " + zkHostString);
    return zkHostString;
  }

  @Override
  public String toString() {
    return zkHost;
  }
}
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java b/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java
index 3ab5a1f8c1a..302ee62e434 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/ClusterState.java
@@ -24,6 +24,7 @@ import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
 
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
@@ -442,13 +443,19 @@ public class ClusterState implements JSONWriter.Writable {
   }
 
   public static class CollectionRef {
    protected final AtomicInteger gets = new AtomicInteger();
     private final DocCollection coll;
 
    public int getCount(){
      return gets.get();
    }

     public CollectionRef(DocCollection coll) {
       this.coll = coll;
     }
 
     public DocCollection get(){
      gets.incrementAndGet();
       return coll;
     }
 
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
index a858f340f6b..fea59780f8c 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/ZkStateReader.java
@@ -633,6 +633,7 @@ public class ZkStateReader implements Closeable {
 
     @Override
     public DocCollection get() {
      gets.incrementAndGet();
       // TODO: consider limited caching
       return getCollectionLive(ZkStateReader.this, collName);
     }
@@ -915,14 +916,18 @@ public class ZkStateReader implements Closeable {
     }
     return null;
   }
  

   /**
    * Returns the baseURL corresponding to a given node's nodeName --
   * NOTE: does not (currently) imply that the nodeName (or resulting 
   * NOTE: does not (currently) imply that the nodeName (or resulting
    * baseURL) exists in the cluster.
    * @lucene.experimental
    */
   public String getBaseUrlForNodeName(final String nodeName) {
    return getBaseUrlForNodeName(nodeName, getClusterProperty(URL_SCHEME, "http"));
  }

  public static String getBaseUrlForNodeName(final String nodeName, String urlScheme) {
     final int _offset = nodeName.indexOf("_");
     if (_offset < 0) {
       throw new IllegalArgumentException("nodeName does not contain expected '_' seperator: " + nodeName);
@@ -930,7 +935,6 @@ public class ZkStateReader implements Closeable {
     final String hostAndPort = nodeName.substring(0,_offset);
     try {
       final String path = URLDecoder.decode(nodeName.substring(1+_offset), "UTF-8");
      String urlScheme = getClusterProperty(URL_SCHEME, "http");
       return urlScheme + "://" + hostAndPort + (path.isEmpty() ? "" : ("/" + path));
     } catch (UnsupportedEncodingException e) {
       throw new IllegalStateException("JVM Does not seem to support UTF-8", e);
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientCacheTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientCacheTest.java
new file mode 100644
index 00000000000..70f8dbdc6bc
-- /dev/null
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientCacheTest.java
@@ -0,0 +1,206 @@
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

package org.apache.solr.client.solrj.impl;


import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import org.apache.http.NoHttpResponseException;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.util.NamedList;
import org.easymock.EasyMock;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CloudSolrClientCacheTest extends SolrTestCaseJ4 {

  public void testCaching() throws Exception {
    String collName = "gettingstarted";
    Set<String> livenodes = new HashSet<>();
    Map<String, ClusterState.CollectionRef> refs = new HashMap<>();
    Map<String, DocCollection> colls = new HashMap<>();

    class Ref extends ClusterState.CollectionRef {
      private String c;

      public Ref(String c) {
        super(null);
        this.c = c;
      }

      @Override
      public boolean isLazilyLoaded() {
        return true;
      }

      @Override
      public DocCollection get() {
        gets.incrementAndGet();
        return colls.get(c);
      }
    }
    Map<String, Function> responses = new HashMap<>();
    NamedList okResponse = new NamedList();
    okResponse.add("responseHeader", new NamedList<>(Collections.singletonMap("status", 0)));

    LBHttpSolrClient mockLbclient = getMockLbHttpSolrClient(responses);
    AtomicInteger lbhttpRequestCount = new AtomicInteger();
    try (CloudSolrClient cloudClient = new CloudSolrClient.Builder()
        .withLBHttpSolrClient(mockLbclient)
        .withClusterStateProvider(getStateProvider(livenodes, refs))

        .build()) {
      livenodes.addAll(ImmutableSet.of("192.168.1.108:7574_solr", "192.168.1.108:8983_solr"));
      ClusterState cs = ClusterState.load(1, coll1State.getBytes(UTF_8),
          Collections.emptySet(), "/collections/gettingstarted/state.json");
      refs.put(collName, new Ref(collName));
      colls.put(collName, cs.getCollectionOrNull(collName));
      responses.put("request", o -> {
        int i = lbhttpRequestCount.incrementAndGet();
        if (i == 1) return new ConnectException("TEST");
        if (i == 2) return new SocketException("TEST");
        if (i == 3) return new NoHttpResponseException("TEST");
        return okResponse;
      });
      UpdateRequest update = new UpdateRequest()
          .add("id", "123", "desc", "Something 0");

      cloudClient.request(update, collName);
      assertEquals(2, refs.get(collName).getCount());
    }

  }


  private LBHttpSolrClient getMockLbHttpSolrClient(Map<String, Function> responses) throws Exception {
    LBHttpSolrClient mockLbclient = EasyMock.createMock(LBHttpSolrClient.class);
    EasyMock.reset(mockLbclient);

    mockLbclient.request(EasyMock.anyObject(LBHttpSolrClient.Req.class));
    EasyMock.expectLastCall().andAnswer(() -> {
      LBHttpSolrClient.Req req = (LBHttpSolrClient.Req) EasyMock.getCurrentArguments()[0];
      Function f = responses.get("request");
      if (f == null) return null;
      Object res = f.apply(null);
      if (res instanceof Exception) throw (Throwable) res;
      LBHttpSolrClient.Rsp rsp = new LBHttpSolrClient.Rsp();
      rsp.rsp = (NamedList<Object>) res;
      rsp.server = req.servers.get(0);
      return rsp;
    }).anyTimes();

    mockLbclient.getHttpClient();
    EasyMock.expectLastCall().andAnswer(() -> null).anyTimes();

    EasyMock.replay(mockLbclient);
    return mockLbclient;
  }

  private CloudSolrClient.ClusterStateProvider getStateProvider(Set<String> livenodes,
                                                                Map<String, ClusterState.CollectionRef> colls) {
    return new CloudSolrClient.ClusterStateProvider() {
      @Override
      public ClusterState.CollectionRef getState(String collection) {
        return colls.get(collection);
      }

      @Override
      public Set<String> liveNodes() {
        return livenodes;
      }

      @Override
      public Map<String, Object> getClusterProperties() {
        return Collections.EMPTY_MAP;
      }

      @Override
      public String getAlias(String collection) {
        return collection;
      }

      @Override
      public String getCollectionName(String name) {
        return name;
      }

      @Override
      public void connect() { }

      @Override
      public void close() throws IOException {

      }
    };

  }


  private String coll1State = "{'gettingstarted':{\n" +
      "    'replicationFactor':'2',\n" +
      "    'router':{'name':'compositeId'},\n" +
      "    'maxShardsPerNode':'2',\n" +
      "    'autoAddReplicas':'false',\n" +
      "    'shards':{\n" +
      "      'shard1':{\n" +
      "        'range':'80000000-ffffffff',\n" +
      "        'state':'active',\n" +
      "        'replicas':{\n" +
      "          'core_node2':{\n" +
      "            'core':'gettingstarted_shard1_replica1',\n" +
      "            'base_url':'http://192.168.1.108:8983/solr',\n" +
      "            'node_name':'192.168.1.108:8983_solr',\n" +
      "            'state':'active',\n" +
      "            'leader':'true'},\n" +
      "          'core_node4':{\n" +
      "            'core':'gettingstarted_shard1_replica2',\n" +
      "            'base_url':'http://192.168.1.108:7574/solr',\n" +
      "            'node_name':'192.168.1.108:7574_solr',\n" +
      "            'state':'active'}}},\n" +
      "      'shard2':{\n" +
      "        'range':'0-7fffffff',\n" +
      "        'state':'active',\n" +
      "        'replicas':{\n" +
      "          'core_node1':{\n" +
      "            'core':'gettingstarted_shard2_replica1',\n" +
      "            'base_url':'http://192.168.1.108:8983/solr',\n" +
      "            'node_name':'192.168.1.108:8983_solr',\n" +
      "            'state':'active',\n" +
      "            'leader':'true'},\n" +
      "          'core_node3':{\n" +
      "            'core':'gettingstarted_shard2_replica2',\n" +
      "            'base_url':'http://192.168.1.108:7574/solr',\n" +
      "            'node_name':'192.168.1.108:7574_solr',\n" +
      "            'state':'active'}}}}}}";


}
- 
2.19.1.windows.1

