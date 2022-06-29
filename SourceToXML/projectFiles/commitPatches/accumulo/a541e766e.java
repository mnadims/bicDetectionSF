From a541e766e1e8b3cba307ca0e1535233ff6384802 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 20 Aug 2015 18:38:47 -0400
Subject: [PATCH] ACCUMULO-3966 Fix broken AuditMessageIT

--
 .../server/security/AuditedSecurityOperation.java  |  2 +-
 .../org/apache/accumulo/test/AuditMessageIT.java   | 14 ++++++++++----
 2 files changed, 11 insertions(+), 5 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java b/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
index 5aea5a28f..ff611b3c8 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
++ b/server/base/src/main/java/org/apache/accumulo/server/security/AuditedSecurityOperation.java
@@ -655,7 +655,7 @@ public class AuditedSecurityOperation extends SecurityOperation {
   }
 
   // The audit log is already logging the principal, so we don't have anything else to audit
  public static final String AUTHENICATE_AUDIT_TEMPLATE = "";
  public static final String AUTHENICATE_AUDIT_TEMPLATE = "action: authenticate;";
 
   @Override
   protected void authenticate(TCredentials credentials) throws ThriftSecurityException {
diff --git a/test/src/test/java/org/apache/accumulo/test/AuditMessageIT.java b/test/src/test/java/org/apache/accumulo/test/AuditMessageIT.java
index 90d18e05c..2896267de 100644
-- a/test/src/test/java/org/apache/accumulo/test/AuditMessageIT.java
++ b/test/src/test/java/org/apache/accumulo/test/AuditMessageIT.java
@@ -80,6 +80,7 @@ public class AuditMessageIT extends ConfigurableMacIT {
 
   @Override
   public void beforeClusterStart(MiniAccumuloConfigImpl cfg) throws Exception {
    cfg.setNumTservers(1);
     new File(cfg.getConfDir(), "auditLog.xml").delete();
   }
 
@@ -243,7 +244,8 @@ public class AuditMessageIT extends ConfigurableMacIT {
 
     ArrayList<String> auditMessages = getAuditMessages("testUserOperationsAudits");
 
    assertEquals(1, findAuditMessage(auditMessages, "action: createUser; targetUser: " + AUDIT_USER_2).size());
    // The user is allowed to create this user and it succeeded
    assertEquals(2, findAuditMessage(auditMessages, "action: createUser; targetUser: " + AUDIT_USER_2).size());
     assertEquals(
         1,
         findAuditMessage(auditMessages,
@@ -260,10 +262,13 @@ public class AuditMessageIT extends ConfigurableMacIT {
         1,
         findAuditMessage(auditMessages,
             "action: revokeTablePermission; permission: " + TablePermission.READ.toString() + "; targetTable: " + NEW_TEST_TABLE_NAME).size());
    assertEquals(1, findAuditMessage(auditMessages, "action: changePassword; targetUser: " + AUDIT_USER_2 + "").size());
    // changePassword is allowed and succeeded
    assertEquals(2, findAuditMessage(auditMessages, "action: changePassword; targetUser: " + AUDIT_USER_2 + "").size());
     assertEquals(1, findAuditMessage(auditMessages, "action: changeAuthorizations; targetUser: " + AUDIT_USER_2 + "; authorizations: " + auths.toString())
         .size());
    assertEquals(1, findAuditMessage(auditMessages, "action: dropUser; targetUser: " + AUDIT_USER_2).size());

    // allowed to dropUser and succeeded
    assertEquals(2, findAuditMessage(auditMessages, "action: dropUser; targetUser: " + AUDIT_USER_2).size());
   }
 
   @Test
@@ -488,7 +493,8 @@ public class AuditMessageIT extends ConfigurableMacIT {
     // ... that will do for now.
     // End of testing activities
 
    assertEquals(1, findAuditMessage(auditMessages, String.format(AuditedSecurityOperation.DROP_USER_AUDIT_TEMPLATE, AUDIT_USER_2)).size());
    // We're permitted to drop this user, but it fails because the user doesn't actually exist.
    assertEquals(2, findAuditMessage(auditMessages, String.format(AuditedSecurityOperation.DROP_USER_AUDIT_TEMPLATE, AUDIT_USER_2)).size());
     assertEquals(
         1,
         findAuditMessage(auditMessages,
- 
2.19.1.windows.1

