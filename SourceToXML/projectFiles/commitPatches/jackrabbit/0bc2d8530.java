From 0bc2d85303dd1a065c2639bf245588316ccbb11d Mon Sep 17 00:00:00 2001
From: Amit Jain <amitj@apache.org>
Date: Thu, 11 Dec 2014 04:24:31 +0000
Subject: [PATCH] JCR-3839: [aws-ext] Regression to JCR-3734 Slow local cache
 built-up time Patch from Shashank

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1644551 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/data/LocalCache.java      | 45 ++++++++-----------
 1 file changed, 18 insertions(+), 27 deletions(-)

diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java
index 4f4430a82..ef9fd1afc 100644
-- a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/LocalCache.java
@@ -107,26 +107,7 @@ public class LocalCache {
                 (cachePurgeResizeFactor * maxSizeInBytes) });
         cache = new LRUCache(maxSizeInBytes, cachePurgeTrigFactor, cachePurgeResizeFactor);
         this.asyncUploadCache = asyncUploadCache;
        long startTime = System.currentTimeMillis();
        ArrayList<File> allFiles = new ArrayList<File>();
        Iterator<File> it = FileUtils.iterateFiles(directory, null, true);
        while (it.hasNext()) {
            File f = it.next();
            allFiles.add(f);
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("Time taken to recursive [{}] took [{}] sec",
            allFiles.size(), ((t1 - startTime) / 1000));
        Collections.sort(allFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                long l1 = o1.lastModified(), l2 = o2.lastModified();
                return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
            }
        });
        long t2 = System.currentTimeMillis();
        LOG.debug("Time taken to sort [{}] took [{}] sec",
            allFiles.size(), ((t2 - t1) / 1000));
        new Thread(new CacheBuildJob(allFiles)).start();
        new Thread(new CacheBuildJob()).start();
     }
 
     /**
@@ -597,17 +578,26 @@ public class LocalCache {
      */
     private class CacheBuildJob implements Runnable {
 
        List<File> allFiles ;
         
        private CacheBuildJob(List<File> allFiles) {
            this.allFiles = allFiles;
        }
         public void run() {
             long startTime = System.currentTimeMillis();
            ArrayList<File> allFiles = new ArrayList<File>();
            Iterator<File> it = FileUtils.iterateFiles(directory, null, true);
            while (it.hasNext()) {
                File f = it.next();
                allFiles.add(f);
            }
            long t1 = System.currentTimeMillis();
            LOG.debug("Time taken to recursive [{}] took [{}] sec",
                allFiles.size(), ((t1 - startTime) / 1000));

             String dataStorePath = directory.getAbsolutePath();
            LOG.info("directoryPath = " + dataStorePath);
            // convert to java path format
             dataStorePath = dataStorePath.replace("\\", "/");
            LOG.info("directoryPath = " + dataStorePath);

             String tmpPath = tmp.getAbsolutePath();
            tmpPath = tmpPath.replace("\\", "/");
             LOG.debug("tmp path [{}]", tmpPath); 
             long time = System.currentTimeMillis();
             int count = 0;
@@ -616,6 +606,9 @@ public class LocalCache {
                     count++;
                     String name = f.getPath();
                     String filePath = f.getAbsolutePath();
                    // convert to java path format
                    name = name.replace("\\", "/");
                    filePath = filePath.replace("\\", "/");
                     // skipped any temp file
                     if(filePath.startsWith(tmpPath) ) {
                         LOG.info    ("tmp file [{}] skipped ", filePath);
@@ -624,8 +617,6 @@ public class LocalCache {
                     if (name.startsWith(dataStorePath)) {
                         name = name.substring(dataStorePath.length());
                     }
                    // convert to java path format
                    name = name.replace("\\", "/");
                     if (name.startsWith("/") || name.startsWith("\\")) {
                         name = name.substring(1);
                     }
- 
2.19.1.windows.1

