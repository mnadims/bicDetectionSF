From c475c85ffcd249b54911a02912992337da8e4c83 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Tue, 10 Feb 2015 20:13:32 -0500
Subject: [PATCH] ACCUMULO-3576 Use checkstyle to prevent use of jline
 Preconditions

--
 pom.xml | 4 ++++
 1 file changed, 4 insertions(+)

diff --git a/pom.xml b/pom.xml
index 5aff5b573..869fe8cd5 100644
-- a/pom.xml
++ b/pom.xml
@@ -748,6 +748,10 @@
                   <property name="format" value="[@]see\s+[{][@]link" />
                   <property name="message" value="Javadoc @see does not need @link: pick one or the other." />
                 </module>
                <module name="RegexpSinglelineJava">
                  <property name="format" value="jline[.]internal[.]Preconditions" />
                  <property name="message" value="Please use Guava Preconditions not JLine" />
                </module>
                 <module name="OuterTypeFilename" />
                 <module name="LineLength">
                   <!-- needs extra, because Eclipse formatter ignores the ending left brace -->
- 
2.19.1.windows.1

