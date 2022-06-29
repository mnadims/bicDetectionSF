From 85eb280521a46f3668c2e8e2743479eac7d82488 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Mon, 23 Feb 2015 12:45:53 -0500
Subject: [PATCH] ACCUMULO-3612 Get PermissionsIT running again, stub out test
 changes to make.

--
 .../accumulo/harness/MiniClusterHarness.java  |  4 +++
 .../test/functional/PermissionsIT.java        | 31 ++++++++++++++-----
 2 files changed, 27 insertions(+), 8 deletions(-)

diff --git a/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java b/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java
index e53d686a0..6245f96b4 100644
-- a/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java
++ b/test/src/test/java/org/apache/accumulo/harness/MiniClusterHarness.java
@@ -65,6 +65,10 @@ public class MiniClusterHarness {
     return create(MiniClusterHarness.class.getName(), Long.toString(COUNTER.incrementAndGet()), token);
   }
 
  public MiniAccumuloClusterImpl create(AuthenticationToken token, TestingKdc kdc) throws Exception {
    return create(MiniClusterHarness.class.getName(), Long.toString(COUNTER.incrementAndGet()), token, kdc);
  }

   public MiniAccumuloClusterImpl create(AccumuloIT testBase, AuthenticationToken token) throws Exception {
     return create(testBase.getClass().getName(), testBase.testName.getMethodName(), token);
   }
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/PermissionsIT.java b/test/src/test/java/org/apache/accumulo/test/functional/PermissionsIT.java
index 4ad0927bc..e20a25264 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/PermissionsIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/PermissionsIT.java
@@ -30,6 +30,8 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.MutationsRejectedException;
 import org.apache.accumulo.core.client.NamespaceExistsException;
@@ -147,7 +149,7 @@ public class PermissionsIT extends AccumuloIT {
     return result;
   }
 
  private static void testMissingSystemPermission(String tableNamePrefix, Connector root_conn, Connector test_user_conn, SystemPermission perm)
  private void testMissingSystemPermission(String tableNamePrefix, Connector root_conn, Connector test_user_conn, SystemPermission perm)
       throws AccumuloException, TableExistsException, AccumuloSecurityException, TableNotFoundException, NamespaceExistsException, NamespaceNotFoundException,
       NamespaceNotEmptyException {
     String tableName, user, password = "password", namespace;
@@ -294,12 +296,18 @@ public class PermissionsIT extends AccumuloIT {
             throw e;
         }
         break;
      case OBTAIN_DELEGATION_TOKEN:
        ClientConfiguration clientConf = cluster.getClientConfig();
        if (clientConf.getBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getKey(), false)) {
          // TODO Try to obtain a delegation token without the permission
        }
        break;
       default:
         throw new IllegalArgumentException("Unrecognized System Permission: " + perm);
     }
   }
 
  private static void testGrantedSystemPermission(String tableNamePrefix, Connector root_conn, Connector test_user_conn, SystemPermission perm)
  private void testGrantedSystemPermission(String tableNamePrefix, Connector root_conn, Connector test_user_conn, SystemPermission perm)
       throws AccumuloException, AccumuloSecurityException, TableNotFoundException, TableExistsException, NamespaceExistsException, NamespaceNotFoundException,
       NamespaceNotEmptyException {
     String tableName, user, password = "password", namespace;
@@ -388,12 +396,19 @@ public class PermissionsIT extends AccumuloIT {
         if (root_conn.namespaceOperations().list().contains(namespace) || !root_conn.namespaceOperations().list().contains(namespace2))
           throw new IllegalStateException("Should be able to rename a table");
         break;
      case OBTAIN_DELEGATION_TOKEN:
        ClientConfiguration clientConf = cluster.getClientConfig();
        if (clientConf.getBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getKey(), false)) {
          // TODO Try to obtain a delegation token with the permission
        }
        break;

       default:
         throw new IllegalArgumentException("Unrecognized System Permission: " + perm);
     }
   }
 
  private static void verifyHasOnlyTheseSystemPermissions(Connector root_conn, String user, SystemPermission... perms) throws AccumuloException,
  private void verifyHasOnlyTheseSystemPermissions(Connector root_conn, String user, SystemPermission... perms) throws AccumuloException,
       AccumuloSecurityException {
     List<SystemPermission> permList = Arrays.asList(perms);
     for (SystemPermission p : SystemPermission.values()) {
@@ -409,7 +424,7 @@ public class PermissionsIT extends AccumuloIT {
     }
   }
 
  private static void verifyHasNoSystemPermissions(Connector root_conn, String user, SystemPermission... perms) throws AccumuloException,
  private void verifyHasNoSystemPermissions(Connector root_conn, String user, SystemPermission... perms) throws AccumuloException,
       AccumuloSecurityException {
     for (SystemPermission p : perms)
       if (root_conn.securityOperations().hasSystemPermission(user, p))
@@ -466,7 +481,7 @@ public class PermissionsIT extends AccumuloIT {
     }
   }
 
  private static void testMissingTablePermission(Connector root_conn, Connector test_user_conn, TablePermission perm, String tableName) throws Exception {
  private void testMissingTablePermission(Connector root_conn, Connector test_user_conn, TablePermission perm, String tableName) throws Exception {
     Scanner scanner;
     BatchWriter writer;
     Mutation m;
@@ -543,7 +558,7 @@ public class PermissionsIT extends AccumuloIT {
     }
   }
 
  private static void testGrantedTablePermission(Connector root_conn, Connector test_user_conn, TablePermission perm, String tableName)
  private void testGrantedTablePermission(Connector root_conn, Connector test_user_conn, TablePermission perm, String tableName)
       throws AccumuloException, TableExistsException, AccumuloSecurityException, TableNotFoundException, MutationsRejectedException {
     Scanner scanner;
     BatchWriter writer;
@@ -583,7 +598,7 @@ public class PermissionsIT extends AccumuloIT {
     }
   }
 
  private static void verifyHasOnlyTheseTablePermissions(Connector root_conn, String user, String table, TablePermission... perms) throws AccumuloException,
  private void verifyHasOnlyTheseTablePermissions(Connector root_conn, String user, String table, TablePermission... perms) throws AccumuloException,
       AccumuloSecurityException {
     List<TablePermission> permList = Arrays.asList(perms);
     for (TablePermission p : TablePermission.values()) {
@@ -599,7 +614,7 @@ public class PermissionsIT extends AccumuloIT {
     }
   }
 
  private static void verifyHasNoTablePermissions(Connector root_conn, String user, String table, TablePermission... perms) throws AccumuloException,
  private void verifyHasNoTablePermissions(Connector root_conn, String user, String table, TablePermission... perms) throws AccumuloException,
       AccumuloSecurityException {
     for (TablePermission p : perms)
       if (root_conn.securityOperations().hasTablePermission(user, table, p))
- 
2.19.1.windows.1

