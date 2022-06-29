From 8393c6daea536b8aca22010c135f7f53515ce74a Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Tue, 6 Oct 2015 11:39:10 +0000
Subject: [PATCH] JCR-3915: undo incorrect change to lock token test

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1707006 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/test/api/lock/LockTest.java    | 9 +++------
 1 file changed, 3 insertions(+), 6 deletions(-)

diff --git a/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java b/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java
index be47d8c75..9778bd510 100644
-- a/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java
++ b/jackrabbit-jcr-tests/src/main/java/org/apache/jackrabbit/test/api/lock/LockTest.java
@@ -112,7 +112,6 @@ public class LockTest extends AbstractJCRTest {
 
         // lock node
         Lock lock = n1.lock(false, true);
        String lt = lock.getLockToken();
 
         // assert: isLive must return true
         assertTrue("Lock must be live", lock.isLive());
@@ -124,11 +123,9 @@ public class LockTest extends AbstractJCRTest {
             // get same node
             Node n2 = (Node) otherSuperuser.getItem(n1.getPath());
 
            Lock lock2 = n2.getLock();
            assertNotNull("other session must see the lock", lock2);
            String lt2 = lock2.getLockToken();
            assertTrue("other session must either not get the lock token, or see the actual one (tokens: " + lt + ", " + lt2 + ")",
                    lt2 == null || lt2.equals(lt));
            // assert: lock token must be null for other session
            assertNull("Lock token must be null for other session",
                    n2.getLock().getLockToken());
 
             // assert: modifying same node in other session must fail
             try {
- 
2.19.1.windows.1

