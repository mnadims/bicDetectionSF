From 57ffd19c11de6984bfc2123b301917c807854ca9 Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <eric.newton@gmail.com>
Date: Tue, 24 Feb 2015 14:41:51 -0500
Subject: [PATCH] ACCUMULO-3580 ACCUMULO-3618 disable metadata table scanning
 optimizations

--
 .../src/main/java/org/apache/accumulo/server/master/Master.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/server/src/main/java/org/apache/accumulo/server/master/Master.java b/server/src/main/java/org/apache/accumulo/server/master/Master.java
index 8e072e992..61ba7cf01 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/Master.java
++ b/server/src/main/java/org/apache/accumulo/server/master/Master.java
@@ -2310,8 +2310,8 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
     TCredentials systemAuths = SecurityConstants.getSystemCredentials();
     final TabletStateStore stores[] = {
         new ZooTabletStateStore(new ZooStore(zroot)),
        new RootTabletStateStore(instance, systemAuths, this),
         // ACCUMULO-3580 ACCUMULO-3618 disable metadata table scanning optimizations
        new RootTabletStateStore(instance, systemAuths, null),
         new MetaDataStateStore(instance, systemAuths, null)
         };
     watchers.add(new TabletGroupWatcher(stores[2], null));
- 
2.19.1.windows.1

