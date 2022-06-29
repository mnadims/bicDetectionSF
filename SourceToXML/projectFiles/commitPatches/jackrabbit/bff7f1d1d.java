From bff7f1d1d0d42b76e23427de758ee505b509dc6b Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Wed, 1 Apr 2009 14:48:44 +0000
Subject: [PATCH] JCR-2035: IndexingQueue not checked on initial index creation

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@760906 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/MultiIndex.java         |  70 ++++++++--
 .../apache/jackrabbit/core/TestHelper.java    |  38 ++++++
 .../core/query/AbstractIndexingTest.java      |   4 +-
 .../core/query/lucene/IndexingQueueTest.java  | 121 +++++++++++++++++-
 .../workspaces/indexing-test/workspace.xml    |   1 +
 5 files changed, 220 insertions(+), 14 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestHelper.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java
index af83bca15..3ef0715fe 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiIndex.java
@@ -29,6 +29,8 @@ import org.apache.jackrabbit.util.Timer;
 import org.apache.jackrabbit.spi.Path;
 import org.apache.jackrabbit.spi.PathFactory;
 import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.apache.lucene.document.Document;
@@ -353,21 +355,26 @@ public class MultiIndex {
      *
      * @param stateMgr the item state manager.
      * @param rootId   the id of the node from where to start.
     * @param rootPath the path of the node from where to start.
      * @throws IOException           if an error occurs while indexing the
      *                               workspace.
      * @throws IllegalStateException if this index is not empty.
      */
    void createInitialIndex(ItemStateManager stateMgr, NodeId rootId, Path rootPath)
    void createInitialIndex(ItemStateManager stateMgr,
                            NodeId rootId,
                            Path rootPath)
             throws IOException {
         // only do an initial index if there are no indexes at all
         if (indexNames.size() == 0) {
             reindexing = true;
             try {
                long count = 0;
                 // traverse and index workspace
                 executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
                 NodeState rootState = (NodeState) stateMgr.getItemState(rootId);
                createIndex(rootState, rootPath, stateMgr);
                count = createIndex(rootState, rootPath, stateMgr, count);
                 executeAndLog(new Commit(getTransactionId()));
                log.info("Created initial index for {} nodes", new Long(count));
                 scheduleFlushTask();
             } catch (Exception e) {
                 String msg = "Error indexing workspace";
@@ -1042,19 +1049,33 @@ public class MultiIndex {
      * <code>node</code>.
      *
      * @param node     the current NodeState.
     * @param path     the path of the current node.
      * @param stateMgr the shared item state manager.
     * @param count    the number of nodes already indexed.
     * @return the number of nodes indexed so far.
      * @throws IOException         if an error occurs while writing to the
      *                             index.
      * @throws ItemStateException  if an node state cannot be found.
      * @throws RepositoryException if any other error occurs
      */
    private void createIndex(NodeState node, Path path, ItemStateManager stateMgr)
    private long createIndex(NodeState node,
                             Path path,
                             ItemStateManager stateMgr,
                             long count)
             throws IOException, ItemStateException, RepositoryException {
         NodeId id = node.getNodeId();
         if (excludedIDs.contains(id)) {
            return;
            return count;
         }
         executeAndLog(new AddNode(getTransactionId(), id.getUUID()));
        if (++count % 100 == 0) {
            PathResolver resolver = new DefaultNamePathResolver(
                    handler.getContext().getNamespaceRegistry());
            log.info("indexing... {} ({})", resolver.getJCRPath(path), new Long(count));
        }
        if (count % 10 == 0) {
            checkIndexingQueue(true);
        }
         checkVolatileCommit();
         List children = node.getChildNodeEntries();
         for (Iterator it = children.iterator(); it.hasNext();) {
@@ -1069,9 +1090,10 @@ public class MultiIndex {
                         e, handler, path, node, child);
             }
             if (childState != null) {
                createIndex(childState, childPath, stateMgr);
                count = createIndex(childState, childPath, stateMgr, count);
             }
         }
        return count;
     }
 
     /**
@@ -1140,10 +1162,27 @@ public class MultiIndex {
     }
 
     /**
     * Checks the indexing queue for finished text extrator jobs and
     * updates the index accordingly if there are any new ones.
     * Checks the indexing queue for finished text extrator jobs and updates the
     * index accordingly if there are any new ones. This method is synchronized
     * and should only be called by the timer task that periodically checks if
     * there are documents ready in the indexing queue. A new transaction is
     * used when documents are transfered from the indexing queue to the index.
      */
     private synchronized void checkIndexingQueue() {
        checkIndexingQueue(false);
    }

    /**
     * Checks the indexing queue for finished text extrator jobs and updates the
     * index accordingly if there are any new ones.
     *
     * @param transactionPresent whether a transaction is in progress and the
     *                           current {@link #getTransactionId()} should be
     *                           used. If <code>false</code> a new transaction
     *                           is created when documents are transfered from
     *                           the indexing queue to the index.
     */
    private void checkIndexingQueue(boolean transactionPresent) {
         Document[] docs = indexingQueue.getFinishedDocuments();
         Map finished = new HashMap();
         for (int i = 0; i < docs.length; i++) {
@@ -1153,17 +1192,26 @@ public class MultiIndex {
 
         // now update index with the remaining ones if there are any
         if (!finished.isEmpty()) {
            log.debug("updating index with {} nodes from indexing queue.",
            log.info("updating index with {} nodes from indexing queue.",
                     new Long(finished.size()));
 
             // remove documents from the queue
            Iterator it = finished.keySet().iterator();
            while (it.hasNext()) {
            for (Iterator it = finished.keySet().iterator(); it.hasNext(); ) {
                 indexingQueue.removeDocument(it.next().toString());
             }
 
             try {
                update(finished.keySet(), finished.values());
                if (transactionPresent) {
                    for (Iterator it = finished.keySet().iterator(); it.hasNext(); ) {
                        executeAndLog(new DeleteNode(getTransactionId(), (UUID) it.next()));
                    }
                    for (Iterator it = finished.values().iterator(); it.hasNext(); ) {
                        executeAndLog(new AddNode(
                                getTransactionId(), (Document) it.next()));
                    }
                } else {
                    update(finished.keySet(), finished.values());
                }
             } catch (IOException e) {
                 // update failed
                 log.warn("Failed to update index with deferred text extraction", e);
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestHelper.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestHelper.java
new file mode 100644
index 000000000..0b8d24cee
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestHelper.java
@@ -0,0 +1,38 @@
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

import javax.jcr.RepositoryException;

/**
 * <code>TestHelper</code> provides test utility methods.
 */
public class TestHelper {

    /**
     * Shuts down the workspace with the given <code>name</code>.
     *
     * @param name the name of the workspace to shut down.
     * @param repo the repository.
     * @throws RepositoryException if the shutdown fails or there is no
     *                             workspace with the given name.
     */
    public static void shutdownWorkspace(String name, RepositoryImpl repo)
            throws RepositoryException {
        repo.getWorkspaceInfo(name).dispose();
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AbstractIndexingTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AbstractIndexingTest.java
index d8142d6cc..707c6bb9e 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AbstractIndexingTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/AbstractIndexingTest.java
@@ -25,13 +25,15 @@ import javax.jcr.Node;
  */
 public class AbstractIndexingTest extends AbstractQueryTest {
 
    protected static final String WORKSPACE_NAME = "indexing-test";

     protected Session session;
 
     protected Node testRootNode;
 
     protected void setUp() throws Exception {
         super.setUp();
        session = helper.getSuperuserSession("indexing-test");
        session = helper.getSuperuserSession(WORKSPACE_NAME);
         testRootNode = cleanUpTestRoot(session);
         // overwrite query manager
         qm = session.getWorkspace().getQueryManager();
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexingQueueTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexingQueueTest.java
index be82dc85c..d4879a6a1 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexingQueueTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexingQueueTest.java
@@ -18,28 +18,38 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.jackrabbit.extractor.TextExtractor;
 import org.apache.jackrabbit.core.query.AbstractIndexingTest;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TestHelper;
import org.apache.jackrabbit.core.fs.local.FileUtil;
 
 import javax.jcr.Node;
 import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
 import javax.jcr.query.Query;
 import java.io.Reader;
 import java.io.InputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
 import java.util.Calendar;
 
 /**
  * <code>IndexingQueueTest</code> checks if the indexing queue properly indexes
 * nodes in a background thread when text extraction takes more than 100 ms.
 * nodes in a background thread when text extraction takes more than 10 ms. See
 * the workspace.xml file for the indexing-test workspace.
  */
 public class IndexingQueueTest extends AbstractIndexingTest {
 
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir")); 

     private static final String CONTENT_TYPE = "application/indexing-queue-test";
 
     private static final String ENCODING = "UTF-8";
 
     public void testQueue() throws Exception {
        Extractor.sleepTime = 200;
         SearchIndex index = (SearchIndex) getQueryHandler();
         IndexingQueue queue = index.getIndex().getIndexingQueue();
 
@@ -71,8 +81,115 @@ public class IndexingQueueTest extends AbstractIndexingTest {
         assertTrue(nodes.hasNext());
     }
 
    public void testInitialIndex() throws Exception {
        Extractor.sleepTime = 200;
        SearchIndex index = (SearchIndex) getQueryHandler();
        File indexDir = new File(index.getPath());

        // fill workspace
        Node testFolder = testRootNode.addNode("folder", "nt:folder");
        String text = "the quick brown fox jumps over the lazy dog.";
        int num = createFiles(testFolder, text.getBytes(ENCODING), 10, 2, 0);
        session.save();

        // shutdown workspace
        RepositoryImpl repo = (RepositoryImpl) session.getRepository();
        session.logout();
        session = null;
        superuser.logout();
        superuser = null;
        TestHelper.shutdownWorkspace(WORKSPACE_NAME, repo);

        // delete index
        try {
            FileUtil.delete(indexDir);
        } catch (IOException e) {
            fail("Unable to delete index directory");
        }

        int initialNumExtractorFiles = getNumExtractorFiles();

        Extractor.sleepTime = 20;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    session = helper.getSuperuserSession(WORKSPACE_NAME);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();

        while (t.isAlive()) {
            // there must not be more than 20 extractor files, because:
            // - initial index creation checks indexing queue every 10 nodes
            // - there is an aggregate definition on the workspace that causes
            //   2 extractor jobs per nt:resource
            // => 2 * 10 = 20
            int numFiles = getNumExtractorFiles() - initialNumExtractorFiles;
            assertTrue(numFiles <= 20);
            Thread.sleep(50);
        }

        qm = session.getWorkspace().getQueryManager();
        index = (SearchIndex) getQueryHandler();
        IndexingQueue queue = index.getIndex().getIndexingQueue();

        // flush index to make sure any documents in the buffer are written
        // to the index. this is to make sure all nodes are pushed either to
        // the index or to the indexing queue
        index.getIndex().flush();

        synchronized (index.getIndex()) {
            while (queue.getNumPendingDocuments() > 0) {
                index.getIndex().wait(50);
            }
        }

        String stmt = testPath + "//element(*, nt:resource)[jcr:contains(., 'fox')]";
        Query q = qm.createQuery(stmt, Query.XPATH);
        assertEquals(num, q.execute().getNodes().getSize());
    }

    private int createFiles(Node folder, byte[] data,
                            int filesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < filesPerLevel; i++) {
            // create files
            Node file = folder.addNode("file" + i, "nt:file");
            InputStream in = new ByteArrayInputStream(data);
            Node resource = file.addNode("jcr:content", "nt:resource");
            resource.setProperty("jcr:data", in);
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
            resource.setProperty("jcr:mimeType", CONTENT_TYPE);
            resource.setProperty("jcr:encoding", ENCODING);
            count++;
        }
        if (levels > 0) {
            for (int i = 0; i < filesPerLevel; i++) {
                // create files
                Node subFolder = folder.addNode("folder" + i, "nt:folder");
                count = createFiles(subFolder, data,
                        filesPerLevel, levels, count);
            }
        }
        return count;
    }

    private int getNumExtractorFiles() throws IOException {
        return TEMP_DIR.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("extractor");
            }
        }).length;
    }

     public static final class Extractor implements TextExtractor {
 
        protected static volatile int sleepTime = 200;

         public String[] getContentTypes() {
             return new String[]{CONTENT_TYPE};
         }
@@ -80,7 +197,7 @@ public class IndexingQueueTest extends AbstractIndexingTest {
         public Reader extractText(InputStream stream, String type, String encoding)
         throws IOException {
             try {
                Thread.sleep(200);
                Thread.sleep(sleepTime);
             } catch (InterruptedException e) {
                 throw new IOException();
             }
diff --git a/jackrabbit-core/src/test/repository/workspaces/indexing-test/workspace.xml b/jackrabbit-core/src/test/repository/workspaces/indexing-test/workspace.xml
index 93e578aa0..e0d77de01 100644
-- a/jackrabbit-core/src/test/repository/workspaces/indexing-test/workspace.xml
++ b/jackrabbit-core/src/test/repository/workspaces/indexing-test/workspace.xml
@@ -41,6 +41,7 @@
     <param name="excerptProviderClass" value="org.apache.jackrabbit.core.query.lucene.WeightedHTMLExcerpt"/>
     <param name="textFilterClasses" value="org.apache.jackrabbit.extractor.PlainTextExtractor,org.apache.jackrabbit.core.query.lucene.IndexingQueueTest$Extractor"/>
     <param name="extractorPoolSize" value="2"/>
    <param name="extractorTimeout" value="10"/>
   </SearchIndex>
 </Workspace>
 
- 
2.19.1.windows.1

