From 97e3f9044c93e5c78ad10e2ff47c130902f4ce5c Mon Sep 17 00:00:00 2001
From: Alejandro Abdelnur <tucu@apache.org>
Date: Tue, 22 Nov 2011 07:12:51 +0000
Subject: [PATCH] OOZIE-609 OOZIE-609 Oozie services fail to start with log
 enabled. (tucu)

git-svn-id: https://svn.apache.org/repos/asf/incubator/oozie/trunk@1204834 13f79535-47bb-0310-9956-ffa450edef68
--
 core/src/main/java/org/apache/oozie/service/Services.java | 4 ++--
 release-log.txt                                           | 1 +
 2 files changed, 3 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/service/Services.java b/core/src/main/java/org/apache/oozie/service/Services.java
index 7c627a54d..f0e1a06b1 100644
-- a/core/src/main/java/org/apache/oozie/service/Services.java
++ b/core/src/main/java/org/apache/oozie/service/Services.java
@@ -235,7 +235,7 @@ public class Services {
         for (Class klass : classes) {
             try {
                 Service service = (Service) klass.newInstance();
                log.debug("Loading service [{}] implementation [{}]", service.getInterface(),
                log.debug("Loading service [{0}] implementation [{1}]", service.getInterface(),
                         service.getClass());
                 if (!service.getInterface().isInstance(service)) {
                     throw new ServiceException(ErrorCode.E0101, klass, service.getInterface().getName());
@@ -269,7 +269,7 @@ public class Services {
             //removing duplicate services, strategy: last one wins
             for (Service service : list) {
                 if (map.containsKey(service.getInterface())) {
                    log.debug("Replacing service [{}] implementation [{}]", service.getInterface(),
                    log.debug("Replacing service [{0}] implementation [{1}]", service.getInterface(),
                             service.getClass());
                 }
                 map.put(service.getInterface(), service);
diff --git a/release-log.txt b/release-log.txt
index 81f7edb0b..83f7fb36c 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.2.0 release
 
OOZIE-609 OOZIE-609 Oozie services fail to start with log enabled. (tucu)
 OOZIE-607 Pig POM brings in several unneeded dependencies. (tucu)
 OOZIE-601 Oozie's POMs should use org.apache.oozie as group. (tucu)
 OOZIE-480 In Oozie-site.xml, if we specify oozie.services.ext property is not overriding the services. (tucu)
- 
2.19.1.windows.1

