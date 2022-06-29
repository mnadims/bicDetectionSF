From 222b014584083f101173bbaecd89649128330854 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Fri, 24 Jan 2014 10:12:42 -0800
Subject: [PATCH] OOZIE-1674 DB upgrade from 3.3.0 to trunk fails on postgres
 (rkanter)

--
 release-log.txt                               |  1 +
 .../org/apache/oozie/tools/OozieDBCLI.java    | 41 ++++++++++++-------
 2 files changed, 27 insertions(+), 15 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index 1bc5e7d9c..05cb661c2 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1674 DB upgrade from 3.3.0 to trunk fails on postgres (rkanter)
 OOZIE-1581 Workflow performance optimizations (mona)
 OOZIE-1663 Queuedump to display command type (shwethags via virag)
 OOZIE-1672 UI info fetch fails for bundle having large number of coordinators (puru via rohini)
diff --git a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
index 9b650b78d..116be5221 100644
-- a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
++ b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
@@ -202,6 +202,7 @@ public class OozieDBCLI {
             createOozieSysTable(sqlFile, run, DB_VERSION_PRE_4_0);
         }
         String ver = getOozieDBVersion().trim();
        String startingVersion = ver;
         if (ver.equals(DB_VERSION_FOR_5_0)) {
             System.out.println("Oozie DB already upgraded to Oozie version '" + version + "'");
             return;
@@ -216,7 +217,7 @@ public class OozieDBCLI {
             }
             else if (ver.equals(DB_VERSION_FOR_4_0)) {
                 System.out.println("Upgrading to db schema for Oozie " + version);
                upgradeDBto50(sqlFile, run);
                upgradeDBto50(sqlFile, run, startingVersion);
                 ver = run ? getOozieDBVersion().trim() : DB_VERSION_FOR_5_0;
             }
         }
@@ -234,9 +235,9 @@ public class OozieDBCLI {
         ddlTweaks(sqlFile, run);
     }
 
    private void upgradeDBto50(String sqlFile, boolean run) throws Exception {
    private void upgradeDBto50(String sqlFile, boolean run, String startingVersion) throws Exception {
         upgradeOozieDBVersion(sqlFile, run, DB_VERSION_FOR_5_0);
        ddlTweaksFor50(sqlFile, run);
        ddlTweaksFor50(sqlFile, run, startingVersion);
     }
 
     private final static String UPDATE_OOZIE_VERSION =
@@ -244,9 +245,11 @@ public class OozieDBCLI {
             + "' where name='oozie.version'";
 
     private void upgradeOozieDBVersion(String sqlFile, boolean run, String version) throws Exception {
        String updateDBVersion = "update OOZIE_SYS set data='" + version + "' where name='db.version'";
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
         writer.println();
         writer.println(UPDATE_OOZIE_VERSION);
        writer.println(updateDBVersion);
         writer.close();
         System.out.println("Update db.version in OOZIE_SYS table to " + version);
         if (run) {
@@ -254,7 +257,7 @@ public class OozieDBCLI {
             try {
                 conn.setAutoCommit(true);
                 Statement st = conn.createStatement();
                st.executeUpdate("update OOZIE_SYS set data='" + version + "' where name='db.version'");
                st.executeUpdate(updateDBVersion);
                 st.executeUpdate(UPDATE_OOZIE_VERSION);
                 st.close();
             }
@@ -421,7 +424,7 @@ public class OozieDBCLI {
         }
     }
 
    private void convertClobToBlobInOracle(boolean run, Connection conn) throws Exception {
    private void convertClobToBlobInOracle(Connection conn) throws Exception {
         if (conn == null) {
             return ;
         }
@@ -484,7 +487,7 @@ public class OozieDBCLI {
         System.out.println("Done");
     }
 
    private void convertClobToBlobInMysql(String sqlFile, boolean run, Connection conn) throws Exception {
    private void convertClobToBlobInMysql(String sqlFile, Connection conn) throws Exception {
         System.out.println("Converting mediumtext/text columns to mediumblob for all tables");
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
         writer.println();
@@ -510,7 +513,7 @@ public class OozieDBCLI {
         System.out.println("Done");
     }
 
    private void convertClobToBlobInPostgres(String sqlFile, boolean run, Connection conn) throws Exception {
    private void convertClobToBlobInPostgres(String sqlFile, Connection conn, String startingVersion) throws Exception {
         System.out.println("Converting text columns to bytea for all tables");
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
         writer.println();
@@ -519,10 +522,18 @@ public class OozieDBCLI {
             String tableName = tableClobColumnMap.getKey();
             List<String> columnNames = tableClobColumnMap.getValue();
             for (String column : columnNames) {
                if (startingVersion.equals(DB_VERSION_PRE_4_0)
                        && tableName.equals("COORD_ACTIONS") && column.equals("push_missing_dependencies")) {
                    // The push_missing_depdencies column was added in DB_VERSION_FOR_4_0 as TEXT and we're going to convert it to
                    // BYTEA in DB_VERSION_FOR_5_0.  However, if Oozie 5 did the upgrade from DB_VERSION_PRE_4_0 to
                    // DB_VERSION_FOR_4_0 (and is now doing it for DB_VERSION_FOR_5_0 push_missing_depdencies will already be a
                    // BYTEA because Oozie 5 created the column instead of Oozie 4; and the update query below will fail.
                    continue;
                }
                 String addQuery = getAddColumnQuery(tableName, TEMP_COLUMN_PREFIX + column, "bytea");
                 writer.println(addQuery + ";");
                String updateQuery = "update " + tableName + " set " + TEMP_COLUMN_PREFIX + column + "=decode(replace("
                        + column + ", '\\', '\\\\'), 'escape')";
                String updateQuery = "update " + tableName + " set " + TEMP_COLUMN_PREFIX + column + "=(decode(replace("
                        + column + ", E'\\\\', E'\\\\\\\\'), 'escape'))";
                 writer.println(updateQuery + ";");
                 String dropQuery = getDropColumnQuery(tableName, column);
                 writer.println(dropQuery + ";");
@@ -543,7 +554,7 @@ public class OozieDBCLI {
         System.out.println("DONE");
     }
 
    private void convertClobToBlobinDerby(boolean run, Connection conn) throws Exception {
    private void convertClobToBlobinDerby(Connection conn) throws Exception {
         if (conn == null) {
             return;
         }
@@ -607,21 +618,21 @@ public class OozieDBCLI {
         return selectQuery.toString();
     }
 
    private void ddlTweaksFor50(String sqlFile, boolean run) throws Exception {
    private void ddlTweaksFor50(String sqlFile, boolean run, String startingVersion) throws Exception {
         String dbVendor = getDBVendor();
         Connection conn = (run) ? createConnection() : null;
 
         if (dbVendor.equals("oracle")) {
            convertClobToBlobInOracle(run, conn);
            convertClobToBlobInOracle(conn);
         }
         else if (dbVendor.equals("mysql")) {
            convertClobToBlobInMysql(sqlFile, run, conn);
            convertClobToBlobInMysql(sqlFile, conn);
         }
         else if (dbVendor.equals("postgresql")) {
            convertClobToBlobInPostgres(sqlFile, run, conn);
            convertClobToBlobInPostgres(sqlFile, conn, startingVersion);
         }
         else if (dbVendor.equals("derby")) {
            convertClobToBlobinDerby(run, conn);
            convertClobToBlobinDerby(conn);
         }
         System.out.println("Dropping discriminator column");
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
- 
2.19.1.windows.1

