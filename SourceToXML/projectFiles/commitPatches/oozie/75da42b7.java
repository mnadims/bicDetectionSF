From 75da42b7377a34e21ca458857ecc7c60f7dc21e0 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Thu, 30 Oct 2014 13:44:05 -0700
Subject: [PATCH] OOZIE-1959 TestZKUtilsWithSecurity fails (rkanter)

--
 core/pom.xml                                  |  12 ---
 .../org/apache/oozie/test/ZKXTestCase.java    |   2 +-
 pom.xml                                       |   6 +-
 release-log.txt                               |   1 +
 zookeeper-security-tests/pom.xml              | 101 ++++++++++++++++++
 .../oozie/test/ZKXTestCaseWithSecurity.java   |   5 +-
 .../oozie/util/TestZKUtilsWithSecurity.java   |   0
 7 files changed, 107 insertions(+), 20 deletions(-)
 create mode 100644 zookeeper-security-tests/pom.xml
 rename {core => zookeeper-security-tests}/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java (97%)
 rename {core => zookeeper-security-tests}/src/test/java/org/apache/oozie/util/TestZKUtilsWithSecurity.java (100%)

diff --git a/core/pom.xml b/core/pom.xml
index 597775cb6..ca40e2e22 100644
-- a/core/pom.xml
++ b/core/pom.xml
@@ -42,12 +42,6 @@
             <scope>test</scope>
         </dependency>
 
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-minikdc</artifactId>
            <scope>test</scope>
        </dependency>

         <dependency>
             <groupId>org.apache.oozie</groupId>
             <artifactId>oozie-hadoop</artifactId>
@@ -479,12 +473,6 @@
                     </dependency>
                 </dependencies>
             </plugin>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <inherited>true</inherited>
                <extensions>true</extensions>
            </plugin>
         </plugins>
     </build>
 
diff --git a/core/src/test/java/org/apache/oozie/test/ZKXTestCase.java b/core/src/test/java/org/apache/oozie/test/ZKXTestCase.java
index 84d6e5d8f..dfbea8880 100644
-- a/core/src/test/java/org/apache/oozie/test/ZKXTestCase.java
++ b/core/src/test/java/org/apache/oozie/test/ZKXTestCase.java
@@ -144,7 +144,7 @@ public abstract class ZKXTestCase extends XDataTestCase {
     }
 
     private void createClient() throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        RetryPolicy retryPolicy = ZKUtils.getRetryPolicy();
         String zkConnectionString = Services.get().getConf().get("oozie.zookeeper.connection.string", zkServer.getConnectString());
         String zkNamespace = Services.get().getConf().get("oozie.zookeeper.namespace", "oozie");
         client = CuratorFrameworkFactory.builder()
diff --git a/pom.xml b/pom.xml
index d5059166c..1e7918629 100644
-- a/pom.xml
++ b/pom.xml
@@ -109,6 +109,7 @@
         <module>tools</module>
         <module>minitest</module>
         <module>distro</module>
        <module>zookeeper-security-tests</module>
     </modules>
 
     <distributionManagement>
@@ -1051,11 +1052,6 @@
                              to test the SSH action
                         -->
                         <exclude>**/TestSsh*.java</exclude>

                        <!-- Explictly use -Dtest=TestZKUtilsWithSecurity to test the ZKUtils with security.
                             It can conflict with other non-secure tests that use zookeeper
                        -->
                        <exclude>**/TestZKUtilsWithSecurity.java</exclude>
                     </excludes>
                     <!-- DO NOT CHANGE THIS VALUES, TESTCASES CANNOT RUN IN PARALLEL -->
                     <parallel>classes</parallel>
diff --git a/release-log.txt b/release-log.txt
index 991a4a63f..e8b9aff5b 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -40,6 +40,7 @@ OOZIE-1943 Bump up trunk to 4.2.0-SNAPSHOT (bzhang)
 
 -- Oozie 4.1.0 release (4.1 - unreleased)
 
OOZIE-1959 TestZKUtilsWithSecurity fails (rkanter)
 OOZIE-2033 HA and misc improvements to SSL docs (rkanter)
 OOZIE-1789 Support backward compatibility of oozie share lib (shwethags)
 OOZIE-2034 Disable SSLv3 (POODLEbleed vulnerability) (rkanter)
diff --git a/zookeeper-security-tests/pom.xml b/zookeeper-security-tests/pom.xml
new file mode 100644
index 000000000..b24ce338b
-- /dev/null
++ b/zookeeper-security-tests/pom.xml
@@ -0,0 +1,101 @@
<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.apache.oozie</groupId>
        <artifactId>oozie-main</artifactId>
        <version>4.2.0-SNAPSHOT</version>
    </parent>
    <groupId>org.apache.oozie</groupId>
    <artifactId>oozie-zookeeper-security-tests</artifactId>
    <version>4.2.0-SNAPSHOT</version>
    <description>Apache Oozie ZooKeeper Security Tests</description>
    <name>Apache Oozie ZooKeeper Security Tests</name>
    <packaging>jar</packaging>

    <!-- The unit tests in here change JVM level security configs in such a way that tests non expecting them would
         fail.  In order to not interfere with other tests, these unit tests need to be run in their own JVM, which
         happens when they're in a separate module -->

    <dependencies>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-minikdc</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-core</artifactId>
            <classifier>tests</classifier>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.oozie</groupId>
            <artifactId>oozie-hadoop</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
           <groupId>org.apache.oozie</groupId>
           <artifactId>oozie-hadoop-test</artifactId>
           <scope>test</scope>
       </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-deploy-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.rat</groupId>
                <artifactId>apache-rat-plugin</artifactId>
                <configuration>
                    <excludeSubProjects>false</excludeSubProjects>
                    <excludes>
                        <!-- excluding all as the root POM does the full check-->
                        <exclude>**</exclude>
                    </excludes>
                </configuration>
            </plugin>
            <!-- Required for MiniKDC -->
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <inherited>true</inherited>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
diff --git a/core/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java b/zookeeper-security-tests/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java
similarity index 97%
rename from core/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java
rename to zookeeper-security-tests/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java
index f8482aa85..f9f3e8804 100644
-- a/core/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java
++ b/zookeeper-security-tests/src/test/java/org/apache/oozie/test/ZKXTestCaseWithSecurity.java
@@ -89,7 +89,8 @@ public abstract class ZKXTestCaseWithSecurity extends ZKXTestCase {
      * automatically connect using the same authentication; trying to stop this is futile and either results in an error or has no
      * effect.  This means that there's no way to do any tests with an unauthenticated client.  Also, if any tests using secure
      * ZooKeeper get run before tests not using secure ZooKeeper, they will likely fail because it will try to use authentication:
     * so they should be run separately.
     * so they should be run separately.  For this reason, the secure tests should be run in a separate module where they will get
     * their own JVM.
      *
      * @return the embedded ZooKeeper server
      * @throws Exception
@@ -105,7 +106,7 @@ public abstract class ZKXTestCaseWithSecurity extends ZKXTestCase {
         kdc = new MiniKdc(MiniKdc.createConf(), new File(getTestCaseDir()));
         kdc.start();
         keytabFile = new File(getTestCaseDir(), "test.keytab");
        String serverPrincipal = "zookeeper/" + kdc.getHost();
        String serverPrincipal = "zookeeper/127.0.0.1";
         kdc.createPrincipal(keytabFile, getPrincipal(), serverPrincipal);
 
         setSystemProperty("zookeeper.authProvider.1", "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
diff --git a/core/src/test/java/org/apache/oozie/util/TestZKUtilsWithSecurity.java b/zookeeper-security-tests/src/test/java/org/apache/oozie/util/TestZKUtilsWithSecurity.java
similarity index 100%
rename from core/src/test/java/org/apache/oozie/util/TestZKUtilsWithSecurity.java
rename to zookeeper-security-tests/src/test/java/org/apache/oozie/util/TestZKUtilsWithSecurity.java
- 
2.19.1.windows.1

