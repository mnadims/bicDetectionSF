From e28edbffe15e9d176d14ea2af8d9460d807b3fc4 Mon Sep 17 00:00:00 2001
From: Tsz-wo Sze <szetszwo@apache.org>
Date: Wed, 6 Feb 2013 01:13:16 +0000
Subject: [PATCH] HDFS-4468.  Use the new StringUtils methods added by
 HADOOP-9252 and fix TestHDFSCLI and TestQuota.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1442824 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt   |  3 ++
 .../java/org/apache/hadoop/hdfs/DFSUtil.java  |  5 ++
 .../protocol/DSQuotaExceededException.java    |  8 ++--
 .../hadoop/hdfs/protocol/DatanodeInfo.java    |  9 ++--
 .../server/namenode/ClusterJspHelper.java     |  6 +--
 .../server/namenode/NamenodeJspHelper.java    | 46 +++++++++++--------
 .../apache/hadoop/hdfs/tools/DFSAdmin.java    |  3 +-
 .../org/apache/hadoop/hdfs/TestQuota.java     |  4 +-
 .../src/test/resources/testHDFSConf.xml       |  4 +-
 9 files changed, 52 insertions(+), 36 deletions(-)

diff --git a/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt b/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
index d86404af6c1..fa6a8fd951a 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
++ b/hadoop-hdfs-project/hadoop-hdfs/CHANGES.txt
@@ -762,6 +762,9 @@ Release 2.0.3-alpha - Unreleased
     HDFS-4344. dfshealth.jsp throws NumberFormatException when
     dfs.hosts/dfs.hosts.exclude includes port number. (Andy Isaacson via atm)
 
    HDFS-4468.  Use the new StringUtils methods added by HADOOP-9252 and fix
    TestHDFSCLI and TestQuota. (szetszwo)

   BREAKDOWN OF HDFS-3077 SUBTASKS
 
     HDFS-3077. Quorum-based protocol for reading and writing edit logs.
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSUtil.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSUtil.java
index 7eaff61611e..39f839246fc 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSUtil.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/DFSUtil.java
@@ -925,6 +925,11 @@ public static float getPercentRemaining(long remaining, long capacity) {
     return capacity <= 0 ? 0 : (remaining * 100.0f)/capacity; 
   }
 
  /** Convert percentage to a string. */
  public static String percent2String(double percentage) {
    return StringUtils.format("%.2f%%", percentage);
  }

   /**
    * Round bytes to GiB (gibibyte)
    * @param bytes number of bytes
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DSQuotaExceededException.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DSQuotaExceededException.java
index c7b22f7ac24..481c1305e8a 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DSQuotaExceededException.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DSQuotaExceededException.java
@@ -20,7 +20,7 @@
 
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.util.StringUtils;
import static org.apache.hadoop.util.StringUtils.TraditionalBinaryPrefix.long2String;
 
 @InterfaceAudience.Private
 @InterfaceStability.Evolving
@@ -41,9 +41,9 @@ public DSQuotaExceededException(long quota, long count) {
   public String getMessage() {
     String msg = super.getMessage();
     if (msg == null) {
      return "The DiskSpace quota" + (pathName==null?"":(" of " + pathName)) + 
          " is exceeded: quota=" + StringUtils.humanReadableInt(quota) + 
          " diskspace consumed=" + StringUtils.humanReadableInt(count);
      return "The DiskSpace quota" + (pathName==null?"": " of " + pathName)
          + " is exceeded: quota = " + quota + " B = " + long2String(quota, "B", 2)
          + " but diskspace consumed = " + count + " B = " + long2String(count, "B", 2);
     } else {
       return msg;
     }
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DatanodeInfo.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DatanodeInfo.java
index cf7438a7ed5..df46cd6a0c2 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DatanodeInfo.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/protocol/DatanodeInfo.java
@@ -17,10 +17,13 @@
  */
 package org.apache.hadoop.hdfs.protocol;
 
import static org.apache.hadoop.hdfs.DFSUtil.percent2String;

 import java.util.Date;
 
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hdfs.DFSConfigKeys;
 import org.apache.hadoop.hdfs.DFSUtil;
 import org.apache.hadoop.net.NetUtils;
 import org.apache.hadoop.net.NetworkTopology;
@@ -244,8 +247,8 @@ public String getDatanodeReport() {
     buffer.append("DFS Used: "+u+" ("+StringUtils.byteDesc(u)+")"+"\n");
     buffer.append("Non DFS Used: "+nonDFSUsed+" ("+StringUtils.byteDesc(nonDFSUsed)+")"+"\n");
     buffer.append("DFS Remaining: " +r+ " ("+StringUtils.byteDesc(r)+")"+"\n");
    buffer.append("DFS Used%: "+StringUtils.limitDecimalTo2(usedPercent)+"%\n");
    buffer.append("DFS Remaining%: "+StringUtils.limitDecimalTo2(remainingPercent)+"%\n");
    buffer.append("DFS Used%: "+percent2String(usedPercent) + "\n");
    buffer.append("DFS Remaining%: "+percent2String(remainingPercent) + "\n");
     buffer.append("Last contact: "+new Date(lastUpdate)+"\n");
     return buffer.toString();
   }
@@ -269,7 +272,7 @@ public String dumpDatanode() {
     }
     buffer.append(" " + c + "(" + StringUtils.byteDesc(c)+")");
     buffer.append(" " + u + "(" + StringUtils.byteDesc(u)+")");
    buffer.append(" " + StringUtils.limitDecimalTo2(((1.0*u)/c)*100)+"%");
    buffer.append(" " + percent2String(u/(double)c));
     buffer.append(" " + r + "(" + StringUtils.byteDesc(r)+")");
     buffer.append(" " + new Date(lastUpdate));
     return buffer.toString();
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/ClusterJspHelper.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/ClusterJspHelper.java
index 1b3db818d15..0f0a989f8c9 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/ClusterJspHelper.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/ClusterJspHelper.java
@@ -569,12 +569,10 @@ public void toXML(XMLOutputter doc) throws IOException {
       toXmlItemBlock(doc, "DFS Remaining", StringUtils.byteDesc(free));
     
       // dfsUsedPercent
      toXmlItemBlock(doc, "DFS Used%", 
          StringUtils.limitDecimalTo2(dfsUsedPercent)+ "%");
      toXmlItemBlock(doc, "DFS Used%", DFSUtil.percent2String(dfsUsedPercent));
     
       // dfsRemainingPercent
      toXmlItemBlock(doc, "DFS Remaining%",
          StringUtils.limitDecimalTo2(dfsRemainingPercent) + "%");
      toXmlItemBlock(doc, "DFS Remaining%", DFSUtil.percent2String(dfsRemainingPercent));
     
       doc.endTag(); // storage
     
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NamenodeJspHelper.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NamenodeJspHelper.java
index c4ae5d7756e..005ba6a51ec 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NamenodeJspHelper.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/NamenodeJspHelper.java
@@ -17,6 +17,8 @@
  */
 package org.apache.hadoop.hdfs.server.namenode;
 
import static org.apache.hadoop.hdfs.DFSUtil.percent2String;

 import java.io.IOException;
 import java.lang.management.ManagementFactory;
 import java.lang.management.MemoryMXBean;
@@ -64,6 +66,14 @@
 import com.google.common.base.Preconditions;
 
 class NamenodeJspHelper {
  static String fraction2String(double value) {
    return StringUtils.format("%.2f", value);
  }

  static String fraction2String(long numerator, long denominator) {
    return fraction2String(numerator/(double)denominator);
  }

   static String getSafeModeText(FSNamesystem fsn) {
     if (!fsn.isInSafeMode())
       return "";
@@ -361,20 +371,20 @@ void generateHealthReport(JspWriter out, NameNode nn,
           + "DFS Remaining" + colTxt() + ":" + colTxt()
           + StringUtils.byteDesc(remaining) + rowTxt() + colTxt() + "DFS Used%"
           + colTxt() + ":" + colTxt()
          + StringUtils.limitDecimalTo2(percentUsed) + " %" + rowTxt()
          + percent2String(percentUsed) + rowTxt()
           + colTxt() + "DFS Remaining%" + colTxt() + ":" + colTxt()
          + StringUtils.limitDecimalTo2(percentRemaining) + " %"
          + percent2String(percentRemaining)
           + rowTxt() + colTxt() + "Block Pool Used" + colTxt() + ":" + colTxt()
           + StringUtils.byteDesc(bpUsed) + rowTxt()
           + colTxt() + "Block Pool Used%"+ colTxt() + ":" + colTxt()
          + StringUtils.limitDecimalTo2(percentBpUsed) + " %" 
          + percent2String(percentBpUsed) 
           + rowTxt() + colTxt() + "DataNodes usages" + colTxt() + ":" + colTxt()
           + "Min %" + colTxt() + "Median %" + colTxt() + "Max %" + colTxt()
           + "stdev %" + rowTxt() + colTxt() + colTxt() + colTxt()
          + StringUtils.limitDecimalTo2(min) + " %"
          + colTxt() + StringUtils.limitDecimalTo2(median) + " %"
          + colTxt() + StringUtils.limitDecimalTo2(max) + " %"
          + colTxt() + StringUtils.limitDecimalTo2(dev) + " %"
          + percent2String(min)
          + colTxt() + percent2String(median)
          + colTxt() + percent2String(max)
          + colTxt() + percent2String(dev)
           + rowTxt() + colTxt()
           + "<a href=\"dfsnodelist.jsp?whatNodes=LIVE\">Live Nodes</a> "
           + colTxt() + ":" + colTxt() + live.size()
@@ -562,9 +572,9 @@ void generateNodeData(JspWriter out, DatanodeDescriptor d, String suffix,
       long u = d.getDfsUsed();
       long nu = d.getNonDfsUsed();
       long r = d.getRemaining();
      String percentUsed = StringUtils.limitDecimalTo2(d.getDfsUsedPercent());
      String percentRemaining = StringUtils.limitDecimalTo2(d
          .getRemainingPercent());
      final double percentUsedValue = d.getDfsUsedPercent();
      String percentUsed = fraction2String(percentUsedValue);
      String percentRemaining = fraction2String(d.getRemainingPercent());
 
       String adminState = d.getAdminState().toString();
 
@@ -572,32 +582,30 @@ void generateNodeData(JspWriter out, DatanodeDescriptor d, String suffix,
       long currentTime = Time.now();
       
       long bpUsed = d.getBlockPoolUsed();
      String percentBpUsed = StringUtils.limitDecimalTo2(d
          .getBlockPoolUsedPercent());
      String percentBpUsed = fraction2String(d.getBlockPoolUsedPercent());
 
       out.print("<td class=\"lastcontact\"> "
           + ((currentTime - timestamp) / 1000)
           + "<td class=\"adminstate\">"
           + adminState
           + "<td align=\"right\" class=\"capacity\">"
          + StringUtils.limitDecimalTo2(c * 1.0 / diskBytes)
          + fraction2String(c, diskBytes)
           + "<td align=\"right\" class=\"used\">"
          + StringUtils.limitDecimalTo2(u * 1.0 / diskBytes)
          + fraction2String(u, diskBytes)
           + "<td align=\"right\" class=\"nondfsused\">"
          + StringUtils.limitDecimalTo2(nu * 1.0 / diskBytes)
          + fraction2String(nu, diskBytes)
           + "<td align=\"right\" class=\"remaining\">"
          + StringUtils.limitDecimalTo2(r * 1.0 / diskBytes)
          + fraction2String(r, diskBytes)
           + "<td align=\"right\" class=\"pcused\">"
           + percentUsed
           + "<td class=\"pcused\">"
          + ServletUtil.percentageGraph((int) Double.parseDouble(percentUsed),
              100) 
          + ServletUtil.percentageGraph((int)percentUsedValue, 100) 
           + "<td align=\"right\" class=\"pcremaining\">"
           + percentRemaining 
           + "<td title=" + "\"blocks scheduled : "
           + d.getBlocksScheduled() + "\" class=\"blocks\">" + d.numBlocks()+"\n"
           + "<td align=\"right\" class=\"bpused\">"
          + StringUtils.limitDecimalTo2(bpUsed * 1.0 / diskBytes)
          + fraction2String(bpUsed, diskBytes)
           + "<td align=\"right\" class=\"pcbpused\">"
           + percentBpUsed
           + "<td align=\"right\" class=\"volfails\">"
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java
index b3c9309054d..7db2b5614ad 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/tools/DFSAdmin.java
@@ -316,8 +316,7 @@ public void report() throws IOException {
       System.out.println("DFS Used: " + used
                          + " (" + StringUtils.byteDesc(used) + ")");
       System.out.println("DFS Used%: "
                         + StringUtils.limitDecimalTo2(((1.0 * used) / presentCapacity) * 100)
                         + "%");
          + StringUtils.formatPercent(used/(double)presentCapacity, 2));
       
       /* These counts are not always upto date. They are updated after  
        * iteration of an internal list. Should be updated in a few seconds to 
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestQuota.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestQuota.java
index 600829b1186..0f6d7ada666 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestQuota.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestQuota.java
@@ -70,8 +70,8 @@ public void testDSQuotaExceededExceptionIsHumanReadable() throws Exception {
       throw new DSQuotaExceededException(bytes, bytes);
     } catch(DSQuotaExceededException e) {
       
      assertEquals("The DiskSpace quota is exceeded: quota=1.0k " +
          "diskspace consumed=1.0k", e.getMessage());
      assertEquals("The DiskSpace quota is exceeded: quota = 1024 B = 1 KB"
          + " but diskspace consumed = 1024 B = 1 KB", e.getMessage());
     }
   }
   
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/resources/testHDFSConf.xml b/hadoop-hdfs-project/hadoop-hdfs/src/test/resources/testHDFSConf.xml
index 81f955a9be8..2fb10837fcd 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/resources/testHDFSConf.xml
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/resources/testHDFSConf.xml
@@ -1182,7 +1182,7 @@
         </comparator>
         <comparator>
           <type>RegexpComparator</type>
          <expected-output>^1\.0k\s+hdfs:///dir0/data1k</expected-output>
          <expected-output>^1\.0 K\s+hdfs:///dir0/data1k</expected-output>
         </comparator>
       </comparators>
     </test>
@@ -15590,7 +15590,7 @@
       <comparators>
         <comparator>
           <type>RegexpComparator</type>
          <expected-output>put: The DiskSpace quota of /dir1 is exceeded: quota=1.0k diskspace consumed=[0-9.]+[kmg]*</expected-output>
          <expected-output>put: The DiskSpace quota of /dir1 is exceeded: quota = 1024 B = 1 KB but diskspace consumed = [0-9]+ B = [0-9.]+ [KMG]B*</expected-output>
         </comparator>
       </comparators>
     </test>
- 
2.19.1.windows.1

