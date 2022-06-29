From 8a2cb5b35a6c9b7a98185e96808551123e7fc647 Mon Sep 17 00:00:00 2001
From: Artem Baranchuk <abaranchuk@hortonworks.con>
Date: Fri, 19 Jun 2015 16:32:03 +0300
Subject: [PATCH] AMBARI-11991 - Fixing of leak resources which potentially can
 reduce system performance and security (Dmytro Shkvyra via abaranchuk)

--
 .../timeline/PhoenixHBaseAccessor.java        |  18 +-
 .../timeline/query/PhoenixTransactSQL.java    | 153 ++++----
 .../ambari/eventdb/db/PostgresConnector.java  |  12 +-
 .../ambari/server/agent/HeartBeatHandler.java |   7 +-
 .../server/api/services/AmbariMetaInfo.java   |   4 +-
 .../services/serializers/CsvSerializer.java   |  23 +-
 .../gsinstaller/ClusterDefinition.java        |  10 +-
 .../internal/URLStreamProvider.java           |  13 +-
 .../controller/jdbc/JDBCResourceProvider.java | 154 ++++----
 .../metrics/RestMetricsPropertyProvider.java  |   9 +-
 .../ambari/server/orm/DBAccessorImpl.java     | 337 ++++++++++--------
 .../server/orm/entities/ViewEntity.java       |   1 +
 .../server/orm/helpers/ScriptRunner.java      |  14 +-
 .../CredentialStoreServiceImpl.java           |   9 +-
 .../server/upgrade/UpgradeCatalog170.java     |  86 +++--
 .../server/view/ViewArchiveUtility.java       |  30 +-
 .../ambari/server/view/ViewRegistry.java      |   4 +-
 .../CredentialStoreServiceTest.java           |   1 -
 .../ambari/server/view/ViewRegistryTest.java  |  14 +-
 .../org/apache/ambari/shell/AmbariShell.java  |   1 -
 .../shell/commands/BlueprintCommands.java     |  11 +-
 .../shell/commands/ElephantCommand.java       |  13 +-
 .../shell/customization/AmbariBanner.java     |  18 +-
 .../ambari/shell/flash/InstallProgress.java   |   6 +-
 24 files changed, 585 insertions(+), 363 deletions(-)

diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/PhoenixHBaseAccessor.java b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/PhoenixHBaseAccessor.java
index 8e5d101ebb..d018f2984c 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/PhoenixHBaseAccessor.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/PhoenixHBaseAccessor.java
@@ -396,7 +396,7 @@ public class PhoenixHBaseAccessor {
     try {
       //get latest
       if(condition.isPointInTime()){
        stmt = getLatestMetricRecords(condition, conn, metrics);
        getLatestMetricRecords(condition, conn, metrics);
       } else {
         stmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
         rs = stmt.executeQuery();
@@ -465,7 +465,7 @@ public class PhoenixHBaseAccessor {
     }
   }
 
  private PreparedStatement getLatestMetricRecords(
  private void getLatestMetricRecords(
     Condition condition, Connection conn, TimelineMetrics metrics)
     throws SQLException, IOException {
 
@@ -490,9 +490,10 @@ public class PhoenixHBaseAccessor {
           // Ignore
         }
       }
      if (stmt != null) {
        stmt.close();
      }
     }

    return stmt;
   }
 
   /**
@@ -515,7 +516,7 @@ public class PhoenixHBaseAccessor {
     try {
       //get latest
       if(condition.isPointInTime()) {
        stmt = getLatestAggregateMetricRecords(condition, conn, metrics, metricFunctions);
        getLatestAggregateMetricRecords(condition, conn, metrics, metricFunctions);
       } else {
         stmt = PhoenixTransactSQL.prepareGetAggregateSqlStmt(conn, condition);
 
@@ -577,7 +578,7 @@ public class PhoenixHBaseAccessor {
     }
   }
 
  private PreparedStatement getLatestAggregateMetricRecords(Condition condition,
  private void getLatestAggregateMetricRecords(Condition condition,
       Connection conn, TimelineMetrics metrics,
       Map<String, List<Function>> metricFunctions) throws SQLException {
 
@@ -619,10 +620,11 @@ public class PhoenixHBaseAccessor {
             // Ignore
           }
         }
        if (stmt != null) {
          stmt.close();
        }        
       }
     }

    return stmt;
   }
 
   private SingleValuedTimelineMetric getAggregateTimelineMetricFromResultSet(ResultSet rs,
diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/query/PhoenixTransactSQL.java b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/query/PhoenixTransactSQL.java
index 2606773329..71f53caa8c 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/query/PhoenixTransactSQL.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/query/PhoenixTransactSQL.java
@@ -318,7 +318,9 @@ public class PhoenixTransactSQL {
     if (LOG.isDebugEnabled()) {
       LOG.debug("SQL: " + sb.toString() + ", condition: " + condition);
     }
    PreparedStatement stmt = connection.prepareStatement(sb.toString());
    PreparedStatement stmt = null;
    try {
    stmt = connection.prepareStatement(sb.toString());
     int pos = 1;
     if (condition.getMetricNames() != null) {
       for (; pos <= condition.getMetricNames().size(); pos++) {
@@ -363,6 +365,12 @@ public class PhoenixTransactSQL {
     if (condition.getFetchSize() != null) {
       stmt.setFetchSize(condition.getFetchSize());
     }
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
      }
      throw e;
    }
 
     return stmt;
   }
@@ -375,7 +383,7 @@ public class PhoenixTransactSQL {
 
   private static void validateRowCountLimit(Condition condition) {
     if (condition.getMetricNames() == null
      || condition.getMetricNames().size() ==0 ) {
      || condition.getMetricNames().isEmpty() ) {
       //aggregator can use empty metrics query
       return;
     }
@@ -399,14 +407,14 @@ public class PhoenixTransactSQL {
   }
 
   public static PreparedStatement prepareGetLatestMetricSqlStmt(
    Connection connection, Condition condition) throws SQLException {
          Connection connection, Condition condition) throws SQLException {
 
     validateConditionIsNotEmpty(condition);
 
     if (condition.getMetricNames() == null
      || condition.getMetricNames().size() == 0) {
      throw new IllegalArgumentException("Point in time query without " +
        "metric names not supported ");
            || condition.getMetricNames().isEmpty()) {
      throw new IllegalArgumentException("Point in time query without "
              + "metric names not supported ");
     }
 
     String stmtStr;
@@ -415,15 +423,15 @@ public class PhoenixTransactSQL {
     } else {
       //if not a single metric for a single host
       if (condition.getHostnames().size() > 1
        && condition.getMetricNames().size() > 1) {
              && condition.getMetricNames().size() > 1) {
         stmtStr = String.format(GET_LATEST_METRIC_SQL,
          METRICS_RECORD_TABLE_NAME,
          METRICS_RECORD_TABLE_NAME,
          condition.getConditionClause());
                METRICS_RECORD_TABLE_NAME,
                METRICS_RECORD_TABLE_NAME,
                condition.getConditionClause());
       } else {
         StringBuilder sb = new StringBuilder(String.format(GET_METRIC_SQL,
          "",
          METRICS_RECORD_TABLE_NAME));
                "",
                METRICS_RECORD_TABLE_NAME));
         sb.append(" WHERE ");
         sb.append(condition.getConditionClause());
         String orderByClause = condition.getOrderByClause(false);
@@ -439,47 +447,55 @@ public class PhoenixTransactSQL {
     if (LOG.isDebugEnabled()) {
       LOG.debug("SQL: " + stmtStr + ", condition: " + condition);
     }
    PreparedStatement stmt = connection.prepareStatement(stmtStr);
    int pos = 1;
    if (condition.getMetricNames() != null) {
      //IGNORE condition limit, set one based on number of metric names
      for (; pos <= condition.getMetricNames().size(); pos++) {
    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement(stmtStr);
      int pos = 1;
      if (condition.getMetricNames() != null) {
        //IGNORE condition limit, set one based on number of metric names
        for (; pos <= condition.getMetricNames().size(); pos++) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting pos: " + pos + ", value = " + condition.getMetricNames().get(pos - 1));
          }
          stmt.setString(pos, condition.getMetricNames().get(pos - 1));
        }
      }
      if (condition.getHostnames() != null) {
        for (String hostname : condition.getHostnames()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting pos: " + pos + ", value: " + hostname);
          }
          stmt.setString(pos++, hostname);
        }
      }
      if (condition.getAppId() != null) {
         if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value = " + condition.getMetricNames().get(pos - 1));
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getAppId());
         }
        stmt.setString(pos, condition.getMetricNames().get(pos - 1));
        stmt.setString(pos++, condition.getAppId());
       }
    }
    if (condition.getHostnames() != null) {
      for (String hostname: condition.getHostnames()) {
      if (condition.getInstanceId() != null) {
         if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + hostname);
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getInstanceId());
         }
        stmt.setString(pos++, hostname);
        stmt.setString(pos, condition.getInstanceId());
       }
    }
    if (condition.getAppId() != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting pos: " + pos + ", value: " + condition.getAppId());

      if (condition.getFetchSize() != null) {
        stmt.setFetchSize(condition.getFetchSize());
       }
      stmt.setString(pos++, condition.getAppId());
    }
    if (condition.getInstanceId() != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting pos: " + pos + ", value: " + condition.getInstanceId());
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
       }
      stmt.setString(pos, condition.getInstanceId());
    }

    if (condition.getFetchSize() != null) {
      stmt.setFetchSize(condition.getFetchSize());
      throw e;
     }
 
     return stmt;
   }
 
   public static PreparedStatement prepareGetAggregateSqlStmt(
    Connection connection, Condition condition) throws SQLException {
          Connection connection, Condition condition) throws SQLException {
 
     validateConditionIsNotEmpty(condition);
 
@@ -519,8 +535,8 @@ public class PhoenixTransactSQL {
     }
 
     queryStmt = String.format(queryStmt,
      getNaiveTimeRangeHint(condition.getStartTime(), NATIVE_TIME_RANGE_DELTA),
      metricsAggregateTable);
            getNaiveTimeRangeHint(condition.getStartTime(), NATIVE_TIME_RANGE_DELTA),
            metricsAggregateTable);
 
     StringBuilder sb = new StringBuilder(queryStmt);
     sb.append(" WHERE ");
@@ -534,25 +550,34 @@ public class PhoenixTransactSQL {
     if (LOG.isDebugEnabled()) {
       LOG.debug("SQL => " + query + ", condition => " + condition);
     }
    PreparedStatement stmt = connection.prepareStatement(query);
    int pos = 1;
    if (condition.getMetricNames() != null) {
      for (; pos <= condition.getMetricNames().size(); pos++) {
        stmt.setString(pos, condition.getMetricNames().get(pos - 1));
    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement(query);
      int pos = 1;

      if (condition.getMetricNames() != null) {
        for (; pos <= condition.getMetricNames().size(); pos++) {
          stmt.setString(pos, condition.getMetricNames().get(pos - 1));
        }
       }
    }
    // TODO: Upper case all strings on POST
    if (condition.getAppId() != null) {
      stmt.setString(pos++, condition.getAppId());
    }
    if (condition.getInstanceId() != null) {
      stmt.setString(pos++, condition.getInstanceId());
    }
    if (condition.getStartTime() != null) {
      stmt.setLong(pos++, condition.getStartTime());
    }
    if (condition.getEndTime() != null) {
      stmt.setLong(pos, condition.getEndTime());
      // TODO: Upper case all strings on POST
      if (condition.getAppId() != null) {
        stmt.setString(pos++, condition.getAppId());
      }
      if (condition.getInstanceId() != null) {
        stmt.setString(pos++, condition.getInstanceId());
      }
      if (condition.getStartTime() != null) {
        stmt.setLong(pos++, condition.getStartTime());
      }
      if (condition.getEndTime() != null) {
        stmt.setLong(pos, condition.getEndTime());
      }
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
      }
      throw e;
     }
 
     return stmt;
@@ -588,7 +613,9 @@ public class PhoenixTransactSQL {
       LOG.debug("SQL: " + query + ", condition: " + condition);
     }
 
    PreparedStatement stmt = connection.prepareStatement(query);
    PreparedStatement stmt = null;
    try {
    stmt = connection.prepareStatement(query);
     int pos = 1;
     if (condition.getMetricNames() != null) {
       for (; pos <= condition.getMetricNames().size(); pos++) {
@@ -604,6 +631,12 @@ public class PhoenixTransactSQL {
     if (condition.getInstanceId() != null) {
       stmt.setString(pos, condition.getInstanceId());
     }
    } catch (SQLException e) {
      if (stmt != null) {
        
      }
      throw e;
    }
 
     return stmt;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/eventdb/db/PostgresConnector.java b/ambari-server/src/main/java/org/apache/ambari/eventdb/db/PostgresConnector.java
index 552df9cfef..421d0019e8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/eventdb/db/PostgresConnector.java
++ b/ambari-server/src/main/java/org/apache/ambari/eventdb/db/PostgresConnector.java
@@ -164,8 +164,12 @@ public class PostgresConnector implements DBConnector {
       throw new IOException(e);
     } finally {
       try {
        if (rs != null)
        if (rs != null){
           rs.close();
        }
        if (ps != null) {
          ps.close();
        }
       } catch (SQLException e) {
         LOG.error("Exception while closing ResultSet", e);
       }
@@ -193,8 +197,12 @@ public class PostgresConnector implements DBConnector {
       throw new IOException(e);
     } finally {
       try {
        if (rs != null)
        if (rs != null) {
           rs.close();
        }
        if (ps != null) {
          ps.close();
        }        
       } catch (SQLException e) {
         LOG.error("Exception while closing ResultSet", e);
       }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java b/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java
index 1c993ef38e..6f34b62d78 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/agent/HeartBeatHandler.java
@@ -1125,7 +1125,12 @@ public class HeartBeatHandler {
                   keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS));
 
                   BufferedInputStream bufferedIn = new BufferedInputStream(new FileInputStream(keytabFile));
                  byte[] keytabContent = IOUtils.toByteArray(bufferedIn);
                  byte[] keytabContent = null;
                  try {
                    keytabContent = IOUtils.toByteArray(bufferedIn);
                  } finally {
                    bufferedIn.close();
                  }
                   String keytabContentBase64 = Base64.encodeBase64String(keytabContent);
                   keytabMap.put(KerberosServerAction.KEYTAB_CONTENT_BASE64, keytabContentBase64);
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java
index a77f7b1fa9..4afa9b0ca5 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/AmbariMetaInfo.java
@@ -736,7 +736,9 @@ public class AmbariMetaInfo {
     if (!versionFile.exists()) {
       throw new AmbariException("Server version file does not exist.");
     }
    serverVersion = new Scanner(versionFile).useDelimiter("\\Z").next();
    Scanner scanner = new Scanner(versionFile);
    serverVersion = scanner.useDelimiter("\\Z").next();
    scanner.close();
   }
 
   private void getCustomActionDefinitions(File customActionDefinitionRoot) throws JAXBException, AmbariException {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/services/serializers/CsvSerializer.java b/ambari-server/src/main/java/org/apache/ambari/server/api/services/serializers/CsvSerializer.java
index 87751dc728..04dd1abe2a 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/services/serializers/CsvSerializer.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/services/serializers/CsvSerializer.java
@@ -32,6 +32,8 @@ import java.util.Collection;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
 
 /**
  * CSV serializer used to generate a CSV-formatted document from a result.
@@ -77,7 +79,7 @@ public class CsvSerializer implements ResultSerializer {
     if (result.getStatus().isErrorState()) {
       return serializeError(result.getStatus());
     } else {

      CSVPrinter csvPrinter = null;
       try {
         // A StringBuffer to store the CSV-formatted document while building it.  It may be
         // necessary to use file-based storage if the data set is expected to be really large.
@@ -86,7 +88,7 @@ public class CsvSerializer implements ResultSerializer {
         TreeNode<Resource> root = result.getResultTree();
 
         if (root != null) {
          CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT);
          csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT);
 
           // TODO: recursively handle tree structure, for now only handle single level of detail
           if ("true".equalsIgnoreCase(root.getStringProperty("isCollection"))) {
@@ -107,15 +109,23 @@ public class CsvSerializer implements ResultSerializer {
       } catch (IOException e) {
         //todo: exception handling.  Create ResultStatus 500 and call serializeError
         throw new RuntimeException("Unable to serialize to csv: " + e, e);
      } finally {
        if (csvPrinter != null) {
          try {
            csvPrinter.close();
          } catch (IOException ex) {
          }
        }
       }
     }
   }
 
   @Override
   public Object serializeError(ResultStatus error) {
    CSVPrinter csvPrinter = null;
     try {
       StringBuffer buffer = new StringBuffer();
      CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT);
      csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT);
 
       csvPrinter.printRecord(Arrays.asList("status", "message"));
       csvPrinter.printRecord(Arrays.asList(error.getStatus().getStatus(), error.getMessage()));
@@ -124,6 +134,13 @@ public class CsvSerializer implements ResultSerializer {
     } catch (IOException e) {
       //todo: exception handling.  Create ResultStatus 500 and call serializeError
       throw new RuntimeException("Unable to serialize to csv: " + e, e);
    } finally {
      if (csvPrinter != null) {
        try {
          csvPrinter.close();
        } catch (IOException ex) {
        }
      }
     }
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/gsinstaller/ClusterDefinition.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/gsinstaller/ClusterDefinition.java
index b6dfa30e1b..6f9876abfa 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/gsinstaller/ClusterDefinition.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/gsinstaller/ClusterDefinition.java
@@ -228,8 +228,9 @@ public class ClusterDefinition {
    * Read the gsInstaller cluster definition file.
    */
   private void readClusterDefinition() {
    InputStream is = null;
     try {
      InputStream    is = this.getClass().getClassLoader().getResourceAsStream(CLUSTER_DEFINITION_FILE);
      is = this.getClass().getClassLoader().getResourceAsStream(CLUSTER_DEFINITION_FILE);
       BufferedReader br = new BufferedReader(new InputStreamReader(is));
 
       String line;
@@ -280,6 +281,13 @@ public class ClusterDefinition {
     } catch (IOException e) {
       String msg = "Caught exception reading " + CLUSTER_DEFINITION_FILE + ".";
       throw new IllegalStateException(msg, e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
        }
      }
     }
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/URLStreamProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/URLStreamProvider.java
index 94940e6da6..bc11646396 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/URLStreamProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/URLStreamProvider.java
@@ -285,25 +285,24 @@ public class URLStreamProvider implements StreamProvider {
             LOG.error(msg);
             throw new IllegalStateException(msg);
           }

          FileInputStream in = null;
           try {
            FileInputStream in = new FileInputStream(new File(trustStorePath));
            in = new FileInputStream(new File(trustStorePath));
             KeyStore store = KeyStore.getInstance(trustStoreType == null ?
                 KeyStore.getDefaultType() : trustStoreType);

             store.load(in, trustStorePassword.toCharArray());
            in.close();

             TrustManagerFactory tmf = TrustManagerFactory
                 .getInstance(TrustManagerFactory.getDefaultAlgorithm());

             tmf.init(store);
             SSLContext context = SSLContext.getInstance("TLS");
             context.init(null, tmf.getTrustManagers(), null);

             sslSocketFactory = context.getSocketFactory();
           } catch (Exception e) {
             throw new IOException("Can't get connection.", e);
          } finally {
            if (in != null) {
              in.close();
            }
           }
         }
       }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/jdbc/JDBCResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/jdbc/JDBCResourceProvider.java
index 2f2eab1998..e9695059ce 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/jdbc/JDBCResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/jdbc/JDBCResourceProvider.java
@@ -40,6 +40,7 @@ import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
import java.util.logging.Level;
 
 /**
  * Generic JDBC based resource provider.
@@ -162,86 +163,109 @@ public class JDBCResourceProvider extends BaseProvider implements ResourceProvid
                UnsupportedPropertyException,
                ResourceAlreadyExistsException,
                NoSuchParentResourceException {

        Connection connection = null;
         try {
            Connection connection = connectionFactory.getConnection();

            connection = connectionFactory.getConnection();
            Statement statement = null;
             try {

                 Set<Map<String, Object>> propertySet = request.getProperties();

                statement = connection.createStatement(); 
                 for (Map<String, Object> properties : propertySet) {
                     String sql = getInsertSQL(properties);

                    Statement statement = connection.createStatement();

                     statement.execute(sql);

                    statement.close();
                 }
             } finally {
                connection.close();
              if (statement != null) {
                statement.close();
              }  
             }
 
         } catch (SQLException e) {
             throw new IllegalStateException("DB error : ", e);
        } finally {
          if (connection != null) {
            try {
              connection.close();
            } catch (SQLException ex) {
              throw new IllegalStateException("DB error : ", ex);
            }
          }
         }
 
         return getRequestStatus();
     }
 
     @Override
    public RequestStatus updateResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

        try {
            Connection connection = connectionFactory.getConnection();
            try {
                Set<Map<String, Object>> propertySet = request.getProperties();
  public RequestStatus updateResources(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Connection connection = null;
    try {
      connection = connectionFactory.getConnection();
      Statement statement = null;
      try {
        Set<Map<String, Object>> propertySet = request.getProperties();
 
                Map<String, Object> properties = propertySet.iterator().next();
        Map<String, Object> properties = propertySet.iterator().next();
 
                String sql = getUpdateSQL(properties, predicate);
        String sql = getUpdateSQL(properties, predicate);
 
                Statement statement = connection.createStatement();
        statement = connection.createStatement();
 
                statement.execute(sql);
        statement.execute(sql);
 
                statement.close();
            } finally {
                connection.close();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("DB error : ", e);
      } finally {
        if (statement != null) {
          statement.close();
         }
      }
 
        return getRequestStatus();
    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ex) {
          throw new IllegalStateException("DB error : ", ex);
        }
      }
     }
 
    @Override
    public RequestStatus deleteResources(Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    return getRequestStatus();
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Connection connection = null;
    try {
      connection = connectionFactory.getConnection();
      Statement statement = null;
      try {
        String sql = getDeleteSQL(predicate);
        statement = connection.createStatement();
        statement.execute(sql);
      } finally {
        if (statement != null) {
          statement.close();
        }
      }
 
    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    } finally {
      if (connection != null) {
         try {
            Connection connection = connectionFactory.getConnection();
            try {
                String sql = getDeleteSQL(predicate);

                Statement statement = connection.createStatement();
                statement.execute(sql);
                statement.close();
            } finally {
                connection.close();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("DB error : ", e);
          connection.close();
        } catch (SQLException ex) {
          throw new IllegalStateException("DB error : ", ex);
         }

        return getRequestStatus();
      }
     }
 
    return getRequestStatus();
  }

 
     private String getInsertSQL(Map<String, Object> properties) {
 
@@ -409,28 +433,34 @@ public class JDBCResourceProvider extends BaseProvider implements ResourceProvid
      * @param table      the table
      * @throws SQLException thrown if the meta data for the given connection cannot be obtained
      */
    private void getImportedKeys(Connection connection, String table) throws SQLException {
        if (!this.importedKeys.containsKey(table)) {

            Map<String, String> importedKeys = new HashMap<String, String>();
            this.importedKeys.put(table, importedKeys);
  private void getImportedKeys(Connection connection, String table) throws SQLException {
    if (!this.importedKeys.containsKey(table)) {
 
            DatabaseMetaData metaData = connection.getMetaData();
      Map<String, String> importedKeys = new HashMap<String, String>();
      this.importedKeys.put(table, importedKeys);
 
            ResultSet rs = metaData.getImportedKeys(connection.getCatalog(), null, table);
      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet rs = null;
      try {
        rs = metaData.getImportedKeys(connection.getCatalog(), null, table);
 
            while (rs.next()) {
        while (rs.next()) {
 
                String pkPropertyId = PropertyHelper.getPropertyId(
                    rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"));
          String pkPropertyId = PropertyHelper.getPropertyId(
                  rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"));
 
                String fkPropertyId = PropertyHelper.getPropertyId(
                    rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"));
          String fkPropertyId = PropertyHelper.getPropertyId(
                  rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"));
 
                importedKeys.put(pkPropertyId, fkPropertyId);
            }
          importedKeys.put(pkPropertyId, fkPropertyId);
        }
      } finally {
        if (rs != null) {
          rs.close();
         }
      }
     }
  }
 
     /**
      * Get a request status
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java
index b92537b634..b32adda106 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/RestMetricsPropertyProvider.java
@@ -225,13 +225,16 @@ public class RestMetricsPropertyProvider extends ThreadPoolEnabledPropertyProvid
       try {
         InputStream in = streamProvider.readFrom(getSpec(protocol, hostname, port, url));
         if (!ticket.isValid()) {
          if (in != null) {
            in.close();
          }
           return resource;
        }
        }       
         try {
           extractValuesFromJSON(in, urls.get(url), resource, propertyInfos);
         } finally {
          in.close();
        }
            in.close();
          }
       } catch (IOException e) {
         logException(e);
       }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/DBAccessorImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/DBAccessorImpl.java
index c8916913ea..6e31eee8e1 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/DBAccessorImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/DBAccessorImpl.java
@@ -41,11 +41,12 @@ import org.eclipse.persistence.sessions.DatabaseLogin;
 import org.eclipse.persistence.sessions.DatabaseSession;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 

 import java.io.BufferedReader;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
import java.nio.charset.Charset;
 import java.sql.Blob;
 import java.sql.Connection;
 import java.sql.DatabaseMetaData;
@@ -70,7 +71,7 @@ public class DBAccessorImpl implements DBAccessor {
   private static final String dbURLPatternString = "jdbc:(.*?):.*";
   private Pattern dbURLPattern = Pattern.compile(dbURLPatternString, Pattern.CASE_INSENSITIVE);
   private DbType dbType;

  
   @Inject
   public DBAccessorImpl(Configuration configuration) {
     this.configuration = configuration;
@@ -79,14 +80,14 @@ public class DBAccessorImpl implements DBAccessor {
       Class.forName(configuration.getDatabaseDriver());
 
       connection = DriverManager.getConnection(configuration.getDatabaseUrl(),
        configuration.getDatabaseUser(),
        configuration.getDatabasePassword());
              configuration.getDatabaseUser(),
              configuration.getDatabasePassword());
 
       connection.setAutoCommit(true); //enable autocommit
 
       //TODO create own mapping and platform classes for supported databases
      String vendorName = connection.getMetaData().getDatabaseProductName() +
        connection.getMetaData().getDatabaseMajorVersion();
      String vendorName = connection.getMetaData().getDatabaseProductName()
              + connection.getMetaData().getDatabaseMajorVersion();
       String dbPlatform = DBPlatformHelper.getDBPlatform(vendorName, new AbstractSessionLog() {
         @Override
         public void log(SessionLogEntry sessionLogEntry) {
@@ -106,13 +107,13 @@ public class DBAccessorImpl implements DBAccessor {
     if (databasePlatform instanceof OraclePlatform) {
       dbType = DbType.ORACLE;
       return new OracleHelper(databasePlatform);
    }else if (databasePlatform instanceof MySQLPlatform) {
    } else if (databasePlatform instanceof MySQLPlatform) {
       dbType = DbType.MYSQL;
       return new MySqlHelper(databasePlatform);
    }else if (databasePlatform instanceof PostgreSQLPlatform) {
    } else if (databasePlatform instanceof PostgreSQLPlatform) {
       dbType = DbType.POSTGRES;
       return new PostgresHelper(databasePlatform);
    }else if (databasePlatform instanceof DerbyPlatform) {
    } else if (databasePlatform instanceof DerbyPlatform) {
       dbType = DbType.DERBY;
       return new DerbyHelper(databasePlatform);
     } else {
@@ -129,8 +130,8 @@ public class DBAccessorImpl implements DBAccessor {
   public Connection getNewConnection() {
     try {
       return DriverManager.getConnection(configuration.getDatabaseUrl(),
        configuration.getDatabaseUser(),
        configuration.getDatabasePassword());
              configuration.getDatabaseUser(),
              configuration.getDatabasePassword());
     } catch (SQLException e) {
       throw new RuntimeException("Unable to connect to database", e);
     }
@@ -143,7 +144,7 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public void createTable(String tableName, List<DBColumnInfo> columnInfo,
                          String... primaryKeyColumns) throws SQLException {
          String... primaryKeyColumns) throws SQLException {
     if (!tableExists(tableName)) {
       String query = dbmsHelper.getCreateTableStatement(tableName, columnInfo, Arrays.asList(primaryKeyColumns));
 
@@ -167,27 +168,24 @@ public class DBAccessorImpl implements DBAccessor {
     DatabaseMetaData metaData = getDatabaseMetaData();
     if (metaData.storesLowerCaseIdentifiers()) {
       return objectName.toLowerCase();
    }else if (metaData.storesUpperCaseIdentifiers()) {
    } else if (metaData.storesUpperCaseIdentifiers()) {
       return objectName.toUpperCase();
     }
 
     return objectName;
   }
 


   @Override
   public boolean tableExists(String tableName) throws SQLException {
     boolean result = false;
     DatabaseMetaData metaData = getDatabaseMetaData();
 
    ResultSet res = metaData.getTables(null, null, convertObjectName(tableName), new String[] { "TABLE" });
    ResultSet res = metaData.getTables(null, null, convertObjectName(tableName), new String[]{"TABLE"});
 
     if (res != null) {
       try {
         if (res.next()) {
          return res.getString("TABLE_NAME") != null && res.getString
            ("TABLE_NAME").equalsIgnoreCase(tableName);
          return res.getString("TABLE_NAME") != null && res.getString("TABLE_NAME").equalsIgnoreCase(tableName);
         }
       } finally {
         res.close();
@@ -208,7 +206,7 @@ public class DBAccessorImpl implements DBAccessor {
     boolean retVal = false;
     ResultSet rs = null;
     try {
       rs = statement.executeQuery(query);
      rs = statement.executeQuery(query);
       if (rs != null) {
         if (rs.next()) {
           return rs.getInt(1) > 0;
@@ -217,6 +215,9 @@ public class DBAccessorImpl implements DBAccessor {
     } catch (Exception e) {
       LOG.error("Unable to check if table " + tableName + " has any data. Exception: " + e.getMessage());
     } finally {
      if (statement != null) {
        statement.close();
      }
       if (rs != null) {
         rs.close();
       }
@@ -233,8 +234,7 @@ public class DBAccessorImpl implements DBAccessor {
     if (rs != null) {
       try {
         if (rs.next()) {
          return rs.getString("COLUMN_NAME") != null && rs.getString
            ("COLUMN_NAME").equalsIgnoreCase(columnName);
          return rs.getString("COLUMN_NAME") != null && rs.getString("COLUMN_NAME").equalsIgnoreCase(columnName);
         }
       } finally {
         rs.close();
@@ -245,7 +245,7 @@ public class DBAccessorImpl implements DBAccessor {
   }
 
   @Override
  public boolean tableHasColumn(String tableName, String... columnName) throws SQLException{
  public boolean tableHasColumn(String tableName, String... columnName) throws SQLException {
     List<String> columnsList = new ArrayList<String>(Arrays.asList(columnName));
     DatabaseMetaData metaData = getDatabaseMetaData();
 
@@ -292,19 +292,18 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public boolean tableHasForeignKey(String tableName, String refTableName,
              String columnName, String refColumnName) throws SQLException {
          String columnName, String refColumnName) throws SQLException {
     return tableHasForeignKey(tableName, refTableName, new String[]{columnName}, new String[]{refColumnName});
   }
 
   @Override
   public boolean tableHasForeignKey(String tableName, String referenceTableName, String[] keyColumns,
                                    String[] referenceColumns) throws SQLException {
          String[] referenceColumns) throws SQLException {
     DatabaseMetaData metaData = getDatabaseMetaData();
 
     //NB: reference table contains pk columns while key table contains fk columns

     ResultSet rs = metaData.getCrossReference(null, null, convertObjectName(referenceTableName),
      null, null, convertObjectName(tableName));
            null, null, convertObjectName(tableName));
 
     List<String> pkColumns = new ArrayList<String>(referenceColumns.length);
     for (String referenceColumn : referenceColumns) {
@@ -332,12 +331,10 @@ public class DBAccessorImpl implements DBAccessor {
               fkColumns.remove(fkIndex);
             }
 

           } else {
             LOG.debug("pkCol={}, fkCol={} not found in provided column names, skipping", pkColumn, fkColumn); //TODO debug
           }
 

         }
         if (pkColumns.isEmpty() && fkColumns.isEmpty()) {
           return true;
@@ -348,14 +345,13 @@ public class DBAccessorImpl implements DBAccessor {
       }
     }
 

     return false;
 
   }
 
   @Override
   public void createIndex(String indexName, String tableName,
                          String... columnNames) throws SQLException {
          String... columnNames) throws SQLException {
     String query = dbmsHelper.getCreateIndexStatement(indexName, tableName, columnNames);
 
     executeQuery(query);
@@ -363,48 +359,49 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public void addFKConstraint(String tableName, String constraintName,
                              String keyColumn, String referenceTableName,
                              String referenceColumn, boolean ignoreFailure) throws SQLException {
          String keyColumn, String referenceTableName,
          String referenceColumn, boolean ignoreFailure) throws SQLException {
 
     addFKConstraint(tableName, constraintName, new String[]{keyColumn}, referenceTableName,
        new String[]{referenceColumn}, false, ignoreFailure);
            new String[]{referenceColumn}, false, ignoreFailure);
   }

   @Override
   public void addFKConstraint(String tableName, String constraintName,
                              String keyColumn, String referenceTableName,
                              String referenceColumn, boolean shouldCascadeOnDelete,
                              boolean ignoreFailure) throws SQLException {
          String keyColumn, String referenceTableName,
          String referenceColumn, boolean shouldCascadeOnDelete,
          boolean ignoreFailure) throws SQLException {
 
     addFKConstraint(tableName, constraintName, new String[]{keyColumn}, referenceTableName,
      new String[]{referenceColumn}, shouldCascadeOnDelete, ignoreFailure);
            new String[]{referenceColumn}, shouldCascadeOnDelete, ignoreFailure);
   }
 
   @Override
   public void addFKConstraint(String tableName, String constraintName,
                              String[] keyColumns, String referenceTableName,
                              String[] referenceColumns,
                              boolean ignoreFailure) throws SQLException {
          String[] keyColumns, String referenceTableName,
          String[] referenceColumns,
          boolean ignoreFailure) throws SQLException {
     addFKConstraint(tableName, constraintName, keyColumns, referenceTableName, referenceColumns, false, ignoreFailure);
   }
 
   @Override
   public void addFKConstraint(String tableName, String constraintName,
                              String[] keyColumns, String referenceTableName,
                              String[] referenceColumns, boolean shouldCascadeOnDelete,
                              boolean ignoreFailure) throws SQLException {
          String[] keyColumns, String referenceTableName,
          String[] referenceColumns, boolean shouldCascadeOnDelete,
          boolean ignoreFailure) throws SQLException {
     if (!tableHasForeignKey(tableName, referenceTableName, keyColumns, referenceColumns)) {
       String query = dbmsHelper.getAddForeignKeyStatement(tableName, constraintName,
          Arrays.asList(keyColumns),
          referenceTableName,
          Arrays.asList(referenceColumns),
          shouldCascadeOnDelete);
              Arrays.asList(keyColumns),
              referenceTableName,
              Arrays.asList(referenceColumns),
              shouldCascadeOnDelete);
 
       try {
         executeQuery(query, ignoreFailure);
       } catch (SQLException e) {
        LOG.warn("Add FK constraint failed" +
                ", constraintName = " + constraintName +
                ", tableName = " + tableName, e.getMessage());
        LOG.warn("Add FK constraint failed"
                + ", constraintName = " + constraintName
                + ", tableName = " + tableName, e.getMessage());
         if (!ignoreFailure) {
           throw e;
         }
@@ -414,13 +411,13 @@ public class DBAccessorImpl implements DBAccessor {
     }
   }
 
  public boolean tableHasConstraint(String tableName, String constraintName) throws SQLException{
  public boolean tableHasConstraint(String tableName, String constraintName) throws SQLException {
     // this kind of request is well lower level as we querying system tables, due that we need for some the name of catalog.
     String query = dbmsHelper.getTableConstraintsStatement(connection.getCatalog(), tableName);
     ResultSet rs = executeSelect(query);
    if (rs != null){
    if (rs != null) {
       while (rs.next()) {
        if (rs.getString("CONSTRAINT_NAME").equalsIgnoreCase(constraintName)){
        if (rs.getString("CONSTRAINT_NAME").equalsIgnoreCase(constraintName)) {
           return true;
         }
       }
@@ -430,7 +427,7 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public void addUniqueConstraint(String tableName, String constraintName, String... columnNames)
    throws SQLException{
          throws SQLException {
     if (!tableHasConstraint(tableName, constraintName)) {
       String query = dbmsHelper.getAddUniqueConstraintStatement(tableName, constraintName, columnNames);
       try {
@@ -445,25 +442,25 @@ public class DBAccessorImpl implements DBAccessor {
   }
 
   @Override
  public void addPKConstraint(String tableName, String constraintName, boolean ignoreErrors, String... columnName) throws SQLException{
  public void addPKConstraint(String tableName, String constraintName, boolean ignoreErrors, String... columnName) throws SQLException {
     if (!tableHasPrimaryKey(tableName, null) && tableHasColumn(tableName, columnName)) {
       String query = dbmsHelper.getAddPrimaryKeyConstraintStatement(tableName, constraintName, columnName);
 
       executeQuery(query, ignoreErrors);
     } else {
       LOG.warn("Primary constraint {} not altered to table {} as column {} not present or constraint already exists",
        constraintName, tableName, columnName);
              constraintName, tableName, columnName);
     }
   }
 
   @Override
  public void addPKConstraint(String tableName, String constraintName, String... columnName) throws SQLException{
  public void addPKConstraint(String tableName, String constraintName, String... columnName) throws SQLException {
     addPKConstraint(tableName, constraintName, false, columnName);
   }
 
   @Override
   public void renameColumn(String tableName, String oldColumnName,
                           DBColumnInfo columnInfo) throws SQLException {
          DBColumnInfo columnInfo) throws SQLException {
     //it is mandatory to specify type in column change clause for mysql
     String renameColumnStatement = dbmsHelper.getRenameColumnStatement(tableName, oldColumnName, columnInfo);
     executeQuery(renameColumnStatement);
@@ -488,7 +485,7 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public void alterColumn(String tableName, DBColumnInfo columnInfo)
      throws SQLException {
          throws SQLException {
     //varchar extension only (derby limitation, but not too much for others),
     if (dbmsHelper.supportsColumnTypeChange()) {
       String statement = dbmsHelper.getAlterColumnStatement(tableName,
@@ -497,9 +494,9 @@ public class DBAccessorImpl implements DBAccessor {
     } else {
       //use addColumn: add_tmp-update-drop-rename for Derby
       DBColumnInfo columnInfoTmp = new DBColumnInfo(
          columnInfo.getName() + "_TMP",
          columnInfo.getType(),
          columnInfo.getLength());
              columnInfo.getName() + "_TMP",
              columnInfo.getType(),
              columnInfo.getLength());
       String statement = dbmsHelper.getAddColumnStatement(tableName, columnInfoTmp);
       executeQuery(statement);
       updateTable(tableName, columnInfo, columnInfoTmp);
@@ -510,32 +507,43 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public void updateTable(String tableName, DBColumnInfo columnNameFrom,
      DBColumnInfo columnNameTo) throws SQLException {
          DBColumnInfo columnNameTo) throws SQLException {
     LOG.info("Executing query: UPDATE TABLE " + tableName + " SET "
        + columnNameTo.getName() + "=" + columnNameFrom.getName());
            + columnNameTo.getName() + "=" + columnNameFrom.getName());
 
     String statement = "SELECT * FROM " + tableName;
     int typeFrom = getColumnType(tableName, columnNameFrom.getName());
     int typeTo = getColumnType(tableName, columnNameTo.getName());
    ResultSet rs = executeSelect(statement, ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_UPDATABLE);
    Statement dbStatement = null;
    ResultSet rs = null;
    try {
    dbStatement = getConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_UPDATABLE); 
    rs = dbStatement.executeQuery(statement);
 
     while (rs.next()) {
       convertUpdateData(rs, columnNameFrom, typeFrom, columnNameTo, typeTo);
       rs.updateRow();
     }
    rs.close();
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (dbStatement != null) {
        dbStatement.close();
      }
    }
   }
 
   private void convertUpdateData(ResultSet rs, DBColumnInfo columnNameFrom,
      int typeFrom,
      DBColumnInfo columnNameTo, int typeTo) throws SQLException {
          int typeFrom,
          DBColumnInfo columnNameTo, int typeTo) throws SQLException {
     if (typeFrom == Types.BLOB && typeTo == Types.CLOB) {
       //BLOB-->CLOB
       Blob data = rs.getBlob(columnNameFrom.getName());
       if (data != null) {
         rs.updateClob(columnNameTo.getName(),
            new BufferedReader(new InputStreamReader(data.getBinaryStream())));
                new BufferedReader(new InputStreamReader(data.getBinaryStream(), Charset.defaultCharset())));
       }
     } else {
       Object data = rs.getObject(columnNameFrom.getName());
@@ -554,7 +562,7 @@ public class DBAccessorImpl implements DBAccessor {
 
     for (int i = 0; i < columnNames.length; i++) {
       builder.append(columnNames[i]);
      if(i!=columnNames.length-1){
      if (i != columnNames.length - 1) {
         builder.append(",");
       }
     }
@@ -563,7 +571,7 @@ public class DBAccessorImpl implements DBAccessor {
 
     for (int i = 0; i < values.length; i++) {
       builder.append(values[i]);
      if(i!=values.length-1){
      if (i != values.length - 1) {
         builder.append(",");
       }
     }
@@ -580,18 +588,20 @@ public class DBAccessorImpl implements DBAccessor {
       if (!ignoreFailure) {
         throw e;
       }
    } finally {
      if (statement != null) {
        statement.close();
      }
     }
 
     return rowsUpdated != 0;
   }
 

   @Override
   public int updateTable(String tableName, String columnName, Object value,
                         String whereClause) throws SQLException {
          String whereClause) throws SQLException {
 
    StringBuilder query = new StringBuilder
      (String.format("UPDATE %s SET %s = ", tableName, columnName));
    StringBuilder query = new StringBuilder(String.format("UPDATE %s SET %s = ", tableName, columnName));
 
     // Only String and number supported.
     // Taken from: org.eclipse.persistence.internal.databaseaccess.appendParameterInternal
@@ -606,24 +616,31 @@ public class DBAccessorImpl implements DBAccessor {
     query.append(whereClause);
 
     Statement statement = getConnection().createStatement();

    return statement.executeUpdate(query.toString());
    int res = -1;
    try {
      res = statement.executeUpdate(query.toString());
    } finally {
      if (statement != null) {
        statement.close();
      }
    }
    return res;
   }
 
   @Override
  public int executeUpdate(String query) throws SQLException{
    return  executeUpdate(query, false);
  public int executeUpdate(String query) throws SQLException {
    return executeUpdate(query, false);
   }
 
   @Override
  public int executeUpdate(String query, boolean ignoreErrors) throws SQLException{
  public int executeUpdate(String query, boolean ignoreErrors) throws SQLException {
     Statement statement = getConnection().createStatement();
     try {
       return statement.executeUpdate(query);
    } catch (SQLException e){
      LOG.warn("Error executing query: " + query + ", " +
                 "errorCode = " + e.getErrorCode() + ", message = " + e.getMessage());
      if (!ignoreErrors){
    } catch (SQLException e) {
      LOG.warn("Error executing query: " + query + ", "
              + "errorCode = " + e.getErrorCode() + ", message = " + e.getMessage());
      if (!ignoreErrors) {
         throw e;
       }
     }
@@ -631,8 +648,8 @@ public class DBAccessorImpl implements DBAccessor {
   }
 
   @Override
  public void executeQuery(String query, String tableName, String hasColumnName) throws SQLException{
    if (tableHasColumn(tableName, hasColumnName)){
  public void executeQuery(String query, String tableName, String hasColumnName) throws SQLException {
    if (tableHasColumn(tableName, hasColumnName)) {
       executeQuery(query);
     }
   }
@@ -645,9 +662,19 @@ public class DBAccessorImpl implements DBAccessor {
   @Override
   public ResultSet executeSelect(String query) throws SQLException {
     Statement statement = getConnection().createStatement();
    return statement.executeQuery(query);
    ResultSet rs = statement.executeQuery(query);
    statement.closeOnCompletion();
    return rs;
   }
 
  @Override
  public ResultSet executeSelect(String query, int resultSetType, int resultSetConcur) throws SQLException {
    Statement statement = getConnection().createStatement(resultSetType, resultSetConcur);
    ResultSet rs = statement.executeQuery(query);
    statement.closeOnCompletion();
    return rs;
  }  
  
   @Override
   public void executeQuery(String query, boolean ignoreFailure) throws SQLException {
     LOG.info("Executing query: {}", query);
@@ -659,8 +686,12 @@ public class DBAccessorImpl implements DBAccessor {
         LOG.error("Error executing query: " + query, e);
         throw e;
       } else {
        LOG.warn("Error executing query: " + query + ", " +
          "errorCode = " + e.getErrorCode() + ", message = " + e.getMessage());
        LOG.warn("Error executing query: " + query + ", "
                + "errorCode = " + e.getErrorCode() + ", message = " + e.getMessage());
      }
    } finally {
      if (statement != null) {
        statement.close();
       }
     }
   }
@@ -671,12 +702,8 @@ public class DBAccessorImpl implements DBAccessor {
     executeQuery(query);
   }
 
  @Override
  public ResultSet executeSelect(String query, int resultSetType, int resultSetConcur) throws SQLException {
    Statement statement = getConnection().createStatement(resultSetType, resultSetConcur);
    return statement.executeQuery(query);
  }
 
  @Override
   public void truncateTable(String tableName) throws SQLException {
     String query = "DELETE FROM " + tableName;
     executeQuery(query);
@@ -712,8 +739,8 @@ public class DBAccessorImpl implements DBAccessor {
   }
 
   @Override
  public void dropUniqueConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException{
    if (tableHasConstraint(convertObjectName(tableName), convertObjectName(constraintName))){
  public void dropUniqueConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException {
    if (tableHasConstraint(convertObjectName(tableName), convertObjectName(constraintName))) {
       String query = dbmsHelper.getDropUniqueConstraintStatement(tableName, constraintName);
       executeQuery(query, ignoreFailure);
     } else {
@@ -722,22 +749,22 @@ public class DBAccessorImpl implements DBAccessor {
   }
 
   @Override
  public void dropUniqueConstraint(String tableName, String constraintName) throws SQLException{
  public void dropUniqueConstraint(String tableName, String constraintName) throws SQLException {
     dropUniqueConstraint(tableName, constraintName, false);
   }
 
   @Override
  public void dropPKConstraint(String tableName, String constraintName, String columnName) throws SQLException{
    if (tableHasPrimaryKey(tableName, columnName)){
        String query = dbmsHelper.getDropPrimaryKeyStatement(convertObjectName(tableName), constraintName);
        executeQuery(query, false);
    } else{
        LOG.warn("Primary key doesn't exists for {} table, skipping", tableName);
  public void dropPKConstraint(String tableName, String constraintName, String columnName) throws SQLException {
    if (tableHasPrimaryKey(tableName, columnName)) {
      String query = dbmsHelper.getDropPrimaryKeyStatement(convertObjectName(tableName), constraintName);
      executeQuery(query, false);
    } else {
      LOG.warn("Primary key doesn't exists for {} table, skipping", tableName);
     }
   }
 
   @Override
  public void dropPKConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException{
  public void dropPKConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException {
     /*
      * Note, this is un-safe implementation as constraint name checking will work only for PostgresSQL,
      * MySQL and Oracle doesn't use constraint name for drop primary key
@@ -746,24 +773,31 @@ public class DBAccessorImpl implements DBAccessor {
     if (tableHasPrimaryKey(tableName, null)) {
       String query = dbmsHelper.getDropPrimaryKeyStatement(convertObjectName(tableName), constraintName);
       executeQuery(query, ignoreFailure);
    } else{
    } else {
       LOG.warn("Primary key doesn't exists for {} table, skipping", tableName);
     }
   }
 
   @Override
  public void dropPKConstraint(String tableName, String constraintName) throws SQLException{
  public void dropPKConstraint(String tableName, String constraintName) throws SQLException {
     dropPKConstraint(tableName, constraintName, false);
   }
 
   @Override
   /**
   * Execute script with autocommit and error tolerance, like psql and sqlplus do by default
   * Execute script with autocommit and error tolerance, like psql and sqlplus
   * do by default
    */
   public void executeScript(String filePath) throws SQLException, IOException {
     BufferedReader br = new BufferedReader(new FileReader(filePath));
    ScriptRunner scriptRunner = new ScriptRunner(getConnection(), false, false);
    scriptRunner.runScript(br);
    try {
      ScriptRunner scriptRunner = new ScriptRunner(getConnection(), false, false);
      scriptRunner.runScript(br);
    } finally {
      if (br != null) {
        br.close();
      }
    }
   }
 
   @Override
@@ -779,31 +813,55 @@ public class DBAccessorImpl implements DBAccessor {
   }
 
   @Override
  public boolean tableHasPrimaryKey(String tableName, String columnName) throws SQLException{
  public boolean tableHasPrimaryKey(String tableName, String columnName) throws SQLException {
     ResultSet rs = getDatabaseMetaData().getPrimaryKeys(null, null, convertObjectName(tableName));
    if (rs != null && columnName != null){
      while (rs.next()){
        if (rs.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
          return true;
    boolean res = false;
    try {
      if (rs != null && columnName != null) {
        while (rs.next()) {
          if (rs.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
            res = true;
            break;
          }
         }
      } else if (rs != null) {
        res = rs.next();
      }
    } finally {
      if (rs != null) {
        rs.close();
       }
    } else if (rs != null){
      return rs.next();
     }
    return false;
    return res;
   }
 
   @Override
   public int getColumnType(String tableName, String columnName)
      throws SQLException {
          throws SQLException {
     // We doesn't require any actual result except metadata, so WHERE clause shouldn't match
    String query = String.format("SELECT %s FROM %s WHERE 1=2", columnName, convertObjectName(tableName));
    ResultSet rs = executeSelect(query);
    ResultSetMetaData rsmd = rs.getMetaData();
    return rsmd.getColumnType(1);
    int res;
    String query;
    Statement statement = null;
    ResultSet rs = null;
    ResultSetMetaData rsmd = null;
    try {
    query = String.format("SELECT %s FROM %s WHERE 1=2", columnName, convertObjectName(tableName));
    statement = getConnection().createStatement();
    rs = statement.executeQuery(query);
    rsmd = rs.getMetaData();
    res = rsmd.getColumnType(1);
    } finally {
      if (rs != null){
        rs.close();
      }
      if (statement != null) {
        statement.close();
      }
    }
    return res;
   }
 
  private ResultSetMetaData getColumnMetadata(String tableName, String columnName) throws SQLException{
  private ResultSetMetaData getColumnMetadata(String tableName, String columnName) throws SQLException {
     // We doesn't require any actual result except metadata, so WHERE clause shouldn't match
     String query = String.format("SELECT %s FROM %s WHERE 1=2", convertObjectName(columnName), convertObjectName(tableName));
     ResultSet rs = executeSelect(query);
@@ -812,20 +870,20 @@ public class DBAccessorImpl implements DBAccessor {
 
   @Override
   public Class getColumnClass(String tableName, String columnName)
          throws SQLException, ClassNotFoundException{
      ResultSetMetaData rsmd = getColumnMetadata(tableName, columnName);
      return Class.forName(rsmd.getColumnClassName(1));
          throws SQLException, ClassNotFoundException {
    ResultSetMetaData rsmd = getColumnMetadata(tableName, columnName);
    return Class.forName(rsmd.getColumnClassName(1));
   }
 
   @Override
  public boolean isColumnNullable(String tableName, String columnName) throws SQLException{
  public boolean isColumnNullable(String tableName, String columnName) throws SQLException {
     ResultSetMetaData rsmd = getColumnMetadata(tableName, columnName);
     return !(rsmd.isNullable(1) == ResultSetMetaData.columnNoNulls);
   }
 
   @Override
   public void setColumnNullable(String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable)
      throws SQLException {
          throws SQLException {
 
     String statement = dbmsHelper.getSetNullableStatement(tableName, columnInfo, nullable);
     executeQuery(statement);
@@ -842,7 +900,7 @@ public class DBAccessorImpl implements DBAccessor {
         executeQuery(query);
       } else {
         LOG.info("Column nullability property is not changed due to {} column from {} table is already in {} state, skipping",
                   columnName, tableName, (nullable)?"nullable":"not nullable");
                columnName, tableName, (nullable) ? "nullable" : "not nullable");
       }
     } catch (ClassNotFoundException e) {
       LOG.error("Could not modify table=[], column={}, error={}", tableName, columnName, e.getMessage());
@@ -854,27 +912,22 @@ public class DBAccessorImpl implements DBAccessor {
     // ToDo: create column with more random name
     String tempColumnName = columnName + "_temp";
 
    switch (configuration.getDatabaseType()){
    switch (configuration.getDatabaseType()) {
       case ORACLE:
        // ToDo: add check, if target column is a part of constraint.
        // oracle doesn't support direct type change from varchar2 -> clob
        if (String.class.equals(fromType) && (Character[].class.equals(toType) || char[].class.equals(toType))){
        if (String.class.equals(fromType)
                && (toType.equals(Character[].class))
                || toType.equals(char[].class)) {
           addColumn(tableName, new DBColumnInfo(tempColumnName, toType));
           executeUpdate(String.format("UPDATE %s SET %s = %s", convertObjectName(tableName),
                                       convertObjectName(tempColumnName), convertObjectName(columnName)));
                  convertObjectName(tempColumnName), convertObjectName(columnName)));
           dropColumn(tableName, columnName);
          renameColumn(tableName,tempColumnName, new DBColumnInfo(columnName, toType));
          renameColumn(tableName, tempColumnName, new DBColumnInfo(columnName, toType));
           return;
         }
        break;
     }
 
     alterColumn(tableName, new DBColumnInfo(columnName, toType, null));
   }
 






 }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java
index f3ddaffab2..c7630edfdd 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/entities/ViewEntity.java
@@ -802,6 +802,7 @@ public class ViewEntity implements ViewDefinition {
    *
    * @return the mask class name.
    */
  @Override
   public String getMask() {
     return mask;
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/orm/helpers/ScriptRunner.java b/ambari-server/src/main/java/org/apache/ambari/server/orm/helpers/ScriptRunner.java
index 539532961b..29fb370a0c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/orm/helpers/ScriptRunner.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/orm/helpers/ScriptRunner.java
@@ -128,9 +128,11 @@ public class ScriptRunner {
   private void runScript(Connection conn, Reader reader) throws IOException,
     SQLException {
     StringBuffer command = null;
    Statement statement = null;
     try {
       LineNumberReader lineReader = new LineNumberReader(reader);
       String line = null;
      statement = conn.createStatement();
       while ((line = lineReader.readLine()) != null) {
         if (command == null) {
           command = new StringBuffer();
@@ -151,7 +153,6 @@ public class ScriptRunner {
           command.append(line.substring(0, line
             .lastIndexOf(getDelimiter())));
           command.append(" ");
          Statement statement = conn.createStatement();
 
           println(command);
 
@@ -162,7 +163,6 @@ public class ScriptRunner {
             try {
               statement.execute(command.toString());
             } catch (SQLException e) {
              e.fillInStackTrace();
               printlnError("Error executing: " + command);
               printlnError(e);
             }
@@ -191,11 +191,6 @@ public class ScriptRunner {
           }
 
           command = null;
          try {
            statement.close();
          } catch (Exception e) {
            // Ignore to workaround a bug in Jakarta DBCP
          }
           Thread.yield();
         } else {
           command.append(line);
@@ -206,12 +201,10 @@ public class ScriptRunner {
         conn.commit();
       }
     } catch (SQLException e) {
      e.fillInStackTrace();
       printlnError("Error executing: " + command);
       printlnError(e);
       throw e;
     } catch (IOException e) {
      e.fillInStackTrace();
       printlnError("Error executing: " + command);
       printlnError(e);
       throw e;
@@ -219,6 +212,9 @@ public class ScriptRunner {
       if (!autoCommit) {
         conn.rollback();
       }
      if (statement != null) {
        statement.close();
      }
       flush();
     }
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceImpl.java b/ambari-server/src/main/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceImpl.java
index 767911c7ec..d93faecb71 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceImpl.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceImpl.java
@@ -131,8 +131,9 @@ public class CredentialStoreServiceImpl implements CredentialStoreService {
   }
 
   private void createKeystore(String filename, String keystoreType) {
    FileOutputStream out = null;
     try {
      FileOutputStream out = new FileOutputStream(filename);
      out = new FileOutputStream(filename);
       KeyStore ks = KeyStore.getInstance(keystoreType);
       ks.load(null, null);
       ks.store(out, masterService.getMasterSecret());
@@ -146,6 +147,12 @@ public class CredentialStoreServiceImpl implements CredentialStoreService {
       e.printStackTrace();
     } catch (IOException e) {
       e.printStackTrace();
    } finally {
      try {
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
     }
   }
 
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog170.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog170.java
index 0558c09640..508b5dac4c 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog170.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog170.java
@@ -543,51 +543,63 @@ public class UpgradeCatalog170 extends AbstractUpgradeCatalog {
     }
 
     //use new connection to not affect state of internal one
    Connection connection = dbAccessor.getNewConnection();
    PreparedStatement orderedConfigsStatement =
      connection.prepareStatement("SELECT config_id FROM clusterconfig WHERE type_name = ? ORDER BY create_timestamp");

    Connection connection = null;
    PreparedStatement orderedConfigsStatement = null;
     Map<String, List<Long>> configVersionMap = new HashMap<String, List<Long>>();
    for (String configType : configTypes) {
      List<Long> configIds = new ArrayList<Long>();
      orderedConfigsStatement.setString(1, configType);
      resultSet = orderedConfigsStatement.executeQuery();
      if (resultSet != null) {
        try {
          while (resultSet.next()) {
            configIds.add(resultSet.getLong("config_id"));
    try {
      connection = dbAccessor.getNewConnection();
      try {
        orderedConfigsStatement
                = connection.prepareStatement("SELECT config_id FROM clusterconfig WHERE type_name = ? ORDER BY create_timestamp");

        for (String configType : configTypes) {
          List<Long> configIds = new ArrayList<Long>();
          orderedConfigsStatement.setString(1, configType);
          resultSet = orderedConfigsStatement.executeQuery();
          if (resultSet != null) {
            try {
              while (resultSet.next()) {
                configIds.add(resultSet.getLong("config_id"));
              }
            } finally {
              resultSet.close();
            }
           }
        } finally {
          resultSet.close();
          configVersionMap.put(configType, configIds);
        }
      } finally {
        if (orderedConfigsStatement != null) {
          orderedConfigsStatement.close();
         }
       }
      configVersionMap.put(configType, configIds);
    }

    orderedConfigsStatement.close();

    connection.setAutoCommit(false); //disable autocommit
    PreparedStatement configVersionStatement =
      connection.prepareStatement("UPDATE clusterconfig SET version = ? WHERE config_id = ?");
 

    try {
      for (Entry<String, List<Long>> entry : configVersionMap.entrySet()) {
        long version = 1L;
        for (Long configId : entry.getValue()) {
          configVersionStatement.setLong(1, version++);
          configVersionStatement.setLong(2, configId);
          configVersionStatement.addBatch();
      connection.setAutoCommit(false); //disable autocommit
      PreparedStatement configVersionStatement = null;
      try {
        configVersionStatement = connection.prepareStatement("UPDATE clusterconfig SET version = ? WHERE config_id = ?");

        for (Entry<String, List<Long>> entry : configVersionMap.entrySet()) {
          long version = 1L;
          for (Long configId : entry.getValue()) {
            configVersionStatement.setLong(1, version++);
            configVersionStatement.setLong(2, configId);
            configVersionStatement.addBatch();
          }
          configVersionStatement.executeBatch();
        }
        connection.commit(); //commit changes manually
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        if (configVersionStatement != null){
          configVersionStatement.close();
         }
        configVersionStatement.executeBatch();
       }
      connection.commit(); //commit changes manually
    } catch (SQLException e) {
      connection.rollback();
      throw e;
     } finally {
      configVersionStatement.close();
      connection.close();
      if (connection != null) {
        connection.close();
      }
     }
 
   }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java
index 3ff640e03d..04727562ea 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewArchiveUtility.java
@@ -64,18 +64,26 @@ public class ViewArchiveUtility {
    * @throws JAXBException if xml is malformed
    */
   public ViewConfig getViewConfigFromArchive(File archiveFile)
      throws MalformedURLException, JAXBException {
      throws MalformedURLException, JAXBException, IOException {
    ViewConfig res = null;
    InputStream configStream = null;
    try {
     ClassLoader cl = URLClassLoader.newInstance(new URL[]{archiveFile.toURI().toURL()});
 
    InputStream configStream = cl.getResourceAsStream(VIEW_XML);
    configStream = cl.getResourceAsStream(VIEW_XML);
     if (configStream == null) {
       configStream = cl.getResourceAsStream(WEB_INF_VIEW_XML);
     }
 
     JAXBContext jaxbContext       = JAXBContext.newInstance(ViewConfig.class);
     Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    res = (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    } finally {
      if (configStream != null) {
        configStream.close();
      }
    }
    return res;
   }
 
   /**
@@ -92,7 +100,9 @@ public class ViewArchiveUtility {
    */
   public ViewConfig getViewConfigFromExtractedArchive(String archivePath, boolean validate)
       throws JAXBException, IOException, SAXException {

    ViewConfig res = null;
    InputStream  configStream = null;
    try {
     File configFile = new File(archivePath + File.separator + VIEW_XML);
 
     if (!configFile.exists()) {
@@ -103,11 +113,17 @@ public class ViewArchiveUtility {
       validateConfig(new FileInputStream(configFile));
     }
 
    InputStream  configStream     = new FileInputStream(configFile);
    configStream     = new FileInputStream(configFile);
     JAXBContext  jaxbContext      = JAXBContext.newInstance(ViewConfig.class);
     Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    res = (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    } finally {
      if (configStream != null) {
        configStream.close();
      }
    }
 
    return (ViewConfig) jaxbUnmarshaller.unmarshal(configStream);
    return res;
   }
 
   /**
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java
index 61b93272e2..28016eaef2 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/view/ViewRegistry.java
@@ -1535,8 +1535,6 @@ public class ViewRegistry {
     LOG.info("Reading view archive " + archiveFile + ".");
 
     try {
      // extract the archive and get the class loader
      ClassLoader cl = extractor.extractViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile);
 
       ViewConfig viewConfig = archiveUtility.getViewConfigFromExtractedArchive(extractedArchiveDirPath,
           configuration.isViewValidationEnabled());
@@ -1547,7 +1545,7 @@ public class ViewRegistry {
       viewDefinition.setConfiguration(viewConfig);
 
       if (checkViewVersions(viewDefinition, serverVersion)) {
        setupViewDefinition(viewDefinition, cl);
        setupViewDefinition(viewDefinition, extractor.extractViewArchive(viewDefinition, archiveFile, extractedArchiveDirFile));
 
         Set<ViewInstanceEntity> instanceDefinitions = new HashSet<ViewInstanceEntity>();
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceTest.java b/ambari-server/src/test/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceTest.java
index 877d2f415e..0652a52c00 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/security/encryption/CredentialStoreServiceTest.java
@@ -21,7 +21,6 @@ import junit.framework.Assert;
 import junit.framework.TestCase;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
import org.junit.Before;
 import org.junit.Rule;
 import org.junit.Test;
 import org.junit.rules.TemporaryFolder;
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java b/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java
index ebe607b1e0..09df011ca3 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/view/ViewRegistryTest.java
@@ -108,19 +108,19 @@ import org.springframework.security.core.GrantedAuthority;
  */
 public class ViewRegistryTest {
 
  private static String view_xml1 = "<view>\n" +
  private static final String view_xml1 = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
       "</view>";
 
  private static String view_xml2 = "<view>\n" +
  private static final String view_xml2 = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>2.0.0</version>\n" +
       "</view>";
 
  private static String xml_valid_instance = "<view>\n" +
  private static final String xml_valid_instance = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
@@ -149,7 +149,7 @@ public class ViewRegistryTest {
       "    </instance>\n" +
       "</view>";
 
  private static String xml_invalid_instance = "<view>\n" +
  private static final String xml_invalid_instance = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
@@ -169,7 +169,7 @@ public class ViewRegistryTest {
       "    </instance>\n" +
       "</view>";
 
  private static String AUTO_VIEW_XML = "<view>\n" +
  private static final String AUTO_VIEW_XML = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
@@ -180,7 +180,7 @@ public class ViewRegistryTest {
       "    </auto-instance>\n" +
       "</view>";
 
  private static String AUTO_VIEW_WILD_STACK_XML = "<view>\n" +
  private static final String AUTO_VIEW_WILD_STACK_XML = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
@@ -191,7 +191,7 @@ public class ViewRegistryTest {
       "    </auto-instance>\n" +
       "</view>";
 
  private static String AUTO_VIEW_BAD_STACK_XML = "<view>\n" +
  private static final String AUTO_VIEW_BAD_STACK_XML = "<view>\n" +
       "    <name>MY_VIEW</name>\n" +
       "    <label>My View!</label>\n" +
       "    <version>1.0.0</version>\n" +
diff --git a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/AmbariShell.java b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/AmbariShell.java
index e842620c7f..02269a89d2 100644
-- a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/AmbariShell.java
++ b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/AmbariShell.java
@@ -55,7 +55,6 @@ public class AmbariShell implements CommandLineRunner, ShellStatusListener {
           break;
         }
       }
      System.exit(0);
     } else {
       shell.addShellStatusListener(this);
       shell.start();
diff --git a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/BlueprintCommands.java b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/BlueprintCommands.java
index 73000d0627..96549090cb 100644
-- a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/BlueprintCommands.java
++ b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/BlueprintCommands.java
@@ -168,10 +168,19 @@ public class BlueprintCommands implements CommandMarker {
 
   private String readContent(File file) {
     String content = null;
    FileInputStream fis = null;
     try {
      content = IOUtils.toString(new FileInputStream(file));
      fis = new FileInputStream(file);
      content = IOUtils.toString(fis);
     } catch (IOException e) {
       // not important
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException ex) {
        }
      }
     }
     return content;
   }
diff --git a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/ElephantCommand.java b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/ElephantCommand.java
index a236054982..3988c6f846 100644
-- a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/ElephantCommand.java
++ b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/commands/ElephantCommand.java
@@ -18,6 +18,7 @@
 package org.apache.ambari.shell.commands;
 
 import java.io.IOException;
import java.io.InputStream;
 
 import org.apache.commons.io.IOUtils;
 import org.springframework.shell.core.CommandMarker;
@@ -48,6 +49,16 @@ public class ElephantCommand implements CommandMarker {
    */
   @CliCommand(value = "hello", help = "Prints a simple elephant to the console")
   public String elephant() throws IOException {
    return IOUtils.toString(getClass().getResourceAsStream("/elephant.txt"));
    String res = null;
    InputStream is = null;
    try{
    is = getClass().getResourceAsStream("/elephant.txt");
    res = IOUtils.toString(is);
    } finally {
      if (is != null) {
        is.close();
      }
    }
    return res;
   }
 }
\ No newline at end of file
diff --git a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/customization/AmbariBanner.java b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/customization/AmbariBanner.java
index 850687eca2..ea0e39d942 100644
-- a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/customization/AmbariBanner.java
++ b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/customization/AmbariBanner.java
@@ -18,6 +18,9 @@
 package org.apache.ambari.shell.customization;
 
 import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
 
 import org.apache.commons.io.IOUtils;
 import org.springframework.shell.plugin.BannerProvider;
@@ -36,11 +39,22 @@ public class AmbariBanner implements BannerProvider {
 
   @Override
   public String getBanner() {
    String res = null;
    InputStream is = null;
     try {
      return IOUtils.toString(getClass().getResourceAsStream("/banner.txt"));
      is = getClass().getResourceAsStream("/banner.txt");
      res = IOUtils.toString(is);
     } catch (IOException e) {
      return "AmbariShell";
      res = "AmbariShell";
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
        }
      }
     }
    return res;
   }
 
   @Override
diff --git a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/flash/InstallProgress.java b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/flash/InstallProgress.java
index dbf8e65c87..bad5b30180 100644
-- a/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/flash/InstallProgress.java
++ b/ambari-shell/ambari-groovy-shell/src/main/java/org/apache/ambari/shell/flash/InstallProgress.java
@@ -51,8 +51,8 @@ public class InstallProgress extends AbstractFlash {
         int intValue = decimal.intValue();
         if (intValue != SUCCESS && intValue != FAILED) {
           sb.append("Installation: ").append(decimal).append("% ");
          int rounded = round(progress.setScale(0, BigDecimal.ROUND_UP).intValue() / 10);
          for (int i = 0; i < 10; i++) {
          long rounded = round(progress.setScale(0, BigDecimal.ROUND_UP).floatValue() / 10);
          for (long i = 0; i < 10; i++) {
             if (i < rounded) {
               sb.append("=");
             } else {
@@ -71,7 +71,7 @@ public class InstallProgress extends AbstractFlash {
       }
     } else {
       if (exit) {
        System.exit(0);
        done = true;
       }
     }
     return sb.toString();
- 
2.19.1.windows.1

