From 94c2a31fce0012ca9708aa99b93b5d932e4a08f3 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Sun, 10 Aug 2014 00:29:42 -0400
Subject: [PATCH] ACCUMULO-3055 Guard against failure due to multiple
 invocations of MAC.stop

MAC has allowed stop to be invoked a few times, so we want to ensure that we
don't start throwing an exception on multiple calls. There are also a few
race conditions (user invoking stop(), shutdown hook invoking stop(),
user invoking start again after MAC was stopped, etc) in which
we want to guard against this concurrency.

Adds synchronized to start and stop to ensure invocations of these
methods are exclusive and uses the `executor` member track start/stop
state. Adds a unit test to ensure multiple starts is an error, multiple
stops is not, reduces inline logging configuration, and applies
redirection to both surefire and failsafe tests.
--
 minicluster/pom.xml                           |  9 ++--
 .../minicluster/MiniAccumuloCluster.java      | 14 +++--
 .../MiniAccumuloClusterGCTest.java            |  3 --
 .../MiniAccumuloClusterStartStopTest.java     | 54 +++++++++++++++++++
 .../minicluster/MiniAccumuloClusterTest.java  |  4 --
 .../src/test/resources/log4j.properties       | 23 +++++++-
 pom.xml                                       |  4 +-
 7 files changed, 94 insertions(+), 17 deletions(-)
 create mode 100644 minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java

diff --git a/minicluster/pom.xml b/minicluster/pom.xml
index 7284ca467..d9468e2a5 100644
-- a/minicluster/pom.xml
++ b/minicluster/pom.xml
@@ -29,6 +29,10 @@
       <groupId>com.beust</groupId>
       <artifactId>jcommander</artifactId>
     </dependency>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
    </dependency>
     <dependency>
       <groupId>org.apache.accumulo</groupId>
       <artifactId>accumulo-core</artifactId>
@@ -66,11 +70,6 @@
       <artifactId>zookeeper</artifactId>
       <scope>provided</scope>
     </dependency>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <scope>test</scope>
    </dependency>
     <dependency>
       <groupId>junit</groupId>
       <artifactId>junit</artifactId>
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
index 7a9bc0d36..8246c51a4 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/MiniAccumuloCluster.java
@@ -53,6 +53,8 @@ import org.apache.accumulo.start.Main;
 import org.apache.log4j.Logger;
 import org.apache.zookeeper.server.ZooKeeperServerMain;
 
import com.google.common.base.Preconditions;

 /**
  * A utility class that will create Zookeeper and Accumulo processes that write all of their data to a single local directory. This class makes it easy to test
  * code against a real Accumulo instance. Its much more accurate for testing than MockAccumulo, but much slower than MockAccumulo.
@@ -306,7 +308,7 @@ public class MiniAccumuloCluster {
    * @throws IllegalStateException
    *           if already started
    */
  public void start() throws IOException, InterruptedException {
  public synchronized void start() throws IOException, InterruptedException {
     if (zooKeeperProcess != null)
       throw new IllegalStateException("Already started");
     
@@ -365,10 +367,15 @@ public class MiniAccumuloCluster {
   }
   
   /**
   * Stops Accumulo and Zookeeper processes. If stop is not called, there is a shutdown hook that is setup to kill the processes. Howerver its probably best to
   * Stops Accumulo and Zookeeper processes. If stop is not called, there is a shutdown hook that is setup to kill the processes. However its probably best to
    * call stop in a finally block as soon as possible.
    */
  public void stop() throws IOException, InterruptedException {
  public synchronized void stop() throws IOException, InterruptedException {
    if (null == executor) {
      // keep repeated calls to stop() from failing
      return;
    }

     if (zooKeeperProcess != null) {
       try {
         stopProcessWithTimeout(zooKeeperProcess, 30, TimeUnit.SECONDS);
@@ -436,6 +443,7 @@ public class MiniAccumuloCluster {
   }
   
   private int stopProcessWithTimeout(final Process proc, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    Preconditions.checkNotNull(executor, "Executor was already null");
     FutureTask<Integer> future = new FutureTask<Integer>(new Callable<Integer>() {
       @Override
       public Integer call() throws InterruptedException {
diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java
index 318f0af4e..610396bc4 100644
-- a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterGCTest.java
@@ -30,8 +30,6 @@ import org.apache.accumulo.server.util.PortUtils;
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.io.filefilter.SuffixFileFilter;
 import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
 import org.junit.Assert;
 import org.junit.Ignore;
 import org.junit.rules.TemporaryFolder;
@@ -50,7 +48,6 @@ public class MiniAccumuloClusterGCTest {
   
   public static void setupMiniCluster() throws Exception {
     tmpDir.create();
    Logger.getLogger("org.apache.zookeeper").setLevel(Level.ERROR);
     
     macConfig = new MiniAccumuloConfig(tmpDir.getRoot(), passwd);
     macConfig.setNumTservers(1);
diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
new file mode 100644
index 000000000..ef82af3b3
-- /dev/null
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
@@ -0,0 +1,54 @@
package org.apache.accumulo.minicluster;

import java.io.IOException;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MiniAccumuloClusterStartStopTest {
  
  public TemporaryFolder folder = new TemporaryFolder();
  
  @Before
  public void createMacDir() throws IOException {
    folder.create();
  }
  
  @After
  public void deleteMacDir() {
    folder.delete();
  }
  
  @Test
  public void multipleStartsThrowsAnException() throws Exception {
    MiniAccumuloCluster accumulo = new MiniAccumuloCluster(folder.getRoot(), "superSecret");
    accumulo.start();
    
    try {
      accumulo.start();
      Assert.fail("Invoking start() while already started is an error");
    } catch (IllegalStateException e) {
      // pass
    } finally {
      accumulo.stop();
    }
  }
  
  @Test
  public void multipleStopsIsAllowed() throws Exception {
    MiniAccumuloCluster accumulo = new MiniAccumuloCluster(folder.getRoot(), "superSecret");
    accumulo.start();
    
    Connector conn = new ZooKeeperInstance(accumulo.getInstanceName(), accumulo.getZooKeepers()).getConnector("root", new PasswordToken("superSecret"));
    conn.tableOperations().create("foo");

    accumulo.stop();
    accumulo.stop();
  }
}
diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java
index 75ae808e0..1b4659118 100644
-- a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterTest.java
@@ -38,8 +38,6 @@ import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.ColumnVisibility;
 import org.apache.accumulo.core.security.TablePermission;
 import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
 import org.junit.AfterClass;
 import org.junit.Assert;
 import org.junit.BeforeClass;
@@ -57,8 +55,6 @@ public class MiniAccumuloClusterTest {
     
     folder.create();
     
    Logger.getLogger("org.apache.zookeeper").setLevel(Level.ERROR);
    
     accumulo = new MiniAccumuloCluster(folder.getRoot(), "superSecret");
     
     accumulo.start();
diff --git a/minicluster/src/test/resources/log4j.properties b/minicluster/src/test/resources/log4j.properties
index b5efe8d6e..9a98a8836 100644
-- a/minicluster/src/test/resources/log4j.properties
++ b/minicluster/src/test/resources/log4j.properties
@@ -1 +1,22 @@
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

log4j.logger.org.apache.accumulo.core.client.impl.ServerClient=ERROR
\ No newline at end of file
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

log4j.logger.org.apache.accumulo.core.client.impl.ServerClient=ERROR
log4j.logger.org.apache.zookeeper=ERROR
diff --git a/pom.xml b/pom.xml
index eba44a90e..4b8bdea0e 100644
-- a/pom.xml
++ b/pom.xml
@@ -113,6 +113,8 @@
     <httpclient.version>3.0.1</httpclient.version>
     <!-- the maven-release-plugin makes this recommendation, due to plugin bugs -->
     <maven.min-version>3.0.4</maven.min-version>
    <!-- surefire/failsafe plugin option -->
    <maven.test.redirectTestOutputToFile>true</maven.test.redirectTestOutputToFile>
     <powermock.version>1.5</powermock.version>
     <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
     <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
@@ -501,7 +503,7 @@
         <plugin>
           <artifactId>maven-surefire-plugin</artifactId>
           <configuration>
            <redirectTestOutputToFile>true</redirectTestOutputToFile>
            <argLine>-Xmx1G</argLine>
           </configuration>
         </plugin>
         <plugin>
- 
2.19.1.windows.1

