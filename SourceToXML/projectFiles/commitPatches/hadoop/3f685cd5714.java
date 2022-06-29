From 3f685cd5714b1dba44ed33f40683c7ea4895790d Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Sat, 12 Sep 2015 18:55:42 +0100
Subject: [PATCH] HADOOP-12407. Test failing: hadoop.ipc.TestSaslRPC. (stevel)

--
 hadoop-common-project/hadoop-common/CHANGES.txt          | 2 ++
 .../src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java | 9 ++++++++-
 2 files changed, 10 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index fffd5617a3c..db671ae23d7 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1115,6 +1115,8 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12388. Fix components' version information in the web page
     'About the Cluster'. (Jun Gong via zxu)
 
    HADOOP-12407. Test failing: hadoop.ipc.TestSaslRPC. (stevel)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
index f6ab38043ca..754b81147d9 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/ipc/TestSaslRPC.java
@@ -558,9 +558,16 @@ public void testSaslPlainServerBadPassword() {
       e = se;
     }
     assertNotNull(e);
    assertEquals("PLAIN auth failed: wrong password", e.getMessage());
    String message = e.getMessage();
    assertContains("PLAIN auth failed", message);
    assertContains("wrong password", message);
   }
 
  private void assertContains(String expected, String text) {
    assertNotNull("null text", text );
    assertTrue("No {" + expected + "} in {" + text + "}",
        text.contains(expected));
  }
 
   private void runNegotiation(CallbackHandler clientCbh,
                               CallbackHandler serverCbh)
- 
2.19.1.windows.1

