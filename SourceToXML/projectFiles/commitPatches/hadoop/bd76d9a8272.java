From bd76d9a8272f6037be3938df78c0d3589d04e044 Mon Sep 17 00:00:00 2001
From: Harsh J <harsh@apache.org>
Date: Sat, 22 Sep 2012 18:56:07 +0000
Subject: [PATCH] HADOOP-8833. fs -text should make sure to call
 inputstream.seek(0) before using input stream. Contributed by Tom White and
 Harsh J. (harsh)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1388869 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../org/apache/hadoop/fs/shell/Display.java   |  1 +
 .../org/apache/hadoop/hdfs/TestDFSShell.java  | 47 ++++++++++++++++++-
 3 files changed, 50 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index e37d46acaf7..b049af43232 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -253,6 +253,9 @@ Release 2.0.3-alpha - Unreleased
     HADOOP-8780. Update DeprecatedProperties apt file. (Ahmed Radwan via
     tomwhite)
 
    HADOOP-8833. fs -text should make sure to call inputstream.seek(0)
    before using input stream. (tomwhite and harsh)

 Release 2.0.2-alpha - 2012-09-07 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java
index af47d4027ff..503ac05a364 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Display.java
@@ -142,6 +142,7 @@ protected InputStream getInputStream(PathData item) throws IOException {
           CompressionCodecFactory cf = new CompressionCodecFactory(getConf());
           CompressionCodec codec = cf.getCodec(item.path);
           if (codec != null) {
            i.seek(0);
             return codec.createInputStream(i);
           }
           break;
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
index 426d8e70152..a6c40844f83 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestDFSShell.java
@@ -35,6 +35,7 @@
 import java.util.List;
 import java.util.Random;
 import java.util.Scanner;
import java.util.zip.DeflaterOutputStream;
 import java.util.zip.GZIPOutputStream;
 
 import org.apache.commons.logging.Log;
@@ -52,7 +53,10 @@
 import org.apache.hadoop.io.IOUtils;
 import org.apache.hadoop.io.SequenceFile;
 import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.CompressionCodec;
 import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.ReflectionUtils;
 import org.apache.hadoop.util.StringUtils;
 import org.apache.hadoop.util.ToolRunner;
 import org.junit.Test;
@@ -577,6 +581,8 @@ private void textTest(Path root, Configuration conf) throws Exception {
     try {
       final FileSystem fs = root.getFileSystem(conf);
       fs.mkdirs(root);

      // Test the gzip type of files. Magic detection.
       OutputStream zout = new GZIPOutputStream(
           fs.create(new Path(root, "file.gz")));
       Random r = new Random();
@@ -601,7 +607,7 @@ private void textTest(Path root, Configuration conf) throws Exception {
           Arrays.equals(file.toByteArray(), out.toByteArray()));
 
       // Create a sequence file with a gz extension, to test proper
      // container detection
      // container detection. Magic detection.
       SequenceFile.Writer writer = SequenceFile.createWriter(
           conf,
           SequenceFile.Writer.file(new Path(root, "file.gz")),
@@ -619,6 +625,45 @@ private void textTest(Path root, Configuration conf) throws Exception {
       assertTrue("Output doesn't match input",
           Arrays.equals("Foo\tBar\n".getBytes(), out.toByteArray()));
       out.reset();

      // Test deflate. Extension-based detection.
      OutputStream dout = new DeflaterOutputStream(
          fs.create(new Path(root, "file.deflate")));
      byte[] outbytes = "foo".getBytes();
      dout.write(outbytes);
      dout.close();
      out = new ByteArrayOutputStream();
      System.setOut(new PrintStream(out));
      argv = new String[2];
      argv[0] = "-text";
      argv[1] = new Path(root, "file.deflate").toString();
      ret = ToolRunner.run(new FsShell(conf), argv);
      assertEquals("'-text " + argv[1] + " returned " + ret, 0, ret);
      assertTrue("Output doesn't match input",
          Arrays.equals(outbytes, out.toByteArray()));
      out.reset();

      // Test a simple codec. Extension based detection. We use
      // Bzip2 cause its non-native.
      CompressionCodec codec = (CompressionCodec)
          ReflectionUtils.newInstance(BZip2Codec.class, conf);
      String extension = codec.getDefaultExtension();
      Path p = new Path(root, "file." + extension);
      OutputStream fout = new DataOutputStream(codec.createOutputStream(
          fs.create(p, true)));
      byte[] writebytes = "foo".getBytes();
      fout.write(writebytes);
      fout.close();
      out = new ByteArrayOutputStream();
      System.setOut(new PrintStream(out));
      argv = new String[2];
      argv[0] = "-text";
      argv[1] = new Path(root, p).toString();
      ret = ToolRunner.run(new FsShell(conf), argv);
      assertEquals("'-text " + argv[1] + " returned " + ret, 0, ret);
      assertTrue("Output doesn't match input",
          Arrays.equals(writebytes, out.toByteArray()));
      out.reset();
     } finally {
       if (null != bak) {
         System.setOut(bak);
- 
2.19.1.windows.1

