From e152575f5ea5ea798ca989c852afb763189dee60 Mon Sep 17 00:00:00 2001
From: markrmiller <markrmiller@apache.org>
Date: Tue, 25 Oct 2016 12:39:37 -0400
Subject: [PATCH] SOLR-9536: OldBackupDirectory timestamp field needs to be
 initialized to avoid NPE.

--
 solr/CHANGES.txt                                               | 3 +++
 .../src/java/org/apache/solr/handler/OldBackupDirectory.java   | 2 +-
 2 files changed, 4 insertions(+), 1 deletion(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 2f4827b2294..b693543c1c0 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -214,6 +214,9 @@ Bug Fixes
 * SOLR-9441: Solr collection backup on HDFS can only be manipulated by the Solr process owner. 
  (Hrishikesh Gadre via Mark Miller)
 
* SOLR-9536: OldBackupDirectory timestamp field needs to be initialized to avoid NPE.
 (Hrishikesh Gadre via Mark Miller)
 
 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java b/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java
index 2b19116c926..79c5f09f7e9 100644
-- a/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java
++ b/solr/core/src/java/org/apache/solr/handler/OldBackupDirectory.java
@@ -32,7 +32,7 @@ class OldBackupDirectory implements Comparable<OldBackupDirectory> {
 
   private URI basePath;
   private String dirName;
  private Optional<Date> timestamp;
  private Optional<Date> timestamp = Optional.empty();
 
   public OldBackupDirectory(URI basePath, String dirName) {
     this.dirName = Preconditions.checkNotNull(dirName);
- 
2.19.1.windows.1

