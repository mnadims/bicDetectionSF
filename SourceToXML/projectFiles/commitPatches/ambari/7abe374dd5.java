From 7abe374dd5375f908514a0e4c266e661d6b20298 Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Mon, 29 Feb 2016 17:24:37 -0500
Subject: [PATCH] AMBARI-15173 - Express Upgrade Stuck At Manual Prompt Due To
 HRC Status Calculation Cache Problem (part4) (jonathanhurley)

--
 .../ambari/server/orm/dao/HostRoleCommandDAO.java      | 10 +++++++++-
 1 file changed, 9 insertions(+), 1 deletion(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
index c25606685c..14dac797bf 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/HostRoleCommandDAO.java
@@ -634,7 +634,15 @@ public class HostRoleCommandDAO {
       entity = entityManager.merge(entity);
       managedList.add(entity);
 
      requestsToInvalidate.add(entity.getRequestId());
      Long requestId = entity.getRequestId();
      if (requestId == null) {
        StageEntity stageEntity = entity.getStage();
        if (stageEntity != null) {
          requestId = stageEntity.getRequestId();
        }
      }

      requestsToInvalidate.add(requestId);
     }
 
     invalidateHostRoleCommandStatusSummaryCache(requestsToInvalidate);
- 
2.19.1.windows.1

