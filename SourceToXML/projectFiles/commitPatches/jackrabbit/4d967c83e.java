From 4d967c83e616cafd3db8302f2727802732f55371 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Tue, 1 Aug 2006 20:32:16 +0000
Subject: [PATCH] JCR-367: Xerces dependencies no longer needed.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@427713 13f79535-47bb-0310-9956-ffa450edef68
--
 jackrabbit/project.xml | 16 ----------------
 1 file changed, 16 deletions(-)

diff --git a/jackrabbit/project.xml b/jackrabbit/project.xml
index 1ff4c82aa..e04efb1e2 100644
-- a/jackrabbit/project.xml
++ b/jackrabbit/project.xml
@@ -526,22 +526,6 @@ indexing, etc.
                 of the query features of Jackrabbit.
             -->
         </dependency>
        <dependency>
            <!-- The Xerces XML parser -->
            <groupId>xerces</groupId>
            <artifactId>xercesImpl</artifactId>
            <version>2.6.2</version>
            <url>http://xerces.apache.org/xerces2-j/</url>
            <!--
                The XML parser APIs and implementation used directly for
                features  not included in the standard XML APIs in J2SE 1.4.
            -->
        </dependency>
        <dependency>
            <groupId>xerces</groupId>
            <artifactId>xmlParserAPIs</artifactId>
            <version>2.0.2</version>
        </dependency>
         <dependency>
             <!-- Apache Derby -->
             <groupId>org.apache.derby</groupId>
- 
2.19.1.windows.1

