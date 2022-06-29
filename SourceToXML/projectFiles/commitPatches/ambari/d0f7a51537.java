From d0f7a51537469740e5397486b1e2c19862c26c01 Mon Sep 17 00:00:00 2001
From: Attila Doroszlai <adoroszlai@hortonworks.com>
Date: Sun, 9 Jul 2017 12:15:28 +0200
Subject: [PATCH] AMBARI-21430. Allow Multiple Versions of Stack Tools to
 Co-Exist - fix illegal import

--
 .../org/apache/ambari/server/upgrade/UpgradeCatalog252.java     | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java
index fa3aea326f..0656f68077 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog252.java
@@ -29,7 +29,7 @@ import org.apache.ambari.server.state.Clusters;
 import org.apache.ambari.server.state.Config;
 import org.apache.ambari.server.state.ConfigHelper;
 import org.apache.ambari.server.state.PropertyInfo;
import org.apache.hadoop.metrics2.sink.relocated.commons.lang.StringUtils;
import org.apache.commons.lang.StringUtils;
 
 import com.google.common.collect.Sets;
 import com.google.inject.Inject;
- 
2.19.1.windows.1

