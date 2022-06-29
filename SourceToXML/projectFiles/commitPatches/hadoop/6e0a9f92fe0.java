From 6e0a9f92fe0052d39b95a605174b3f6423c6aae7 Mon Sep 17 00:00:00 2001
From: Colin Patrick Mccabe <cmccabe@cloudera.com>
Date: Thu, 18 Jun 2015 11:30:29 -0700
Subject: [PATCH] HADOOP-12100. ImmutableFsPermission should not override
 applyUmask since that method doesn't modify the FsPermission (Bibin A.
 Chundatt via Colin P. McCabe)

--
 hadoop-common-project/hadoop-common/CHANGES.txt            | 4 ++++
 .../java/org/apache/hadoop/fs/permission/FsPermission.java | 7 ++-----
 2 files changed, 6 insertions(+), 5 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 3430da62179..2f5eda398d4 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -914,6 +914,10 @@ Release 2.7.1 - UNRELEASED
     HADOOP-12078. The default retry policy does not handle RetriableException
     correctly. (Arpit Agarwal)
 
    HADOOP-12100. ImmutableFsPermission should not override applyUmask since
    that method doesn't modify the FsPermission (Bibin A Chundatt via Colin P.
    McCabe)

 Release 2.7.0 - 2015-04-20
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
index 264a0952706..0258293823e 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
@@ -375,13 +375,10 @@ else if (unixSymbolicPermission.length() != MAX_PERMISSION_LENGTH) {
     public ImmutableFsPermission(short permission) {
       super(permission);
     }
    @Override
    public FsPermission applyUMask(FsPermission umask) {
      throw new UnsupportedOperationException();
    }

     @Override
     public void readFields(DataInput in) throws IOException {
       throw new UnsupportedOperationException();
    }    
    }
   }
 }
- 
2.19.1.windows.1

