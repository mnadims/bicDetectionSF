From 92f4079e54a1fad847ca6721f5ef6b72a625a6b2 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Mon, 30 Jun 2014 09:06:38 -0700
Subject: [PATCH] OOZIE-1907 DB upgrade from 3.3.0 to trunk fails on derby
 (rkanter)

--
 release-log.txt                               |  1 +
 .../org/apache/oozie/tools/OozieDBCLI.java    | 20 +++++++++++++------
 2 files changed, 15 insertions(+), 6 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index 45b241479..dbfad8ee0 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1907 DB upgrade from 3.3.0 to trunk fails on derby (rkanter)
 OOZIE-1877 Setting to fail oozie server startup in case of sharelib misconfiguration (puru via rohini)
 OOZIE-1388 Add a admin servlet to show thread stack trace and CPU usage per thread (rohini)
 OOZIE-1893 Recovery service will never recover bundle action if CoordSubmitXCommand command is lost (puru via rohini)
diff --git a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
index 0733e6f3b..556cbe5d5 100644
-- a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
++ b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
@@ -570,7 +570,7 @@ public class OozieDBCLI {
                         && tableName.equals("COORD_ACTIONS") && column.equals("push_missing_dependencies")) {
                     // The push_missing_depdencies column was added in DB_VERSION_FOR_4_0 as TEXT and we're going to convert it to
                     // BYTEA in DB_VERSION_FOR_5_0.  However, if Oozie 5 did the upgrade from DB_VERSION_PRE_4_0 to
                    // DB_VERSION_FOR_4_0 (and is now doing it for DB_VERSION_FOR_5_0 push_missing_depdencies will already be a
                    // DB_VERSION_FOR_4_0 (and is now doing it for DB_VERSION_FOR_5_0) push_missing_depdencies will already be a
                     // BYTEA because Oozie 5 created the column instead of Oozie 4; and the update query below will fail.
                     continue;
                 }
@@ -598,7 +598,7 @@ public class OozieDBCLI {
         System.out.println("DONE");
     }
 
    private void convertClobToBlobinDerby(Connection conn) throws Exception {
    private void convertClobToBlobinDerby(Connection conn, String startingVersion) throws Exception {
         if (conn == null) {
             return;
         }
@@ -612,13 +612,21 @@ public class OozieDBCLI {
             }
             ResultSet rs = statement.executeQuery(getSelectQuery(tableName, columnNames));
             while (rs.next()) {
                for (int i = 0; i < columnNames.size(); i++) {
                    Clob confClob = rs.getClob(columnNames.get(i));
                for (String column : columnNames) {
                    if (startingVersion.equals(DB_VERSION_PRE_4_0)
                            && tableName.equals("COORD_ACTIONS") && column.equals("push_missing_dependencies")) {
                        // The push_missing_depdencies column was added in DB_VERSION_FOR_4_0 as a CLOB and we're going to convert
                        // it to BLOB in DB_VERSION_FOR_5_0.  However, if Oozie 5 did the upgrade from DB_VERSION_PRE_4_0 to
                        // DB_VERSION_FOR_4_0 (and is now doing it for DB_VERSION_FOR_5_0) push_missing_depdencies will already be a
                        // BLOB because Oozie 5 created the column instead of Oozie 4; and the update query below will fail.
                        continue;
                    }
                    Clob confClob = rs.getClob(column);
                     if (confClob == null) {
                         continue;
                     }
                     PreparedStatement ps = conn.prepareStatement("update " + tableName + " set " + TEMP_COLUMN_PREFIX
                            + columnNames.get(i) + "=? where id = ?");
                            + column + "=? where id = ?");
                     byte[] data = IOUtils.toByteArray(confClob.getCharacterStream(), "UTF-8");
                     ps.setBinaryStream(1, new ByteArrayInputStream(data), data.length);
                     ps.setString(2, rs.getString(1));
@@ -676,7 +684,7 @@ public class OozieDBCLI {
             convertClobToBlobInPostgres(sqlFile, conn, startingVersion);
         }
         else if (dbVendor.equals("derby")) {
            convertClobToBlobinDerby(conn);
            convertClobToBlobinDerby(conn, startingVersion);
         }
         System.out.println("Dropping discriminator column");
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
- 
2.19.1.windows.1

