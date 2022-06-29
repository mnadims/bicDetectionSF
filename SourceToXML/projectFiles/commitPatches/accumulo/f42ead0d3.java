From f42ead0d39e34578c6fe9636af4cfbd9d91e47a5 Mon Sep 17 00:00:00 2001
From: Sean Busbey <busbey@cloudera.com>
Date: Mon, 20 Jan 2014 14:26:20 -0600
Subject: [PATCH] ACCUMULO-2224 Make ZooSession more resiliant in the face of
 transient DNS issues.

* retries if host is not found, up to 2xZK timeout (same as other IOExceptions), rather than bailing on any host name problem.
* adds utility method for getting the max time the JVM will cache host failures
* add test for said method
--
 .../accumulo/core/util/AddressUtil.java       | 39 +++++++++++
 .../accumulo/core/zookeeper/ZooSession.java   | 11 +--
 .../accumulo/core/util/AddressUtilTest.java   | 69 ++++++++++++++++++-
 src/core/src/test/resources/log4j.properties  | 23 +++++++
 4 files changed, 137 insertions(+), 5 deletions(-)
 create mode 100644 src/core/src/test/resources/log4j.properties

diff --git a/src/core/src/main/java/org/apache/accumulo/core/util/AddressUtil.java b/src/core/src/main/java/org/apache/accumulo/core/util/AddressUtil.java
index 0b821289d..96c2e18b7 100644
-- a/src/core/src/main/java/org/apache/accumulo/core/util/AddressUtil.java
++ b/src/core/src/main/java/org/apache/accumulo/core/util/AddressUtil.java
@@ -16,12 +16,20 @@
  */
 package org.apache.accumulo.core.util;
 
import java.net.InetAddress; // workaround to enable @see/@link hyperlink
 import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.Security;
 
 import org.apache.hadoop.io.Text;
 import org.apache.thrift.transport.TSocket;
 
import org.apache.log4j.Logger;

 public class AddressUtil {

  private static final Logger log = Logger.getLogger(AddressUtil.class);

   static public InetSocketAddress parseAddress(String address, int defaultPort) throws NumberFormatException {
     final String[] parts = address.split(":", 2);
     if (parts.length == 2) {
@@ -44,5 +52,36 @@ public class AddressUtil {
   static public String toString(InetSocketAddress addr) {
     return addr.getAddress().getHostAddress() + ":" + addr.getPort();
   }

  /**
   * Fetch the security value that determines how long DNS failures are cached.
   * Looks up the security property 'networkaddress.cache.negative.ttl'. Should that fail returns
   * the default value used in the Oracle JVM 1.4+, which is 10 seconds.
   *
   * @param originalException the host lookup that is the source of needing this lookup. maybe be null.
   * @return positive integer number of seconds
   * @see java.net.InetAddress
   * @throws IllegalArgumentException if dns failures are cached forever
   */
  static public int getAddressCacheNegativeTtl(UnknownHostException originalException) {
    int negativeTtl = 10;
    try {
      negativeTtl = Integer.parseInt(Security.getProperty("networkaddress.cache.negative.ttl"));
    } catch (NumberFormatException exception) {
      log.warn("Failed to get JVM negative DNS respones cache TTL due to format problem (e.g. this JVM might not have the " +
                "property). Falling back to default based on Oracle JVM 1.6 (10s)", exception);
    } catch (SecurityException exception) {
      log.warn("Failed to get JVM negative DNS response cache TTL due to security manager. Falling back to default based on Oracle JVM 1.6 (10s)", exception);
    }
    if (-1 == negativeTtl) {
      log.error("JVM negative DNS repsonse cache TTL is set to 'forever' and host lookup failed. TTL can be changed with security property " +
                "'networkaddress.cache.negative.ttl', see java.net.InetAddress.", originalException);
      throw new IllegalArgumentException(originalException);
    } else if (0 > negativeTtl) {
      log.warn("JVM specified negative DNS response cache TTL was negative (and not 'forever'). Falling back to default based on Oracle JVM 1.6 (10s)");
      negativeTtl = 10;
    }
    return negativeTtl;
  }
   
 }
diff --git a/src/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooSession.java b/src/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooSession.java
index e64f0c52e..e3c9cc735 100644
-- a/src/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooSession.java
++ b/src/core/src/main/java/org/apache/accumulo/core/zookeeper/ZooSession.java
@@ -21,6 +21,7 @@ import java.net.UnknownHostException;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.accumulo.core.util.AddressUtil;
 import org.apache.accumulo.core.util.UtilWaitThread;
 import org.apache.log4j.Logger;
 import org.apache.zookeeper.WatchedEvent;
@@ -88,11 +89,13 @@ public class ZooSession {
         if (System.currentTimeMillis() - startTime > 2 * timeout)
           throw new RuntimeException("Failed to connect to zookeeper (" + host + ") within 2x zookeeper timeout period " + timeout);
 
      } catch (UnknownHostException uhe) {
        // do not expect to recover from this
        log.warn(uhe.getClass().getName() + " : " + uhe.getMessage());
        throw new RuntimeException(uhe);
       } catch (IOException e) {
        if (e instanceof UnknownHostException) {
          /*
             Make sure we wait atleast as long as the JVM TTL for negative DNS responses
           */
          sleepTime = Math.max(sleepTime, (AddressUtil.getAddressCacheNegativeTtl((UnknownHostException) e) + 1) * 1000);
        }
         log.warn("Connection to zooKeeper failed, will try again in " + String.format("%.2f secs", sleepTime / 1000.0), e);
       } finally {
         if (tryAgain && zooKeeper != null)
diff --git a/src/core/src/test/java/org/apache/accumulo/core/util/AddressUtilTest.java b/src/core/src/test/java/org/apache/accumulo/core/util/AddressUtilTest.java
index f46f4277d..e71ba0e47 100644
-- a/src/core/src/test/java/org/apache/accumulo/core/util/AddressUtilTest.java
++ b/src/core/src/test/java/org/apache/accumulo/core/util/AddressUtilTest.java
@@ -17,10 +17,12 @@
 package org.apache.accumulo.core.util;
 
 import java.net.InetSocketAddress;
import java.security.Security;
 
 import junit.framework.TestCase;
 
 import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
 import org.apache.thrift.transport.TSocket;
 
 /**
@@ -28,6 +30,9 @@ import org.apache.thrift.transport.TSocket;
  * 
  */
 public class AddressUtilTest extends TestCase {

  private static final Logger log = Logger.getLogger(AddressUtilTest.class);

   public void testAddress() {
     InetSocketAddress addr = AddressUtil.parseAddress("127.0.0.1", 12345);
     assertTrue(addr.equals(new InetSocketAddress("127.0.0.1", 12345)));
@@ -51,5 +56,67 @@ public class AddressUtilTest extends TestCase {
   public void testToString() {
     assertTrue(AddressUtil.toString(new InetSocketAddress("127.0.0.1", 1234)).equals("127.0.0.1:1234"));
   }
  

  public void testGetNegativeTtl() {
    log.info("Checking that we can get the ttl on dns failures.");
    int expectedTtl = 20;
    boolean expectException = false;
    /* TODO replace all of this with Powermock on the Security class */
    try {
      Security.setProperty("networkaddress.cache.negative.ttl", Integer.toString(expectedTtl));
    } catch (SecurityException exception) {
      log.warn("We can't set the DNS cache period, so we're only testing fetching the system value.");
      expectedTtl = 10;
    }
    try {
      expectedTtl = Integer.parseInt(Security.getProperty("networkaddress.cache.negative.ttl"));
    } catch (SecurityException exception) {
      log.debug("Security manager won't let us fetch the property, testing default path.");
      expectedTtl = 10;
    } catch (NumberFormatException exception) {
      log.debug("property isn't a number, testing default path.");
      expectedTtl = 10;
    }
    if (-1 == expectedTtl) {
      log.debug("property is set to 'forever', testing exception path");
      expectException = true;
    }
    if (0 > expectedTtl) {
      log.debug("property is a negative value other than 'forever', testing default path.");
      expectedTtl = 10;
    }
    try {
      if (expectException) {
        log.info("AddressUtil is (hopefully) going to spit out an error about DNS lookups. you can ignore it.");
      }
      int result = AddressUtil.getAddressCacheNegativeTtl(null);
      if (expectException) {
        fail("The JVM Security settings cache DNS failures forever. In this case we expect an exception but didn't get one.");
      }
      assertEquals("Didn't get the ttl we expected", expectedTtl, result);
    } catch (IllegalArgumentException exception) {
      if (!expectException) {
        log.error("Got an exception when we weren't expecting.", exception);
        fail("We only expect to throw an IllegalArgumentException when the JVM caches DNS failures forever.");
      }
    }
  }

  public void testGetNegativeTtlThrowsOnForever() {
    log.info("When DNS is cached forever, we should throw.");
    /* TODO replace all of this with Powermock on the Security class */
    try {
      Security.setProperty("networkaddress.cache.negative.ttl", "-1");
    } catch (SecurityException exception) {
      log.error("We can't set the DNS cache period, so this test is effectively ignored.");
      return;
    }
    try {
      log.info("AddressUtil is (hopefully) going to spit out an error about DNS lookups. you can ignore it.");
      int result = AddressUtil.getAddressCacheNegativeTtl(null);
      fail("The JVM Security settings cache DNS failures forever, this should cause an exception.");
    } catch(IllegalArgumentException exception) {
      assertTrue(true);
    }
  }
 }
diff --git a/src/core/src/test/resources/log4j.properties b/src/core/src/test/resources/log4j.properties
new file mode 100644
index 000000000..28244919f
-- /dev/null
++ b/src/core/src/test/resources/log4j.properties
@@ -0,0 +1,23 @@
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
log4j.appender.CA.layout.ConversionPattern=[%t] %-5p %c %x - %m%n

log4j.logger.org.apache.zookeeper=ERROR,CA
log4j.logger.org.apache.accumulo.core.client.impl.ServerClient=ERROR
log4j.logger.org.apache.accumulo.server.security.Auditor=off
- 
2.19.1.windows.1

