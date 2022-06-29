From ad95cb1f0d329e8d80d468cd8d1eb6b0cffd75c2 Mon Sep 17 00:00:00 2001
From: Eli Collins <eli@apache.org>
Date: Thu, 30 Jun 2011 07:04:58 +0000
Subject: [PATCH] Minor update to HADOOP-7429.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1141415 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/hadoop/io/IOUtils.java    | 28 ++++++++++++-------
 1 file changed, 18 insertions(+), 10 deletions(-)

diff --git a/common/src/java/org/apache/hadoop/io/IOUtils.java b/common/src/java/org/apache/hadoop/io/IOUtils.java
index 29db28032ba..c60b95befec 100644
-- a/common/src/java/org/apache/hadoop/io/IOUtils.java
++ b/common/src/java/org/apache/hadoop/io/IOUtils.java
@@ -115,24 +115,32 @@ public static void copyBytes(InputStream in, OutputStream out, Configuration con
    * @param in InputStream to read from
    * @param out OutputStream to write to
    * @param count number of bytes to copy
   * @param close whether to close the streams
    * @throws IOException if bytes can not be read or written
    */
  public static void copyBytes(InputStream in, OutputStream out, long count)
      throws IOException {
  public static void copyBytes(InputStream in, OutputStream out, long count,
      boolean close) throws IOException {
     byte buf[] = new byte[4096];
     long bytesRemaining = count;
     int bytesRead;
 
    while (bytesRemaining > 0) {
      int bytesToRead = (int)
        (bytesRemaining < buf.length ? bytesRemaining : buf.length);
    try {
      while (bytesRemaining > 0) {
        int bytesToRead = (int)
          (bytesRemaining < buf.length ? bytesRemaining : buf.length);
 
      bytesRead = in.read(buf, 0, bytesToRead);
      if (bytesRead == -1)
        break;
        bytesRead = in.read(buf, 0, bytesToRead);
        if (bytesRead == -1)
          break;
 
      out.write(buf, 0, bytesRead);
      bytesRemaining -= bytesRead;
        out.write(buf, 0, bytesRead);
        bytesRemaining -= bytesRead;
      }
    } finally {
      if (close) {
        closeStream(out);
        closeStream(in);
      }
     }
   }
   
- 
2.19.1.windows.1

