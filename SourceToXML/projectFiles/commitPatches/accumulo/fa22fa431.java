From fa22fa431d1de7b7f636f8011ced1b4d45bab7cd Mon Sep 17 00:00:00 2001
From: Max Jordan <mjordan@bbn.com>
Date: Fri, 8 May 2015 11:30:42 -0400
Subject: [PATCH] ACCUMULO-3784 fix getauths

Signed-off-by: Josh Elser <elserj@apache.org>
--
 .../util/shell/commands/GetAuthsCommand.java  | 19 +++++---
 .../shell/commands/GetAuthsCommandTest.java   | 44 +++++++++++++++++++
 2 files changed, 57 insertions(+), 6 deletions(-)
 create mode 100644 core/src/test/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommandTest.java

diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java b/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java
index 74aefaace..8c1e88be7 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommand.java
@@ -17,8 +17,9 @@
 package org.apache.accumulo.core.util.shell.commands;
 
 import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
@@ -38,14 +39,20 @@ public class GetAuthsCommand extends Command {
     final String user = cl.getOptionValue(userOpt.getOpt(), shellState.getConnector().whoami());
     // Sort authorizations
     Authorizations auths = shellState.getConnector().securityOperations().getUserAuthorizations(user);
    SortedSet<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    for (byte[] auth : auths) {
      set.add(new String(auth));
    }
    List<String> set = sortAuthorizations(auths);
     shellState.getReader().println(StringUtils.join(set, ','));
     return 0;
   }
 
  protected List<String> sortAuthorizations(Authorizations auths) {
    List<String> list = new ArrayList<String>();
    for (byte[] auth : auths) {
      list.add(new String(auth));
    }
    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
    return list;
  }

   @Override
   public String description() {
     return "displays the maximum scan authorizations for a user";
diff --git a/core/src/test/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommandTest.java b/core/src/test/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommandTest.java
new file mode 100644
index 000000000..b0f06723c
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/util/shell/commands/GetAuthsCommandTest.java
@@ -0,0 +1,44 @@
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

package org.apache.accumulo.core.util.shell.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.accumulo.core.security.Authorizations;
import org.junit.Test;

public class GetAuthsCommandTest {

  @Test
  public void removeAccumuloNamespaceTables() {
    Authorizations auths = new Authorizations("AAA", "aaa", "bbb", "BBB");
    GetAuthsCommand cmd = new GetAuthsCommand();
    List<String> sorted = cmd.sortAuthorizations(auths);

    assertNotNull(sorted);
    assertEquals(sorted.size(), 4);

    assertEquals(sorted.get(0), "AAA");
    assertEquals(sorted.get(1), "aaa");
    assertEquals(sorted.get(2), "BBB");
    assertEquals(sorted.get(3), "bbb");
  }
}
- 
2.19.1.windows.1

