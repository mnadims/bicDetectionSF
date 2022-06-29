From d4ce93343853f73955509cf7a6314aed3d99be6a Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Thu, 17 Sep 2009 19:58:07 +0000
Subject: [PATCH] JCR-2170: Remove PropDefId and NodeDefId - nt:resource
 extends mix:referenceable

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@816360 13f79535-47bb-0310-9956-ffa450edef68
--
 jackrabbit-core/pom.xml                                       | 3 ++-
 .../org/apache/jackrabbit/core/nodetype/builtin_nodetypes.cnd | 4 ++--
 .../java/org/apache/jackrabbit/jcr2spi/HierarchyNodeTest.java | 3 ++-
 jackrabbit-spi2jcr/pom.xml                                    | 1 +
 4 files changed, 7 insertions(+), 4 deletions(-)

diff --git a/jackrabbit-core/pom.xml b/jackrabbit-core/pom.xml
index c22a2df88..356e424bd 100644
-- a/jackrabbit-core/pom.xml
++ b/jackrabbit-core/pom.xml
@@ -95,7 +95,8 @@
               <name>known.issues</name>
               <value>
                 org.apache.jackrabbit.core.xml.DocumentViewTest#testMultiValue
                org.apache.jackrabbit.core.ConcurrentImportTest  
                org.apache.jackrabbit.core.ConcurrentImportTest
                org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest#testResource
               </value>
             </property>
           </systemProperties>
diff --git a/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/nodetype/builtin_nodetypes.cnd b/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/nodetype/builtin_nodetypes.cnd
index e4e98ea43..908ec3ff3 100644
-- a/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/nodetype/builtin_nodetypes.cnd
++ b/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/nodetype/builtin_nodetypes.cnd
@@ -92,7 +92,7 @@
  *
  * @since 1.0
  */
[nt:resource] > mix:mimeType, mix:lastModified
[nt:resource] > mix:mimeType, mix:lastModified, mix:referenceable
   primaryitem jcr:data
   - jcr:data (BINARY) mandatory
   
@@ -617,4 +617,4 @@
 [rep:RetentionManageable]
   mixin
   - rep:hold (UNDEFINED) protected  multiple IGNORE
  - rep:retentionPolicy (UNDEFINED) protected IGNORE
\ No newline at end of file
  - rep:retentionPolicy (UNDEFINED) protected IGNORE
diff --git a/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/HierarchyNodeTest.java b/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/HierarchyNodeTest.java
index eac04e279..5ad7375aa 100644
-- a/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/HierarchyNodeTest.java
++ b/jackrabbit-jcr2spi/src/test/java/org/apache/jackrabbit/jcr2spi/HierarchyNodeTest.java
@@ -62,6 +62,7 @@ public class HierarchyNodeTest extends AbstractJCRTest {
         resourceProps.add(jcrPrefix+":lastModifiedBy");
         resourceProps.add(jcrPrefix+":mimeType");
         resourceProps.add(jcrPrefix+":data");
        resourceProps.add(jcrPrefix+":uuid");
 
         try {
             Node folder = testRootNode.addNode("folder", ntFolder);
@@ -116,4 +117,4 @@ public class HierarchyNodeTest extends AbstractJCRTest {
             dump(nodes.nextNode());
         }
     }
}
\ No newline at end of file
}
diff --git a/jackrabbit-spi2jcr/pom.xml b/jackrabbit-spi2jcr/pom.xml
index 11f8b17ca..9a3213233 100644
-- a/jackrabbit-spi2jcr/pom.xml
++ b/jackrabbit-spi2jcr/pom.xml
@@ -75,6 +75,7 @@
                 org.apache.jackrabbit.test.api.version.ActivitiesTest#testActivitiesRelation
                 org.apache.jackrabbit.test.api.version.ConfigurationsTest#testCreateConfigWithBaseline
                 org.apache.jackrabbit.test.api.LifecycleTest
                org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest#testResource
               </value>
             </property>
           </systemProperties>
- 
2.19.1.windows.1

