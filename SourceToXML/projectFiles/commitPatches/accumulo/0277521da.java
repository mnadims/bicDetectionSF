From 0277521daa753ac2168b7ebabc57f01baecfc02e Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Sun, 10 Aug 2014 02:14:13 -0400
Subject: [PATCH] ACCUMULO-3055 Fix the 1.6 test to verify what the current
 state is.

--
 .../minicluster/MiniAccumuloClusterStartStopTest.java | 11 ++++-------
 1 file changed, 4 insertions(+), 7 deletions(-)

diff --git a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
index 246632ccd..9e38d092f 100644
-- a/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
++ b/minicluster/src/test/java/org/apache/accumulo/minicluster/MiniAccumuloClusterStartStopTest.java
@@ -41,17 +41,14 @@ public class MiniAccumuloClusterStartStopTest {
     folder.delete();
   }
   
  // Multiple start()'s failed in 1.5, but apparently is successful in 1.6.0
  //  @Test
  @Test
   public void multipleStartsThrowsAnException() throws Exception {
     MiniAccumuloCluster accumulo = new MiniAccumuloCluster(folder.getRoot(), "superSecret");
    accumulo.start();
    

    // In 1.6.0, multiple start's did not throw an exception as advertised
     try {
       accumulo.start();
      Assert.fail("Invoking start() while already started is an error");
    } catch (IllegalStateException e) {
      // pass
      accumulo.start();
     } finally {
       accumulo.stop();
     }
- 
2.19.1.windows.1

