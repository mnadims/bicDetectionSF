From 8ca266face9f7848fde26dd83dafbd45ff7eedb1 Mon Sep 17 00:00:00 2001
From: Virag Kothari <virag@yahoo-inc.com>
Date: Mon, 30 Dec 2013 11:49:52 -0800
Subject: [PATCH] OOZIE-1548 OozieDBCLI changes to convert clob to blob and
 remove the discriminator column (virag)

--
 release-log.txt                               |   1 +
 .../org/apache/oozie/tools/OozieDBCLI.java    | 473 ++++++++++++------
 .../apache/oozie/tools/TestOozieDBCLI.java    |  11 +-
 .../oozie/tools/TestOozieMySqlDBCLI.java      |   2 +-
 4 files changed, 320 insertions(+), 167 deletions(-)

diff --git a/release-log.txt b/release-log.txt
index 618219044..3aa3186c7 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1548 OozieDBCLI changes to convert clob to blob and remove the discriminator column (virag)
 OOZIE-1504 Allow specifying a fixed instance as the start instance of a data-in (puru via rohini)
 OOZIE-1576 Add documentation for Oozie Sqoop CLI (bowenzhangusa via rkanter)
 OOZIE-1616 Add sharelib and launcherlib locations to the instrumentation info (rkanter)
diff --git a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
index e3e70f2cb..9b650b78d 100644
-- a/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
++ b/tools/src/main/java/org/apache/oozie/tools/OozieDBCLI.java
@@ -21,23 +21,27 @@ import org.apache.commons.cli.CommandLine;
 import org.apache.commons.cli.Option;
 import org.apache.commons.cli.Options;
 import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.BuildInfo;
 import org.apache.oozie.cli.CLIParser;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 
import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.PrintWriter;
import java.sql.Blob;
import java.sql.CallableStatement;
 import java.sql.Clob;
 import java.sql.Connection;
import java.sql.DatabaseMetaData;
 import java.sql.DriverManager;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.Statement;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
@@ -53,7 +57,12 @@ public class OozieDBCLI {
     public static final String POST_UPGRADE_CMD = "postupgrade";
     public static final String SQL_FILE_OPT = "sqlfile";
     public static final String RUN_OPT = "run";
    private final static String DB_VERSION = "2";
    private final static String DB_VERSION_PRE_4_0 = "1";
    private final static String DB_VERSION_FOR_4_0 = "2";
    final static String DB_VERSION_FOR_5_0 = "3";
    private final static String DISCRIMINATOR_COLUMN = "bean_type";
    private final static String TEMP_COLUMN_PREFIX = "temp_";
    private HashMap <String, List<String>> clobColumnMap;
 
     public static final String[] HELP_INFO = {
         "",
@@ -173,8 +182,7 @@ public class OozieDBCLI {
         validateDBSchema(false);
         verifyOozieSysTable(false);
         createUpgradeDB(sqlFile, run, true);
        createOozieSysTable(sqlFile, run);
        setSQLMediumTextFlag(sqlFile, run);
        createOozieSysTable(sqlFile, run, DB_VERSION_FOR_5_0);
         System.out.println();
         if (run) {
             System.out.println("Oozie DB has been created for Oozie version '" +
@@ -187,63 +195,66 @@ public class OozieDBCLI {
         validateConnection();
         validateDBSchema(true);
         verifyDBState();
        String version = BuildInfo.getBuildInfo().getProperty(BuildInfo.BUILD_VERSION);
 
        if (!verifyOozieSysTable(false, false)) { // If OOZIE_SYS table doesn't exist (pre 3.2)
            upgradeDBTo40(sqlFile, run, false);
        if (!verifyOozieSysTable(false, false)) { // If OOZIE_SYS table doesn't
                                                  // exist (pre 3.2)
            createOozieSysTable(sqlFile, run, DB_VERSION_PRE_4_0);
         }
        else {
            String ver = getOozieDBVersion().trim();
            if (ver.equals("1")) { // if db.version equals to 1 (after 3.2+), need to upgrade
                upgradeDBTo40(sqlFile, run, true);
        String ver = getOozieDBVersion().trim();
        if (ver.equals(DB_VERSION_FOR_5_0)) {
            System.out.println("Oozie DB already upgraded to Oozie version '" + version + "'");
            return;
        }
        createUpgradeDB(sqlFile, run, false);

        while (!ver.equals(DB_VERSION_FOR_5_0)) {
            if (ver.equals(DB_VERSION_PRE_4_0)) {
                System.out.println("Upgrading to db schema for Oozie 4.0");
                upgradeDBTo40(sqlFile, run);
                ver = run ? getOozieDBVersion().trim() : DB_VERSION_FOR_4_0;
             }
            else if (ver.equals(DB_VERSION)) { // if db.version equals to 2, it's already upgraded
                throw new Exception("Oozie DB has already been upgraded");
            else if (ver.equals(DB_VERSION_FOR_4_0)) {
                System.out.println("Upgrading to db schema for Oozie " + version);
                upgradeDBto50(sqlFile, run);
                ver = run ? getOozieDBVersion().trim() : DB_VERSION_FOR_5_0;
             }
         }
 
         if (run) {
             System.out.println();
            System.out.println("Oozie DB has been upgraded to Oozie version '"
                    + BuildInfo.getBuildInfo().getProperty(BuildInfo.BUILD_VERSION) + "'");
            System.out.println("Oozie DB has been upgraded to Oozie version '" + version + "'");
         }
         System.out.println();
     }
 
    private void upgradeDBTo40(String sqlFile, boolean run, boolean fromVerOne) throws Exception {
        createUpgradeDB(sqlFile, run, false);
        if (fromVerOne) {
            upgradeOozieDBVersion(sqlFile, run);
        }
        else {
            createOozieSysTable(sqlFile, run);
        }
    private void upgradeDBTo40(String sqlFile, boolean run) throws Exception {
        upgradeOozieDBVersion(sqlFile, run, DB_VERSION_FOR_4_0);
         postUpgradeTasks(sqlFile, run, false);
         ddlTweaks(sqlFile, run);
        if (!fromVerOne || verifySQLMediumText()) {
            doSQLMediumTextTweaks(sqlFile, run);
            setSQLMediumTextFlag(sqlFile, run);
        }
     }
 
    private final static String UPDATE_DB_VERSION =
            "update OOZIE_SYS set data='" + DB_VERSION + "' where name='db.version'";
    private void upgradeDBto50(String sqlFile, boolean run) throws Exception {
        upgradeOozieDBVersion(sqlFile, run, DB_VERSION_FOR_5_0);
        ddlTweaksFor50(sqlFile, run);
    }

     private final static String UPDATE_OOZIE_VERSION =
             "update OOZIE_SYS set data='" + BuildInfo.getBuildInfo().getProperty(BuildInfo.BUILD_VERSION)
             + "' where name='oozie.version'";
 
    private void upgradeOozieDBVersion(String sqlFile, boolean run) throws Exception {
    private void upgradeOozieDBVersion(String sqlFile, boolean run, String version) throws Exception {
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
         writer.println();
        writer.println(UPDATE_DB_VERSION);
         writer.println(UPDATE_OOZIE_VERSION);
         writer.close();
        System.out.println("Update db.version in OOZIE_SYS table to " + DB_VERSION);
        System.out.println("Update db.version in OOZIE_SYS table to " + version);
         if (run) {
             Connection conn = createConnection();
             try {
                 conn.setAutoCommit(true);
                 Statement st = conn.createStatement();
                st.executeUpdate(UPDATE_DB_VERSION);
                st.executeUpdate("update OOZIE_SYS set data='" + version + "' where name='db.version'");
                 st.executeUpdate(UPDATE_OOZIE_VERSION);
                 st.close();
             }
@@ -258,14 +269,19 @@ public class OozieDBCLI {
     }
 
     private void postUpgradeDB(String sqlFile, boolean run) throws Exception {
        postUpgradeDBTo40(sqlFile, run);
        String version = getOozieDBVersion();
        if (getOozieDBVersion().equals(DB_VERSION_FOR_4_0)) {
            postUpgradeDBTo40(sqlFile, run);
        }
        else {
            System.out.println("No Post ugprade updates available for " + version);
        }
     }
 
     private void postUpgradeDBTo40(String sqlFile, boolean run) throws Exception {
         validateConnection();
         validateDBSchema(true);
         verifyOozieSysTable(true);
        verifyOozieDBVersion();
         verifyDBState();
         postUpgradeTasks(sqlFile, run, true);
         if (run) {
@@ -405,6 +421,255 @@ public class OozieDBCLI {
         }
     }
 
    private void convertClobToBlobInOracle(boolean run, Connection conn) throws Exception {
        if (conn == null) {
            return ;
        }
        System.out.println("Converting clob columns to blob for all tables");
        Statement statement = conn.createStatement();
        CallableStatement tempBlobCall = conn.prepareCall("{call dbms_lob.CREATETEMPORARY(?, TRUE)}");
        tempBlobCall.registerOutParameter(1, java.sql.Types.BLOB);
        CallableStatement dbmsLobCallStmt = conn.prepareCall("{call dbms_lob.CONVERTTOBLOB(?, ?, ?, ?, ?, 0, ?, ?)}");
        dbmsLobCallStmt.registerOutParameter(1, java.sql.Types.BLOB);
        // Lob max size
        dbmsLobCallStmt.setInt(3, Integer.MAX_VALUE);
        dbmsLobCallStmt.registerOutParameter(4, java.sql.Types.INTEGER);
        // dest_offset
        dbmsLobCallStmt.setInt(4, 1);
        // src_offset
        dbmsLobCallStmt.registerOutParameter(5, java.sql.Types.INTEGER);
        dbmsLobCallStmt.setInt(5, 1);
        // blob_csid
        dbmsLobCallStmt.registerOutParameter(6, java.sql.Types.INTEGER);
        // lang_context
        dbmsLobCallStmt.setInt(6, 0);
        // warning
        dbmsLobCallStmt.registerOutParameter(7, java.sql.Types.INTEGER);
        dbmsLobCallStmt.setInt(7, 1);
        for (Map.Entry<String, List<String>> tableClobColumnMap : getTableClobColumnMap().entrySet()) {
            String tableName = tableClobColumnMap.getKey();
            List<String> columnNames = tableClobColumnMap.getValue();
            for (String column : columnNames) {
                statement.executeUpdate(getAddColumnQuery(tableName, TEMP_COLUMN_PREFIX + column, "blob"));
            }
            ResultSet rs = statement.executeQuery(getSelectQuery(tableName, columnNames));
            while (rs.next()) {
                for (int i = 0; i < columnNames.size(); i++) {
                    Clob srcClob = rs.getClob(columnNames.get(i));
                    if (srcClob == null) {
                        continue;
                    }
                    tempBlobCall.execute();
                    Blob destLob = tempBlobCall.getBlob(1);
                    dbmsLobCallStmt.setBlob(1, destLob);
                    dbmsLobCallStmt.setClob(2, srcClob);
                    dbmsLobCallStmt.execute();
                    Blob blob = dbmsLobCallStmt.getBlob(1);
                    PreparedStatement ps = conn.prepareStatement("update " + tableName + " set " + TEMP_COLUMN_PREFIX
                            + columnNames.get(i) + "=? where id = ?");
                    ps.setBlob(1, blob);
                    ps.setString(2, rs.getString(1));
                    ps.executeUpdate();
                    ps.close();
                }
            }
            rs.close();
            for (String column : columnNames) {
                statement.executeUpdate(getDropColumnQuery(tableName, column));
                statement.executeUpdate(getRenameColumnQuery(tableName, TEMP_COLUMN_PREFIX + column, column));
            }
        }
        dbmsLobCallStmt.close();
        tempBlobCall.close();
        System.out.println("Done");
    }

    private void convertClobToBlobInMysql(String sqlFile, boolean run, Connection conn) throws Exception {
        System.out.println("Converting mediumtext/text columns to mediumblob for all tables");
        PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
        writer.println();
        Statement statement = conn != null ? conn.createStatement() : null;
        for (Map.Entry<String, List<String>> tableClobColumnMap : getTableClobColumnMap().entrySet()) {
            String tableName = tableClobColumnMap.getKey();
            List<String> columnNames = tableClobColumnMap.getValue();
            StringBuilder modifyColumn = new StringBuilder();
            modifyColumn.append(" ALTER TABLE " + tableName);
            for (String column : columnNames) {
                modifyColumn.append(" MODIFY " + column + " mediumblob,");
            }
            modifyColumn.replace(modifyColumn.length() - 1, modifyColumn.length(), "");
            writer.println(modifyColumn.toString() + ";");
            if (statement != null) {
                statement.executeUpdate(modifyColumn.toString());
            }
        }
        writer.close();
        if (statement != null) {
            statement.close();
        }
        System.out.println("Done");
    }

    private void convertClobToBlobInPostgres(String sqlFile, boolean run, Connection conn) throws Exception {
        System.out.println("Converting text columns to bytea for all tables");
        PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
        writer.println();
        Statement statement = conn != null ? conn.createStatement() : null;
        for (Map.Entry<String, List<String>> tableClobColumnMap : getTableClobColumnMap().entrySet()) {
            String tableName = tableClobColumnMap.getKey();
            List<String> columnNames = tableClobColumnMap.getValue();
            for (String column : columnNames) {
                String addQuery = getAddColumnQuery(tableName, TEMP_COLUMN_PREFIX + column, "bytea");
                writer.println(addQuery + ";");
                String updateQuery = "update " + tableName + " set " + TEMP_COLUMN_PREFIX + column + "=decode(replace("
                        + column + ", '\\', '\\\\'), 'escape')";
                writer.println(updateQuery + ";");
                String dropQuery = getDropColumnQuery(tableName, column);
                writer.println(dropQuery + ";");
                String renameQuery = getRenameColumnQuery(tableName, TEMP_COLUMN_PREFIX + column, column);
                writer.println(renameQuery + ";");
                if (statement != null) {
                    statement.executeUpdate(addQuery);
                    statement.executeUpdate(updateQuery);
                    statement.executeUpdate(dropQuery);
                    statement.executeUpdate(renameQuery);
                }
            }
        }
        writer.close();
        if (statement != null) {
            statement.close();
        }
        System.out.println("DONE");
    }

    private void convertClobToBlobinDerby(boolean run, Connection conn) throws Exception {
        if (conn == null) {
            return;
        }
        System.out.println("Converting clob columns to blob for all tables");
        Statement statement = conn.createStatement();
        for (Map.Entry<String, List<String>> tableClobColumnMap : getTableClobColumnMap().entrySet()) {
            String tableName = tableClobColumnMap.getKey();
            List<String> columnNames = tableClobColumnMap.getValue();
            for (String column : columnNames) {
                statement.executeUpdate(getAddColumnQuery(tableName, TEMP_COLUMN_PREFIX + column, "blob"));
            }
            ResultSet rs = statement.executeQuery(getSelectQuery(tableName, columnNames));
            while (rs.next()) {
                for (int i = 0; i < columnNames.size(); i++) {
                    Clob confClob = rs.getClob(columnNames.get(i));
                    if (confClob == null) {
                        continue;
                    }
                    PreparedStatement ps = conn.prepareStatement("update " + tableName + " set " + TEMP_COLUMN_PREFIX
                            + columnNames.get(i) + "=? where id = ?");
                    byte[] data = IOUtils.toByteArray(confClob.getCharacterStream(), "UTF-8");
                    ps.setBinaryStream(1, new ByteArrayInputStream(data), data.length);
                    ps.setString(2, rs.getString(1));
                    ps.executeUpdate();
                    ps.close();
                }
            }
            rs.close();
            for (String column : columnNames) {
                statement.executeUpdate(getDropColumnQuery(tableName, column));
                statement.executeUpdate("RENAME COLUMN " + tableName + "." + TEMP_COLUMN_PREFIX + column + " TO "
                        + column);
            }
        }
        statement.close();
        System.out.println("DONE");
    }

    private String getRenameColumnQuery(String tableName, String srcColumn, String destColumn) {
        return new String("ALTER TABLE " + tableName + " RENAME column " + srcColumn + " TO " + destColumn);
    }

    private String getDropColumnQuery(String tableName, String column) {
        return new String("ALTER TABLE " + tableName + " DROP column " + column);
    }

    private String getAddColumnQuery(String tableName, String tempColumn, String type) {
        return new String("ALTER TABLE " + tableName + " ADD " + tempColumn + " " + type);
    }

    private String getSelectQuery(String tableName, List<String> columnNames) {
        StringBuilder selectQuery = new StringBuilder();
        selectQuery.append("SELECT id,");
        for (String column : columnNames) {
            selectQuery.append(column);
            selectQuery.append(",");
        }
        selectQuery.replace(selectQuery.length() - 1, selectQuery.length(), "");
        selectQuery.append(" FROM ");
        selectQuery.append(tableName);
        return selectQuery.toString();
    }

    private void ddlTweaksFor50(String sqlFile, boolean run) throws Exception {
        String dbVendor = getDBVendor();
        Connection conn = (run) ? createConnection() : null;

        if (dbVendor.equals("oracle")) {
            convertClobToBlobInOracle(run, conn);
        }
        else if (dbVendor.equals("mysql")) {
            convertClobToBlobInMysql(sqlFile, run, conn);
        }
        else if (dbVendor.equals("postgresql")) {
            convertClobToBlobInPostgres(sqlFile, run, conn);
        }
        else if (dbVendor.equals("derby")) {
            convertClobToBlobinDerby(run, conn);
        }
        System.out.println("Dropping discriminator column");
        PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
        writer.println();
        ArrayList<String> ddlQueries = new ArrayList<String>();
        ddlQueries.add(getDropColumnQuery("WF_JOBS", DISCRIMINATOR_COLUMN));
        ddlQueries.add(getDropColumnQuery("WF_ACTIONS", DISCRIMINATOR_COLUMN));
        ddlQueries.add(getDropColumnQuery("COORD_JOBS", DISCRIMINATOR_COLUMN));
        ddlQueries.add(getDropColumnQuery("COORD_ACTIONS", DISCRIMINATOR_COLUMN));
        ddlQueries.add(getDropColumnQuery("BUNDLE_JOBS", DISCRIMINATOR_COLUMN));
        ddlQueries.add(getDropColumnQuery("BUNDLE_ACTIONS", DISCRIMINATOR_COLUMN));
        Statement stmt = conn != null ? conn.createStatement() : null;
        for (String query : ddlQueries) {
            writer.println(query + ";");
            if (run) {
                stmt.executeUpdate(query);
            }
        }
        System.out.println("DONE");
        writer.close();
        if (run) {
            stmt.close();
            conn.close();
        }
    }

    private Map<String, List<String>> getTableClobColumnMap() {
        if (clobColumnMap != null) {
            return clobColumnMap;
        }
        else {
            clobColumnMap = new HashMap<String, List<String>>();
            clobColumnMap.put("WF_ACTIONS",
                    new ArrayList<String>(Arrays.asList("conf", "sla_xml", "data", "stats", "external_child_ids")));
            clobColumnMap.put("WF_JOBS", new ArrayList<String>(Arrays.asList("proto_action_conf", "sla_xml", "conf")));
            clobColumnMap.put(
                    "COORD_ACTIONS",
                    new ArrayList<String>(Arrays.asList("sla_xml", "created_conf", "run_conf", "action_xml",
                            "missing_dependencies", "push_missing_dependencies")));
            clobColumnMap.put("COORD_JOBS",
                    new ArrayList<String>(Arrays.asList("conf", "job_xml", "orig_job_xml", "sla_xml")));
            clobColumnMap.put("BUNDLE_JOBS", new ArrayList<String>(Arrays.asList("conf", "job_xml", "orig_job_xml")));

        }
        return clobColumnMap;
    }


     private void ddlTweaks(String sqlFile, boolean run) throws Exception {
         PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
         writer.println();
@@ -512,90 +777,6 @@ public class OozieDBCLI {
         "ALTER TABLE COORD_JOBS DROP COLUMN AUTH_TOKEN",
         "ALTER TABLE WF_JOBS DROP COLUMN AUTH_TOKEN"};
 
    private final static String SET_SQL_MEDIUMTEXT_TRUE = "insert into OOZIE_SYS (name, data) values ('mysql.mediumtext', 'true')";

    private void setSQLMediumTextFlag(String sqlFile, boolean run) throws Exception {
        if (getDBVendor().equals("mysql")) {
            PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
            writer.println();
            writer.println(SET_SQL_MEDIUMTEXT_TRUE);
            writer.close();
            System.out.println("Set MySQL MEDIUMTEXT flag");
            if (run) {
                Connection conn = createConnection();
                try {
                    conn.setAutoCommit(true);
                    Statement st = conn.createStatement();
                    st.executeUpdate(SET_SQL_MEDIUMTEXT_TRUE);
                    st.close();
                }
                catch (Exception ex) {
                    throw new Exception("Could not set MySQL MEDIUMTEXT flag: " + ex.toString(), ex);
                }
                finally {
                    conn.close();
                }
            }
            System.out.println("DONE");
        }
    }

    private final static String[] SQL_MEDIUMTEXT_DDL_QUERIES = {"ALTER TABLE BUNDLE_JOBS MODIFY conf MEDIUMTEXT",
                                                                "ALTER TABLE BUNDLE_JOBS MODIFY job_xml MEDIUMTEXT",
                                                                "ALTER TABLE BUNDLE_JOBS MODIFY orig_job_xml MEDIUMTEXT",

                                                                "ALTER TABLE COORD_ACTIONS MODIFY action_xml MEDIUMTEXT",
                                                                "ALTER TABLE COORD_ACTIONS MODIFY created_conf MEDIUMTEXT",
                                                                "ALTER TABLE COORD_ACTIONS MODIFY missing_dependencies MEDIUMTEXT",
                                                                "ALTER TABLE COORD_ACTIONS MODIFY run_conf MEDIUMTEXT",
                                                                "ALTER TABLE COORD_ACTIONS MODIFY sla_xml MEDIUMTEXT",

                                                                "ALTER TABLE COORD_JOBS MODIFY conf MEDIUMTEXT",
                                                                "ALTER TABLE COORD_JOBS MODIFY job_xml MEDIUMTEXT",
                                                                "ALTER TABLE COORD_JOBS MODIFY orig_job_xml MEDIUMTEXT",
                                                                "ALTER TABLE COORD_JOBS MODIFY sla_xml MEDIUMTEXT",

                                                                "ALTER TABLE SLA_EVENTS MODIFY job_data MEDIUMTEXT",
                                                                "ALTER TABLE SLA_EVENTS MODIFY notification_msg MEDIUMTEXT",
                                                                "ALTER TABLE SLA_EVENTS MODIFY upstream_apps MEDIUMTEXT",

                                                                "ALTER TABLE WF_ACTIONS MODIFY conf MEDIUMTEXT",
                                                                "ALTER TABLE WF_ACTIONS MODIFY external_child_ids MEDIUMTEXT",
                                                                "ALTER TABLE WF_ACTIONS MODIFY stats MEDIUMTEXT",
                                                                "ALTER TABLE WF_ACTIONS MODIFY data MEDIUMTEXT",
                                                                "ALTER TABLE WF_ACTIONS MODIFY sla_xml MEDIUMTEXT",

                                                                "ALTER TABLE WF_JOBS MODIFY conf MEDIUMTEXT",
                                                                "ALTER TABLE WF_JOBS MODIFY proto_action_conf MEDIUMTEXT",
                                                                "ALTER TABLE WF_JOBS MODIFY sla_xml MEDIUMTEXT"};


    private void doSQLMediumTextTweaks(String sqlFile, boolean run) throws Exception {
        if (getDBVendor().equals("mysql")) {
            PrintWriter writer = new PrintWriter(new FileWriter(sqlFile, true));
            writer.println();
            Connection conn = (run) ? createConnection() : null;
            try {
                System.out.println("All MySQL TEXT columns changed to MEDIUMTEXT");
                for (String ddlQuery : SQL_MEDIUMTEXT_DDL_QUERIES) {
                    writer.println(ddlQuery + ";");
                    if (run) {
                        conn.setAutoCommit(true);
                        Statement st = conn.createStatement();
                        st.executeUpdate(ddlQuery);
                        st.close();
                    }
                }
                writer.close();
                System.out.println("DONE");
            }
            finally {
                if (run) {
                    conn.close();
                }
            }
        }
    }
 
     private Connection createConnection() throws Exception {
         Map<String, String> conf = getJdbcConf();
@@ -674,15 +855,6 @@ public class OozieDBCLI {
 
     private final static String GET_OOZIE_DB_VERSION = "select data from OOZIE_SYS where name = 'db.version'";
 
    private void verifyOozieDBVersion() throws Exception {
        System.out.println("Verify Oozie DB version");
        String version = getOozieDBVersion();
        if (!DB_VERSION.equals(version.trim())) {
            throw new Exception("ERROR: Expected Oozie DB version '" + DB_VERSION + "', found '" + version.trim() + "'");
        }
        System.out.println("DONE");
    }

     private String getOozieDBVersion() throws Exception {
         String version;
         System.out.println("Get Oozie DB version");
@@ -709,41 +881,9 @@ public class OozieDBCLI {
         return version;
     }
 
    private final static String GET_USE_MYSQL_MEDIUMTEXT = "select data from OOZIE_SYS where name = 'mysql.mediumtext'";

    private boolean verifySQLMediumText() throws Exception {
        boolean ret = false;
        if (getDBVendor().equals("mysql")) {
            System.out.println("Check MySQL MEDIUMTEXT flag exists");
            String flag = null;
            Connection conn = createConnection();
            try {
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(GET_USE_MYSQL_MEDIUMTEXT);
                rs.next();
                flag = rs.getString(1).trim();
                rs.close();
                st.close();
            }
            catch (Exception ex) {
                flag = null;
            }
            finally {
                conn.close();
            }
            if (flag == null) {
                ret = true;
            }
            System.out.println("DONE");
        }
        return ret;
    }

     private final static String CREATE_OOZIE_SYS =
         "create table OOZIE_SYS (name varchar(100), data varchar(100))";
 
    private final static String SET_DB_VERSION =
        "insert into OOZIE_SYS (name, data) values ('db.version', '" + DB_VERSION + "')";
 
     private final static String SET_OOZIE_VERSION =
         "insert into OOZIE_SYS (name, data) values ('oozie.version', '" +
@@ -752,7 +892,8 @@ public class OozieDBCLI {
     private final static String CREATE_OOZIE_SYS_INDEX =
         "create clustered index OOZIE_SYS_PK on OOZIE_SYS (name);";
 
    private void createOozieSysTable(String sqlFile, boolean run) throws Exception {
    private void createOozieSysTable(String sqlFile, boolean run, String version) throws Exception {
        String insertDbVerion = "insert into OOZIE_SYS (name, data) values ('db.version', '" + version + "')";
         // Some databases do not support tables without a clustered index
         // so we need to explicitly create a clustered index for OOZIE_SYS table
         boolean createIndex = getDBVendor().equals("sqlserver");
@@ -763,7 +904,7 @@ public class OozieDBCLI {
         if (createIndex){
             writer.println(CREATE_OOZIE_SYS_INDEX);
         }
        writer.println(SET_DB_VERSION);
        writer.println(insertDbVerion);
         writer.println(SET_OOZIE_VERSION);
         writer.close();
         System.out.println("Create OOZIE_SYS table");
@@ -776,7 +917,7 @@ public class OozieDBCLI {
                 if (createIndex){
                     st.executeUpdate(CREATE_OOZIE_SYS_INDEX);
                 }
                st.executeUpdate(SET_DB_VERSION);
                st.executeUpdate(insertDbVerion);
                 st.executeUpdate(SET_OOZIE_VERSION);
                 st.close();
             }
@@ -856,6 +997,8 @@ public class OozieDBCLI {
             args.add("-sqlFile");
             args.add(sqlFile);
         }
        args.add("-indexes");
        args.add("true");
         args.add("org.apache.oozie.WorkflowJobBean");
         args.add("org.apache.oozie.WorkflowActionBean");
         args.add("org.apache.oozie.CoordinatorJobBean");
@@ -874,7 +1017,11 @@ public class OozieDBCLI {
         System.out.println((create) ? "Create SQL schema" : "Upgrade SQL schema");
         String[] args = createMappingToolArguments(sqlFile);
         org.apache.openjpa.jdbc.meta.MappingTool.main(args);
        if (run) {
        // With oracle, mapping tool tries to create a table even if already
        // exists and fails
        // However the update is reflected in the database even though the below
        // block is not executed
        if (run && (create || !getDBVendor().equals("oracle"))) {
             args = createMappingToolArguments(null);
             org.apache.openjpa.jdbc.meta.MappingTool.main(args);
         }
diff --git a/tools/src/test/java/org/apache/oozie/tools/TestOozieDBCLI.java b/tools/src/test/java/org/apache/oozie/tools/TestOozieDBCLI.java
index acbb36ddd..0dc446277 100644
-- a/tools/src/test/java/org/apache/oozie/tools/TestOozieDBCLI.java
++ b/tools/src/test/java/org/apache/oozie/tools/TestOozieDBCLI.java
@@ -94,8 +94,7 @@ public class TestOozieDBCLI extends XTestCase {
             System.setOut(new PrintStream(data));
             String[] argsVersion = { "version" };
             assertEquals(0, execOozieDBCLICommands(argsVersion));

            assertTrue(data.toString().contains("db.version: 2"));
            assertTrue(data.toString().contains("db.version: "+ OozieDBCLI.DB_VERSION_FOR_5_0));
             // show help information
             data.reset();
             String[] argsHelp = { "help" };
@@ -118,6 +117,13 @@ public class TestOozieDBCLI extends XTestCase {
         execSQL("ALTER TABLE BUNDLE_JOBS ADD COLUMN AUTH_TOKEN CLOB");
         execSQL("ALTER TABLE COORD_JOBS ADD COLUMN AUTH_TOKEN CLOB");
         execSQL("ALTER TABLE WF_JOBS ADD COLUMN AUTH_TOKEN CLOB");

        execSQL("ALTER TABLE WF_JOBS ADD COLUMN BEAN_TYPE VARCHAR(31)");
        execSQL("ALTER TABLE WF_ACTIONS ADD COLUMN BEAN_TYPE VARCHAR(31)");
        execSQL("ALTER TABLE COORD_JOBS ADD COLUMN BEAN_TYPE VARCHAR(31)");
        execSQL("ALTER TABLE COORD_ACTIONS ADD COLUMN BEAN_TYPE VARCHAR(31)");
        execSQL("ALTER TABLE BUNDLE_JOBS ADD COLUMN BEAN_TYPE VARCHAR(31)");
        execSQL("ALTER TABLE BUNDLE_ACTIONS ADD COLUMN BEAN_TYPE VARCHAR(31)");
         String[] argsUpgrade = { "upgrade", "-sqlfile", upgrade.getAbsolutePath(), "-run" };
         assertEquals(0, execOozieDBCLICommands(argsUpgrade));
 
@@ -125,7 +131,6 @@ public class TestOozieDBCLI extends XTestCase {
         File postUpgrade = new File(getTestCaseConfDir() + File.separator + "postUpdate.sql");
         String[] argsPostUpgrade = { "postupgrade", "-sqlfile", postUpgrade.getAbsolutePath(), "-run" };
         assertEquals(0, execOozieDBCLICommands(argsPostUpgrade));
        assertTrue(postUpgrade.exists());
     }
 
     private int execOozieDBCLICommands(String[] args) {
diff --git a/tools/src/test/java/org/apache/oozie/tools/TestOozieMySqlDBCLI.java b/tools/src/test/java/org/apache/oozie/tools/TestOozieMySqlDBCLI.java
index eec5eee48..f7f8456ad 100644
-- a/tools/src/test/java/org/apache/oozie/tools/TestOozieMySqlDBCLI.java
++ b/tools/src/test/java/org/apache/oozie/tools/TestOozieMySqlDBCLI.java
@@ -88,7 +88,7 @@ public class TestOozieMySqlDBCLI extends XTestCase {
         FakeConnection.CREATE = false;
 
         File upgrade = new File(getTestCaseConfDir() + File.separator + "update.sql");
        String[] argsUpgrade = { "upgrade", "-sqlfile", upgrade.getAbsolutePath(), "-run"};
        String[] argsUpgrade = { "upgrade", "-sqlfile", upgrade.getAbsolutePath()};
 
         assertEquals(0, execOozieDBCLICommands(argsUpgrade));
         assertTrue(upgrade.exists());
- 
2.19.1.windows.1

