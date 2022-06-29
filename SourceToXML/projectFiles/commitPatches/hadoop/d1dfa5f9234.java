From d1dfa5f923408fea94fe18b7886ead4573560e6a Mon Sep 17 00:00:00 2001
From: Chris Nauroth <cnauroth@apache.org>
Date: Thu, 22 Aug 2013 21:13:51 +0000
Subject: [PATCH] HADOOP-9887. globStatus does not correctly handle paths
 starting with a drive spec on Windows. Contributed by Chuan Liu.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1516608 13f79535-47bb-0310-9956-ffa450edef68
--
 .../hadoop-common/CHANGES.txt                 |  3 +++
 .../java/org/apache/hadoop/fs/Globber.java    | 22 ++++++++++++++-----
 2 files changed, 19 insertions(+), 6 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index a2d1304ead7..7b749b7a1bd 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -420,6 +420,9 @@ Release 2.1.1-beta - UNRELEASED
     HADOOP-9880. SASL changes from HADOOP-9421 breaks Secure HA NN. (daryn via
     jing9)
 
    HADOOP-9887. globStatus does not correctly handle paths starting with a drive
    spec on Windows. (Chuan Liu via cnauroth)

 Release 2.1.0-beta - 2013-08-22
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
index 57ad45e81d4..b0bd8490715 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/Globber.java
@@ -97,7 +97,7 @@ private Path fixRelativePart(Path path) {
   /**
    * Translate an absolute path into a list of path components.
    * We merge double slashes into a single slash here.
   * The first path component (i.e. root) does not get an entry in the list.
   * POSIX root path, i.e. '/', does not get an entry in the list.
    */
   private static List<String> getPathComponents(String path)
       throws IOException {
@@ -167,8 +167,8 @@ private static String unquotePathComponent(String name) {
       // Get the absolute path for this flattened pattern.  We couldn't do 
       // this prior to flattening because of patterns like {/,a}, where which
       // path you go down influences how the path must be made absolute.
      Path absPattern =
          fixRelativePart(new Path(flatPattern .isEmpty() ? "." : flatPattern ));
      Path absPattern = fixRelativePart(new Path(
          flatPattern.isEmpty() ? Path.CUR_DIR : flatPattern));
       // Now we break the flattened, absolute pattern into path components.
       // For example, /a/*/c would be broken into the list [a, *, c]
       List<String> components =
@@ -176,9 +176,19 @@ private static String unquotePathComponent(String name) {
       // Starting out at the root of the filesystem, we try to match
       // filesystem entries against pattern components.
       ArrayList<FileStatus> candidates = new ArrayList<FileStatus>(1);
      candidates.add(new FileStatus(0, true, 0, 0, 0,
          new Path(scheme, authority, "/")));

      if (Path.WINDOWS && !components.isEmpty()
          && Path.isWindowsAbsolutePath(absPattern.toUri().getPath(), true)) {
        // On Windows the path could begin with a drive letter, e.g. /E:/foo.
        // We will skip matching the drive letter and start from listing the
        // root of the filesystem on that drive.
        String driveLetter = components.remove(0);
        candidates.add(new FileStatus(0, true, 0, 0, 0, new Path(scheme,
            authority, Path.SEPARATOR + driveLetter + Path.SEPARATOR)));
      } else {
        candidates.add(new FileStatus(0, true, 0, 0, 0,
            new Path(scheme, authority, Path.SEPARATOR)));
      }
      
       for (String component : components) {
         ArrayList<FileStatus> newCandidates =
             new ArrayList<FileStatus>(candidates.size());
- 
2.19.1.windows.1

