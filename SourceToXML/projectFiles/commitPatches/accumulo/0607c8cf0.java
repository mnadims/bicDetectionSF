From 0607c8cf0bb1a4088c0c333063c8fea9bf834fd5 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 10 Apr 2014 23:05:41 -0400
Subject: [PATCH] ACCUMULO-2586 Ensure that all of the constructors from 1.5.0
 are present

--
 .../accumulo/core/client/mapred/InputFormatBase.java       | 7 ++++++-
 .../accumulo/core/client/mapreduce/InputFormatBase.java    | 7 ++++++-
 2 files changed, 12 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
index bc568e8fd..e2ab25a36 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
@@ -917,7 +917,12 @@ public abstract class InputFormatBase<K,V> implements InputFormat<K,V> {
       super();
     }
 
    public RangeInputSplit(Range range, String[] locations) {
    public RangeInputSplit(RangeInputSplit split) throws IOException {
      this.setRange(split.getRange());
      this.setLocations(split.getLocations());
    }

    protected RangeInputSplit(String table, Range range, String[] locations) {
       super(range, locations);
     }
   }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 710c56571..4c88bd52f 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -1329,7 +1329,12 @@ public abstract class InputFormatBase<K,V> extends InputFormat<K,V> {
       super();
     }
 
    public RangeInputSplit(Range range, String[] locations) {
    public RangeInputSplit(RangeInputSplit split) throws IOException {
      this.setRange(split.getRange());
      this.setLocations(split.getLocations());
    }

    protected RangeInputSplit(String table, Range range, String[] locations) {
       super(range, locations);
     }
   }
- 
2.19.1.windows.1

