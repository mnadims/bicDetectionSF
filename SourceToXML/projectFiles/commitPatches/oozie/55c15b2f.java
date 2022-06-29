From 55c15b2f605ce7d0eb799faa3d5e5240ac581c0f Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Mon, 3 Feb 2014 23:17:57 -0800
Subject: [PATCH] OOZIE-1684 DB upgrade from 3.3.0 to trunk fails on Oracle
 (rkanter)

--
 release-log.txt                                            | 1 +
 tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java | 6 +-----
 2 files changed, 2 insertions(+), 5 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index f424dd393..876a25671 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1684 DB upgrade from 3.3.0 to trunk fails on Oracle (rkanter)
 OOZIE-1675 Adding absolute URI of local cluster to dist cache not working with hadoop version 0.20.2 and before (satish via ryota)
 OOZIE-1683 UserGroupInformationService should close any filesystems opened by it (rkanter)
 OOZIE-1646 HBase Table Copy between two HBase servers doesn't work with Kerberos (rkanter)
diff --git a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
index 7312faa28..a116984e6 100644
-- a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
++ b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
@@ -1038,11 +1038,7 @@ public class OozieDBCLI {
         System.out.println((create) ? "Create SQL schema" : "Upgrade SQL schema");
         String[] args = createMappingToolArguments(sqlFile);
         org.apache.openjpa.jdbc.meta.MappingTool.main(args);
        // With oracle, mapping tool tries to create a table even if already
        // exists and fails
        // However the update is reflected in the database even though the below
        // block is not executed
        if (run && (create || !getDBVendor().equals("oracle"))) {
        if (run) {
             args = createMappingToolArguments(null);
             org.apache.openjpa.jdbc.meta.MappingTool.main(args);
         }
- 
2.19.1.windows.1

