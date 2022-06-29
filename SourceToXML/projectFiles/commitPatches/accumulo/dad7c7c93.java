From dad7c7c93a7825dd967d42a99380ccd8beb06e24 Mon Sep 17 00:00:00 2001
From: jamesmello <james.mello@atsid.com>
Date: Tue, 11 Aug 2015 13:21:04 -0700
Subject: [PATCH] ACCUMULO-3946 Added missing audits on security operations.

Signed-off-by: Josh Elser <elserj@apache.org>
--
 .../apache/accumulo/server/master/Master.java |  21 +-
 .../security/AuditedSecurityOperation.java    | 355 +++++++++++++++++-
 .../server/security/SecurityOperation.java    |  10 +-
 .../test/randomwalk/security/CreateTable.java |   2 +-
 .../test/randomwalk/security/TableOp.java     |   4 +-
 5 files changed, 370 insertions(+), 22 deletions(-)

diff --git a/server/src/main/java/org/apache/accumulo/server/master/Master.java b/server/src/main/java/org/apache/accumulo/server/master/Master.java
index dd9468332..924e12030 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/Master.java
++ b/server/src/main/java/org/apache/accumulo/server/master/Master.java
@@ -857,15 +857,16 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
       switch (op) {
         case CREATE: {
           String tableName = ByteBufferUtil.toString(arguments.get(0));
          if (!security.canCreateTable(c))
          if (!security.canCreateTable(c, tableName))
             throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);

           checkNotMetadataTable(tableName, TableOperation.CREATE);
           checkTableName(tableName, TableOperation.CREATE);
 
           org.apache.accumulo.core.client.admin.TimeType timeType = org.apache.accumulo.core.client.admin.TimeType.valueOf(ByteBufferUtil.toString(arguments
               .get(1)));
          fate.seedTransaction(opid, new TraceRepo<Master>(new CreateTable(c.getPrincipal(), tableName, timeType, options)), autoCleanup);
 
          fate.seedTransaction(opid, new TraceRepo<Master>(new CreateTable(c.getPrincipal(), tableName, timeType, options)), autoCleanup);
           break;
         }
         case RENAME: {
@@ -879,7 +880,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
 
           final boolean canRename;
           try {
            canRename = security.canRenameTable(c, tableId);
            canRename = security.canRenameTable(c, tableId, newTableName, oldTableName);
           } catch (ThriftSecurityException e) {
             throwIfTableMissingSecurityException(e, tableId, oldTableName, TableOperation.RENAME);
             throw e;
@@ -926,7 +927,6 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
 
             propertiesToSet.put(entry.getKey(), entry.getValue());
           }

           fate.seedTransaction(opid, new TraceRepo<Master>(new CloneTable(c.getPrincipal(), srcTableId, tableName, propertiesToSet, propertiesToExclude)),
               autoCleanup);
 
@@ -1051,7 +1051,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
 
           final boolean canBulkImport;
           try {
            canBulkImport = security.canBulkImport(c, tableId);
            canBulkImport = security.canBulkImport(c, tableId, dir);
           } catch (ThriftSecurityException e) {
             throwIfTableMissingSecurityException(e, tableId, tableName, TableOperation.BULK_IMPORT);
             throw e;
@@ -1104,7 +1104,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
           String tableName = ByteBufferUtil.toString(arguments.get(0));
           String exportDir = ByteBufferUtil.toString(arguments.get(1));
 
          if (!security.canImport(c))
          if (!security.canImport(c, tableName, exportDir))
             throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
 
           checkNotMetadataTable(tableName, TableOperation.CREATE);
@@ -1121,7 +1121,7 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
 
           final boolean canExport;
           try {
            canExport = security.canExport(c, tableId);
            canExport = security.canExport(c, tableId, exportDir);
           } catch (ThriftSecurityException e) {
             throwIfTableMissingSecurityException(e, tableId, tableName, TableOperation.EXPORT);
             throw e;
@@ -2308,12 +2308,9 @@ public class Master implements LiveTServerSet.Listener, TableObserver, CurrentSt
     });
 
     TCredentials systemAuths = SecurityConstants.getSystemCredentials();
    final TabletStateStore stores[] = {
        new ZooTabletStateStore(new ZooStore(zroot)),
    final TabletStateStore stores[] = {new ZooTabletStateStore(new ZooStore(zroot)),
         // ACCUMULO-3580 ACCUMULO-3618 disable metadata table scanning optimizations
        new RootTabletStateStore(instance, systemAuths, null),
        new MetaDataStateStore(instance, systemAuths, null)
        };
        new RootTabletStateStore(instance, systemAuths, null), new MetaDataStateStore(instance, systemAuths, null)};
     watchers.add(new TabletGroupWatcher(stores[2], null));
     watchers.add(new TabletGroupWatcher(stores[1], watchers.get(0)));
     watchers.add(new TabletGroupWatcher(stores[0], watchers.get(1)));
diff --git a/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java b/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
index b2e810d36..68dcb27c9 100644
-- a/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
++ b/server/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
@@ -201,10 +201,13 @@ public class AuditedSecurityOperation extends SecurityOperation {
   public boolean hasSystemPermission(TCredentials credentials, String user, SystemPermission permission) throws ThriftSecurityException {
     try {
       boolean result = super.hasSystemPermission(credentials, user, permission);
      audit(credentials, "checked permission %s on %s", permission, user);
      if (result)
        audit(credentials, "checked permission %s on %s", permission, user);
      else
        audit(credentials, "checked permission %s on %s denied", permission, user);
       return result;
     } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "checking permission %s on %s", permission, user);
      audit(credentials, ex, "checking permission %s on %s denied", permission, user);
       log.debug(ex);
       throw ex;
     }
@@ -248,6 +251,354 @@ public class AuditedSecurityOperation extends SecurityOperation {
     }
   }
 
  @Override
  public boolean canCreateTable(TCredentials c, String tablename) throws ThriftSecurityException {
    try {
      boolean result = super.canCreateTable(c, tablename);
      if (result)
        audit(c, "create table %s allowed", tablename);
      else
        audit(c, "create table %s denied", tablename);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "create table %s denied", tablename);
      throw ex;
    }
  }

  @Override
  public boolean canRenameTable(TCredentials c, String tableId, String newTableName, String oldTableName) throws ThriftSecurityException {
    try {
      boolean result = super.canRenameTable(c, tableId, newTableName, oldTableName);
      if (result)
        audit(c, "rename table on tableId %s from %s to %s allowed", tableId, oldTableName, newTableName);
      else
        audit(c, "rename table on tableId %s from %s to %s denied", tableId, oldTableName, newTableName);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "rename table on tableId %s from %s to %s denied", tableId, oldTableName, newTableName);
      throw ex;
    }
  }

  @Override
  public boolean canSplitTablet(TCredentials credentials, String table) throws ThriftSecurityException {
    try {
      boolean result = super.canSplitTablet(credentials, table);
      if (result)
        audit(credentials, "split tablet on table %s allowed", table);
      else
        audit(credentials, "split tablet on table %s denied", table);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "split tablet on table %s denied", table);
      throw ex;
    }
  }

  @Override
  public boolean canPerformSystemActions(TCredentials credentials) throws ThriftSecurityException {
    try {
      boolean result = super.canPerformSystemActions(credentials);
      if (result)
        audit(credentials, "system action allowed");
      else
        audit(credentials, "system action denied");
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "system action denied");
      throw ex;
    }
  }

  @Override
  public boolean canFlush(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canFlush(c, tableId);
      if (result)
        audit(c, "flush on tableId %s allowed ", tableId);
      else
        audit(c, "flush on tableId %s denied ", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "flush on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canAlterTable(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canAlterTable(c, tableId);
      if (result)
        audit(c, "alter table on tableId %s allowed", tableId);
      else
        audit(c, "alter table on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "alter table on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canCloneTable(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canCloneTable(c, tableId);
      if (result)
        audit(c, "clone table on tableId %s allowed", tableId);
      else
        audit(c, "clone table on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "clone table on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canDeleteTable(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canDeleteTable(c, tableId);
      if (result)
        audit(c, "delete table on tableId %s allowed", tableId);
      else
        audit(c, "delete table on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "delete table on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canOnlineOfflineTable(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canOnlineOfflineTable(c, tableId);
      if (result)
        audit(c, "offline table on tableId %s allowed", tableId);
      else
        audit(c, "offline table on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "offline table on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canMerge(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canMerge(c, tableId);
      if (result)
        audit(c, "merge table on tableId %s allowed", tableId);
      else
        audit(c, "merge table on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "merge table on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canDeleteRange(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canDeleteRange(c, tableId);
      if (result)
        audit(c, "delete range on tableId %s allowed", tableId);
      else
        audit(c, "delete range on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "delete range on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canBulkImport(TCredentials c, String tableId, String importDir) throws ThriftSecurityException {
    try {
      boolean result = super.canBulkImport(c, tableId, importDir);
      if (result)
        audit(c, "bulk import on tableId %s from directory %s allowed", tableId, importDir);
      else
        audit(c, "bulk import on tableId %s from directory %s denied", tableId, importDir);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "bulk import on tableId %s from directory %s denied", tableId, importDir);
      throw ex;
    }
  }

  @Override
  public boolean canCompact(TCredentials c, String tableId) throws ThriftSecurityException {
    try {
      boolean result = super.canCompact(c, tableId);
      if (result)
        audit(c, "compact on tableId %s allowed", tableId);
      else
        audit(c, "compact on tableId %s denied", tableId);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "compact on tableId %s denied", tableId);
      throw ex;
    }
  }

  @Override
  public boolean canChangeAuthorizations(TCredentials c, String user) throws ThriftSecurityException {
    try {
      boolean result = super.canChangeAuthorizations(c, user);
      if (result)
        audit(c, "change authorizations on user %s allowed", user);
      else
        audit(c, "change authorizations on user %s denied", user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "change authorizations on user %s denied", user);
      throw ex;
    }
  }

  @Override
  public boolean canChangePassword(TCredentials c, String user) throws ThriftSecurityException {
    try {
      boolean result = super.canChangePassword(c, user);
      if (result)
        audit(c, "change password on user %s allowed", user);
      else
        audit(c, "change password on user %s denied", user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "change password on user %s denied", user);
      throw ex;
    }
  }

  @Override
  public boolean canCreateUser(TCredentials c, String user) throws ThriftSecurityException {
    try {
      boolean result = super.canCreateUser(c, user);
      if (result)
        audit(c, "create user on user %s allowed", user);
      else
        audit(c, "create user on user %s denied", user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "create user on user %s denied", user);
      throw ex;
    }
  }

  @Override
  public boolean canDropUser(TCredentials c, String user) throws ThriftSecurityException {
    try {
      boolean result = super.canDropUser(c, user);
      if (result)
        audit(c, "drop user on user %s allowed", user);
      else
        audit(c, "drop user on user %s denied", user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "drop user on user %s denied", user);

      throw ex;
    }
  }

  @Override
  public boolean canGrantSystem(TCredentials c, String user, SystemPermission sysPerm) throws ThriftSecurityException {
    try {
      boolean result = super.canGrantSystem(c, user, sysPerm);
      if (result)
        audit(c, "grant system permission %s for user %s allowed", sysPerm, user);
      else
        audit(c, "grant system permission %s for user %s denied", sysPerm, user);
      return result;

    } catch (ThriftSecurityException ex) {
      audit(c, ex, "grant system permission %s for user %s denied", sysPerm, user);

      throw ex;
    }
  }

  @Override
  public boolean canGrantTable(TCredentials c, String user, String table) throws ThriftSecurityException {
    try {
      boolean result = super.canGrantTable(c, user, table);
      if (result)
        audit(c, "grant table on table %s for user %s allowed", table, user);
      else
        audit(c, "grant table on table %s for user %s denied", table, user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "grant table on table %s for user %s denied", table, user);
      throw ex;
    }
  }

  @Override
  public boolean canRevokeSystem(TCredentials c, String user, SystemPermission sysPerm) throws ThriftSecurityException {
    try {
      boolean result = super.canRevokeSystem(c, user, sysPerm);
      if (result)
        audit(c, "revoke system permission %s for user %s allowed", sysPerm, user);
      else
        audit(c, "revoke system permission %s for user %s denied", sysPerm, user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "revoke system permission %s for user %s denied", sysPerm, user);
      throw ex;
    }
  }

  @Override
  public boolean canRevokeTable(TCredentials c, String user, String table) throws ThriftSecurityException {
    try {
      boolean result = super.canRevokeTable(c, user, table);
      if (result)
        audit(c, "revoke table on table %s for user %s allowed", table, user);
      else
        audit(c, "revoke table on table %s for user %s denied", table, user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(c, ex, "revoke table on table %s for user %s denied", table, user);
      throw ex;
    }
  }

  @Override
  public boolean canExport(TCredentials credentials, String tableId, String exportDir) throws ThriftSecurityException {
    try {
      boolean result = super.canExport(credentials, tableId, exportDir);
      if (result)
        audit(credentials, "export table on tableId %s to directory %s allowed", tableId, exportDir);
      else
        audit(credentials, "export table on tableId %s to directory %s denied", tableId, exportDir);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "export table on tableId %s to directory %s denied", tableId, exportDir);
      throw ex;
    }
  }

  @Override
  public boolean canImport(TCredentials credentials, String tableName, String importDir) throws ThriftSecurityException {
    try {
      boolean result = super.canImport(credentials, tableName, importDir);
      if (result)
        audit(credentials, "import table %s from directory %s allowed", tableName, importDir);
      else
        audit(credentials, "import table %s from directory %s denied", tableName, importDir);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "import table %s from directory %s denied", tableName, importDir);
      throw ex;
    }
  }

   @Override
   public void initializeSecurity(TCredentials credentials, String principal, byte[] token) throws AccumuloSecurityException, ThriftSecurityException {
     super.initializeSecurity(credentials, principal, token);
diff --git a/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java b/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
index 13d1c32d7..6a817f248 100644
-- a/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
++ b/server/src/main/java/org/apache/accumulo/server/security/SecurityOperation.java
@@ -314,12 +314,12 @@ public class SecurityOperation {
         || hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false);
   }
 
  public boolean canCreateTable(TCredentials c) throws ThriftSecurityException {
  public boolean canCreateTable(TCredentials c, String tablename) throws ThriftSecurityException {
     authenticate(c);
     return hasSystemPermission(c.getPrincipal(), SystemPermission.CREATE_TABLE, false);
   }
 
  public boolean canRenameTable(TCredentials c, String tableId) throws ThriftSecurityException {
  public boolean canRenameTable(TCredentials c, String tableId, String newTableName, String oldTableName) throws ThriftSecurityException {
     authenticate(c);
     return hasSystemPermission(c.getPrincipal(), SystemPermission.ALTER_TABLE, false)
         || hasTablePermission(c.getPrincipal(), tableId, TablePermission.ALTER_TABLE, false);
@@ -354,7 +354,7 @@ public class SecurityOperation {
     return hasSystemPermission(c.getPrincipal(), SystemPermission.SYSTEM, false) || hasTablePermission(c.getPrincipal(), tableId, TablePermission.WRITE, false);
   }
 
  public boolean canBulkImport(TCredentials c, String tableId) throws ThriftSecurityException {
  public boolean canBulkImport(TCredentials c, String tableId, String importDir) throws ThriftSecurityException {
     authenticate(c);
     return hasTablePermission(c.getPrincipal(), tableId, TablePermission.BULK_IMPORT, false);
   }
@@ -601,12 +601,12 @@ public class SecurityOperation {
     }
   }
 
  public boolean canExport(TCredentials credentials, String tableId) throws ThriftSecurityException {
  public boolean canExport(TCredentials credentials, String tableId, String exportDir) throws ThriftSecurityException {
     authenticate(credentials);
     return hasTablePermission(credentials.getPrincipal(), tableId, TablePermission.READ, false);
   }
 
  public boolean canImport(TCredentials credentials) throws ThriftSecurityException {
  public boolean canImport(TCredentials credentials, String tableName, String importDir) throws ThriftSecurityException {
     authenticate(credentials);
     return hasSystemPermission(credentials.getPrincipal(), SystemPermission.CREATE_TABLE, false);
   }
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/security/CreateTable.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/security/CreateTable.java
index 61b146ad0..318c2a698 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/security/CreateTable.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/security/CreateTable.java
@@ -36,7 +36,7 @@ public class CreateTable extends Test {
     String tableName = WalkingSecurity.get(state).getTableName();
 
     boolean exists = WalkingSecurity.get(state).getTableExists();
    boolean hasPermission = WalkingSecurity.get(state).canCreateTable(WalkingSecurity.get(state).getSysCredentials());
    boolean hasPermission = WalkingSecurity.get(state).canCreateTable(WalkingSecurity.get(state).getSysCredentials(), tableName);
 
     try {
       conn.tableOperations().create(tableName);
diff --git a/test/src/main/java/org/apache/accumulo/test/randomwalk/security/TableOp.java b/test/src/main/java/org/apache/accumulo/test/randomwalk/security/TableOp.java
index 6db56b28b..dfe9c156d 100644
-- a/test/src/main/java/org/apache/accumulo/test/randomwalk/security/TableOp.java
++ b/test/src/main/java/org/apache/accumulo/test/randomwalk/security/TableOp.java
@@ -216,7 +216,7 @@ public class TableOp extends Test {
           return;
         } catch (AccumuloSecurityException ae) {
           if (ae.getSecurityErrorCode().equals(SecurityErrorCode.PERMISSION_DENIED)) {
            if (WalkingSecurity.get(state).canBulkImport(WalkingSecurity.get(state).getTabCredentials(), tableName))
            if (WalkingSecurity.get(state).canBulkImport(WalkingSecurity.get(state).getTabCredentials(), tableName, dir.getName()))
               throw new AccumuloException("Bulk Import failed when it should have worked: " + tableName);
             return;
           } else if (ae.getSecurityErrorCode().equals(SecurityErrorCode.BAD_CREDENTIALS)) {
@@ -230,7 +230,7 @@ public class TableOp extends Test {
         fs.delete(dir, true);
         fs.delete(fail, true);
 
        if (!WalkingSecurity.get(state).canBulkImport(WalkingSecurity.get(state).getTabCredentials(), tableName))
        if (!WalkingSecurity.get(state).canBulkImport(WalkingSecurity.get(state).getTabCredentials(), tableName, dir.getName()))
           throw new AccumuloException("Bulk Import succeeded when it should have failed: " + dir + " table " + tableName);
         break;
       case ALTER_TABLE:
- 
2.19.1.windows.1

