From e4724ee78c6e0b5c50108f0c0b88c6091cc16a58 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 22 Sep 2009 15:27:30 +0000
Subject: [PATCH] JCR-2170: Remove PropDefId and NodeDefId - remove know issues
 now that JCRTCK-5 is fixed

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@817700 13f79535-47bb-0310-9956-ffa450edef68
--
 jackrabbit-core/pom.xml    | 1 -
 jackrabbit-spi2jcr/pom.xml | 1 -
 2 files changed, 2 deletions(-)

diff --git a/jackrabbit-core/pom.xml b/jackrabbit-core/pom.xml
index 356e424bd..8b3e7e3e8 100644
-- a/jackrabbit-core/pom.xml
++ b/jackrabbit-core/pom.xml
@@ -96,7 +96,6 @@
               <value>
                 org.apache.jackrabbit.core.xml.DocumentViewTest#testMultiValue
                 org.apache.jackrabbit.core.ConcurrentImportTest
                org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest#testResource
               </value>
             </property>
           </systemProperties>
diff --git a/jackrabbit-spi2jcr/pom.xml b/jackrabbit-spi2jcr/pom.xml
index 9a3213233..11f8b17ca 100644
-- a/jackrabbit-spi2jcr/pom.xml
++ b/jackrabbit-spi2jcr/pom.xml
@@ -75,7 +75,6 @@
                 org.apache.jackrabbit.test.api.version.ActivitiesTest#testActivitiesRelation
                 org.apache.jackrabbit.test.api.version.ConfigurationsTest#testCreateConfigWithBaseline
                 org.apache.jackrabbit.test.api.LifecycleTest
                org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest#testResource
               </value>
             </property>
           </systemProperties>
- 
2.19.1.windows.1

