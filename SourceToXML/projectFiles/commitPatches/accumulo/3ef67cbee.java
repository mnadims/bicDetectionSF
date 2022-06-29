From 3ef67cbeea54f072c901f25f7bc17740cd30f981 Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Fri, 14 Mar 2014 19:30:40 -0400
Subject: [PATCH] ACCUMULO-2460 hide experimental props in shell

--
 .../util/shell/commands/ConfigCommand.java    |   2 +-
 .../apache/accumulo/test/ShellConfigIT.java   |  52 +
 .../apache/accumulo/test/ShellServerIT.java   | 943 +++++++++---------
 3 files changed, 529 insertions(+), 468 deletions(-)
 create mode 100644 test/src/test/java/org/apache/accumulo/test/ShellConfigIT.java

diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/commands/ConfigCommand.java b/core/src/main/java/org/apache/accumulo/core/util/shell/commands/ConfigCommand.java
index 81cce7b8b..c76a51fbc 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/commands/ConfigCommand.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/commands/ConfigCommand.java
@@ -211,7 +211,7 @@ public class ConfigCommand extends Command {
           siteVal = sysVal = dfault = curVal = curVal.replaceAll(".", "*");
         }
         if (sysVal != null) {
          if (defaults.containsKey(key)) {
          if (defaults.containsKey(key) && !Property.getPropertyByKey(key).isExperimental()) {
             printConfLine(output, "default", key, dfault);
             printed = true;
           }
diff --git a/test/src/test/java/org/apache/accumulo/test/ShellConfigIT.java b/test/src/test/java/org/apache/accumulo/test/ShellConfigIT.java
new file mode 100644
index 000000000..3188c8e0d
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/ShellConfigIT.java
@@ -0,0 +1,52 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
import org.apache.accumulo.test.ShellServerIT.TestShell;
import org.apache.accumulo.test.functional.ConfigurableMacIT;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

/**
 * 
 */
public class ShellConfigIT extends ConfigurableMacIT {
  public void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setProperty(Property.CRYPTO_BLOCK_STREAM_SIZE, "7K");
  }

  @Test(timeout = 30000)
  public void experimentalPropTest() throws Exception {
    // ensure experimental props do not show up in config output unless set

    TestShell ts = new TestShell(ROOT_PASSWORD, getCluster().getInstanceName(), getCluster().getZooKeepers(), getCluster().getConfig().getClientConfFile()
        .getAbsolutePath());

    assertTrue(Property.CRYPTO_BLOCK_STREAM_SIZE.isExperimental());
    assertTrue(Property.CRYPTO_CIPHER_ALGORITHM_NAME.isExperimental());

    String configOutput = ts.exec("config");

    assertTrue(configOutput.contains(Property.CRYPTO_BLOCK_STREAM_SIZE.getKey()));
    assertFalse(configOutput.contains(Property.CRYPTO_CIPHER_ALGORITHM_NAME.getKey()));
  }
}
diff --git a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
index 92bae4877..87cd06954 100644
-- a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
@@ -123,83 +123,101 @@ public class ShellServerIT extends SimpleMacIT {
     }
   }
 
  private static final NoOpErrorMessageCallback noop = new NoOpErrorMessageCallback();
  public static class TestShell {
    public TestOutputStream output;
    public StringInputStream input;
    public Shell shell;

    TestShell(String rootPass, String instanceName, String zookeepers, String configFile) throws IOException {
      // start the shell
      output = new TestOutputStream();
      input = new StringInputStream();
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(output));
      shell = new Shell(new ConsoleReader(input, output), pw);
      shell.setLogErrorsToConsole();
      shell.config("-u", "root", "-p", rootPass, "-z", instanceName, zookeepers, "--config-file", configFile);
      exec("quit", true);
      shell.start();
      shell.setExit(false);
    }
 
  public TestOutputStream output;
  public StringInputStream input;
  public Shell shell;
  private static Process traceProcess;
  
  @Rule
  public TestName name = new TestName();
    String exec(String cmd) throws IOException {
      output.clear();
      shell.execCommand(cmd, true, true);
      return output.get();
    }
 
  String exec(String cmd) throws IOException {
    output.clear();
    shell.execCommand(cmd, true, true);
    return output.get();
  }
    String exec(String cmd, boolean expectGoodExit) throws IOException {
      return exec(cmd, expectGoodExit, noop);
    }
 
  String exec(String cmd, boolean expectGoodExit) throws IOException {
    return exec(cmd, expectGoodExit, noop);
  }
    String exec(String cmd, boolean expectGoodExit, ErrorMessageCallback callback) throws IOException {
      String result = exec(cmd);
      if (expectGoodExit)
        assertGoodExit("", true, callback);
      else
        assertBadExit("", true, callback);
      return result;
    }
 
  String exec(String cmd, boolean expectGoodExit, ErrorMessageCallback callback) throws IOException {
    String result = exec(cmd);
    if (expectGoodExit)
      assertGoodExit("", true, callback);
    else
      assertBadExit("", true, callback);
    return result;
  }
    String exec(String cmd, boolean expectGoodExit, String expectString) throws IOException {
      return exec(cmd, expectGoodExit, expectString, noop);
    }
 
  String exec(String cmd, boolean expectGoodExit, String expectString) throws IOException {
    return exec(cmd, expectGoodExit, expectString, noop);
  }
    String exec(String cmd, boolean expectGoodExit, String expectString, ErrorMessageCallback callback) throws IOException {
      return exec(cmd, expectGoodExit, expectString, true, callback);
    }
 
  String exec(String cmd, boolean expectGoodExit, String expectString, ErrorMessageCallback callback) throws IOException {
    return exec(cmd, expectGoodExit, expectString, true, callback);
  }
    String exec(String cmd, boolean expectGoodExit, String expectString, boolean stringPresent) throws IOException {
      return exec(cmd, expectGoodExit, expectString, stringPresent, noop);
    }
 
  String exec(String cmd, boolean expectGoodExit, String expectString, boolean stringPresent) throws IOException {
    return exec(cmd, expectGoodExit, expectString, stringPresent, noop);
  }
    String exec(String cmd, boolean expectGoodExit, String expectString, boolean stringPresent, ErrorMessageCallback callback) throws IOException {
      String result = exec(cmd);
      if (expectGoodExit)
        assertGoodExit(expectString, stringPresent, callback);
      else
        assertBadExit(expectString, stringPresent, callback);
      return result;
    }
 
  String exec(String cmd, boolean expectGoodExit, String expectString, boolean stringPresent, ErrorMessageCallback callback) throws IOException {
    String result = exec(cmd);
    if (expectGoodExit)
      assertGoodExit(expectString, stringPresent, callback);
    else
      assertBadExit(expectString, stringPresent, callback);
    return result;
  }
    void assertGoodExit(String s, boolean stringPresent) {
      assertGoodExit(s, stringPresent, noop);
    }
 
  void assertGoodExit(String s, boolean stringPresent) {
    assertGoodExit(s, stringPresent, noop);
  }
    void assertGoodExit(String s, boolean stringPresent, ErrorMessageCallback callback) {
      Shell.log.info(output.get());
      if (0 != shell.getExitCode()) {
        String errorMsg = callback.getErrorMessage();
        assertEquals(errorMsg, 0, shell.getExitCode());
      }
 
  void assertGoodExit(String s, boolean stringPresent, ErrorMessageCallback callback) {
    Shell.log.info(output.get());
    if (0 != shell.getExitCode()) {
      String errorMsg = callback.getErrorMessage();
      assertEquals(errorMsg, 0, shell.getExitCode());
      if (s.length() > 0)
        assertEquals(s + " present in " + output.get() + " was not " + stringPresent, stringPresent, output.get().contains(s));
     }
 
    if (s.length() > 0)
      assertEquals(s + " present in " + output.get() + " was not " + stringPresent, stringPresent, output.get().contains(s));
  }
    void assertBadExit(String s, boolean stringPresent, ErrorMessageCallback callback) {
      Shell.log.debug(output.get());
      if (0 == shell.getExitCode()) {
        String errorMsg = callback.getErrorMessage();
        assertTrue(errorMsg, shell.getExitCode() > 0);
      }
 
  void assertBadExit(String s, boolean stringPresent, ErrorMessageCallback callback) {
    Shell.log.debug(output.get());
    if (0 == shell.getExitCode()) {
      String errorMsg = callback.getErrorMessage();
      assertTrue(errorMsg, shell.getExitCode() > 0);
      if (s.length() > 0)
        assertEquals(s + " present in " + output.get() + " was not " + stringPresent, stringPresent, output.get().contains(s));
      shell.resetExitCode();
     }

    if (s.length() > 0)
      assertEquals(s + " present in " + output.get() + " was not " + stringPresent, stringPresent, output.get().contains(s));
    shell.resetExitCode();
   }
 
  private static final NoOpErrorMessageCallback noop = new NoOpErrorMessageCallback();

  private TestShell ts;

  private static Process traceProcess;

  @Rule
  public TestName name = new TestName();

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
     // history file is updated in $HOME
@@ -217,17 +235,8 @@ public class ShellServerIT extends SimpleMacIT {
   
   @Before
   public void setupShell() throws Exception {
    // start the shell
    output = new TestOutputStream();
    input = new StringInputStream();
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(output));
    shell = new Shell(new ConsoleReader(input, output), pw);
    shell.setLogErrorsToConsole();
    shell.config("-u", "root", "-p", ROOT_PASSWORD, "-z", getStaticCluster().getConfig().getInstanceName(), getStaticCluster().getConfig().getZooKeepers(),
        "--config-file", getStaticCluster().getConfig().getClientConfFile().getAbsolutePath());
    exec("quit", true);
    shell.start();
    shell.setExit(false);
    ts = new TestShell(ROOT_PASSWORD, getStaticCluster().getConfig().getInstanceName(), getStaticCluster().getConfig().getZooKeepers(),
        getStaticCluster().getConfig().getClientConfFile().getAbsolutePath());
   }
 
   @AfterClass
@@ -250,7 +259,7 @@ public class ShellServerIT extends SimpleMacIT {
 
   @After
   public void tearDownShell() {
    shell.shutdown();
    ts.shell.shutdown();
   }
 
   @Test(timeout = 60000)
@@ -258,23 +267,23 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName(), table2 = table + "2";
     
     // exporttable / importtable
    exec("createtable " + table + " -evc", true);
    ts.exec("createtable " + table + " -evc", true);
     make10();
    exec("addsplits row5", true);
    exec("config -t " + table + " -s table.split.threshold=345M", true);
    exec("offline " + table, true);
    ts.exec("addsplits row5", true);
    ts.exec("config -t " + table + " -s table.split.threshold=345M", true);
    ts.exec("offline " + table, true);
     String export = "file://" + new File(getFolder(), "ShellServerIT.export").toString();
    exec("exporttable -t " + table + " " + export, true);
    ts.exec("exporttable -t " + table + " " + export, true);
     DistCp cp = newDistCp();
     String import_ = "file://" + new File(getFolder(), "ShellServerIT.import").toString();
     cp.run(new String[] {"-f", export + "/distcp.txt", import_});
    exec("importtable " + table2 + " " + import_, true);
    exec("config -t " + table2 + " -np", true, "345M", true);
    exec("getsplits -t " + table2, true, "row5", true);
    exec("constraint --list -t " + table2, true, "VisibilityConstraint=2", true);
    exec("onlinetable " + table, true);
    exec("deletetable -f " + table, true);
    exec("deletetable -f " + table2, true);
    ts.exec("importtable " + table2 + " " + import_, true);
    ts.exec("config -t " + table2 + " -np", true, "345M", true);
    ts.exec("getsplits -t " + table2, true, "row5", true);
    ts.exec("constraint --list -t " + table2, true, "VisibilityConstraint=2", true);
    ts.exec("onlinetable " + table, true);
    ts.exec("deletetable -f " + table, true);
    ts.exec("deletetable -f " + table2, true);
   }
 
   private DistCp newDistCp() {
@@ -302,16 +311,16 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // setscaniter, deletescaniter
    exec("createtable " + table);
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    input.set("true\n\n\nSTRING");
    exec("setscaniter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    exec("scan", true, "3", true);
    exec("deletescaniter -n name", true);
    exec("scan", true, "1", true);
    exec("deletetable -f " + table);
    ts.exec("createtable " + table);
    ts.exec("insert a cf cq 1");
    ts.exec("insert a cf cq 1");
    ts.exec("insert a cf cq 1");
    ts.input.set("true\n\n\nSTRING");
    ts.exec("setscaniter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    ts.exec("scan", true, "3", true);
    ts.exec("deletescaniter -n name", true);
    ts.exec("scan", true, "1", true);
    ts.exec("deletetable -f " + table);
 
   }
 
@@ -322,7 +331,7 @@ public class ShellServerIT extends SimpleMacIT {
     PrintWriter writer = new PrintWriter(file.getAbsolutePath());
     writer.println("about");
     writer.close();
    exec("execfile " + file.getAbsolutePath(), true, Constants.VERSION, true);
    ts.exec("execfile " + file.getAbsolutePath(), true, Constants.VERSION, true);
 
   }
 
@@ -331,11 +340,11 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // egrep
    exec("createtable " + table);
    ts.exec("createtable " + table);
     make10();
    String lines = exec("egrep row[123]", true);
    String lines = ts.exec("egrep row[123]", true);
     assertTrue(lines.split("\n").length - 1 == 3);
    exec("deletetable -f " + table);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
@@ -343,34 +352,34 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // create and delete a table so we get out of a table context in the shell
    exec("notable", true);
    ts.exec("notable", true);
 
     // Calling du not in a table context shouldn't throw an error
    output.clear();
    exec("du", true, "", true);
    ts.output.clear();
    ts.exec("du", true, "", true);
 
    output.clear();
    exec("createtable " + table);
    ts.output.clear();
    ts.exec("createtable " + table);
     make10();
    exec("flush -t " + table + " -w");
    exec("du " + table, true, " [" + table + "]", true);
    output.clear();
    shell.execCommand("du -h", false, false);
    String o = output.get();
    ts.exec("flush -t " + table + " -w");
    ts.exec("du " + table, true, " [" + table + "]", true);
    ts.output.clear();
    ts.shell.execCommand("du -h", false, false);
    String o = ts.output.get();
     // for some reason, there's a bit of fluctuation
     assertTrue("Output did not match regex: '" + o + "'", o.matches(".*[1-9][0-9][0-9]\\s\\[" + table + "\\]\\n"));
    exec("deletetable -f " + table);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 1000)
   public void debug() throws Exception {
    exec("debug", true, "off", true);
    exec("debug on", true);
    exec("debug", true, "on", true);
    exec("debug off", true);
    exec("debug", true, "off", true);
    exec("debug debug", false);
    exec("debug debug debug", false);
    ts.exec("debug", true, "off", true);
    ts.exec("debug on", true);
    ts.exec("debug", true, "on", true);
    ts.exec("debug off", true);
    ts.exec("debug", true, "off", true);
    ts.exec("debug debug", false);
    ts.exec("debug debug debug", false);
   }
 
   @Test(timeout = 45000)
@@ -378,35 +387,35 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // createuser, deleteuser, user, users, droptable, grant, revoke
    input.set("secret\nsecret\n");
    exec("createuser xyzzy", true);
    exec("users", true, "xyzzy", true);
    String perms = exec("userpermissions -u xyzzy", true);
    ts.input.set("secret\nsecret\n");
    ts.exec("createuser xyzzy", true);
    ts.exec("users", true, "xyzzy", true);
    String perms = ts.exec("userpermissions -u xyzzy", true);
     assertTrue(perms.contains("Table permissions (" + MetadataTable.NAME + "): Table.READ"));
    exec("grant -u xyzzy -s System.CREATE_TABLE", true);
    perms = exec("userpermissions -u xyzzy", true);
    ts.exec("grant -u xyzzy -s System.CREATE_TABLE", true);
    perms = ts.exec("userpermissions -u xyzzy", true);
     assertTrue(perms.contains(""));
    exec("grant -u root -t " + MetadataTable.NAME + " Table.WRITE", true);
    exec("grant -u root -t " + MetadataTable.NAME + " Table.GOOFY", false);
    exec("grant -u root -s foo", false);
    exec("grant -u xyzzy -t " + MetadataTable.NAME + " foo", false);
    input.set("secret\nsecret\n");
    exec("user xyzzy", true);
    exec("createtable " + table, true, "xyzzy@", true);
    exec("insert row1 cf cq 1", true);
    exec("scan", true, "row1", true);
    exec("droptable -f " + table, true);
    exec("deleteuser xyzzy", false, "delete yourself", true);
    input.set(ROOT_PASSWORD + "\n" + ROOT_PASSWORD + "\n");
    exec("user root", true);
    exec("revoke -u xyzzy -s System.CREATE_TABLE", true);
    exec("revoke -u xyzzy -s System.GOOFY", false);
    exec("revoke -u xyzzy -s foo", false);
    exec("revoke -u xyzzy -t " + MetadataTable.NAME + " Table.WRITE", true);
    exec("revoke -u xyzzy -t " + MetadataTable.NAME + " Table.GOOFY", false);
    exec("revoke -u xyzzy -t " + MetadataTable.NAME + " foo", false);
    exec("deleteuser xyzzy", true);
    exec("users", true, "xyzzy", false);
    ts.exec("grant -u root -t " + MetadataTable.NAME + " Table.WRITE", true);
    ts.exec("grant -u root -t " + MetadataTable.NAME + " Table.GOOFY", false);
    ts.exec("grant -u root -s foo", false);
    ts.exec("grant -u xyzzy -t " + MetadataTable.NAME + " foo", false);
    ts.input.set("secret\nsecret\n");
    ts.exec("user xyzzy", true);
    ts.exec("createtable " + table, true, "xyzzy@", true);
    ts.exec("insert row1 cf cq 1", true);
    ts.exec("scan", true, "row1", true);
    ts.exec("droptable -f " + table, true);
    ts.exec("deleteuser xyzzy", false, "delete yourself", true);
    ts.input.set(ROOT_PASSWORD + "\n" + ROOT_PASSWORD + "\n");
    ts.exec("user root", true);
    ts.exec("revoke -u xyzzy -s System.CREATE_TABLE", true);
    ts.exec("revoke -u xyzzy -s System.GOOFY", false);
    ts.exec("revoke -u xyzzy -s foo", false);
    ts.exec("revoke -u xyzzy -t " + MetadataTable.NAME + " Table.WRITE", true);
    ts.exec("revoke -u xyzzy -t " + MetadataTable.NAME + " Table.GOOFY", false);
    ts.exec("revoke -u xyzzy -t " + MetadataTable.NAME + " foo", false);
    ts.exec("deleteuser xyzzy", true);
    ts.exec("users", true, "xyzzy", false);
   }
 
   @Test(timeout = 45000)
@@ -414,47 +423,47 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // setshelliter, listshelliter, deleteshelliter
    exec("createtable " + table);
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    input.set("true\n\n\nSTRING\n");
    exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -pn sum -n name", true);
    exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -pn sum -n name", false);
    exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -pn sum -n other", false);
    input.set("true\n\n\nSTRING\n");
    exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -pn sum -n xyzzy", true);
    exec("scan -pn sum", true, "3", true);
    exec("listshelliter", true, "Iterator name", true);
    exec("listshelliter", true, "Iterator xyzzy", true);
    exec("listshelliter", true, "Profile : sum", true);
    exec("deleteshelliter -pn sum -n name", true);
    exec("listshelliter", true, "Iterator name", false);
    exec("listshelliter", true, "Iterator xyzzy", true);
    exec("deleteshelliter -pn sum -a", true);
    exec("listshelliter", true, "Iterator xyzzy", false);
    exec("listshelliter", true, "Profile : sum", false);
    exec("deletetable -f " + table);
    ts.exec("createtable " + table);
    ts.exec("insert a cf cq 1");
    ts.exec("insert a cf cq 1");
    ts.exec("insert a cf cq 1");
    ts.input.set("true\n\n\nSTRING\n");
    ts.exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -pn sum -n name", true);
    ts.exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -pn sum -n name", false);
    ts.exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -pn sum -n other", false);
    ts.input.set("true\n\n\nSTRING\n");
    ts.exec("setshelliter -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -pn sum -n xyzzy", true);
    ts.exec("scan -pn sum", true, "3", true);
    ts.exec("listshelliter", true, "Iterator name", true);
    ts.exec("listshelliter", true, "Iterator xyzzy", true);
    ts.exec("listshelliter", true, "Profile : sum", true);
    ts.exec("deleteshelliter -pn sum -n name", true);
    ts.exec("listshelliter", true, "Iterator name", false);
    ts.exec("listshelliter", true, "Iterator xyzzy", true);
    ts.exec("deleteshelliter -pn sum -a", true);
    ts.exec("listshelliter", true, "Iterator xyzzy", false);
    ts.exec("listshelliter", true, "Profile : sum", false);
    ts.exec("deletetable -f " + table);
     // list iter
    exec("createtable " + table);
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    exec("insert a cf cq 1");
    input.set("true\n\n\nSTRING\n");
    exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -n name", false);
    exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n other", false);
    input.set("true\n\n\nSTRING\n");
    exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -n xyzzy", true);
    exec("scan", true, "3", true);
    exec("listiter -scan", true, "Iterator name", true);
    exec("listiter -scan", true, "Iterator xyzzy", true);
    exec("listiter -minc", true, "Iterator name", false);
    exec("listiter -minc", true, "Iterator xyzzy", false);
    exec("deleteiter -scan -n name", true);
    exec("listiter -scan", true, "Iterator name", false);
    exec("listiter -scan", true, "Iterator xyzzy", true);
    exec("deletetable -f " + table);
    ts.exec("createtable " + table);
    ts.exec("insert a cf cq 1");
    ts.exec("insert a cf cq 1");
    ts.exec("insert a cf cq 1");
    ts.input.set("true\n\n\nSTRING\n");
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -n name", false);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n other", false);
    ts.input.set("true\n\n\nSTRING\n");
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 11 -n xyzzy", true);
    ts.exec("scan", true, "3", true);
    ts.exec("listiter -scan", true, "Iterator name", true);
    ts.exec("listiter -scan", true, "Iterator xyzzy", true);
    ts.exec("listiter -minc", true, "Iterator name", false);
    ts.exec("listiter -minc", true, "Iterator xyzzy", false);
    ts.exec("deleteiter -scan -n name", true);
    ts.exec("listiter -scan", true, "Iterator name", false);
    ts.exec("listiter -scan", true, "Iterator xyzzy", true);
    ts.exec("deletetable -f " + table);
 
   }
 
@@ -463,41 +472,41 @@ public class ShellServerIT extends SimpleMacIT {
     Connector conn = getConnector();
     String tableName = name.getMethodName();
 
    exec("createtable " + tableName);
    input.set("\n\n");
    ts.exec("createtable " + tableName);
    ts.input.set("\n\n");
     // Setting a non-optiondescriber with no name should fail
    exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30", false);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30", false);
 
     // Name as option will work
    exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30 -name cfcounter", true);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30 -name cfcounter", true);
 
     String expectedKey = "table.iterator.scan.cfcounter";
     String expectedValue = "30,org.apache.accumulo.core.iterators.ColumnFamilyCounter";
     TableOperations tops = conn.tableOperations();
     checkTableForProperty(tops, tableName, expectedKey, expectedValue);
 
    exec("deletetable " + tableName, true);
    ts.exec("deletetable " + tableName, true);
     tableName = tableName + "1";
 
    exec("createtable " + tableName, true);
    ts.exec("createtable " + tableName, true);
 
    input.set("customcfcounter\n\n");
    ts.input.set("customcfcounter\n\n");
 
     // Name on the CLI should override OptionDescriber (or user input name, in this case)
    exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30", true);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30", true);
     expectedKey = "table.iterator.scan.customcfcounter";
     expectedValue = "30,org.apache.accumulo.core.iterators.ColumnFamilyCounter";
     checkTableForProperty(tops, tableName, expectedKey, expectedValue);
 
    exec("deletetable " + tableName, true);
    ts.exec("deletetable " + tableName, true);
     tableName = tableName + "1";
 
    exec("createtable " + tableName, true);
    ts.exec("createtable " + tableName, true);
 
    input.set("customcfcounter\nname1 value1\nname2 value2\n\n");
    ts.input.set("customcfcounter\nname1 value1\nname2 value2\n\n");
 
     // Name on the CLI should override OptionDescriber (or user input name, in this case)
    exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30", true);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30", true);
     expectedKey = "table.iterator.scan.customcfcounter";
     expectedValue = "30,org.apache.accumulo.core.iterators.ColumnFamilyCounter";
     checkTableForProperty(tops, tableName, expectedKey, expectedValue);
@@ -508,15 +517,15 @@ public class ShellServerIT extends SimpleMacIT {
     expectedValue = "value2";
     checkTableForProperty(tops, tableName, expectedKey, expectedValue);
 
    exec("deletetable " + tableName, true);
    ts.exec("deletetable " + tableName, true);
     tableName = tableName + "1";
 
    exec("createtable " + tableName, true);
    ts.exec("createtable " + tableName, true);
 
    input.set("\nname1 value1.1,value1.2,value1.3\nname2 value2\n\n");
    ts.input.set("\nname1 value1.1,value1.2,value1.3\nname2 value2\n\n");
 
     // Name on the CLI should override OptionDescriber (or user input name, in this case)
    exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30 -name cfcounter", true);
    ts.exec("setiter -scan -class org.apache.accumulo.core.iterators.ColumnFamilyCounter -p 30 -name cfcounter", true);
     expectedKey = "table.iterator.scan.cfcounter";
     expectedValue = "30,org.apache.accumulo.core.iterators.ColumnFamilyCounter";
     checkTableForProperty(tops, tableName, expectedKey, expectedValue);
@@ -547,20 +556,20 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // notable
    exec("createtable " + table, true);
    exec("scan", true, " " + table + ">", true);
    assertTrue(output.get().contains(" " + table + ">"));
    exec("notable", true);
    exec("scan", false, "Not in a table context.", true);
    assertFalse(output.get().contains(" " + table + ">"));
    exec("deletetable -f " + table);
    ts.exec("createtable " + table, true);
    ts.exec("scan", true, " " + table + ">", true);
    assertTrue(ts.output.get().contains(" " + table + ">"));
    ts.exec("notable", true);
    ts.exec("scan", false, "Not in a table context.", true);
    assertFalse(ts.output.get().contains(" " + table + ">"));
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
   public void sleep() throws Exception {
     // sleep
     long now = System.currentTimeMillis();
    exec("sleep 0.2", true);
    ts.exec("sleep 0.2", true);
     long diff = System.currentTimeMillis() - now;
     assertTrue("Diff was actually " + diff, diff >= 200);
     assertTrue("Diff was actually " + diff, diff < 600);
@@ -570,8 +579,8 @@ public class ShellServerIT extends SimpleMacIT {
   public void addauths() throws Exception {
     final String table = name.getMethodName();
     // addauths
    exec("createtable " + table + " -evc");
    exec("insert a b c d -l foo", false, "does not have authorization", true, new ErrorMessageCallback() {
    ts.exec("createtable " + table + " -evc");
    ts.exec("insert a b c d -l foo", false, "does not have authorization", true, new ErrorMessageCallback() {
       public String getErrorMessage() {
         try {
           Connector c = getConnector();
@@ -581,41 +590,41 @@ public class ShellServerIT extends SimpleMacIT {
         }
       }
     });
    exec("addauths -s foo,bar", true);
    exec("getauths", true, "foo", true);
    exec("getauths", true, "bar", true);
    exec("insert a b c d -l foo");
    exec("scan", true, "[foo]");
    exec("scan -s bar", true, "[foo]", false);
    exec("deletetable -f " + table);
    ts.exec("addauths -s foo,bar", true);
    ts.exec("getauths", true, "foo", true);
    ts.exec("getauths", true, "bar", true);
    ts.exec("insert a b c d -l foo");
    ts.exec("scan", true, "[foo]");
    ts.exec("scan -s bar", true, "[foo]", false);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
   public void byeQuitExit() throws Exception {
     // bye, quit, exit
     for (String cmd : "bye quit exit".split(" ")) {
      assertFalse(shell.getExit());
      exec(cmd);
      assertTrue(shell.getExit());
      shell.setExit(false);
      assertFalse(ts.shell.getExit());
      ts.exec(cmd);
      assertTrue(ts.shell.getExit());
      ts.shell.setExit(false);
     }
   }
 
   @Test(timeout = 45000)
   public void classpath() throws Exception {
     // classpath
    exec("classpath", true, "Level 2: Java Classloader (loads everything defined by java classpath) URL classpath items are", true);
    ts.exec("classpath", true, "Level 2: Java Classloader (loads everything defined by java classpath) URL classpath items are", true);
   }
 
   @Test(timeout = 45000)
   public void clearCls() throws Exception {
     // clear/cls
    if (shell.getReader().getTerminal().isAnsiSupported()) {
      exec("cls", true, "[1;1H");
      exec("clear", true, "[2J");
    if (ts.shell.getReader().getTerminal().isAnsiSupported()) {
      ts.exec("cls", true, "[1;1H");
      ts.exec("clear", true, "[2J");
     } else {
      exec("cls", false, "does not support");
      exec("clear", false, "does not support");
      ts.exec("cls", false, "does not support");
      ts.exec("clear", false, "does not support");
     }
   }
 
@@ -624,18 +633,18 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName(), clone = table + "_clone";
     
     // clonetable
    exec("createtable " + table + " -evc");
    exec("config -t " + table + " -s table.split.threshold=123M", true);
    exec("addsplits -t " + table + " a b c", true);
    exec("insert a b c value");
    exec("scan", true, "value", true);
    exec("clonetable " + table + " " + clone);
    ts.exec("createtable " + table + " -evc");
    ts.exec("config -t " + table + " -s table.split.threshold=123M", true);
    ts.exec("addsplits -t " + table + " a b c", true);
    ts.exec("insert a b c value");
    ts.exec("scan", true, "value", true);
    ts.exec("clonetable " + table + " " + clone);
     // verify constraint, config, and splits were cloned
    exec("constraint --list -t " + clone, true, "VisibilityConstraint=2", true);
    exec("config -t " + clone + " -np", true, "123M", true);
    exec("getsplits -t " + clone, true, "a\nb\nc\n");
    exec("deletetable -f " + table);
    exec("deletetable -f " + clone);
    ts.exec("constraint --list -t " + clone, true, "VisibilityConstraint=2", true);
    ts.exec("config -t " + clone + " -np", true, "123M", true);
    ts.exec("getsplits -t " + clone, true, "a\nb\nc\n");
    ts.exec("deletetable -f " + table);
    ts.exec("deletetable -f " + clone);
   }
   
   @Test(timeout = 45000)
@@ -643,39 +652,39 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // compact
    exec("createtable " + table);
    ts.exec("createtable " + table);
     
     String tableId = getTableId(table);
     
     // make two files
    exec("insert a b c d");
    exec("flush -w");
    exec("insert x y z v");
    exec("flush -w");
    ts.exec("insert a b c d");
    ts.exec("flush -w");
    ts.exec("insert x y z v");
    ts.exec("flush -w");
     int oldCount = countFiles(tableId);
     // merge two files into one
    exec("compact -t " + table + " -w");
    ts.exec("compact -t " + table + " -w");
     assertTrue(countFiles(tableId) < oldCount);
    exec("addsplits -t " + table + " f");
    ts.exec("addsplits -t " + table + " f");
     // make two more files:
    exec("insert m 1 2 3");
    exec("flush -w");
    exec("insert n 1 2 3");
    exec("flush -w");
    ts.exec("insert m 1 2 3");
    ts.exec("flush -w");
    ts.exec("insert n 1 2 3");
    ts.exec("flush -w");
     List<String> oldFiles = getFiles(tableId);
 
     // at this point there are 4 files in the default tablet
     assertEquals("Files that were found: " + oldFiles, 4, oldFiles.size());
     
     // compact some data:
    exec("compact -b g -e z -w");
    ts.exec("compact -b g -e z -w");
     assertEquals(2, countFiles(tableId));
    exec("compact -w");
    ts.exec("compact -w");
     assertEquals(2, countFiles(tableId));
    exec("merge --all -t " + table);
    exec("compact -w");
    ts.exec("merge --all -t " + table);
    ts.exec("compact -w");
     assertEquals(1, countFiles(tableId));
    exec("deletetable -f " + table);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
@@ -683,18 +692,18 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // constraint
    exec("constraint -l -t " + MetadataTable.NAME + "", true, "MetadataConstraints=1", true);
    exec("createtable " + table + " -evc");
    ts.exec("constraint -l -t " + MetadataTable.NAME + "", true, "MetadataConstraints=1", true);
    ts.exec("createtable " + table + " -evc");
 
     // Make sure the table is fully propagated through zoocache
     getTableId(table);
 
    exec("constraint -l -t " + table, true, "VisibilityConstraint=2", true);
    exec("constraint -t " + table + " -d 2", true, "Removed constraint 2 from table " + table);
    ts.exec("constraint -l -t " + table, true, "VisibilityConstraint=2", true);
    ts.exec("constraint -t " + table + " -d 2", true, "Removed constraint 2 from table " + table);
     // wait for zookeeper updates to propagate
     UtilWaitThread.sleep(1000);
    exec("constraint -l -t " + table, true, "VisibilityConstraint=2", false);
    exec("deletetable -f " + table);
    ts.exec("constraint -l -t " + table, true, "VisibilityConstraint=2", false);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
@@ -702,83 +711,83 @@ public class ShellServerIT extends SimpleMacIT {
     final String table = name.getMethodName();
     
     // deletemany
    exec("createtable " + table);
    ts.exec("createtable " + table);
     make10();
     assertEquals(10, countkeys(table));
    exec("deletemany -f -b row8");
    ts.exec("deletemany -f -b row8");
     assertEquals(8, countkeys(table));
    exec("scan -t " + table + " -np", true, "row8", false);
    ts.exec("scan -t " + table + " -np", true, "row8", false);
     make10();
    exec("deletemany -f -b row4 -e row5");
    ts.exec("deletemany -f -b row4 -e row5");
     assertEquals(8, countkeys(table));
     make10();
    exec("deletemany -f -c cf:col4,cf:col5");
    ts.exec("deletemany -f -c cf:col4,cf:col5");
     assertEquals(8, countkeys(table));
     make10();
    exec("deletemany -f -r row3");
    ts.exec("deletemany -f -r row3");
     assertEquals(9, countkeys(table));
     make10();
    exec("deletemany -f -r row3");
    ts.exec("deletemany -f -r row3");
     assertEquals(9, countkeys(table));
     make10();
    exec("deletemany -f -b row3 -be -e row5 -ee");
    ts.exec("deletemany -f -b row3 -be -e row5 -ee");
     assertEquals(9, countkeys(table));
    exec("deletetable -f " + table);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
   public void deleterows() throws Exception {
     final String table = name.getMethodName();
 
    exec("createtable " + table);
    ts.exec("createtable " + table);
     final String tableId = getTableId(table);
     
     // deleterows
     int base = countFiles(tableId);
     assertEquals(0, base);
     
    exec("addsplits row5 row7");
    ts.exec("addsplits row5 row7");
     make10();
    exec("flush -w -t " + table);
    ts.exec("flush -w -t " + table);
     List<String> files = getFiles(tableId);
     assertEquals("Found the following files: " + files, 3, files.size());
    exec("deleterows -t " + table + " -b row5 -e row7", true);
    ts.exec("deleterows -t " + table + " -b row5 -e row7", true);
     assertEquals(2, countFiles(tableId));
    exec("deletetable -f " + table);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
   public void groups() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table);
    exec("setgroups -t " + table + " alpha=a,b,c num=3,2,1");
    exec("getgroups -t " + table, true, "alpha=a,b,c", true);
    exec("getgroups -t " + table, true, "num=1,2,3", true);
    exec("deletetable -f " + table);
    ts.exec("createtable " + table);
    ts.exec("setgroups -t " + table + " alpha=a,b,c num=3,2,1");
    ts.exec("getgroups -t " + table, true, "alpha=a,b,c", true);
    ts.exec("getgroups -t " + table, true, "num=1,2,3", true);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
   public void grep() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table, true);
    ts.exec("createtable " + table, true);
     make10();
    exec("grep row[123]", true, "row1", false);
    exec("grep row5", true, "row5", true);
    exec("deletetable -f " + table, true);
    ts.exec("grep row[123]", true, "row1", false);
    ts.exec("grep row5", true, "row5", true);
    ts.exec("deletetable -f " + table, true);
   }
 
   @Test(timeout = 45000)
   public void help() throws Exception {
    exec("help -np", true, "Help Commands", true);
    exec("?", true, "Help Commands", true);
    ts.exec("help -np", true, "Help Commands", true);
    ts.exec("?", true, "Help Commands", true);
     for (String c : ("bye exit quit " + "about help info ? " + "deleteiter deletescaniter listiter setiter setscaniter "
         + "grant revoke systempermissions tablepermissions userpermissions " + "execfile history " + "authenticate cls clear notable sleep table user whoami "
         + "clonetable config createtable deletetable droptable du exporttable importtable offline online renametable tables "
         + "addsplits compact constraint flush getgropus getsplits merge setgroups " + "addauths createuser deleteuser dropuser getauths passwd setauths users "
         + "delete deletemany deleterows egrep formatter interpreter grep importdirectory insert maxrow scan").split(" ")) {
      exec("help " + c, true);
      ts.exec("help " + c, true);
     }
   }
 
@@ -786,11 +795,11 @@ public class ShellServerIT extends SimpleMacIT {
   public void history() throws Exception {
     final String table = name.getMethodName();
     
    exec("history -c", true);
    exec("createtable " + table);
    exec("deletetable -f " + table);
    exec("history", true, table, true);
    exec("history", true, "history", true);
    ts.exec("history -c", true);
    ts.exec("createtable " + table);
    ts.exec("deletetable -f " + table);
    ts.exec("history", true, table, true);
    ts.exec("history", true, "history", true);
   }
 
   @Test(timeout = 45000)
@@ -811,161 +820,161 @@ public class ShellServerIT extends SimpleMacIT {
     evenWriter.startDefaultLocalityGroup();
     FileSKVWriter oddWriter = FileOperations.getInstance().openWriter(odd, fs, conf, aconf);
     oddWriter.startDefaultLocalityGroup();
    long ts = System.currentTimeMillis();
    long timestamp = System.currentTimeMillis();
     Text cf = new Text("cf");
     Text cq = new Text("cq");
     Value value = new Value("value".getBytes());
     for (int i = 0; i < 100; i += 2) {
      Key key = new Key(new Text(String.format("%8d", i)), cf, cq, ts);
      Key key = new Key(new Text(String.format("%8d", i)), cf, cq, timestamp);
       evenWriter.append(key, value);
      key = new Key(new Text(String.format("%8d", i + 1)), cf, cq, ts);
      key = new Key(new Text(String.format("%8d", i + 1)), cf, cq, timestamp);
       oddWriter.append(key, value);
     }
     evenWriter.close();
     oddWriter.close();
    assertEquals(0, shell.getExitCode());
    exec("createtable " + table, true);
    exec("importdirectory " + importDir + " " + errorsDir + " true", true);
    exec("scan -r 00000000", true, "00000000", true);
    exec("scan -r 00000099", true, "00000099", true);
    exec("deletetable -f " + table);
    assertEquals(0, ts.shell.getExitCode());
    ts.exec("createtable " + table, true);
    ts.exec("importdirectory " + importDir + " " + errorsDir + " true", true);
    ts.exec("scan -r 00000000", true, "00000000", true);
    ts.exec("scan -r 00000099", true, "00000099", true);
    ts.exec("deletetable -f " + table);
   }
 
   @Test(timeout = 45000)
   public void info() throws Exception {
    exec("info", true, Constants.VERSION, true);
    ts.exec("info", true, Constants.VERSION, true);
   }
 
   @Test(timeout = 45000)
   public void interpreter() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table, true);
    exec("interpreter -l", true, "HexScan", false);
    exec("insert \\x02 cf cq value", true);
    exec("scan -b 02", true, "value", false);
    exec("interpreter -i org.apache.accumulo.core.util.interpret.HexScanInterpreter", true);
    ts.exec("createtable " + table, true);
    ts.exec("interpreter -l", true, "HexScan", false);
    ts.exec("insert \\x02 cf cq value", true);
    ts.exec("scan -b 02", true, "value", false);
    ts.exec("interpreter -i org.apache.accumulo.core.util.interpret.HexScanInterpreter", true);
     // Need to allow time for this to propagate through zoocache/zookeeper
     UtilWaitThread.sleep(3000);
 
    exec("interpreter -l", true, "HexScan", true);
    exec("scan -b 02", true, "value", true);
    exec("deletetable -f " + table, true);
    ts.exec("interpreter -l", true, "HexScan", true);
    ts.exec("scan -b 02", true, "value", true);
    ts.exec("deletetable -f " + table, true);
   }
 
   @Test(timeout = 45000)
   public void listcompactions() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table, true);
    exec("config -t " + table + " -s table.iterator.minc.slow=30,org.apache.accumulo.test.functional.SlowIterator", true);
    exec("config -t " + table + " -s table.iterator.minc.slow.opt.sleepTime=1000", true);
    exec("insert a cf cq value", true);
    exec("insert b cf cq value", true);
    exec("insert c cf cq value", true);
    exec("insert d cf cq value", true);
    exec("flush -t " + table, true);
    exec("sleep 0.2", true);
    exec("listcompactions", true, "default_tablet");
    String[] lines = output.get().split("\n");
    ts.exec("createtable " + table, true);
    ts.exec("config -t " + table + " -s table.iterator.minc.slow=30,org.apache.accumulo.test.functional.SlowIterator", true);
    ts.exec("config -t " + table + " -s table.iterator.minc.slow.opt.sleepTime=1000", true);
    ts.exec("insert a cf cq value", true);
    ts.exec("insert b cf cq value", true);
    ts.exec("insert c cf cq value", true);
    ts.exec("insert d cf cq value", true);
    ts.exec("flush -t " + table, true);
    ts.exec("sleep 0.2", true);
    ts.exec("listcompactions", true, "default_tablet");
    String[] lines = ts.output.get().split("\n");
     String last = lines[lines.length - 1];
     String[] parts = last.split("\\|");
     assertEquals(12, parts.length);
    exec("deletetable -f " + table, true);
    ts.exec("deletetable -f " + table, true);
   }
 
   @Test(timeout = 45000)
   public void maxrow() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table, true);
    exec("insert a cf cq value", true);
    exec("insert b cf cq value", true);
    exec("insert ccc cf cq value", true);
    exec("insert zzz cf cq value", true);
    exec("maxrow", true, "zzz", true);
    exec("delete zzz cf cq", true);
    exec("maxrow", true, "ccc", true);
    exec("deletetable -f " + table, true);
    ts.exec("createtable " + table, true);
    ts.exec("insert a cf cq value", true);
    ts.exec("insert b cf cq value", true);
    ts.exec("insert ccc cf cq value", true);
    ts.exec("insert zzz cf cq value", true);
    ts.exec("maxrow", true, "zzz", true);
    ts.exec("delete zzz cf cq", true);
    ts.exec("maxrow", true, "ccc", true);
    ts.exec("deletetable -f " + table, true);
   }
 
   @Test(timeout = 45000)
   public void merge() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table);
    exec("addsplits a m z");
    exec("getsplits", true, "z", true);
    exec("merge --all", true);
    exec("getsplits", true, "z", false);
    exec("deletetable -f " + table);
    exec("getsplits -t " + MetadataTable.NAME + "", true);
    assertEquals(2, output.get().split("\n").length);
    exec("getsplits -t accumulo.root", true);
    assertEquals(1, output.get().split("\n").length);
    exec("merge --all -t " + MetadataTable.NAME + "");
    exec("getsplits -t " + MetadataTable.NAME + "", true);
    assertEquals(1, output.get().split("\n").length);
    ts.exec("createtable " + table);
    ts.exec("addsplits a m z");
    ts.exec("getsplits", true, "z", true);
    ts.exec("merge --all", true);
    ts.exec("getsplits", true, "z", false);
    ts.exec("deletetable -f " + table);
    ts.exec("getsplits -t " + MetadataTable.NAME + "", true);
    assertEquals(2, ts.output.get().split("\n").length);
    ts.exec("getsplits -t accumulo.root", true);
    assertEquals(1, ts.output.get().split("\n").length);
    ts.exec("merge --all -t " + MetadataTable.NAME + "");
    ts.exec("getsplits -t " + MetadataTable.NAME + "", true);
    assertEquals(1, ts.output.get().split("\n").length);
   }
 
   @Test(timeout = 45000)
   public void ping() throws Exception {
     for (int i = 0; i < 10; i++) {
      exec("ping", true, "OK", true);
      ts.exec("ping", true, "OK", true);
       // wait for both tservers to start up
      if (output.get().split("\n").length == 3)
      if (ts.output.get().split("\n").length == 3)
         break;
       UtilWaitThread.sleep(1000);
 
     }
    assertEquals(3, output.get().split("\n").length);
    assertEquals(3, ts.output.get().split("\n").length);
   }
 
   @Test(timeout = 45000)
   public void renametable() throws Exception {
     final String table = name.getMethodName() + "1", rename = name.getMethodName() + "2";
     
    exec("createtable " + table);
    exec("insert this is a value");
    exec("renametable " + table + " " + rename);
    exec("tables", true, rename, true);
    exec("tables", true, table, false);
    exec("scan -t " + rename, true, "value", true);
    exec("deletetable -f " + rename, true);
    ts.exec("createtable " + table);
    ts.exec("insert this is a value");
    ts.exec("renametable " + table + " " + rename);
    ts.exec("tables", true, rename, true);
    ts.exec("tables", true, table, false);
    ts.exec("scan -t " + rename, true, "value", true);
    ts.exec("deletetable -f " + rename, true);
   }
 
   @Test(timeout = 30000)
   public void tables() throws Exception {
     final String table = name.getMethodName(), table1 = table + "_z", table2 = table + "_a";
    exec("createtable " + table1);
    exec("createtable " + table2);
    exec("notable");
    String lst = exec("tables -l");
    ts.exec("createtable " + table1);
    ts.exec("createtable " + table2);
    ts.exec("notable");
    String lst = ts.exec("tables -l");
     assertTrue(lst.indexOf(table2) < lst.indexOf(table1));
    lst = exec("tables -l -s");
    lst = ts.exec("tables -l -s");
     assertTrue(lst.indexOf(table1) < lst.indexOf(table2));
   }
 
   @Test(timeout = 45000)
   public void systempermission() throws Exception {
    exec("systempermissions");
    assertEquals(11, output.get().split("\n").length - 1);
    exec("tablepermissions", true);
    assertEquals(6, output.get().split("\n").length - 1);
    ts.exec("systempermissions");
    assertEquals(11, ts.output.get().split("\n").length - 1);
    ts.exec("tablepermissions", true);
    assertEquals(6, ts.output.get().split("\n").length - 1);
   }
 
   @Test(timeout = 45000)
   public void listscans() throws Exception {
     final String table = name.getMethodName();
     
    exec("createtable " + table, true);
    ts.exec("createtable " + table, true);
 
     // Should be about a 3 second scan
     for (int i = 0; i < 6; i++) {
      exec("insert " + i + " cf cq value", true);
      ts.exec("insert " + i + " cf cq value", true);
     }
    exec("config -t " + table + " -s table.iterator.scan.slow=30,org.apache.accumulo.test.functional.SlowIterator", true);
    exec("config -t " + table + " -s table.iterator.scan.slow.opt.sleepTime=500", true);
    ts.exec("config -t " + table + " -s table.iterator.scan.slow=30,org.apache.accumulo.test.functional.SlowIterator", true);
    ts.exec("config -t " + table + " -s table.iterator.scan.slow.opt.sleepTime=500", true);
     Thread thread = new Thread() {
       @Override
       public void run() {
@@ -983,7 +992,7 @@ public class ShellServerIT extends SimpleMacIT {
     List<String> scans = new ArrayList<String>();
     // Try to find the active scan for about 5seconds
     for (int i = 0; i < 50 && scans.isEmpty(); i++) {
      String currentScans = exec("listscans", true);
      String currentScans = ts.exec("listscans", true);
       String[] lines = currentScans.split("\n");
       for (int scanOffset = 2; i < lines.length; i++) {
         String currentScan = lines[scanOffset];
@@ -1010,7 +1019,7 @@ public class ShellServerIT extends SimpleMacIT {
       assertTrue(client.matches(hostPortPattern));
     }
     
    exec("deletetable -f " + table, true);
    ts.exec("deletetable -f " + table, true);
   }
 
   @Test(timeout = 45000)
@@ -1026,37 +1035,37 @@ public class ShellServerIT extends SimpleMacIT {
     FileUtils.copyURLToFile(this.getClass().getResource("/FooConstraint.jar"), fooConstraintJar);
     fooConstraintJar.deleteOnExit();
 
    exec(
    ts.exec(
         "config -s " + Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey() + "cx1=" + fooFilterJar.toURI().toString() + "," + fooConstraintJar.toURI().toString(),
         true);
 
    exec("createtable " + table, true);
    exec("config -t " + table + " -s " + Property.TABLE_CLASSPATH.getKey() + "=cx1", true);
    ts.exec("createtable " + table, true);
    ts.exec("config -t " + table + " -s " + Property.TABLE_CLASSPATH.getKey() + "=cx1", true);
 
     UtilWaitThread.sleep(200);
 
     // We can't use the setiter command as Filter implements OptionDescriber which
     // forces us to enter more input that I don't know how to input
     // Instead, we can just manually set the property on the table.
    exec("config -t " + table + " -s " + Property.TABLE_ITERATOR_PREFIX.getKey() + "scan.foo=10,org.apache.accumulo.test.FooFilter");
    ts.exec("config -t " + table + " -s " + Property.TABLE_ITERATOR_PREFIX.getKey() + "scan.foo=10,org.apache.accumulo.test.FooFilter");
 
    exec("insert foo f q v", true);
    ts.exec("insert foo f q v", true);
 
     UtilWaitThread.sleep(100);
 
    exec("scan -np", true, "foo", false);
    ts.exec("scan -np", true, "foo", false);
 
    exec("constraint -a FooConstraint", true);
    ts.exec("constraint -a FooConstraint", true);
 
    exec("offline -w " + table);
    exec("online -w " + table);
    ts.exec("offline -w " + table);
    ts.exec("online -w " + table);
 
    exec("table " + table, true);
    exec("insert foo f q v", false);
    exec("insert ok foo q v", true);
    ts.exec("table " + table, true);
    ts.exec("insert foo f q v", false);
    ts.exec("insert ok foo q v", true);
 
    exec("deletetable -f " + table, true);
    exec("config -d " + Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey() + "cx1");
    ts.exec("deletetable -f " + table, true);
    ts.exec("config -d " + Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey() + "cx1");
 
   }
 
@@ -1065,13 +1074,13 @@ public class ShellServerIT extends SimpleMacIT {
     // Make sure to not collide with the "trace" table
     final String table = name.getMethodName() + "Test";
     
    exec("trace on", true);
    exec("createtable " + table, true);
    exec("insert a b c value", true);
    exec("scan -np", true, "value", true);
    exec("deletetable -f " + table);
    exec("sleep 1");
    String trace = exec("trace off");
    ts.exec("trace on", true);
    ts.exec("createtable " + table, true);
    ts.exec("insert a b c value", true);
    ts.exec("scan -np", true, "value", true);
    ts.exec("deletetable -f " + table);
    ts.exec("sleep 1");
    String trace = ts.exec("trace off");
     System.out.println(trace);
     assertTrue(trace.contains("sendMutations"));
     assertTrue(trace.contains("startScan"));
@@ -1080,136 +1089,136 @@ public class ShellServerIT extends SimpleMacIT {
 
   @Test(timeout = 30000)
   public void badLogin() throws Exception {
    input.set(ROOT_PASSWORD + "\n");
    String err = exec("user NoSuchUser", false);
    ts.input.set(ROOT_PASSWORD + "\n");
    String err = ts.exec("user NoSuchUser", false);
     assertTrue(err.contains("BAD_CREDENTIALS for user NoSuchUser"));
   }
 
   @Test(timeout = 30000)
   public void namespaces() throws Exception {
    exec("namespaces", true, "\"\"", true); // default namespace, displayed as quoted empty string
    exec("namespaces", true, Namespaces.ACCUMULO_NAMESPACE, true);
    exec("createnamespace thing1", true);
    String namespaces = exec("namespaces");
    ts.exec("namespaces", true, "\"\"", true); // default namespace, displayed as quoted empty string
    ts.exec("namespaces", true, Namespaces.ACCUMULO_NAMESPACE, true);
    ts.exec("createnamespace thing1", true);
    String namespaces = ts.exec("namespaces");
     assertTrue(namespaces.contains("thing1"));
 
    exec("renamenamespace thing1 thing2");
    namespaces = exec("namespaces");
    ts.exec("renamenamespace thing1 thing2");
    namespaces = ts.exec("namespaces");
     assertTrue(namespaces.contains("thing2"));
     assertTrue(!namespaces.contains("thing1"));
 
     // can't delete a namespace that still contains tables, unless you do -f
    exec("createtable thing2.thingy", true);
    exec("deletenamespace thing2");
    exec("y");
    exec("namespaces", true, "thing2", true);
    ts.exec("createtable thing2.thingy", true);
    ts.exec("deletenamespace thing2");
    ts.exec("y");
    ts.exec("namespaces", true, "thing2", true);
 
    exec("du -ns thing2", true, "thing2.thingy", true);
    ts.exec("du -ns thing2", true, "thing2.thingy", true);
 
     // all "TableOperation" commands can take a namespace
    exec("offline -ns thing2", true);
    exec("online -ns thing2", true);
    exec("flush -ns thing2", true);
    exec("compact -ns thing2", true);
    exec("createnamespace testers3", true);
    exec("createtable testers3.1", true);
    exec("createtable testers3.2", true);
    exec("deletetable -ns testers3 -f", true);
    exec("tables", true, "testers3.1", false);
    exec("namespaces", true, "testers3", true);
    exec("deletenamespace testers3 -f", true);
    input.set("true\n\n\nSTRING\n");
    exec("setiter -ns thing2 -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    exec("listiter -ns thing2 -scan", true, "Summing", true);
    exec("deleteiter -ns thing2 -n name -scan", true);
    exec("createuser dude");
    exec("pass");
    exec("pass");
    exec("grant Namespace.CREATE_TABLE -ns thing2 -u dude", true);
    exec("revoke Namespace.CREATE_TABLE -ns thing2 -u dude", true);
    ts.exec("offline -ns thing2", true);
    ts.exec("online -ns thing2", true);
    ts.exec("flush -ns thing2", true);
    ts.exec("compact -ns thing2", true);
    ts.exec("createnamespace testers3", true);
    ts.exec("createtable testers3.1", true);
    ts.exec("createtable testers3.2", true);
    ts.exec("deletetable -ns testers3 -f", true);
    ts.exec("tables", true, "testers3.1", false);
    ts.exec("namespaces", true, "testers3", true);
    ts.exec("deletenamespace testers3 -f", true);
    ts.input.set("true\n\n\nSTRING\n");
    ts.exec("setiter -ns thing2 -scan -class org.apache.accumulo.core.iterators.user.SummingCombiner -p 10 -n name", true);
    ts.exec("listiter -ns thing2 -scan", true, "Summing", true);
    ts.exec("deleteiter -ns thing2 -n name -scan", true);
    ts.exec("createuser dude");
    ts.exec("pass");
    ts.exec("pass");
    ts.exec("grant Namespace.CREATE_TABLE -ns thing2 -u dude", true);
    ts.exec("revoke Namespace.CREATE_TABLE -ns thing2 -u dude", true);
 
     // properties override and such
    exec("config -ns thing2 -s table.file.max=44444", true);
    exec("config -ns thing2", true, "44444", true);
    exec("config -t thing2.thingy", true, "44444", true);
    exec("config -t thing2.thingy -s table.file.max=55555", true);
    exec("config -t thing2.thingy", true, "55555", true);
    ts.exec("config -ns thing2 -s table.file.max=44444", true);
    ts.exec("config -ns thing2", true, "44444", true);
    ts.exec("config -t thing2.thingy", true, "44444", true);
    ts.exec("config -t thing2.thingy -s table.file.max=55555", true);
    ts.exec("config -t thing2.thingy", true, "55555", true);
 
     // can copy properties when creating
    exec("createnamespace thing3 -cc thing2", true);
    exec("config -ns thing3", true, "44444", true);
    ts.exec("createnamespace thing3 -cc thing2", true);
    ts.exec("config -ns thing3", true, "44444", true);
 
    exec("deletenamespace -f thing2", true);
    exec("namespaces", true, "thing2", false);
    exec("tables", true, "thing2.thingy", false);
    ts.exec("deletenamespace -f thing2", true);
    ts.exec("namespaces", true, "thing2", false);
    ts.exec("tables", true, "thing2.thingy", false);
 
     // put constraints on a namespace
    exec("constraint -ns thing3 -a org.apache.accumulo.examples.simple.constraints.NumericValueConstraint", true);
    exec("createtable thing3.constrained", true);
    exec("table thing3.constrained", true);
    exec("constraint -d 1");
    ts.exec("constraint -ns thing3 -a org.apache.accumulo.examples.simple.constraints.NumericValueConstraint", true);
    ts.exec("createtable thing3.constrained", true);
    ts.exec("table thing3.constrained", true);
    ts.exec("constraint -d 1");
     // should fail
    exec("constraint -l", true, "NumericValueConstraint", true);
    exec("insert r cf cq abc", false);
    exec("constraint -ns thing3 -d 1");
    exec("sleep 1");
    exec("insert r cf cq abc", true);
    ts.exec("constraint -l", true, "NumericValueConstraint", true);
    ts.exec("insert r cf cq abc", false);
    ts.exec("constraint -ns thing3 -d 1");
    ts.exec("sleep 1");
    ts.exec("insert r cf cq abc", true);
   }
 
   private int countkeys(String table) throws IOException {
    exec("scan -np -t " + table);
    return output.get().split("\n").length - 1;
    ts.exec("scan -np -t " + table);
    return ts.output.get().split("\n").length - 1;
   }
 
   @Test(timeout = 30000)
   public void scans() throws Exception {
    exec("createtable t");
    ts.exec("createtable t");
     make10();
    String result = exec("scan -np -b row1 -e row1");
    String result = ts.exec("scan -np -b row1 -e row1");
     assertEquals(2, result.split("\n").length);
    result = exec("scan -np -b row3 -e row5");
    result = ts.exec("scan -np -b row3 -e row5");
     assertEquals(4, result.split("\n").length);
    result = exec("scan -np -r row3");
    result = ts.exec("scan -np -r row3");
     assertEquals(2, result.split("\n").length);
    result = exec("scan -np -b row:");
    result = ts.exec("scan -np -b row:");
     assertEquals(1, result.split("\n").length);
    result = exec("scan -np -b row");
    result = ts.exec("scan -np -b row");
     assertEquals(11, result.split("\n").length);
    result = exec("scan -np -e row:");
    result = ts.exec("scan -np -e row:");
     assertEquals(11, result.split("\n").length);
    exec("deletetable -f t");
    ts.exec("deletetable -f t");
   }
 
   @Test(timeout = 30000)
   public void whoami() throws Exception {
    assertTrue(exec("whoami", true).contains("root"));
    input.set("secret\nsecret\n");
    exec("createuser test_user");
    exec("setauths -u test_user -s 12,3,4");
    String auths = exec("getauths -u test_user");
    assertTrue(ts.exec("whoami", true).contains("root"));
    ts.input.set("secret\nsecret\n");
    ts.exec("createuser test_user");
    ts.exec("setauths -u test_user -s 12,3,4");
    String auths = ts.exec("getauths -u test_user");
     assertTrue(auths.contains("3") && auths.contains("12") && auths.contains("4"));
    input.set("secret\n");
    exec("user test_user", true);
    assertTrue(exec("whoami", true).contains("test_user"));
    input.set(ROOT_PASSWORD + "\n");
    exec("user root", true);
    ts.input.set("secret\n");
    ts.exec("user test_user", true);
    assertTrue(ts.exec("whoami", true).contains("test_user"));
    ts.input.set(ROOT_PASSWORD + "\n");
    ts.exec("user root", true);
   }
 
   private void make10() throws IOException {
     for (int i = 0; i < 10; i++) {
      exec(String.format("insert row%d cf col%d value", i, i));
      ts.exec(String.format("insert row%d cf col%d value", i, i));
     }
   }
   
   private List<String> getFiles(String tableId) throws IOException {
    output.clear();
    ts.output.clear();
 
    exec("scan -t " + MetadataTable.NAME + " -np -c file -b " + tableId + " -e " + tableId + "~");
    ts.exec("scan -t " + MetadataTable.NAME + " -np -c file -b " + tableId + " -e " + tableId + "~");
     
    log.debug("countFiles(): " + output.get());
    log.debug("countFiles(): " + ts.output.get());
     
    String[] lines = StringUtils.split(output.get(), "\n");
    output.clear();
    String[] lines = StringUtils.split(ts.output.get(), "\n");
    ts.output.clear();
 
     if (0 == lines.length) {
       return Collections.emptyList();
- 
2.19.1.windows.1

