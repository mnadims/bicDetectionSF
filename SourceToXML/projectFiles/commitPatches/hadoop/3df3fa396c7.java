From 3df3fa396c7b338928f77344edd8c4dcda957671 Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Thu, 24 Nov 2016 17:44:14 +0000
Subject: [PATCH] HADOOP-13833
 TestSymlinkHdfsFileSystem#testCreateLinkUsingPartQualPath2 fails after
 HADOOP-13605. Contributed by Brahma Reddy Battula.

--
 .../src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java    | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java
index a5808e6c80f..90e8c90c7ba 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/SymlinkBaseTest.java
@@ -571,7 +571,8 @@ public void testCreateLinkUsingPartQualPath2() throws IOException {
         GenericTestUtils.assertExceptionContains(
             AbstractFileSystem.NO_ABSTRACT_FS_ERROR, e);
       } else if (wrapper instanceof FileSystemTestWrapper) {
        assertEquals("No FileSystem for scheme: null", e.getMessage());
        assertEquals("No FileSystem for scheme " + "\"" + "null" + "\"",
            e.getMessage());
       }
     }
   }
- 
2.19.1.windows.1

