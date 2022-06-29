From 29eadf1b8f7eb75192cc73de927832ac71018649 Mon Sep 17 00:00:00 2001
From: Todd Lipcon <todd@apache.org>
Date: Wed, 14 Sep 2011 06:51:21 +0000
Subject: [PATCH] HADOOP-7629. Allow immutable FsPermission objects to be used
 as IPC parameters. Contributed by Todd Lipcon.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1170451 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 +++
 .../hadoop/fs/permission/FsPermission.java    | 22 ++++++++++++-------
 2 files changed, 17 insertions(+), 8 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 98a78fe065b..c25e159eb02 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -583,6 +583,9 @@ Release 0.23.0 - Unreleased
 
     HADOOP-7626. Bugfix for a config generator (Eric Yang via ddas)
 
    HADOOP-7629. Allow immutable FsPermission objects to be used as IPC
    parameters. (todd)

 Release 0.22.0 - Unreleased
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
index 0926bb1dc0d..af3d5148d59 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
@@ -44,18 +44,12 @@
   };
   static {                                      // register a ctor
     WritableFactories.setFactory(FsPermission.class, FACTORY);
    WritableFactories.setFactory(ImmutableFsPermission.class, FACTORY);
   }
 
   /** Create an immutable {@link FsPermission} object. */
   public static FsPermission createImmutable(short permission) {
    return new FsPermission(permission) {
      public FsPermission applyUMask(FsPermission umask) {
        throw new UnsupportedOperationException();
      }
      public void readFields(DataInput in) throws IOException {
        throw new UnsupportedOperationException();
      }
    };
    return new ImmutableFsPermission(permission);
   }
 
   //POSIX permission style
@@ -301,4 +295,16 @@ else if (unixSymbolicPermission.length() != 10) {
 
     return new FsPermission((short)n);
   }
  
  private static class ImmutableFsPermission extends FsPermission {
    public ImmutableFsPermission(short permission) {
      super(permission);
    }
    public FsPermission applyUMask(FsPermission umask) {
      throw new UnsupportedOperationException();
    }
    public void readFields(DataInput in) throws IOException {
      throw new UnsupportedOperationException();
    }    
  }
 }
- 
2.19.1.windows.1

