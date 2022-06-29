From 67f8d5fc4b115f9cfeffc7ea8b8b623e75fb24a3 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Mon, 22 Mar 2010 20:42:19 +0000
Subject: [PATCH] JCR-2577: SISM.checkAddedChildNodes() prevents merging of
 concurrent changes

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@926324 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/SharedItemStateManager.java    |  4 +-
 .../core/ConcurrentAddRemoveNodeTest.java     | 50 +++++++++++++++++++
 .../org/apache/jackrabbit/core/TestAll.java   |  1 +
 3 files changed, 53 insertions(+), 2 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentAddRemoveNodeTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index c6a1bb9fc..861605480 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -574,8 +574,6 @@ public class SharedItemStateManager
                     checkReferentialIntegrity();
                 }
 
                checkAddedChildNodes();

                 /**
                  * prepare the events. this needs to be after the referential
                  * integrity check, since another transaction could have modified
@@ -698,6 +696,8 @@ public class SharedItemStateManager
                     }
                 }
 
                checkAddedChildNodes();

                 /* create event states */
                 events.createEventStates(rootNodeId, local, SharedItemStateManager.this);
 
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentAddRemoveNodeTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentAddRemoveNodeTest.java
new file mode 100644
index 000000000..784dc3874
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ConcurrentAddRemoveNodeTest.java
@@ -0,0 +1,50 @@
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
package org.apache.jackrabbit.core;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>ConcurrentAddRemoveNodeTest</code> checks if concurrent modifications
 * on a node are properly handled. Adding and removing distinct child nodes
 * by separate sessions must succeed.
 */
public class ConcurrentAddRemoveNodeTest extends AbstractConcurrencyTest {

    private final AtomicInteger count = new AtomicInteger();

    public void testAddRemove() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                String name = "node-" + count.getAndIncrement();
                for (int i = 0; i < 10; i++) {
                    if (test.hasNode(name)) {
                        test.getNode(name).remove();
                    } else {
                        test.addNode(name);
                    }
                    session.save();
                }
            }
        }, 10, testRootNode.getPath());
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index 39c04b162..52ef2a959 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -56,6 +56,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(ConcurrentModificationWithSNSTest.class);
         suite.addTestSuite(ConcurrentMoveTest.class);
         suite.addTestSuite(ConcurrentReorderTest.class);
        suite.addTestSuite(ConcurrentAddRemoveNodeTest.class);
 
         suite.addTestSuite(UserPerWorkspaceSecurityManagerTest.class);
 
- 
2.19.1.windows.1

