From 3d9f464ecd343326e34181d8b72af330bcb17b1b Mon Sep 17 00:00:00 2001
From: Alexandru Parvulescu <alexparvulescu@apache.org>
Date: Fri, 24 Jun 2011 09:46:30 +0000
Subject: [PATCH] JCR-3001 DescendantSelfAxisQuery may fail with IOException
 when session has limited access

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1139224 13f79535-47bb-0310-9956-ffa450edef68
--
 .../query/lucene/DescendantSelfAxisQuery.java | 37 +++++---
 .../apache/jackrabbit/core/NodeImplTest.java  |  9 +-
 .../core/query/LimitedAccessQueryTest.java    | 92 +++++++++++++++++++
 .../apache/jackrabbit/core/query/TestAll.java |  1 +
 4 files changed, 123 insertions(+), 16 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/LimitedAccessQueryTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
index 894444da9..bae88d752 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
@@ -17,8 +17,10 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
 import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
@@ -29,6 +31,7 @@ import org.apache.lucene.search.Weight;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import javax.jcr.ItemNotFoundException;
 import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import java.io.IOException;
@@ -194,7 +197,7 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
     /**
      * {@inheritDoc}
      */
    public void extractTerms(Set terms) {
    public void extractTerms(Set<Term> terms) {
         contextQuery.extractTerms(terms);
         subQuery.extractTerms(terms);
     }
@@ -240,13 +243,17 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
                 }
 
                 ScoreNode sn;
                try {
                    while ((sn = result.nextScoreNode()) != null) {
                        Node node = session.getNodeById(sn.getNodeId());
                while ((sn = result.nextScoreNode()) != null) {
                    NodeId id = sn.getNodeId();
                    try {
                        Node node = session.getNodeById(id);
                         startingPoints.put(node.getPath(), sn);
                    } catch (ItemNotFoundException e) {
                        // JCR-3001 access denied to score node, will just skip it
                        log.warn("Access denied to node id {}.", id);
                    } catch (RepositoryException e) {
                        throw Util.createIOException(e);
                     }
                } catch (RepositoryException e) {
                    throw Util.createIOException(e);
                 }
             } finally {
                 result.close();
@@ -297,17 +304,23 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
                     if (currentTraversal != null) {
                         currentTraversal.close();
                     }
                    if (scoreNodes.hasNext()) {
                    currentTraversal = null;
                    // We only need one node, but because of the acls, we'll
                    // iterate until we find a good one
                    while (scoreNodes.hasNext()) {
                         ScoreNode sn = scoreNodes.next();
                        NodeId id = sn.getNodeId();
                         try {
                            Node node = session.getNodeById(sn.getNodeId());
                            currentTraversal = new NodeTraversingQueryHits(node,
                                    getMinLevels() == 0);
                            Node node = session.getNodeById(id);
                            currentTraversal = new NodeTraversingQueryHits(
                                    node, getMinLevels() == 0);
                            break;
                        } catch (ItemNotFoundException e) {
                            // JCR-3001 node access denied, will just skip it
                            log.warn("Access denied to node id {}.", id);
                         } catch (RepositoryException e) {
                             throw Util.createIOException(e);
                         }
                    } else {
                        currentTraversal = null;
                     }
                 }
             };
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
index a44997c7f..f587e5622 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
@@ -37,6 +37,7 @@ import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
 import org.apache.jackrabbit.commons.JcrUtils;
 import org.apache.jackrabbit.test.AbstractJCRTest;
 import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryHelper;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -52,7 +53,7 @@ public class NodeImplTest extends AbstractJCRTest {
         }
     }
 
    private static void changeReadPermission(Principal principal, Node n, boolean allowRead) throws RepositoryException, NotExecutableException {
    public static void changeReadPermission(Principal principal, Node n, boolean allowRead) throws RepositoryException, NotExecutableException {
         SessionImpl s = (SessionImpl) n.getSession();
         JackrabbitAccessControlList acl = null;
         AccessControlManager acMgr = s.getAccessControlManager();
@@ -84,8 +85,8 @@ public class NodeImplTest extends AbstractJCRTest {
         }
     }
 
    private Principal getReadOnlyPrincipal() throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) getHelper().getReadOnlySession();
    public static Principal getReadOnlyPrincipal(RepositoryHelper helper) throws RepositoryException, NotExecutableException {
        SessionImpl s = (SessionImpl) helper.getReadOnlySession();
         try {
             for (Principal p : s.getSubject().getPrincipals()) {
                 if (!(p instanceof Group)) {
@@ -110,7 +111,7 @@ public class NodeImplTest extends AbstractJCRTest {
         NodeImpl testNode = (NodeImpl) n.addNode(nodeName2);
         testRootNode.save();
 
        Principal principal = getReadOnlyPrincipal();
        Principal principal = getReadOnlyPrincipal(getHelper());
         changeReadPermission(principal, n, false);
         changeReadPermission(principal, testNode, true);
 
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/LimitedAccessQueryTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/LimitedAccessQueryTest.java
new file mode 100644
index 000000000..308c12c54
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/LimitedAccessQueryTest.java
@@ -0,0 +1,92 @@
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
package org.apache.jackrabbit.core.query;

import java.security.Principal;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.jackrabbit.core.NodeImplTest;

/**
 * <code>LimitedAccessQueryTest</code> tests queries that include nodes that are
 * outside their access.
 */
public class LimitedAccessQueryTest extends AbstractQueryTest {

    private Session readOnly;
    private Principal principal;

    private Node a;
    private Node b;

    protected void setUp() throws Exception {
        super.setUp();

        a = testRootNode.addNode("a", "nt:unstructured");
        a.setProperty("p", 1);
        b = testRootNode.addNode("b", "nt:unstructured");
        b.setProperty("p", 1);
        superuser.save();

        principal = NodeImplTest.getReadOnlyPrincipal(getHelper());
        NodeImplTest.changeReadPermission(principal, a, false);
        superuser.save();

        readOnly = getHelper().getReadOnlySession();

        // preliminary tests
        try {
            readOnly.getNodeByIdentifier(a.getIdentifier());
            fail("Access to the node '" + a.getPath() + "' has to be denied.");
        } catch (ItemNotFoundException e) {
            // good acl
        }

        try {
            readOnly.getNodeByIdentifier(b.getIdentifier());
        } catch (ItemNotFoundException e) {
            fail(e.getMessage());
        }

    }

    protected void tearDown() throws Exception {
        readOnly.logout();
        NodeImplTest.changeReadPermission(principal, a, true);
        super.tearDown();
    }

    /**
     * this test is for the DescendantSelfAxisQuery class.
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-3001">JCR-3001</a>
     * 
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public void testDescendantSelfAxisQuery() throws Exception {
        String xpath = "/" + testRootNode.getPath() + "//*";
        checkResult(
                readOnly.getWorkspace().getQueryManager()
                        .createQuery(xpath, Query.XPATH).execute(),
                new Node[] { b });
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java
index 708e9c5c6..50d0e9c29 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java
@@ -69,6 +69,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(SQL2OuterJoinTest.class);
         suite.addTestSuite(SQL2PathEscapingTest.class);
         suite.addTestSuite(SQL2QueryResultTest.class);
        suite.addTestSuite(LimitedAccessQueryTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

