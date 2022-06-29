From 6927fb527260359362af19c651a2f2ffd32b2d4e Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 23 Jun 2014 11:21:44 -0400
Subject: [PATCH] ACCUMULO-2935 Ensure test actually times out.

--
 .../org/apache/accumulo/test/functional/MonitorLoggingIT.java   | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/test/src/test/java/org/apache/accumulo/test/functional/MonitorLoggingIT.java b/test/src/test/java/org/apache/accumulo/test/functional/MonitorLoggingIT.java
index 331a546a4..bfee21370 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/MonitorLoggingIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/MonitorLoggingIT.java
@@ -56,7 +56,7 @@ public class MonitorLoggingIT extends ConfigurableMacIT {
     return (NUM_LOCATION_PASSES + 2) * LOCATION_DELAY;
   }
 
  @Test
  @Test(timeout = (NUM_LOCATION_PASSES + 2) * LOCATION_DELAY)
   public void logToMonitor() throws Exception {
     // Start the monitor.
     log.debug("Starting Monitor");
- 
2.19.1.windows.1

