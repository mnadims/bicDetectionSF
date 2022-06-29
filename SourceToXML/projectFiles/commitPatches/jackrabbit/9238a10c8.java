From 9238a10c800d0f727c8b1121f929d31271adc61f Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Mon, 10 Sep 2012 07:37:55 +0000
Subject: [PATCH] JCR-3419: Overwriting Cache Entry Warnings

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1382673 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/NodeImpl.java  |  24 ++-
 .../ReplacePropertyWhileOthersReadTest.java   | 137 ++++++++++++++++++
 .../org/apache/jackrabbit/core/TestAll.java   |   1 +
 3 files changed, 158 insertions(+), 4 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplacePropertyWhileOthersReadTest.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
index 242270174..a4b15f6d6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
@@ -425,17 +425,33 @@ public class NodeImpl extends ItemImpl implements Node, JackrabbitNode {
             [...create block...]
 
         */
        PropertyId propId = new PropertyId(getNodeId(), name);
         try {
            PropertyId propId = new PropertyId(getNodeId(), name);
             return (PropertyImpl) itemMgr.getItem(propId);
         } catch (AccessDeniedException ade) {
             throw new ItemNotFoundException(name.toString());
         } catch (ItemNotFoundException e) {
            // does not exist yet:
            // find definition for the specified property and create property
            // does not exist yet or has been removed transiently:
            // find definition for the specified property and (re-)create property
             PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     name, type, multiValued, exactTypeMatch);
            PropertyImpl prop = createChildProperty(name, type, def);
            PropertyImpl prop;
            if (stateMgr.hasTransientItemStateInAttic(propId)) {
                // remove from attic
                try {
                    stateMgr.disposeTransientItemStateInAttic(stateMgr.getAttic().getItemState(propId));
                } catch (ItemStateException ise) {
                    // shouldn't happen because we checked if it is in the attic
                    throw new RepositoryException(ise);
                }
                prop = (PropertyImpl) itemMgr.getItem(propId);
                PropertyState state = (PropertyState) prop.getOrCreateTransientItemState();
                state.setMultiValued(multiValued);
                state.setType(type);
                getNodeState().addPropertyName(name);
            } else {
                prop = createChildProperty(name, type, def);
            }
             status.set(CREATED);
             return prop;
         }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplacePropertyWhileOthersReadTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplacePropertyWhileOthersReadTest.java
new file mode 100644
index 000000000..4741294e6
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/ReplacePropertyWhileOthersReadTest.java
@@ -0,0 +1,137 @@
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReplacePropertyWhileOthersReadTest</code>...
 */
public class ReplacePropertyWhileOthersReadTest extends AbstractJCRTest {

    private static final Logger log = LoggerFactory.getLogger(ReplacePropertyWhileOthersReadTest.class);

    private final List<Value> values = new ArrayList<Value>();

    private Node test;

    private final Random rand = new Random();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        test = testRootNode.addNode("test");
        test.addMixin(mixReferenceable);
        superuser.save();
        values.add(vf.createValue("value"));
        values.add(vf.createValue(new BigDecimal(1234)));
        values.add(vf.createValue(Calendar.getInstance()));
        values.add(vf.createValue(1.234));
        values.add(vf.createValue(true));
        values.add(vf.createValue(test));
        values.add(vf.createValue(vf.createBinary(
                new ByteArrayInputStream(new byte[0]))));
    }

    @Override
    protected void tearDown() throws Exception {
        test = null;
        values.clear();
        super.tearDown();
    }

    public void testAddRemove() throws Exception {
        final Property prop = test.setProperty("prop", getRandomValue());
        superuser.save();

        Thread reader = new Thread(new Runnable() {

            String path = prop.getPath();

            public void run() {
                // run for three seconds
                long stop = System.currentTimeMillis()
                        + TimeUnit.SECONDS.toMillis(3);
                while (System.currentTimeMillis() < stop) {
                    try {
                        Session s = getHelper().getSuperuserSession();
                        try {
                            s.getProperty(path);
                        } finally {
                            s.logout();
                        }
                    } catch (RepositoryException e) {
                        log.warn("", e);
                    }
                }
            }
        });
        Tail tail = Tail.start(new File("target", "jcr.log"),
                "overwriting cached entry");
        try {
            reader.start();
            while (reader.isAlive()) {
                test.getProperty("prop").remove();
                int type;
                boolean isMultivalued;
                if (rand.nextBoolean()) {
                    Value v = getRandomValue();
                    isMultivalued = false;
                    type = v.getType();
                    test.setProperty("prop", v);
                } else {
                    Value[] v = getRandomMultiValue();
                    type = v[0].getType();
                    isMultivalued = true;
                    test.setProperty("prop", v);
                }
                superuser.save();
                assertEquals(isMultivalued, test.getProperty("prop").isMultiple());
                assertEquals(type, test.getProperty("prop").getType());
            }
            assertFalse("detected 'overwriting cached entry' messages in log",
                    tail.getLines().iterator().hasNext());
        } finally {
            tail.close();
        }
    }

    private Value getRandomValue() {
        return values.get(rand.nextInt(values.size()));
    }

    private Value[] getRandomMultiValue() {
        return new Value[]{getRandomValue()};
    }
}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
index 5c52dee82..5e13a9025 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/TestAll.java
@@ -78,6 +78,7 @@ public class TestAll extends TestCase {
         suite.addTestSuite(NPEandCMETest.class);
         suite.addTestSuite(ConsistencyCheck.class);
         suite.addTestSuite(RemoveAddNodeWithUUIDTest.class);
        suite.addTestSuite(ReplacePropertyWhileOthersReadTest.class);
 
         return suite;
     }
- 
2.19.1.windows.1

