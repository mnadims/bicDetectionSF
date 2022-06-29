From fd14a1dbd1286840a8b456e3bf7e1c76572f9dc4 Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Fri, 14 Jun 2013 20:50:25 +0000
Subject: [PATCH] OOZIE-1365 The hive action popup in the web UI is broken when
 externalChildIDs is empty string (michalisk via rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1493239 13f79535-47bb-0310-9956-ffa450edef68
--
 release-log.txt                         | 1 +
 webapp/src/main/webapp/oozie-console.js | 2 +-
 2 files changed, 2 insertions(+), 1 deletion(-)

diff --git a/release-log.txt b/release-log.txt
index 7b35930d9..dbb41e82f 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1365 The hive action popup in the web UI is broken when externalChildIDs is empty string (michalisk via rkanter)
 OOZIE-1412 Webapp contains all sharedlib dependencies after launcher refactor (rohini)
 OOZIE-1414 Configuring Oozie for HTTPS still allows HTTP connections to all resources (rkanter)
 OOZIE-1410 V2 servlets are missing from ssl-web.xml (rkanter, rohini via rkanter)
diff --git a/webapp/src/main/webapp/oozie-console.js b/webapp/src/main/webapp/oozie-console.js
index 14658b9c1..65f7404fd 100644
-- a/webapp/src/main/webapp/oozie-console.js
++ b/webapp/src/main/webapp/oozie-console.js
@@ -565,7 +565,7 @@ function jobDetailsPopup(response, request) {
 	function populateUrlUnit(actionStatus, urlUnit) {
 		var consoleUrl = actionStatus["consoleUrl"];
         var externalChildIDs = actionStatus["externalChildIDs"];
		if(undefined !== consoleUrl && null !== consoleUrl && undefined !== externalChildIDs && null !== externalChildIDs) {
		if(!consoleUrl && !externalChildIDs) {
 	        var urlPrefix = consoleUrl.trim().split(/_/)[0];
             //externalChildIds is a comma-separated string of each child job ID.
             //Create URL list by appending jobID portion after stripping "job"
- 
2.19.1.windows.1

