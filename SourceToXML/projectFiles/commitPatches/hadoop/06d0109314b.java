From 06d0109314b39675a205ee6f0898ebde23284d5d Mon Sep 17 00:00:00 2001
From: Harsh J <harsh@apache.org>
Date: Sat, 30 Jun 2012 05:04:23 +0000
Subject: [PATCH] HADOOP-8449. hadoop fs -text fails with compressed sequence
 files with the codec file extension. (harsh)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1355636 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../org/apache/hadoop/fs/shell/Display.java   | 23 +++++++++------
 .../org/apache/hadoop/hdfs/TestDFSShell.java  | 28 ++++++++++++++++---
 3 files changed, 42 insertions(+), 12 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index a093a2301e4..964e379529e 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -215,6 +215,9 @@ Branch-2 ( Unreleased changes )
     HADOOP-8524. Allow users to get source of a Configuration
     parameter (harsh)
 
    HADOOP-8449. hadoop fs -text fails with compressed sequence files
    with the codec file extension (harsh)

   BUG FIXES
 
     HADOOP-8372. NetUtils.normalizeHostName() incorrectly handles hostname
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java
index 59358632a77..5ae0d67c574 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java
@@ -107,30 +107,37 @@ protected InputStream getInputStream(PathData item) throws IOException {
     @Override
     protected InputStream getInputStream(PathData item) throws IOException {
       FSDataInputStream i = (FSDataInputStream)super.getInputStream(item);
      
      // check codecs
      CompressionCodecFactory cf = new CompressionCodecFactory(getConf());
      CompressionCodec codec = cf.getCodec(item.path);
      if (codec != null) {
        return codec.createInputStream(i);
      }
 
      // Check type of stream first
       switch(i.readShort()) {
         case 0x1f8b: { // RFC 1952
          // Must be gzip
           i.seek(0);
           return new GZIPInputStream(i);
         }
         case 0x5345: { // 'S' 'E'
          // Might be a SequenceFile
           if (i.readByte() == 'Q') {
             i.close();
             return new TextRecordInputStream(item.stat);
           }
        }
        default: {
          // Check the type of compression instead, depending on Codec class's
          // own detection methods, based on the provided path.
          CompressionCodecFactory cf = new CompressionCodecFactory(getConf());
          CompressionCodec codec = cf.getCodec(item.path);
          if (codec != null) {
            return codec.createInputStream(i);
          }
           break;
         }
       }

      // File is non-compressed, or not a file container we know.
       i.seek(0);
       return i;
    }    
    }
   }
 
   protected class TextRecordInputStream extends InputStream {
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
index e5cf21fea5b..f1aa7b32168 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
@@ -48,6 +48,8 @@
 import org.apache.hadoop.hdfs.server.datanode.DataNodeTestUtils;
 import org.apache.hadoop.hdfs.tools.DFSAdmin;
 import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.hadoop.util.StringUtils;
 import org.apache.hadoop.util.ToolRunner;
@@ -545,7 +547,7 @@ public void testText() throws Exception {
       textTest(new Path("/texttest").makeQualified(dfs.getUri(),
             dfs.getWorkingDirectory()), conf);
 
      conf.set("fs.default.name", dfs.getUri().toString());
      conf.set("fs.defaultFS", dfs.getUri().toString());
       final FileSystem lfs = FileSystem.getLocal(conf);
       textTest(new Path(TEST_ROOT_DIR, "texttest").makeQualified(lfs.getUri(),
             lfs.getWorkingDirectory()), conf);
@@ -564,6 +566,7 @@ private void textTest(Path root, Configuration conf) throws Exception {
       OutputStream zout = new GZIPOutputStream(
           fs.create(new Path(root, "file.gz")));
       Random r = new Random();
      bak = System.out;
       ByteArrayOutputStream file = new ByteArrayOutputStream();
       for (int i = 0; i < 1024; ++i) {
         char c = Character.forDigit(r.nextInt(26) + 10, 36);
@@ -572,7 +575,6 @@ private void textTest(Path root, Configuration conf) throws Exception {
       }
       zout.close();
 
      bak = System.out;
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       System.setOut(new PrintStream(out));
 
@@ -581,10 +583,28 @@ private void textTest(Path root, Configuration conf) throws Exception {
       argv[1] = new Path(root, "file.gz").toString();
       int ret = ToolRunner.run(new FsShell(conf), argv);
       assertEquals("'-text " + argv[1] + " returned " + ret, 0, ret);
      file.reset();
      out.reset();
       assertTrue("Output doesn't match input",
           Arrays.equals(file.toByteArray(), out.toByteArray()));

      // Create a sequence file with a gz extension, to test proper
      // container detection
      SequenceFile.Writer writer = SequenceFile.createWriter(
          conf,
          SequenceFile.Writer.file(new Path(root, "file.gz")),
          SequenceFile.Writer.keyClass(Text.class),
          SequenceFile.Writer.valueClass(Text.class));
      writer.append(new Text("Foo"), new Text("Bar"));
      writer.close();
      out = new ByteArrayOutputStream();
      System.setOut(new PrintStream(out));
      argv = new String[2];
      argv[0] = "-text";
      argv[1] = new Path(root, "file.gz").toString();
      ret = ToolRunner.run(new FsShell(conf), argv);
      assertEquals("'-text " + argv[1] + " returned " + ret, 0, ret);
      assertTrue("Output doesn't match input",
          Arrays.equals("Foo\tBar\n".getBytes(), out.toByteArray()));
      out.reset();
     } finally {
       if (null != bak) {
         System.setOut(bak);
- 
2.19.1.windows.1

