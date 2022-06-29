From d4d455f1f3beded6f20b6c3946a7b867d6b3e2d4 Mon Sep 17 00:00:00 2001
From: Sean Busbey <busbey@cloudera.com>
Date: Tue, 1 Jul 2014 10:48:43 -0500
Subject: [PATCH] ACCUMULO-2967 Unknown Host should result in timeout.

 Tests that we get an exception within a reasonable bound
 Changes timeout condition to check prior to place where an exception can be thrown.
--
 .../accumulo/fate/zookeeper/ZooSession.java   | 14 ++++++--
 .../fate/zookeeper/ZooSessionTest.java        | 32 +++++++++++++++++++
 2 files changed, 43 insertions(+), 3 deletions(-)
 create mode 100644 fate/src/test/java/org/apache/accumulo/fate/zookeeper/ZooSessionTest.java

diff --git a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooSession.java b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooSession.java
index 205ff0180..a9f630efe 100644
-- a/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooSession.java
++ b/fate/src/main/java/org/apache/accumulo/fate/zookeeper/ZooSession.java
@@ -66,6 +66,13 @@ public class ZooSession {
     
   }
   
  /**
   * @param host comma separated list of zk servers
   * @param timeout in milliseconds
   * @param scheme authentication type, e.g. 'digest', may be null
   * @param auth authentication-scheme-specific token, may be null
   * @param watcher ZK notifications, may be null
   */
   public static ZooKeeper connect(String host, int timeout, String scheme, byte[] auth, Watcher watcher) {
     final int TIME_BETWEEN_CONNECT_CHECKS_MS = 100;
     final int TOTAL_CONNECT_TIME_WAIT_MS = 10 * 1000;
@@ -88,9 +95,6 @@ public class ZooSession {
             UtilWaitThread.sleep(TIME_BETWEEN_CONNECT_CHECKS_MS);
         }
         
        if (System.currentTimeMillis() - startTime > 2 * timeout)
          throw new RuntimeException("Failed to connect to zookeeper (" + host + ") within 2x zookeeper timeout period " + timeout);

       } catch (IOException e) {
         if (e instanceof UnknownHostException) {
           /*
@@ -108,6 +112,10 @@ public class ZooSession {
             log.warn("interrupted", e);
           }
       }

      if (System.currentTimeMillis() - startTime > 2 * timeout) {
        throw new RuntimeException("Failed to connect to zookeeper (" + host + ") within 2x zookeeper timeout period " + timeout);
      }
       
       if (tryAgain) {
         UtilWaitThread.sleep(sleepTime);
diff --git a/fate/src/test/java/org/apache/accumulo/fate/zookeeper/ZooSessionTest.java b/fate/src/test/java/org/apache/accumulo/fate/zookeeper/ZooSessionTest.java
new file mode 100644
index 000000000..6f7928da8
-- /dev/null
++ b/fate/src/test/java/org/apache/accumulo/fate/zookeeper/ZooSessionTest.java
@@ -0,0 +1,32 @@
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
package org.apache.accumulo.fate.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

public class ZooSessionTest {

  private static final int MINIMUM_TIMEOUT=10000;
  private static final String UNKNOWN_HOST = "hostname.that.should.not.exist.example.com:2181";

  @Test(expected=RuntimeException.class, timeout=MINIMUM_TIMEOUT*4)
  public void testUnknownHost() {
    ZooKeeper session = ZooSession.connect(UNKNOWN_HOST, MINIMUM_TIMEOUT, null, null, null);
  }

}
- 
2.19.1.windows.1

