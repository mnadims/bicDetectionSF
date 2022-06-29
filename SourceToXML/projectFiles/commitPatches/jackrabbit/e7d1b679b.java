From e7d1b679b3c6f3df94ae7789207370e95d5dd403 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Fri, 21 Aug 2015 10:44:55 +0000
Subject: [PATCH] JCR-3900: LockTest.testNodeLocked: incorrect assumption about
 when the lock token can be returned

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1696929 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/test/api/lock/LockTest.java    | 9 ++++++---
 1 file changed, 6 insertions(+), 3 deletions(-)

diff --git a/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java b/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java
index 9778bd510..be47d8c75 100644
-- a/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java
++ b/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java
@@ -112,6 +112,7 @@ public class LockTest extends AbstractJCRTest {
 
         // lock node
         Lock lock = n1.lock(false, true);
        String lt = lock.getLockToken();
 
         // assert: isLive must return true
         assertTrue("Lock must be live", lock.isLive());
@@ -123,9 +124,11 @@ public class LockTest extends AbstractJCRTest {
             // get same node
             Node n2 = (Node) otherSuperuser.getItem(n1.getPath());
 
            // assert: lock token must be null for other session
            assertNull("Lock token must be null for other session",
                    n2.getLock().getLockToken());
            Lock lock2 = n2.getLock();
            assertNotNull("other session must see the lock", lock2);
            String lt2 = lock2.getLockToken();
            assertTrue("other session must either not get the lock token, or see the actual one (tokens: " + lt + ", " + lt2 + ")",
                    lt2 == null || lt2.equals(lt));
 
             // assert: modifying same node in other session must fail
             try {
- 
2.19.1.windows.1

