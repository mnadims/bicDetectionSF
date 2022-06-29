From a8e4aaaa46718db37ac0ae862b527a918b11835c Mon Sep 17 00:00:00 2001
From: Thomas Mueller <thomasm@apache.org>
Date: Thu, 14 Jan 2010 08:18:25 +0000
Subject: [PATCH] JCR-2456 Repository is corrupt after concurrent changes with
 the same session - simple benchmark

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@899102 13f79535-47bb-0310-9956-ffa450edef68
--
 .../integration/benchmark/SimpleBench.java    | 87 +++++++++++++++++++
 1 file changed, 87 insertions(+)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/benchmark/SimpleBench.java

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/benchmark/SimpleBench.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/benchmark/SimpleBench.java
new file mode 100644
index 000000000..06fd6180a
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/integration/benchmark/SimpleBench.java
@@ -0,0 +1,87 @@
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
package org.apache.jackrabbit.core.integration.benchmark;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;

/**
 * A simple benchmark application for Jackrabbit.
 */
public class SimpleBench {

    int run;
    long start;
    Repository repository;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 5; i++) {
            new SimpleBench().test(i);
        }
    }

    void start() {
        start = System.currentTimeMillis();
    }

    void end(String message) {
        long time = System.currentTimeMillis() - start;
        if (run > 0) {
            System.out.println("run: " + run + "; time: " + time + " ms; task: " + message);
        }
    }

    void test(int run) throws Exception {
        this.run = run;
        new File("target/jcr.log").delete();
        FileUtils.deleteQuietly(new File("repository"));

        start();
        repository = new TransientRepository();
        Session session = repository.login(new SimpleCredentials("", "".toCharArray()));
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        session.getRootNode().addNode("test");
        session.save();
        end("init");
        Node node = session.getRootNode().getNode("test");
        Node n = null;
        int len = run == 0 ? 100 : 1000;
        start();
        for (int i = 0; i < len; i++) {
            if (i % 100 == 0) {
                n = node.addNode("sub" + i);
            }
            Node x = n.addNode("x" + (i % 100));
            x.setProperty("name", "John");
            x.setProperty("firstName", "Doe");
            session.save();
        }
        end("addNodes");
        session.logout();
    }

}
- 
2.19.1.windows.1

