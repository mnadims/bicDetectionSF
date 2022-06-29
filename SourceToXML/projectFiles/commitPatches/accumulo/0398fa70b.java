From 0398fa70be7758279c61735435be4c67f96bb104 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Thu, 18 Dec 2014 17:24:18 -0500
Subject: [PATCH] ACCUMULO-3291 Use target instead of /tmp

--
 .../MiniAccumuloClusterStartStopTest.java     | 38 ++++++++++---------
 1 file changed, 21 insertions(+), 17 deletions(-)

diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
index f7440e8d6..a92342ec2 100644
-- a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
@@ -16,36 +16,40 @@
  */
 package org.apache.accumulo.minicluster;
 
import java.io.File;
 import java.io.IOException;
 
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.junit.After;
import org.apache.commons.io.FileUtils;
 import org.junit.Assert;
 import org.junit.Before;
import org.junit.Rule;
 import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
 
 public class MiniAccumuloClusterStartStopTest {
  
  public TemporaryFolder folder = new TemporaryFolder();
  

  private File baseDir = new File(System.getProperty("user.dir") + "/target/mini-tests/" + this.getClass().getName());
  private File testDir;

  @Rule
  public TestName testName = new TestName();

   @Before
   public void createMacDir() throws IOException {
    folder.create();
  }
  
  @After
  public void deleteMacDir() {
    folder.delete();
    baseDir.mkdirs();
    testDir = new File(baseDir, testName.getMethodName());
    FileUtils.deleteQuietly(testDir);
    testDir.mkdir();
   }
  

   @Test
   public void multipleStartsThrowsAnException() throws Exception {
    MiniAccumuloCluster accumulo = new MiniAccumuloCluster(folder.getRoot(), "superSecret");
    MiniAccumuloCluster accumulo = new MiniAccumuloCluster(testDir, "superSecret");
     accumulo.start();
    

     try {
       accumulo.start();
       Assert.fail("Invoking start() while already started is an error");
@@ -55,12 +59,12 @@ public class MiniAccumuloClusterStartStopTest {
       accumulo.stop();
     }
   }
  

   @Test
   public void multipleStopsIsAllowed() throws Exception {
    MiniAccumuloCluster accumulo = new MiniAccumuloCluster(folder.getRoot(), "superSecret");
    MiniAccumuloCluster accumulo = new MiniAccumuloCluster(testDir, "superSecret");
     accumulo.start();
    

     Connector conn = new ZooKeeperInstance(accumulo.getInstanceName(), accumulo.getZooKeepers()).getConnector("root", new PasswordToken("superSecret"));
     conn.tableOperations().create("foo");
 
- 
2.19.1.windows.1

