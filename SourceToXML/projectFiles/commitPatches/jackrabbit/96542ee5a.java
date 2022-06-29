From 96542ee5ac1cc253ded14254791712efdeea637e Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 1 Aug 2006 20:35:12 +0000
Subject: [PATCH] JCR-367: Xerces dependencies no longer needed.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@427716 13f79535-47bb-0310-9956-ffa450edef68
--
 jca/project.xml | 16 ----------------
 1 file changed, 16 deletions(-)

diff --git a/jca/project.xml b/jca/project.xml
index 2a92406ae..1555f568d 100644
-- a/jca/project.xml
++ b/jca/project.xml
@@ -74,22 +74,6 @@
 		<rar.bundle>true</rar.bundle>
 	    </properties>	    
 	</dependency>
	<dependency>
	    <groupId>xerces</groupId>
	    <artifactId>xercesImpl</artifactId>
	    <version>2.6.2</version>
	    <properties>
		<rar.bundle>true</rar.bundle>
	    </properties>	    
	</dependency>
	<dependency>
	    <groupId>xerces</groupId>
	    <artifactId>xmlParserAPIs</artifactId>
	    <version>2.0.2</version>
	    <properties>
		<rar.bundle>true</rar.bundle>
	    </properties>	    
	</dependency> 
 	<dependency>
 	    <groupId>geronimo-spec</groupId>
 	    <artifactId>geronimo-spec-j2ee-connector</artifactId>
- 
2.19.1.windows.1

