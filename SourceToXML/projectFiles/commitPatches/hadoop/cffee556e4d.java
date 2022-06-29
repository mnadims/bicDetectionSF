From cffee556e4d7897f65ef52020f5b10a278cb9068 Mon Sep 17 00:00:00 2001
From: Robert Joseph Evans <bobby@apache.org>
Date: Fri, 24 Aug 2012 14:16:41 +0000
Subject: [PATCH] HADOOP-8725. MR is broken when security is off (daryn via
 bobby)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1376929 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt                 | 2 ++
 .../java/org/apache/hadoop/security/UserGroupInformation.java   | 2 +-
 2 files changed, 3 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 7474fb87e4b..89a297ce485 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -975,6 +975,8 @@ Release 0.23.3 - UNRELEASED
     HADOOP-8709. globStatus changed behavior from 0.20/1.x (Jason Lowe via
     bobby)
 
    HADOOP-8725. MR is broken when security is off (daryn via bobby)

 Release 0.23.2 - UNRELEASED 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
index 967f0df89ff..0d3c4822892 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/UserGroupInformation.java
@@ -642,7 +642,7 @@ static UserGroupInformation getLoginUser() throws IOException {
                                           AuthenticationMethod.SIMPLE);
         loginUser = new UserGroupInformation(login.getSubject());
         String fileLocation = System.getenv(HADOOP_TOKEN_FILE_LOCATION);
        if (fileLocation != null && isSecurityEnabled()) {
        if (fileLocation != null) {
           // load the token storage file and put all of the tokens into the
           // user.
           Credentials cred = Credentials.readTokenStorageFile(
- 
2.19.1.windows.1

