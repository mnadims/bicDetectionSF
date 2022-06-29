From 57d1762e124e16bba572e444146df892be3d3add Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Thu, 2 Jan 2014 10:06:34 -0800
Subject: [PATCH] OOZIE-1655 Change
 oozie.service.JPAService.validate.db.connection to true (rkanter)

--
 core/src/main/java/org/apache/oozie/service/JPAService.java | 2 +-
 core/src/main/resources/oozie-default.xml                   | 4 ++--
 release-log.txt                                             | 1 +
 3 files changed, 4 insertions(+), 3 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/service/JPAService.java b/core/src/main/java/org/apache/oozie/service/JPAService.java
index 908e473c7..4d6d13a88 100644
-- a/core/src/main/java/org/apache/oozie/service/JPAService.java
++ b/core/src/main/java/org/apache/oozie/service/JPAService.java
@@ -113,7 +113,7 @@ public class JPAService implements Service, Instrumentable {
         String dataSource = conf.get(CONF_CONN_DATA_SOURCE, "org.apache.commons.dbcp.BasicDataSource");
         String connPropsConfig = conf.get(CONF_CONN_PROPERTIES);
         boolean autoSchemaCreation = conf.getBoolean(CONF_CREATE_DB_SCHEMA, true);
        boolean validateDbConn = conf.getBoolean(CONF_VALIDATE_DB_CONN, false);
        boolean validateDbConn = conf.getBoolean(CONF_VALIDATE_DB_CONN, true);
         String evictionInterval = conf.get(CONF_VALIDATE_DB_CONN_EVICTION_INTERVAL, "300000").trim();
         String evictionNum = conf.get(CONF_VALIDATE_DB_CONN_EVICTION_NUM, "10").trim();
 
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 0eed43dcd..1009b8543 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -1184,7 +1184,7 @@
 
     <property>
         <name>oozie.service.JPAService.create.db.schema</name>
        <value>true</value>
        <value>false</value>
         <description>
             Creates Oozie DB.
 
@@ -1195,7 +1195,7 @@
 
     <property>
         <name>oozie.service.JPAService.validate.db.connection</name>
        <value>false</value>
        <value>true</value>
         <description>
             Validates DB connections from the DB connection pool.
             If the 'oozie.service.JPAService.create.db.schema' property is set to true, this property is ignored.
diff --git a/release-log.txt b/release-log.txt
index 633a09f25..a14c3c85e 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1655 Change oozie.service.JPAService.validate.db.connection to true (rkanter)
 OOZIE-1643 Oozie doesn't parse Hadoop Job Id from the Hive action (rkanter)
 OOZIE-1632 Coordinators that undergo change endtime but are doneMaterialization, not getting picked for StatusTransit (mona)
 OOZIE-1548 OozieDBCLI changes to convert clob to blob and remove the discriminator column (virag)
- 
2.19.1.windows.1

