From e435611127905f924dab9a8fdcd36c4518871ba7 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 15 Dec 2014 13:24:55 -0500
Subject: [PATCH] ACCUMULO-3421 Retry initialization of trace client

If we fail to connect to ZK for some reason,
we will never get the watcher set and tracing will
never be initialized properly. Add a simple retry
to ensure that it happens.
--
 server/tracer/pom.xml                         |  5 ++
 .../accumulo/tracer/ZooTraceClient.java       | 53 ++++++++++++++++---
 .../accumulo/tracer/ZooTraceClientTest.java   | 42 +++++++++++++++
 .../src/test/resources/log4j.properties       | 21 ++++++++
 4 files changed, 114 insertions(+), 7 deletions(-)
 create mode 100644 server/tracer/src/test/java/org/apache/accumulo/tracer/ZooTraceClientTest.java
 create mode 100644 server/tracer/src/test/resources/log4j.properties

diff --git a/server/tracer/pom.xml b/server/tracer/pom.xml
index 859a471e1..ac9f45fde 100644
-- a/server/tracer/pom.xml
++ b/server/tracer/pom.xml
@@ -72,6 +72,11 @@
       <artifactId>junit</artifactId>
       <scope>test</scope>
     </dependency>
    <dependency>
      <groupId>org.easymock</groupId>
      <artifactId>easymock</artifactId>
      <scope>test</scope>
    </dependency>
     <dependency>
       <groupId>org.slf4j</groupId>
       <artifactId>slf4j-log4j12</artifactId>
diff --git a/server/tracer/src/main/java/org/apache/accumulo/tracer/ZooTraceClient.java b/server/tracer/src/main/java/org/apache/accumulo/tracer/ZooTraceClient.java
index cfb65de37..7b3ee2e81 100644
-- a/server/tracer/src/main/java/org/apache/accumulo/tracer/ZooTraceClient.java
++ b/server/tracer/src/main/java/org/apache/accumulo/tracer/ZooTraceClient.java
@@ -23,15 +23,21 @@ import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.fate.zookeeper.ZooReader;
 import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
 import org.apache.zookeeper.WatchedEvent;
 import org.apache.zookeeper.Watcher;
 import org.htrace.HTraceConfiguration;
 
import com.google.common.util.concurrent.ThreadFactoryBuilder;

 /**
  * Find a Span collector via zookeeper and push spans there via Thrift RPC
  */
@@ -45,6 +51,7 @@ public class ZooTraceClient extends SendSpansViaThrift implements Watcher {
   boolean pathExists = false;
   final Random random = new Random();
   final List<String> hosts = new ArrayList<String>();
  long retryPause = 5000l;
 
   public ZooTraceClient() {
     super();
@@ -54,6 +61,11 @@ public class ZooTraceClient extends SendSpansViaThrift implements Watcher {
     super(millis);
   }
 
  // Visibile for testing
  protected void setRetryPause(long pause) {
    retryPause = pause;
  }

   @Override
   synchronized protected String getSpanKey(Map<ByteBuffer,ByteBuffer> data) {
     if (hosts.size() > 0) {
@@ -72,24 +84,51 @@ public class ZooTraceClient extends SendSpansViaThrift implements Watcher {
     int timeout = conf.getInt(DistributedTrace.TRACER_ZK_TIMEOUT, DEFAULT_TIMEOUT);
     zoo = new ZooReader(keepers, timeout);
     path = conf.get(DistributedTrace.TRACER_ZK_PATH, Constants.ZTRACERS);
    process(null);
    setInitialTraceHosts();
   }
 
   @Override
   public void process(WatchedEvent event) {
     log.debug("Processing event for trace server zk watch");
     try {
      if (pathExists || zoo.exists(path)) {
        pathExists = true;
        updateHosts(path, zoo.getChildren(path, this));
      } else {
        zoo.exists(path, this);
      }
      updateHostsFromZooKeeper();
     } catch (Exception ex) {
       log.error("unable to get destination hosts in zookeeper", ex);
     }
   }
 
  protected void setInitialTraceHosts() {
    // Make a single thread pool with a daemon thread
    final ScheduledExecutorService svc = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        try {
          updateHostsFromZooKeeper();
          log.info("Successfully initialized tracer hosts from ZooKeeper");
          // Once this passes, we can issue a shutdown of the pool
          svc.shutdown();
        } catch (Exception e) {
          log.error("Unabled to get destination tracer hosts in ZooKeeper, will retry in 5 seconds", e);
          // We failed to connect to ZK, try again in 5seconds
          svc.schedule(this, retryPause, TimeUnit.MILLISECONDS);
        }
      }
    };

    // Start things off
    task.run();
  }

  protected void updateHostsFromZooKeeper() throws KeeperException, InterruptedException {
    if (pathExists || zoo.exists(path)) {
      pathExists = true;
      updateHosts(path, zoo.getChildren(path, this));
    } else {
      zoo.exists(path, this);
    }
  }

   @Override
   protected void sendSpans() {
     if (hosts.isEmpty()) {
diff --git a/server/tracer/src/test/java/org/apache/accumulo/tracer/ZooTraceClientTest.java b/server/tracer/src/test/java/org/apache/accumulo/tracer/ZooTraceClientTest.java
new file mode 100644
index 000000000..ebb2a7cee
-- /dev/null
++ b/server/tracer/src/test/java/org/apache/accumulo/tracer/ZooTraceClientTest.java
@@ -0,0 +1,42 @@
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
package org.apache.accumulo.tracer;

import org.easymock.EasyMock;
import org.junit.Test;

public class ZooTraceClientTest {

  @Test
  public void testConnectFailureRetries() throws Exception {
    ZooTraceClient client = EasyMock.createMockBuilder(ZooTraceClient.class).addMockedMethod("updateHostsFromZooKeeper").createStrictMock();
    client.setRetryPause(0l);

    client.updateHostsFromZooKeeper();
    EasyMock.expectLastCall().andThrow(new RuntimeException()).once();
    client.updateHostsFromZooKeeper();
    EasyMock.expectLastCall();

    EasyMock.replay(client);

    client.setInitialTraceHosts();
    
    EasyMock.verify(client);

  }

}
diff --git a/server/tracer/src/test/resources/log4j.properties b/server/tracer/src/test/resources/log4j.properties
new file mode 100644
index 000000000..320683261
-- /dev/null
++ b/server/tracer/src/test/resources/log4j.properties
@@ -0,0 +1,21 @@
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

log4j.rootLogger=INFO, CA
log4j.appender.CA=org.apache.log4j.ConsoleAppender
log4j.appender.CA.layout=org.apache.log4j.PatternLayout
log4j.appender.CA.layout.ConversionPattern=%d{ISO8601} [%-8c{2}] %-5p: %m%n

log4j.logger.org.apache.accumulo.server.util.TabletIterator=ERROR
\ No newline at end of file
- 
2.19.1.windows.1

