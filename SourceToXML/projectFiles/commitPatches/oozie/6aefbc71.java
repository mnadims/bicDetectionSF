From 6aefbc710d28adbe82347dde9d17694f7dd6c753 Mon Sep 17 00:00:00 2001
From: egashira <ryota.egashira@yahoo.com>
Date: Fri, 24 Apr 2015 17:42:08 -0700
Subject: [PATCH] OOZIE-2214 fix test case
 TestCoordRerunXCommand.testCoordRerunDateNeg (ryota)

--
 core/src/main/java/org/apache/oozie/coord/CoordUtils.java | 4 +---
 release-log.txt                                           | 1 +
 2 files changed, 2 insertions(+), 3 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/coord/CoordUtils.java b/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
index 90050b3df..7f05ef5ef 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
@@ -143,9 +143,7 @@ public class CoordUtils {
                     if (e.getErrorCode() == ErrorCode.E0605) {
                         XLog.getLog(CoordUtils.class).info("No action for nominal time:" + s + ". Skipping over");
                     }
                    else {
                        throw new CommandException(e);
                    }
                    throw new CommandException(e);
                 }
 
             }
diff --git a/release-log.txt b/release-log.txt
index dcc3f9a4a..1b5cccda7 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2214 fix test case TestCoordRerunXCommand.testCoordRerunDateNeg (ryota)
 OOZIE-2213 oozie-setup.ps1 should use "start-process" rather than "cmd /c" to invoke OozieSharelibCLI or OozieDBCLI commands (bzhang)
 OOZIE-2210 Update extjs 2.2 link (bzhang)
 OOZIE-2205 add option to load default/site.xml to actionConf on compute node (ryota)
- 
2.19.1.windows.1

