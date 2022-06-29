From 52d2d3fd6f8be7f4285f7c4a8a6898bcde63b876 Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Tue, 19 Mar 2013 05:49:25 +0000
Subject: [PATCH] SOLR-4604: Move CHANGES entry. SOLR-4605: Move CHANGES entry.
 SOLR-4609: Move CHANGES entry.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1458155 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt | 14 +++++++-------
 1 file changed, 7 insertions(+), 7 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 63e80617c79..0019df1aa9d 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -95,13 +95,6 @@ Bug Fixes
 
 * SOLR-4311: Admin UI - Optimize Caching Behaviour (steffkes)
 
* SOLR-4604: UpdateLog#init is over called on SolrCore#reload. (Mark Miller)

* SOLR-4605: Rollback does not work correctly. (Mark S, Mark Miller)

* SOLR-4609: The Collections API should only send the reload command to ACTIVE 
  cores. (Mark Miller)

 Other Changes
 ----------------------
 
@@ -197,6 +190,13 @@ Bug Fixes
 
 * SOLR-4601: A Collection that is only partially created and then deleted will 
   leave pre allocated shard information in ZooKeeper. (Mark Miller)

* SOLR-4604: UpdateLog#init is over called on SolrCore#reload. (Mark Miller)

* SOLR-4605: Rollback does not work correctly. (Mark S, Mark Miller)

* SOLR-4609: The Collections API should only send the reload command to ACTIVE 
  cores. (Mark Miller)
         
 Optimizations
 ----------------------
- 
2.19.1.windows.1

