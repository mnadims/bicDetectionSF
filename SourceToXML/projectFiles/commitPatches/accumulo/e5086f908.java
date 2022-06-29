From e5086f90863f21787b66dcb82b900ca14144900a Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Thu, 12 Feb 2015 17:00:37 -0500
Subject: [PATCH] ACCUMULO-3583 Always return information about merges

Fix broken tests and ensure that TabletStateChangeIterator always returns
tablets for tables involved in merges.
--
 .../server/master/state/TabletStateChangeIterator.java |  4 +++-
 .../apache/accumulo/server/master/TestMergeState.java  |  7 +++----
 .../java/org/apache/accumulo/test/ShellServerTest.java | 10 ++++++++--
 3 files changed, 14 insertions(+), 7 deletions(-)

diff --git a/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java b/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
index b11809c01..a3402dfa6 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
++ b/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
@@ -129,9 +129,11 @@ public class TabletStateChangeIterator extends SkippingIterator {
       }
       // we always want data about merges
       MergeInfo merge = merges.get(tls.extent.getTableId());
      if (merge != null && merge.getRange() != null && merge.getRange().overlaps(tls.extent)) {
      if (merge != null) {
        // could make this smarter by only returning if the tablet is involved in the merge
         return;
       }

       // is the table supposed to be online or offline?
       boolean shouldBeOnline = onlineTables.contains(tls.extent.getTableId().toString());
 
diff --git a/server/src/test/java/org/apache/accumulo/server/master/TestMergeState.java b/server/src/test/java/org/apache/accumulo/server/master/TestMergeState.java
index 3c0fae3dd..bb6294b85 100644
-- a/server/src/test/java/org/apache/accumulo/server/master/TestMergeState.java
++ b/server/src/test/java/org/apache/accumulo/server/master/TestMergeState.java
@@ -45,7 +45,6 @@ import org.apache.accumulo.server.master.state.MergeStats;
 import org.apache.accumulo.server.master.state.MetaDataStateStore;
 import org.apache.accumulo.server.master.state.TServerInstance;
 import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.server.master.state.TabletState;
 import org.apache.hadoop.io.Text;
 import org.junit.Assert;
 import org.junit.Test;
@@ -119,10 +118,10 @@ public class TestMergeState {
     MetaDataStateStore metaDataStateStore = new MetaDataStateStore(instance, auths, state);
     int count = 0;
     for (TabletLocationState tss : metaDataStateStore) {
      Assert.assertEquals(TabletState.HOSTED, tss.getState(state.onlineTabletServers()));
      count++;
      if (tss != null)
        count++;
     }
    Assert.assertEquals(splits.length + 1, count);
    Assert.assertEquals(0, count); // the normal case is to skip tablets in a good state
 
     // Create the hole
     // Split the tablet at one end of the range
diff --git a/test/src/test/java/org/apache/accumulo/test/ShellServerTest.java b/test/src/test/java/org/apache/accumulo/test/ShellServerTest.java
index 1dfb5aeae..aaa2f555f 100644
-- a/test/src/test/java/org/apache/accumulo/test/ShellServerTest.java
++ b/test/src/test/java/org/apache/accumulo/test/ShellServerTest.java
@@ -100,6 +100,7 @@ public class ShellServerTest {
   private static class NoOpErrorMessageCallback extends ErrorMessageCallback {
     private static final String empty = "";
 
    @Override
     public String getErrorMessage() {
       return empty;
     }
@@ -108,7 +109,8 @@ public class ShellServerTest {
   private static final NoOpErrorMessageCallback noop = new NoOpErrorMessageCallback();
 
   private static String secret = "superSecret";
  public static TemporaryFolder folder = new TemporaryFolder();
  private static File baseDir = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + ShellServerTest.class.getName());
  public static TemporaryFolder folder;
   public static MiniAccumuloCluster cluster;
   public TestOutputStream output;
   public Shell shell;
@@ -182,6 +184,8 @@ public class ShellServerTest {
 
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
    baseDir.mkdirs();
    folder = new TemporaryFolder(baseDir);
     folder.create();
     MiniAccumuloConfig cfg = new MiniAccumuloConfig(folder.newFolder("miniAccumulo"), secret);
     cluster = new MiniAccumuloCluster(cfg);
@@ -215,7 +219,6 @@ public class ShellServerTest {
   public static void tearDownAfterClass() throws Exception {
     cluster.stop();
     traceProcess.destroy();
    folder.delete();
   }
 
   @Test(timeout = 60000)
@@ -453,6 +456,7 @@ public class ShellServerTest {
     for (int i = 0; i < 9 && !success; i++) {
       try {
         exec("insert a b c d -l foo", false, "does not have authorization", true, new ErrorMessageCallback() {
          @Override
           public String getErrorMessage() {
             try {
               Connector c = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers()).getConnector("root", new PasswordToken(secret));
@@ -470,6 +474,7 @@ public class ShellServerTest {
     // If we still couldn't do it, try again and let it fail
     if (!success) {
       exec("insert a b c d -l foo", false, "does not have authorization", true, new ErrorMessageCallback() {
        @Override
         public String getErrorMessage() {
           try {
             Connector c = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers()).getConnector("root", new PasswordToken(secret));
@@ -872,6 +877,7 @@ public class ShellServerTest {
     s.addScanIterator(cfg);
 
     Thread thread = new Thread() {
      @Override
       public void run() {
         try {
           for (@SuppressWarnings("unused")
- 
2.19.1.windows.1

