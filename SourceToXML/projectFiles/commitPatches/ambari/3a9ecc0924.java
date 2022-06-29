From 3a9ecc09249320b08c7ff69a1384ece5a90c6b63 Mon Sep 17 00:00:00 2001
From: Bob Nettleton <rnettleton@hortonworks.com>
Date: Mon, 11 Jan 2016 17:30:30 -0500
Subject: [PATCH] AMBARI-14615. Blueprints HostGroupComponentEntity primary key
 is incorrectly specified. (rnettleton)

--
 .../ambari/server/orm/entities/HostGroupComponentEntity.java  | 1 -
 .../server/orm/entities/HostGroupComponentEntityPK.java       | 4 ----
 2 files changed, 5 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java
index 046bbd8827..e917e74dfe 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntity.java
@@ -47,7 +47,6 @@ public class HostGroupComponentEntity {
   @Column(name = "name", nullable = false, insertable = true, updatable = false)
   private String name;
 
  @Id
   @Column(name = "provision_action", nullable = true, insertable = true, updatable = false)
   private String provisionAction;
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java
index 0e97346e68..fb9011b783 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/HostGroupComponentEntityPK.java
@@ -38,10 +38,6 @@ public class HostGroupComponentEntityPK {
   @Column(name = "name", nullable = false, insertable = true, updatable = false, length = 100)
   private String name;
 
  @Id
  @Column(name = "provision_action", nullable = true, insertable = true, updatable = false, length = 100)
  private String provisionAction;

   /**
    * Get the name of the associated host group.
    *
- 
2.19.1.windows.1

