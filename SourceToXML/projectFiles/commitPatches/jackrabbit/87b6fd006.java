From 87b6fd006c54a26eca526d3afa66c339e2a60829 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 4 Sep 2012 09:10:23 +0000
Subject: [PATCH] JCR-3419: Overwriting Cache Entry Warnings

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1380499 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/state/SharedItemStateManager.java    | 11 ++-
 .../core/RemoveAddNodeWithUUIDTest.java       | 71 ++++++++++++++
 .../java/org/apache/jackrabbit/core/Tail.java | 97 +++++++++++++++++++
 .../org/apache/jackrabbit/core/TestAll.java   |  4 +-
 4 files changed, 178 insertions(+), 5 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/RemoveAddNodeWithUUIDTest.java
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/Tail.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 5cd65c225..5e997c65e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -799,8 +799,15 @@ public class SharedItemStateManager
                 // before the ones here are put into the cache (via
                 // shared.persisted()). See JCR-3345
                 for (ItemState state : shared.addedStates()) {
                    state.setStatus(ItemState.STATUS_EXISTING);
                    cache.cache(state);
                    // there is one exception though. it is possible that the
                    // shared ChangeLog contains the an item both as removed and
                    // added. For those items we don't update the cache here,
                    // because that would lead to WARN messages in the
                    // ItemStateReferenceCache. See JCR-3419
                    if (!shared.deleted(state.getId())) {
                        state.setStatus(ItemState.STATUS_EXISTING);
                        cache.cache(state);
                    }
                 }
 
                 // downgrade to read lock
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/RemoveAddNodeWithUUIDTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/RemoveAddNodeWithUUIDTest.java
new file mode 100644
index 000000000..95e142a5e
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/RemoveAddNodeWithUUIDTest.java
@@ -0,0 +1,71 @@
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

import java.io.File;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>RemoveAddNodeWithUUIDTest</code> check if no 'overwriting cached entry'
 * warnings are written to the log when a node is re-created with the same UUID.
 * See: JCR-3419
 */
public class RemoveAddNodeWithUUIDTest extends AbstractJCRTest {

    public void testRemoveAdd() throws Exception {
        Tail tail = Tail.start(new File("target", "jcr.log"), "overwriting cached entry");
        try {
            Node test = testRootNode.addNode("test");
            test.setProperty("prop", 1);
            test.addMixin(mixReferenceable);
            superuser.save();
            String testId = test.getIdentifier();

            Session s = getHelper().getSuperuserSession();
            try {
                Node testOther = s.getNode(test.getPath());

                test.remove();
                test = ((NodeImpl) testRootNode).addNodeWithUuid("test", testId);
                test.setProperty("prop", 2);
                superuser.save();

                // now test node instance is not accessible anymore for s
                try {
                    testOther.getProperty("prop");
                    fail("test node instance must not be accessibly anymore");
                } catch (InvalidItemStateException e) {
                    // expected
                }
                // getting it again must succeed and return updated property value
                testOther = s.getNode(test.getPath());
                assertEquals("property outdated", 2, testOther.getProperty("prop").getLong());

                assertFalse("detected 'overwriting cached entry' messages in log", tail.getLines().iterator().hasNext());
            } finally {
                s.logout();
            }
        } finally {
            tail.close();
        }
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/Tail.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/Tail.java
new file mode 100644
index 000000000..2af715dd1
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/Tail.java
@@ -0,0 +1,97 @@
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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.iterator.FilterIterator;
import org.apache.jackrabbit.commons.predicate.Predicate;

/**
 * <code>Tail</code> is a test utility class to tail and grep a text file.
 */
public class Tail implements Closeable {

    private final String grep;

    private final BufferedReader reader;

    private Tail(File file, String grep) throws IOException {
        this.grep = grep;
        this.reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file)));
        while (reader.skip(Integer.MAX_VALUE) > 0) {
            // skip more, until end of file
        }
    }

    /**
     * Create a tail on the given <code>file</code> with an optional string to
     * match lines.
     *
     * @param file the file to tail.
     * @param grep the string to match or <code>null</code> if all lines should
     *             be returned.
     * @return a tail on the file.
     * @throws IOException if the files does not exist or some other I/O error
     *                     occurs.
     */
    public static Tail start(File file, String grep) throws IOException {
        return new Tail(file, grep);
    }

    /**
     * Returns the lines that were written to the file since
     * <code>Tail.start()</code> or the last call to <code>getLines()</code>.
     *
     * @return the matching lines.
     * @throws IOException if an error occurs while reading from the file.
     */
    public Iterable<String> getLines() throws IOException {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                Iterator<String> it = IOUtils.lineIterator(reader);
                if (grep == null || grep.length() == 0) {
                    return it;
                } else {
                    // filter
                    return new FilterIterator<String>(it, new Predicate() {
                        public boolean evaluate(Object o) {
                            return o.toString().contains(grep);
                        }
                    });
                }
            }
        };
    }

    /**
     * Releases the underlying stream from the file.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void close() throws IOException {
        reader.close();
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index ba4e051cf..5c52dee82 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -74,12 +74,10 @@ public class TestAll extends TestCase {
         }
 
         suite.addTestSuite(UserPerWorkspaceSecurityManagerTest.class);

         suite.addTestSuite(OverlappingNodeAddTest.class);

         suite.addTestSuite(NPEandCMETest.class);

         suite.addTestSuite(ConsistencyCheck.class);
        suite.addTestSuite(RemoveAddNodeWithUUIDTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

