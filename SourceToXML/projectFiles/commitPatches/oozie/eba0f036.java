From eba0f0365f28604a73229a941a4b2e3e4c566c46 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Fri, 5 Aug 2016 13:15:38 -0700
Subject: [PATCH] OOZIE-2623 Oozie should use a dummy OutputFormat (satishsaley
 via rohini)

--
 .../action/hadoop/JavaActionExecutor.java     |  2 +
 .../action/hadoop/LauncherMapperHelper.java   |  3 +-
 release-log.txt                               |  1 +
 .../hadoop/OozieLauncherOutputCommitter.java  | 65 +++++++++++++++++++
 .../hadoop/OozieLauncherOutputFormat.java     | 48 ++++++++++++++
 5 files changed, 118 insertions(+), 1 deletion(-)
 create mode 100644 sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java
 create mode 100644 sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputFormat.java

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index e2e051003..9e1682cb0 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -146,6 +146,8 @@ public class JavaActionExecutor extends ActionExecutor {
         List<Class> classes = new ArrayList<Class>();
         classes.add(LauncherMapper.class);
         classes.add(OozieLauncherInputFormat.class);
        classes.add(OozieLauncherOutputFormat.class);
        classes.add(OozieLauncherOutputCommitter.class);
         classes.add(LauncherMainHadoopUtils.class);
         classes.add(HadoopShims.class);
         classes.addAll(Services.get().get(URIHandlerService.class).getClassesForLauncher());
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java b/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
index ef6b99dcb..ed06707b4 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/LauncherMapperHelper.java
@@ -159,7 +159,8 @@ public class LauncherMapperHelper {
         }
 
         launcherConf.setInputFormat(OozieLauncherInputFormat.class);
        launcherConf.set("mapred.output.dir", new Path(actionDir, "output").toString());
        launcherConf.setOutputFormat(OozieLauncherOutputFormat.class);
        launcherConf.setOutputCommitter(OozieLauncherOutputCommitter.class);
     }
 
     public static void setupYarnRestartHandling(JobConf launcherJobConf, Configuration actionConf, String launcherTag,
diff --git a/release-log.txt b/release-log.txt
index 7df964320..f83aa3345 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2623 Oozie should use a dummy OutputFormat (satishsaley via rohini)
 OOZIE-2625 Drop workflowgenerator (rkanter)
 OOZIE-2602 Upgrade oozie to pig 0.16.0 (nperiwal via jaydeepvishwakarma)
 OOZIE-2493 TestDistcpMain deletes action.xml from wrong filesystem (abhishekbafna via rkanter)
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java
new file mode 100644
index 000000000..153019b5d
-- /dev/null
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputCommitter.java
@@ -0,0 +1,65 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.oozie.action.hadoop;

import java.io.IOException;

import org.apache.hadoop.mapred.JobContext;
import org.apache.hadoop.mapred.OutputCommitter;
import org.apache.hadoop.mapred.TaskAttemptContext;

public class OozieLauncherOutputCommitter extends OutputCommitter {

    @Override
    public void setupJob(JobContext jobContext) throws IOException {
    }

    @Override
    public void setupTask(TaskAttemptContext taskContext) throws IOException {
    }

    /**
     * Did this task write any files in the work directory?
     * @param context the task's context
     */
    @Override
    public boolean needsTaskCommit(TaskAttemptContext taskContext) throws IOException {
        return false;
    }

    @Override
    public void commitTask(TaskAttemptContext taskContext) throws IOException {
    }

    @Override
    public void abortTask(TaskAttemptContext taskContext) throws IOException {
    }

    // If write Override annotation, it will fail on Apache Hadoop versions
    // which does not contain this method.
    @Deprecated
    public boolean isRecoverySupported() {
        return true;
    }

    public boolean isRecoverySupported(JobContext jobContext) throws IOException {
        return isRecoverySupported();
    }
}
\ No newline at end of file
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputFormat.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputFormat.java
new file mode 100644
index 000000000..9e18dd9d5
-- /dev/null
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/OozieLauncherOutputFormat.java
@@ -0,0 +1,48 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.action.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;

public class OozieLauncherOutputFormat implements OutputFormat<Object, Object>{

    @Override
    public RecordWriter<Object, Object> getRecordWriter(FileSystem ignored, JobConf job, String name,
            Progressable progress) throws IOException {
        return new RecordWriter<Object, Object>() {
            @Override
            public void write(Object key, Object value) throws IOException {
            }
            @Override
            public void close(Reporter reporter) throws IOException {
            }
        };
    }

    @Override
    public void checkOutputSpecs(FileSystem ignored, JobConf job) throws IOException {
    }
}
- 
2.19.1.windows.1

