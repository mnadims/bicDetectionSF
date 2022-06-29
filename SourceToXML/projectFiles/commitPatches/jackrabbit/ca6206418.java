From ca6206418ea0ac7f9119df33b8e5f02afce16de2 Mon Sep 17 00:00:00 2001
From: Angela Schreiber <angela@apache.org>
Date: Mon, 11 Nov 2013 16:28:59 +0000
Subject: [PATCH] JCR-3692 : MoveAtRootTest fails and is not included in test
 suite

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1540762 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/SessionMoveOperation.java  | 8 ++++++--
 .../src/test/java/org/apache/jackrabbit/core/TestAll.java | 1 +
 2 files changed, 7 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionMoveOperation.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionMoveOperation.java
index 47f6775b7..314ca0213 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionMoveOperation.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionMoveOperation.java
@@ -128,12 +128,16 @@ public class SessionMoveOperation implements SessionWriteOperation<Object> {
             // no name collision, fall through
         }
 
        // verify that the targetNode can be removed
        int options = ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        context.getItemValidator().checkRemove(targetNode, options, Permission.NONE);

         // verify for both source and destination parent nodes that
         // - they are checked-out
         // - are not protected neither by node type constraints nor by retention/hold
        int options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK |
        options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK |
         ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        context.getItemValidator().checkRemove(srcParentNode, options, Permission.NONE);
        context.getItemValidator().checkModify(srcParentNode, options, Permission.NONE);
         context.getItemValidator().checkModify(destParentNode, options, Permission.NONE);
 
         // check constraints
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index 5e13a9025..532275212 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -79,6 +79,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(ConsistencyCheck.class);
         suite.addTestSuite(RemoveAddNodeWithUUIDTest.class);
         suite.addTestSuite(ReplacePropertyWhileOthersReadTest.class);
        suite.addTestSuite(MoveAtRootTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

