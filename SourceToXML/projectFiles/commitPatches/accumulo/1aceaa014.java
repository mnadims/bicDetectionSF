From 1aceaa01453ebc1709eac1f3eb3908280310a78e Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Wed, 20 Jul 2016 17:32:09 -0400
Subject: [PATCH] ACCUMULO-4381 Cease use of Process#isAlive()

This method only exists in JDK8. We cannot use it in Accumulo 1.8
--
 .../accumulo/test/master/SuspendedTabletsIT.java      | 11 ++++++++++-
 1 file changed, 10 insertions(+), 1 deletion(-)

diff --git a/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java b/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java
index edd1aff6b..898f4293f 100644
-- a/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java
++ b/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java
@@ -88,6 +88,15 @@ public class SuspendedTabletsIT extends ConfigurableMacBase {
     cfg.setNumTservers(TSERVERS);
   }
 
  private boolean isAlive(Process p) {
    try {
      p.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      return true;
    }
  }

   @Test
   public void crashAndResumeTserver() throws Exception {
     // Run the test body. When we get to the point where we need a tserver to go away, get rid of it via crashing
@@ -137,7 +146,7 @@ public class SuspendedTabletsIT extends ConfigurableMacBase {
           List<ProcessReference> deadProcs = new ArrayList<>();
           for (ProcessReference pr : getCluster().getProcesses().get(ServerType.TABLET_SERVER)) {
             Process p = pr.getProcess();
            if (!p.isAlive()) {
            if (!isAlive(p)) {
               deadProcs.add(pr);
             }
           }
- 
2.19.1.windows.1

