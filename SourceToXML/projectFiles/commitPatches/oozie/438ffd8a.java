From 438ffd8aeb9c11c8c233de31b883b29f866caadf Mon Sep 17 00:00:00 2001
From: rkanter <rkanter@unknown>
Date: Sun, 16 Jun 2013 17:52:34 +0000
Subject: [PATCH] OOZIE-1420 OOZIE-1365 breaks the action popup in the Web UI
 (michalisk via rkanter)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1493550 13f79535-47bb-0310-9956-ffa450edef68
--
 release-log.txt                         | 1 +
 webapp/src/main/webapp/oozie-console.js | 2 +-
 2 files changed, 2 insertions(+), 1 deletion(-)

diff --git a/release-log.txt b/release-log.txt
index 366ab07c3..a099b294d 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1420 OOZIE-1365 breaks the action popup in the Web UI (michalisk via rkanter)
 OOZIE-1418 Fix bugs around ActionKillX not setting end time, V2SLAServlet and exception handling for event threads (mona)
 OOZIE-1365 The hive action popup in the web UI is broken when externalChildIDs is empty string (michalisk via rkanter)
 OOZIE-1412 Webapp contains all sharedlib dependencies after launcher refactor (rohini)
diff --git a/webapp/src/main/webapp/oozie-console.js b/webapp/src/main/webapp/oozie-console.js
index 65f7404fd..13cb6aadc 100644
-- a/webapp/src/main/webapp/oozie-console.js
++ b/webapp/src/main/webapp/oozie-console.js
@@ -565,7 +565,7 @@ function jobDetailsPopup(response, request) {
 	function populateUrlUnit(actionStatus, urlUnit) {
 		var consoleUrl = actionStatus["consoleUrl"];
         var externalChildIDs = actionStatus["externalChildIDs"];
		if(!consoleUrl && !externalChildIDs) {
		if(consoleUrl && externalChildIDs) {
 	        var urlPrefix = consoleUrl.trim().split(/_/)[0];
             //externalChildIds is a comma-separated string of each child job ID.
             //Create URL list by appending jobID portion after stripping "job"
- 
2.19.1.windows.1

