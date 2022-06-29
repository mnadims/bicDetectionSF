From 18f680977684710037c07bb068383791e8a33a9e Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Mon, 8 Jun 2015 13:02:26 +0100
Subject: [PATCH] HADOOP-12052 IPC client downgrades all exception types to
 IOE, breaks callers trying to use them. (Brahma Reddy Battula via stevel)

--
 hadoop-common-project/hadoop-common/CHANGES.txt           | 3 +++
 .../src/main/java/org/apache/hadoop/ipc/Client.java       | 8 +++++++-
 2 files changed, 10 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index eacc3bed96f..79f317859c3 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -834,6 +834,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-11924. Tolerate JDK-8047340-related exceptions in
     Shell#isSetSidAvailable preventing class init. (Tsuyoshi Ozawa via gera)
 
    HADOOP-12052 IPC client downgrades all exception types to IOE, breaks
    callers trying to use them. (Brahma Reddy Battula via stevel)

 Release 2.7.1 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
index feb811ed3ae..6996a51b19d 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/ipc/Client.java
@@ -1484,7 +1484,13 @@ public Connection call() throws Exception {
           }
         });
       } catch (ExecutionException e) {
        throw new IOException(e);
        Throwable cause = e.getCause();
        // the underlying exception should normally be IOException
        if (cause instanceof IOException) {
          throw (IOException) cause;
        } else {
          throw new IOException(cause);
        }
       }
       if (connection.addCall(call)) {
         break;
- 
2.19.1.windows.1

