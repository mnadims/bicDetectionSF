From b66a2dc7237e15d106962405f1fc1aa615d57cb6 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Fri, 3 Jan 2014 11:12:51 -0800
Subject: [PATCH] OOZIE-1660 DB connection misconfig causes all or most unit
 tests to fail (rkanter)

--
 core/src/main/java/org/apache/oozie/service/JPAService.java | 2 +-
 core/src/test/java/org/apache/oozie/test/XTestCase.java     | 2 ++
 release-log.txt                                             | 1 +
 3 files changed, 4 insertions(+), 1 deletion(-)

diff --git a/core/src/main/java/org/apache/oozie/service/JPAService.java b/core/src/main/java/org/apache/oozie/service/JPAService.java
index 4d6d13a88..aba8709eb 100644
-- a/core/src/main/java/org/apache/oozie/service/JPAService.java
++ b/core/src/main/java/org/apache/oozie/service/JPAService.java
@@ -112,7 +112,7 @@ public class JPAService implements Service, Instrumentable {
         String maxConn = conf.get(CONF_MAX_ACTIVE_CONN, "10").trim();
         String dataSource = conf.get(CONF_CONN_DATA_SOURCE, "org.apache.commons.dbcp.BasicDataSource");
         String connPropsConfig = conf.get(CONF_CONN_PROPERTIES);
        boolean autoSchemaCreation = conf.getBoolean(CONF_CREATE_DB_SCHEMA, true);
        boolean autoSchemaCreation = conf.getBoolean(CONF_CREATE_DB_SCHEMA, false);
         boolean validateDbConn = conf.getBoolean(CONF_VALIDATE_DB_CONN, true);
         String evictionInterval = conf.get(CONF_VALIDATE_DB_CONN_EVICTION_INTERVAL, "300000").trim();
         String evictionNum = conf.get(CONF_VALIDATE_DB_CONN_EVICTION_NUM, "10").trim();
diff --git a/core/src/test/java/org/apache/oozie/test/XTestCase.java b/core/src/test/java/org/apache/oozie/test/XTestCase.java
index a6ec626cd..1536927bd 100644
-- a/core/src/test/java/org/apache/oozie/test/XTestCase.java
++ b/core/src/test/java/org/apache/oozie/test/XTestCase.java
@@ -353,6 +353,8 @@ public abstract class XTestCase extends TestCase {
         // Disable sharelib service as it cannot find the sharelib jars
         // as maven has target/classes in classpath and not the jar because test phase is before package phase
         oozieSiteConf.set(Services.CONF_SERVICE_CLASSES, classes.replaceAll("org.apache.oozie.service.ShareLibService,",""));
        // Make sure to create the Oozie DB during unit tests
        oozieSiteConf.set(JPAService.CONF_CREATE_DB_SCHEMA, "true");
         File target = new File(testCaseConfDir, "oozie-site.xml");
         oozieSiteConf.writeXml(new FileOutputStream(target));
 
diff --git a/release-log.txt b/release-log.txt
index 7f4613f16..ed25a073b 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1660 DB connection misconfig causes all or most unit tests to fail (rkanter)
 OOZIE-1641 Oozie should mask the signature secret in the configuration output (rkanter)
 OOZIE-1655 Change oozie.service.JPAService.validate.db.connection to true (rkanter)
 OOZIE-1643 Oozie doesn't parse Hadoop Job Id from the Hive action (rkanter)
- 
2.19.1.windows.1

