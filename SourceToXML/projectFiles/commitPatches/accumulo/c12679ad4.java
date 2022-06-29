From c12679ad4ea83f5f3719612a2e550edf199841fe Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Sun, 10 Jul 2016 22:27:09 -0400
Subject: [PATCH] ACCUMULO-4363 Fix incorrect class name for RowSampler in
 ShellServerIT

--
 .../org/apache/accumulo/test/ShellServerIT.java     | 13 +++++++------
 1 file changed, 7 insertions(+), 6 deletions(-)

diff --git a/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java b/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java
index e23e9fad9..61d3d4a68 100644
-- a/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java
++ b/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java
@@ -50,6 +50,7 @@ import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.admin.TableOperations;
 import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.sample.RowSampler;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
@@ -992,8 +993,8 @@ public class ShellServerIT extends SharedMiniClusterBase {
     assertEquals(3, countFiles(cloneId));
 
     String clone2 = table + "_clone_2";
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=7,table.sampler=org.apache.accumulo.core.sample.RowSampler " + clone
        + " " + clone2);
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=7,table.sampler=" + RowSampler.class.getName() + " " + clone + " "
        + clone2);
     String clone2Id = getTableId(clone2);
 
     assertEquals(3, countFiles(clone2Id));
@@ -1040,8 +1041,8 @@ public class ShellServerIT extends SharedMiniClusterBase {
     ts.exec("insert 3900 doc uril file://final_project.txt");
 
     String clone1 = table + "_clone_1";
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=3,table.sampler=org.apache.accumulo.core.sample.RowSampler " + table
        + " " + clone1);
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=3,table.sampler=" + RowSampler.class.getName() + " " + table + " "
        + clone1);
 
     ts.exec("compact -t " + clone1 + " -w --sf-no-sample");
 
@@ -1053,8 +1054,8 @@ public class ShellServerIT extends SharedMiniClusterBase {
 
     // create table where table sample config differs from whats in file
     String clone2 = table + "_clone_2";
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=2,table.sampler=org.apache.accumulo.core.sample.RowSampler " + clone1
        + " " + clone2);
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=2,table.sampler=" + RowSampler.class.getName() + " " + clone1 + " "
        + clone2);
 
     ts.exec("table " + clone2);
     ts.exec("scan --sample", false, "SampleNotPresentException", true);
- 
2.19.1.windows.1

