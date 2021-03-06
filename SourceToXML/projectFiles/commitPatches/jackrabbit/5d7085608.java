From 5d70856087220c44d9bd9d8dfc0a2a8be3303599 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 12 Feb 2008 14:58:47 +0000
Subject: [PATCH] JCR-1365: Query path constraints like foo//*/bar do not scale
 - add test case and fix minor bug

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@620820 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/LuceneQueryBuilder.java |   2 +-
 .../jackrabbit/core/query/AxisQueryTest.java  | 189 ++++++++++++++++++
 .../apache/jackrabbit/core/query/TestAll.java |   1 +
 3 files changed, 191 insertions(+), 1 deletion(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AxisQueryTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
index a9913db59..20c765306 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
@@ -539,7 +539,7 @@ public class LuceneQueryBuilder implements QueryNodeVisitor {
 
         if (node.getIncludeDescendants()) {
             if (nameTest != null) {
                andQuery.add(new DescendantSelfAxisQuery(context, nameTest), Occur.MUST);
                andQuery.add(new DescendantSelfAxisQuery(context, nameTest, false), Occur.MUST);
             } else {
                 // descendant-or-self with nametest=*
                 if (predicates.length > 0) {
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AxisQueryTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AxisQueryTest.java
new file mode 100644
index 000000000..2b0615a4f
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AxisQueryTest.java
@@ -0,0 +1,189 @@
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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * <code>AxisQueryTest</code> runs random queries on generated test data. The
 * amount of test data can be controled by {@link #NUM_LEVELS} and
 * {@link #NODES_PER_LEVEL}. The default values create 363 nodes distributed
 * over 5 hierarchy levels.
 */
public class AxisQueryTest extends AbstractQueryTest {

    /**
     * Number of levels of test data to create.
     */
    private static final int NUM_LEVELS = 5;

    /**
     * Number of nodes per level
     */
    private static final int NODES_PER_LEVEL = 3;

    /**
     * Execute random queries for this amount of time.
     */
    private static final int RUN_NUM_SECONDS = 10;

    /**
     * Controls if query results are checked for their correctness. When the
     * number of test nodes increases this becomes expensive and should be
     * disabled.
     */
    private static final boolean CHECK_RESULT = true;

    /**
     * Set to a name that is different from {@link #testPath} when number of
     * test nodes is increased. This avoids that the test nodes are removed
     * after a test run. The number of test nodes can be controlled by
     * {@link #NUM_LEVELS} and {@link #NODES_PER_LEVEL}.
     */
    private String relTestLocation;

    /**
     * Absolute path to the test location.
     */
    private String absTestLocation;

    private Random rand = new Random();

    protected void setUp() throws Exception {
        super.setUp();
        if (relTestLocation == null) {
            // use default location
            relTestLocation = testPath;
            absTestLocation = "/" + relTestLocation;
            createNodes(testRootNode, NODES_PER_LEVEL, NUM_LEVELS, 0);
        } else {
            // customized location
            absTestLocation = "/" + relTestLocation;
            // only create test data if not yet present
            if (!superuser.itemExists(absTestLocation)) {
                Node testLocation = superuser.getRootNode().addNode(relTestLocation);
                createNodes(testLocation, NODES_PER_LEVEL, NUM_LEVELS, 0);
            }
            // uncomment to write log messages to system out
            //log.setWriter(new PrintWriter(System.out, true));
        }
    }

    public void testExecuteQueries() throws RepositoryException {
        Node testLocation = superuser.getRootNode().getNode(relTestLocation);
        long end = System.currentTimeMillis() + 1000 * RUN_NUM_SECONDS;
        while (end > System.currentTimeMillis()) {
            StringBuffer statement = new StringBuffer(relTestLocation);
            StringBuffer regexp = new StringBuffer(absTestLocation);
            int numSteps = rand.nextInt(NUM_LEVELS) + 1; // at least one step
            while (numSteps-- > 0) {
                String axis = getRandomAxis();
                String name = getRandomName();
                statement.append(axis).append(name);
                if (axis.equals("//")) {
                    regexp.append("/(.*/)?");
                } else {
                    regexp.append("/");
                }
                if (name.equals("*")) {
                    regexp.append("[^/]*");
                } else {
                    regexp.append(name);
                }
            }
            long time = System.currentTimeMillis();
            NodeIterator nodes = executeQuery(statement.toString()).getNodes();
            nodes.hasNext();
            time = System.currentTimeMillis() - time;
            log.print(statement + "\t" + nodes.getSize() + "\t" + time);
            if (CHECK_RESULT) {
                Set paths = new HashSet();
                time = System.currentTimeMillis();
                traversalEvaluate(testLocation,
                        Pattern.compile(regexp.toString()),
                        paths);
                time = System.currentTimeMillis() - time;
                log.println("\t" + time);
                assertEquals("wrong number of results", paths.size(), nodes.getSize());
                while (nodes.hasNext()) {
                    String path = nodes.nextNode().getPath();
                    assertTrue(path + " is not part of the result set", paths.contains(path));
                }
            } else {
                log.println();
            }
            log.flush();
        }
    }

    private String getRandomAxis() {
        if (rand.nextBoolean() && rand.nextBoolean()) {
            // 25% descendant axis
            return "//";
        } else {
            // 75% child axis
            return "/";
        }
    }

    private String getRandomName() {
        if (rand.nextBoolean() && rand.nextBoolean()) {
            // 25% any name
            return "*";
        } else {
            // 75% name
            return "node" + rand.nextInt(NODES_PER_LEVEL);
        }
    }

    private void traversalEvaluate(Node n, Pattern pattern, Set matchingPaths)
            throws RepositoryException {
        if (pattern.matcher(n.getPath()).matches()) {
            matchingPaths.add(n.getPath());
        }
        for (NodeIterator it = n.getNodes(); it.hasNext(); ) {
            traversalEvaluate(it.nextNode(), pattern, matchingPaths);
        }
    }

    private int createNodes(Node n, int nodesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            child.setProperty("count", count++);
            if (count % 1000 == 0) {
                superuser.save();
                log.println("Created " + (count / 1000) + "k nodes");
            }
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count);
            }
        }
        if (levels == 0) {
            // final save
            superuser.save();
        }
        return count;
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java
index e552009c9..b24384527 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/TestAll.java
@@ -56,6 +56,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(ArrayHitsTest.class);
         suite.addTestSuite(ExcerptTest.class);
         suite.addTestSuite(IndexingAggregateTest.class);
        suite.addTestSuite(AxisQueryTest.class);
 
         // exclude long running tests per default
         //suite.addTestSuite(MassiveRangeTest.class);
- 
2.19.1.windows.1

