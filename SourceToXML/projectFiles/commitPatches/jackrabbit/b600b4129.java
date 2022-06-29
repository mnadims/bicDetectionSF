From b600b4129673d4e906dd0376c053ccdf6bed20be Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Mon, 11 Jul 2016 14:44:14 +0000
Subject: [PATCH] JCR-3992: JcrUtils.getOrCreateByPath broken by JCR-3987

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1752165 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/integration/UtilsGetPathTest.java    | 46 +++++++++++++++++++
 .../apache/jackrabbit/commons/JcrUtils.java   |  4 +-
 2 files changed, 48 insertions(+), 2 deletions(-)
 create mode 100755 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/UtilsGetPathTest.java

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/UtilsGetPathTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/UtilsGetPathTest.java
new file mode 100755
index 000000000..2eb052b02
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/UtilsGetPathTest.java
@@ -0,0 +1,46 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.integration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.junit.Test;

/**
 * @see <a href="https://issues.apache.org/jira/browse/JCR-3992">JCR-3992</a>
 */
public class UtilsGetPathTest extends AbstractJCRTest {

    @Test
    public void testGetOrCreateByPath1() throws RepositoryException {
        String path ="/foo";
        Node node = JcrUtils.getOrCreateByPath(path, "nt:unstructured", superuser);
        superuser.save();
        assertEquals(path, node.getPath());
        assertTrue(superuser.nodeExists(path));

        // existing top-level node, two new descendant nodes
        String path2 ="/foo/a/b";
        Node node2 = JcrUtils.getOrCreateByPath(path2, "nt:unstructured", superuser);
        superuser.save();
        assertEquals(path2, node2.getPath());
        assertTrue(superuser.nodeExists(path2));
    }
}
diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java
index 4fefe019a..5855ea2bc 100644
-- a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/JcrUtils.java
@@ -1556,12 +1556,12 @@ public class JcrUtils {
                 existingPath = temp;
                 break;
             }
            currentIndex = temp.lastIndexOf("/");
            currentIndex = temp.lastIndexOf('/');
         }
 
         if (existingPath != null) {
             baseNode = baseNode.getSession().getNode(existingPath);
            path = path.substring(existingPath.length() + 1);
            path = fullPath.substring(existingPath.length() + 1);
         }
 
         Node node = baseNode;
- 
2.19.1.windows.1

