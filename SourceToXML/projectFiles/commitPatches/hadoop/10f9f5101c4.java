From 10f9f5101c44be7c675a44ded4aad212627ecdee Mon Sep 17 00:00:00 2001
From: Jason Lowe <jlowe@apache.org>
Date: Thu, 6 Nov 2014 15:10:40 +0000
Subject: [PATCH] MAPREDUCE-5960. JobSubmitter's check whether job.jar is local
 is incorrect with no authority in job jar path. Contributed by Gera Shegalov

--
 .../org/apache/hadoop/fs/FileContext.java     |  3 ++
 hadoop-mapreduce-project/CHANGES.txt          |  3 ++
 .../v2/app/job/impl/TaskAttemptImpl.java      |  8 ++--
 .../apache/hadoop/mapreduce/JobSubmitter.java |  9 ++--
 .../org/apache/hadoop/mapred/YARNRunner.java  |  5 ++-
 .../hadoop/mapreduce/v2/TestMRJobs.java       | 41 +++++++++++++++----
 .../hadoop/mapreduce/v2/TestUberAM.java       | 40 +++---------------
 7 files changed, 56 insertions(+), 53 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
index 2323650febe..85f8136c0ac 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
@@ -430,6 +430,9 @@ public static FileContext getFileContext(final URI defaultFsUri,
       final Configuration aConf) throws UnsupportedFileSystemException {
     UserGroupInformation currentUser = null;
     AbstractFileSystem defaultAfs = null;
    if (defaultFsUri.getScheme() == null) {
      return getFileContext(aConf);
    }
     try {
       currentUser = UserGroupInformation.getCurrentUser();
       defaultAfs = getAbstractFileSystem(currentUser, defaultFsUri, aConf);
diff --git a/hadoop-mapreduce-project/CHANGES.txt b/hadoop-mapreduce-project/CHANGES.txt
index bbe96c2a8e7..fd42f82c9a8 100644
-- a/hadoop-mapreduce-project/CHANGES.txt
++ b/hadoop-mapreduce-project/CHANGES.txt
@@ -459,6 +459,9 @@ Release 2.6.0 - UNRELEASED
     MAPREDUCE-6048. Fixed TestJavaSerialization failure. (Varun Vasudev via
     jianhe)
 
    MAPREDUCE-5960. JobSubmitter's check whether job.jar is local is incorrect
    with no authority in job jar path. (Gera Shegalov via jlowe)

 Release 2.5.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/job/impl/TaskAttemptImpl.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/job/impl/TaskAttemptImpl.java
index 288e18e6901..dfc6a3f5a12 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/job/impl/TaskAttemptImpl.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/job/impl/TaskAttemptImpl.java
@@ -657,9 +657,11 @@ private static ContainerLaunchContext createCommonContainerLaunchContext(
       // //////////// Set up JobJar to be localized properly on the remote NM.
       String jobJar = conf.get(MRJobConfig.JAR);
       if (jobJar != null) {
        Path remoteJobJar = (new Path(jobJar)).makeQualified(remoteFS
            .getUri(), remoteFS.getWorkingDirectory());
        LocalResource rc = createLocalResource(remoteFS, remoteJobJar,
        final Path jobJarPath = new Path(jobJar);
        final FileSystem jobJarFs = FileSystem.get(jobJarPath.toUri(), conf);
        Path remoteJobJar = jobJarPath.makeQualified(jobJarFs.getUri(),
            jobJarFs.getWorkingDirectory());
        LocalResource rc = createLocalResource(jobJarFs, remoteJobJar,
             LocalResourceType.PATTERN, LocalResourceVisibility.APPLICATION);
         String pattern = conf.getPattern(JobContext.JAR_UNPACK_PATTERN, 
             JobConf.UNPACK_JAR_PATTERN_DEFAULT).pattern();
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmitter.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmitter.java
index b76a734da61..ba496ee84f6 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmitter.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmitter.java
@@ -250,11 +250,10 @@ private void copyAndConfigureFiles(Job job, Path submitJobDir,
       }
       Path jobJarPath = new Path(jobJar);
       URI jobJarURI = jobJarPath.toUri();
      // If the job jar is already in fs, we don't need to copy it from local fs
      if (jobJarURI.getScheme() == null || jobJarURI.getAuthority() == null
              || !(jobJarURI.getScheme().equals(jtFs.getUri().getScheme()) 
                  && jobJarURI.getAuthority().equals(
                                            jtFs.getUri().getAuthority()))) {
      // If the job jar is already in a global fs,
      // we don't need to copy it from local fs
      if (     jobJarURI.getScheme() == null
            || jobJarURI.getScheme().equals("file")) {
         copyJar(jobJarPath, JobSubmissionFiles.getJobJar(submitJobDir), 
             replication);
         job.setJar(JobSubmissionFiles.getJobJar(submitJobDir).toString());
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/main/java/org/apache/hadoop/mapred/YARNRunner.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/main/java/org/apache/hadoop/mapred/YARNRunner.java
index a1c4c32f0ea..7b2cf53417a 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/main/java/org/apache/hadoop/mapred/YARNRunner.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/main/java/org/apache/hadoop/mapred/YARNRunner.java
@@ -357,8 +357,9 @@ public ApplicationSubmissionContext createApplicationSubmissionContext(
             jobConfPath, LocalResourceType.FILE));
     if (jobConf.get(MRJobConfig.JAR) != null) {
       Path jobJarPath = new Path(jobConf.get(MRJobConfig.JAR));
      LocalResource rc = createApplicationResource(defaultFileContext,
          jobJarPath, 
      LocalResource rc = createApplicationResource(
          FileContext.getFileContext(jobJarPath.toUri(), jobConf),
          jobJarPath,
           LocalResourceType.PATTERN);
       String pattern = conf.getPattern(JobContext.JAR_UNPACK_PATTERN, 
           JobConf.UNPACK_JAR_PATTERN_DEFAULT).pattern();
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestMRJobs.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestMRJobs.java
index 00449252b7a..2b45049e08c 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestMRJobs.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestMRJobs.java
@@ -40,6 +40,7 @@
 import org.apache.hadoop.FailingMapper;
 import org.apache.hadoop.RandomTextWriterJob;
 import org.apache.hadoop.RandomTextWriterJob.RandomInputFormat;
import org.apache.hadoop.fs.viewfs.ConfigUtil;
 import org.apache.hadoop.mapreduce.SleepJob;
 import org.apache.hadoop.mapreduce.SleepJob.SleepMapper;
 import org.apache.hadoop.conf.Configuration;
@@ -89,6 +90,7 @@
 import org.apache.hadoop.security.token.Token;
 import org.apache.hadoop.security.token.TokenIdentifier;
 import org.apache.hadoop.util.ApplicationClassLoader;
import org.apache.hadoop.util.ClassUtil;
 import org.apache.hadoop.util.JarFinder;
 import org.apache.hadoop.util.Shell;
 import org.apache.hadoop.yarn.api.records.ApplicationId;
@@ -97,6 +99,7 @@
 import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppState;
 import org.apache.hadoop.yarn.util.ConverterUtils;
 import org.apache.log4j.Level;
import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.Assert;
 import org.junit.BeforeClass;
@@ -111,6 +114,9 @@
   private static final String TEST_IO_SORT_MB = "11";
   private static final String TEST_GROUP_MAX = "200";
 
  private static final int DEFAULT_REDUCES = 2;
  protected int numSleepReducers = DEFAULT_REDUCES;

   protected static MiniMRYarnCluster mrCluster;
   protected static MiniDFSCluster dfsCluster;
 
@@ -175,10 +181,23 @@ public static void tearDown() {
     }
   }
 
  @After
  public void resetInit() {
    numSleepReducers = DEFAULT_REDUCES;
  }

  @Test (timeout = 300000)
  public void testSleepJob() throws Exception {
    testSleepJobInternal(false);
  }

   @Test (timeout = 300000)
  public void testSleepJob() throws IOException, InterruptedException,
      ClassNotFoundException { 
    LOG.info("\n\n\nStarting testSleepJob().");
  public void testSleepJobWithRemoteJar() throws Exception {
    testSleepJobInternal(true);
  }

  private void testSleepJobInternal(boolean useRemoteJar) throws Exception {
    LOG.info("\n\n\nStarting testSleepJob: useRemoteJar=" + useRemoteJar);
 
     if (!(new File(MiniMRYarnCluster.APPJAR)).exists()) {
       LOG.info("MRAppJar " + MiniMRYarnCluster.APPJAR
@@ -192,14 +211,20 @@ public void testSleepJob() throws IOException, InterruptedException,
     
     SleepJob sleepJob = new SleepJob();
     sleepJob.setConf(sleepConf);

    int numReduces = sleepConf.getInt("TestMRJobs.testSleepJob.reduces", 2); // or sleepConf.getConfig().getInt(MRJobConfig.NUM_REDUCES, 2);
    
     // job with 3 maps (10s) and numReduces reduces (5s), 1 "record" each:
    Job job = sleepJob.createJob(3, numReduces, 10000, 1, 5000, 1);
    Job job = sleepJob.createJob(3, numSleepReducers, 10000, 1, 5000, 1);
 
     job.addFileToClassPath(APP_JAR); // The AppMaster jar itself.
    job.setJarByClass(SleepJob.class);
    if (useRemoteJar) {
      final Path localJar = new Path(
          ClassUtil.findContainingJar(SleepJob.class));
      ConfigUtil.addLink(job.getConfiguration(), "/jobjars",
          localFs.makeQualified(localJar.getParent()).toUri());
      job.setJar("viewfs:///jobjars/" + localJar.getName());
    } else {
      job.setJarByClass(SleepJob.class);
    }
     job.setMaxMapAttempts(1); // speed up failures
     job.submit();
     String trackingUrl = job.getTrackingURL();
@@ -381,7 +406,7 @@ protected void verifySleepJobCounters(Job job) throws InterruptedException,
         .getValue());
     Assert.assertEquals(3, counters.findCounter(JobCounter.TOTAL_LAUNCHED_MAPS)
         .getValue());
    Assert.assertEquals(2,
    Assert.assertEquals(numSleepReducers,
         counters.findCounter(JobCounter.TOTAL_LAUNCHED_REDUCES).getValue());
     Assert
         .assertTrue(counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS) != null
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestUberAM.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestUberAM.java
index e89a919e050..e198f994a3e 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestUberAM.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/TestUberAM.java
@@ -20,7 +20,6 @@
 
 import java.io.File;
 import java.io.IOException;
import java.util.Arrays;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
@@ -40,8 +39,7 @@
 public class TestUberAM extends TestMRJobs {
 
   private static final Log LOG = LogFactory.getLog(TestUberAM.class);
  private int numSleepReducers;
  

   @BeforeClass
   public static void setup() throws IOException {
     TestMRJobs.setup();
@@ -54,21 +52,15 @@ public static void setup() throws IOException {
   @Override
   @Test
   public void testSleepJob()
  throws IOException, InterruptedException, ClassNotFoundException {
  throws Exception {
     numSleepReducers = 1;
    if (mrCluster != null) {
    	mrCluster.getConfig().setInt("TestMRJobs.testSleepJob.reduces", numSleepReducers);
    }
     super.testSleepJob();
   }
   
   @Test
   public void testSleepJobWithMultipleReducers()
  throws IOException, InterruptedException, ClassNotFoundException {
  throws Exception {
     numSleepReducers = 3;
    if (mrCluster != null) {
      mrCluster.getConfig().setInt("TestMRJobs.testSleepJob.reduces", numSleepReducers);
    }
     super.testSleepJob();
   }
   
@@ -76,20 +68,7 @@ public void testSleepJobWithMultipleReducers()
   protected void verifySleepJobCounters(Job job) throws InterruptedException,
       IOException {
     Counters counters = job.getCounters();

    Assert.assertEquals(3, counters.findCounter(JobCounter.OTHER_LOCAL_MAPS)
        .getValue());
    Assert.assertEquals(3, counters.findCounter(JobCounter.TOTAL_LAUNCHED_MAPS)
        .getValue());
    Assert.assertEquals(numSleepReducers,
        counters.findCounter(JobCounter.TOTAL_LAUNCHED_REDUCES).getValue());
    Assert
        .assertTrue(counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS) != null
            && counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS).getValue() != 0);
    Assert
        .assertTrue(counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS) != null
            && counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS).getValue() != 0);

    super.verifySleepJobCounters(job);
     Assert.assertEquals(3,
         counters.findCounter(JobCounter.NUM_UBER_SUBMAPS).getValue());
     Assert.assertEquals(numSleepReducers,
@@ -168,16 +147,7 @@ public void testFailingMapper()
   protected void verifyFailingMapperCounters(Job job)
       throws InterruptedException, IOException {
     Counters counters = job.getCounters();
    Assert.assertEquals(2, counters.findCounter(JobCounter.OTHER_LOCAL_MAPS)
        .getValue());
    Assert.assertEquals(2, counters.findCounter(JobCounter.TOTAL_LAUNCHED_MAPS)
        .getValue());
    Assert.assertEquals(2, counters.findCounter(JobCounter.NUM_FAILED_MAPS)
        .getValue());
    Assert
        .assertTrue(counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS) != null
            && counters.findCounter(JobCounter.SLOTS_MILLIS_MAPS).getValue() != 0);

    super.verifyFailingMapperCounters(job);
     Assert.assertEquals(2,
         counters.findCounter(JobCounter.TOTAL_LAUNCHED_UBERTASKS).getValue());
     Assert.assertEquals(2, counters.findCounter(JobCounter.NUM_UBER_SUBMAPS)
- 
2.19.1.windows.1

