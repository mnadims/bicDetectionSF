From 029b34be9e2160ee4f0688bc756e7eb3c4569181 Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Wed, 25 Mar 2015 17:32:30 +0000
Subject: [PATCH] SOLR-6141: fix TestBulkSchemaAPI (expected exception message
 changed)

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1669173 13f79535-47bb-0310-9956-ffa450edef68
--
 .../test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java   | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java b/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java
index 4fe2e07dbc0..c0163bbbe3b 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/TestBulkSchemaAPI.java
@@ -390,7 +390,7 @@ public class TestBulkSchemaAPI extends RestTestBase {
     errors = map.get("errors");
     assertNotNull(errors);
     assertTrue(errors.toString().contains
        ("Can't delete 'NewField1' because it's referred to by at least one copy field directive"));
        ("Can't delete field 'NewField1' because it's referred to by at least one copy field directive"));
 
     cmds = "{'delete-field' : {'name':'NewField2'}}";
     response = harness.post("/schema?wt=json", json(cmds));
@@ -398,7 +398,7 @@ public class TestBulkSchemaAPI extends RestTestBase {
     errors = map.get("errors");
     assertNotNull(errors);
     assertTrue(errors.toString().contains
        ("Can't delete 'NewField2' because it's referred to by at least one copy field directive"));
        ("Can't delete field 'NewField2' because it's referred to by at least one copy field directive"));
 
     cmds = "{'replace-field' : {'name':'NewField1', 'type':'string'}}";
     response = harness.post("/schema?wt=json", json(cmds));
- 
2.19.1.windows.1

