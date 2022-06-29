From 3469c1fa3a7784117b57e678ff674f067831284a Mon Sep 17 00:00:00 2001
From: Attila Doroszlai <adoroszlai@hortonworks.com>
Date: Wed, 11 Jan 2017 16:53:00 +0100
Subject: [PATCH] AMBARI-19458. Compile error in RoleGraphTest (Attila
 Doroszlai via oleewere)

Change-Id: I1ec09dac0efc870f35886a14382df0ab7a2a4b6a
--
 .../java/org/apache/ambari/server/metadata/RoleGraphTest.java   | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java b/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java
index f04efde611..4f1432d9a4 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/metadata/RoleGraphTest.java
@@ -40,11 +40,11 @@ import org.apache.ambari.server.state.Service;
 import org.apache.ambari.server.state.ServiceComponent;
 import org.apache.ambari.server.state.StackId;
 import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.hadoop.metrics2.sink.relocated.google.common.collect.Lists;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
import com.google.common.collect.Lists;
 import com.google.inject.Guice;
 import com.google.inject.Injector;
 import com.google.inject.persist.PersistService;
- 
2.19.1.windows.1

