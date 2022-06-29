From 288573dceaf6a82f3d671186e766688d53407028 Mon Sep 17 00:00:00 2001
From: Myroslav Papirkovskyi <mpapyrkovskyy@hortonworks.com>
Date: Tue, 23 Aug 2016 19:09:04 +0300
Subject: [PATCH] AMBARI-18104. Unit Tests Broken Due to AMBARI-18011.
 (mpapirkovskyy)

--
 .../dao/ServiceComponentDesiredStateDAO.java  |  2 +-
 .../AmbariManagementControllerTest.java       | 19 -------------------
 2 files changed, 1 insertion(+), 20 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ServiceComponentDesiredStateDAO.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ServiceComponentDesiredStateDAO.java
index 4c906cc489..cdaa6f0aaf 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ServiceComponentDesiredStateDAO.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/dao/ServiceComponentDesiredStateDAO.java
@@ -117,7 +117,7 @@ public class ServiceComponentDesiredStateDAO {
 
   @Transactional
   public void remove(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    entityManagerProvider.get().remove(merge(serviceComponentDesiredStateEntity));
    entityManagerProvider.get().remove(serviceComponentDesiredStateEntity);
   }
 
   @Transactional
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
index f8e891b781..3e3f1d60ae 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/AmbariManagementControllerTest.java
@@ -9280,25 +9280,6 @@ public class AmbariManagementControllerTest {
   @Test
   public void testDeleteClusterCreateHost() throws Exception {
 
    Injector injector = Guice.createInjector(new AuditLoggerModule(), new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");

        properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(),"src/test/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(),"../version");
        properties.setProperty(Configuration.OS_VERSION.getKey(), "centos6");
        properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");
        try {
          install(new ControllerModule(properties));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    injector.getInstance(GuiceJpaInitializer.class);

     String STACK_ID = "HDP-2.0.1";
 
     String CLUSTER_NAME = getUniqueName();
- 
2.19.1.windows.1

