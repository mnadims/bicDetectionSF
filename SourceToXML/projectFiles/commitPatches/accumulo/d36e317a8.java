From d36e317a83765984cb83aee27b7616bb2aee226d Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Wed, 10 Feb 2016 12:58:07 -0500
Subject: [PATCH] ACCUMULO-4139 Fix ExistingMacIT test bug

Make ExistingMacIT wait the full ZK timeout (plus half a second) based on mini's
configured ZK timeout value.
--
 .../src/test/java/org/apache/accumulo/test/ExistingMacIT.java | 4 +++-
 1 file changed, 3 insertions(+), 1 deletion(-)

diff --git a/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java b/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java
index 323888a56..bef24863c 100644
-- a/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ExistingMacIT.java
@@ -29,6 +29,7 @@ import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
@@ -100,7 +101,8 @@ public class ExistingMacIT extends ConfigurableMacIT {
     }
 
     // TODO clean out zookeeper? following sleep waits for ephemeral nodes to go away
    UtilWaitThread.sleep(10000);
    long zkTimeout = AccumuloConfiguration.getTimeInMillis(getCluster().getConfig().getSiteConfig().get(Property.INSTANCE_ZK_TIMEOUT.getKey()));
    UtilWaitThread.sleep(zkTimeout + 500);
 
     File hadoopConfDir = createTestDir(ExistingMacIT.class.getSimpleName() + "_hadoop_conf");
     FileUtils.deleteQuietly(hadoopConfDir);
- 
2.19.1.windows.1

