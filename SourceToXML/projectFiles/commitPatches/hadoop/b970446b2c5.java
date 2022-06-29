From b970446b2c59f8897bb2c3a562fa192ed3452db5 Mon Sep 17 00:00:00 2001
From: Akira Ajisaka <aajisaka@apache.org>
Date: Mon, 7 Nov 2016 19:53:43 +0900
Subject: [PATCH] HADOOP-13798. TestHadoopArchives times out.

--
 .../test/java/org/apache/hadoop/tools/TestHadoopArchives.java   | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/hadoop-tools/hadoop-archives/src/test/java/org/apache/hadoop/tools/TestHadoopArchives.java b/hadoop-tools/hadoop-archives/src/test/java/org/apache/hadoop/tools/TestHadoopArchives.java
index 165c51559ed..e9ecf0489b6 100644
-- a/hadoop-tools/hadoop-archives/src/test/java/org/apache/hadoop/tools/TestHadoopArchives.java
++ b/hadoop-tools/hadoop-archives/src/test/java/org/apache/hadoop/tools/TestHadoopArchives.java
@@ -444,7 +444,7 @@ public void testReadFileContent() throws Exception {
       int read; 
       while (true) {
         read = fsdis.read(buffer, readIntoBuffer, buffer.length - readIntoBuffer);
        if (read < 0) {
        if (read <= 0) {
           // end of stream:
           if (readIntoBuffer > 0) {
             baos.write(buffer, 0, readIntoBuffer);
- 
2.19.1.windows.1

