From 84ec4222e1dbd12f35625f33aa706f7478f37711 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 23 Sep 2008 15:57:43 +0000
Subject: [PATCH] JCR-1755: ClassCastException when registering custom node by
 XML file

Avoid the ClassCastException in DOMWalker.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@698209 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/util/DOMWalker.java       |  5 ++-
 .../jackrabbit/core/util/DOMWalkerTest.java   | 45 +++++++++++++++++++
 2 files changed, 49 insertions(+), 1 deletion(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/util/DOMWalkerTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/DOMWalker.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/DOMWalker.java
index bd0b46271..b1001d202 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/DOMWalker.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/DOMWalker.java
@@ -212,7 +212,10 @@ public final class DOMWalker {
         }
 
         if (name.equals(current.getNodeName())) {
            current = (Element) current.getParentNode();
            Node parent = current.getParentNode();
            if (parent instanceof Element) {
                current = (Element) parent;
            }
         }
         return false;
     }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/util/DOMWalkerTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/util/DOMWalkerTest.java
new file mode 100644
index 000000000..16daa31ac
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/util/DOMWalkerTest.java
@@ -0,0 +1,45 @@
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
package org.apache.jackrabbit.core.util;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link DOMWalker} class.
 */
public class DOMWalkerTest extends TestCase {

    /**
     * <a href="https://issues.apache.org/jira/browse/JCR-1755">JCR-1755</a>:
     * ClassCastException when registering custom node by XML file
     */
    public void testIterateTopLevelElements() throws Exception {
        DOMWalker walker = new DOMWalker(
                new ByteArrayInputStream("<nodeType/>".getBytes("UTF-8")));
        try {
            while (walker.iterateElements("nodeType")) {
                // do nothing
            }
        } catch (ClassCastException e) {
            fail("JCR-1755: ClassCastException when registering"
                    + " custom node by XML file");
        }
    }

}
- 
2.19.1.windows.1

