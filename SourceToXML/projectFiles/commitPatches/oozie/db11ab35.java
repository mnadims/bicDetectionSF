From db11ab356af7c29478e41441bd2325dfdb5130c0 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Mon, 23 Jun 2014 13:52:06 -0700
Subject: [PATCH] OOZIE-1878 Can't execute dryrun on the CLI (puru via rohini)

--
 .../java/org/apache/oozie/cli/CLIParser.java  | 50 +++++++++++-
 .../org/apache/oozie/client/TestOozieCLI.java | 79 ++++++++++++++++++-
 .../servlet/MockCoordinatorEngineService.java | 16 ++++
 release-log.txt                               |  1 +
 4 files changed, 144 insertions(+), 2 deletions(-)

diff --git a/client/src/main/java/org/apache/oozie/cli/CLIParser.java b/client/src/main/java/org/apache/oozie/cli/CLIParser.java
index 6d490f275..c8b16814c 100644
-- a/client/src/main/java/org/apache/oozie/cli/CLIParser.java
++ b/client/src/main/java/org/apache/oozie/cli/CLIParser.java
@@ -17,12 +17,15 @@
  */
 package org.apache.oozie.cli;
 
import org.apache.commons.cli.MissingOptionException;
 import org.apache.commons.cli.Options;
 import org.apache.commons.cli.GnuParser;
 import org.apache.commons.cli.ParseException;
 import org.apache.commons.cli.CommandLine;
 import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.UnrecognizedOptionException;
 
import java.util.Arrays;
 import java.util.Map;
 import java.util.LinkedHashMap;
 import java.text.MessageFormat;
@@ -114,9 +117,18 @@ public class CLIParser {
         }
         else {
             if (commands.containsKey(args[0])) {
                GnuParser parser = new GnuParser();
                GnuParser parser ;
                 String[] minusCommand = new String[args.length - 1];
                 System.arraycopy(args, 1, minusCommand, 0, minusCommand.length);

                if (args[0].equals(OozieCLI.JOB_CMD)) {
                    validdateArgs(args, minusCommand);
                    parser = new OozieGnuParser(true);
                }
                else {
                    parser = new OozieGnuParser(false);
                }

                 return new Command(args[0], parser.parse(commands.get(args[0]), minusCommand,
                                                          commandWithArgs.get(args[0])));
             }
@@ -126,6 +138,23 @@ public class CLIParser {
         }
     }
 
    public void validdateArgs(final String[] args, String[] minusCommand) throws ParseException {
        try {
            GnuParser parser = new OozieGnuParser(false);
            parser.parse(commands.get(args[0]), minusCommand, commandWithArgs.get(args[0]));
        }
        catch (MissingOptionException e) {
            if (Arrays.toString(args).contains("-dryrun")) {
                // ignore this, else throw exception
                //Dryrun is also part of update sub-command. CLI parses dryrun as sub-command and throws
                //Missing Option Exception, if -dryrun is used as command. It's ok to skip exception only for dryrun.
            }
            else {
                throw e;
            }
        }
    }

     public String shortHelp() {
         return "use 'help [sub-command]' for help details";
     }
@@ -164,5 +193,24 @@ public class CLIParser {
         pw.flush();
     }
 
    static class OozieGnuParser extends GnuParser {
        private boolean ignoreMissingOption;

        public OozieGnuParser(final boolean ignoreMissingOption) {
            this.ignoreMissingOption = ignoreMissingOption;
        }

        @Override
        protected void checkRequiredOptions() throws MissingOptionException {
            if (ignoreMissingOption) {
                return;
            }
            else {
                super.checkRequiredOptions();
            }
        }
    }

 }
 

diff --git a/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java b/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java
index 8a85cd26a..a8f8cf9b8 100644
-- a/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java
++ b/core/src/test/java/org/apache/oozie/client/TestOozieCLI.java
@@ -28,7 +28,6 @@ import java.util.concurrent.Callable;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.Path;
import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.cli.OozieCLI;
 import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.servlet.DagServletTestCase;
@@ -79,6 +78,16 @@ public class TestOozieCLI extends DagServletTestCase {
         return path;
     }
 
    private String createCoodrConfigFile(String appPath) throws Exception {
        String path = getTestCaseDir() + "/" + getName() + ".xml";
        Configuration conf = new Configuration(false);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPath);
        OutputStream os = new FileOutputStream(path);
        conf.writeXml(os);
        os.close();
        return path;
    }

     private String createPropertiesFile(String appPath) throws Exception {
         String path = getTestCaseDir() + "/" + getName() + ".properties";
         Properties props = new Properties();
@@ -1149,4 +1158,72 @@ public class TestOozieCLI extends DagServletTestCase {
         });
     }
 
    public void testJobDryrun() throws Exception {
        runTest(END_POINTS, SERVLET_CLASSES, false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HeaderTestingVersionServlet.OOZIE_HEADERS.clear();
                Path appPath = new Path(getFsTestCaseDir(), "app");
                getFileSystem().mkdirs(appPath);
                getFileSystem().create(new Path(appPath, "coordinator.xml")).close();
                String oozieUrl = getContextURL();
                String[] args = new String[] { "job", "-dryrun", "-config", createCoodrConfigFile(appPath.toString()),
                        "-oozie", oozieUrl, "-Doozie.proxysubmission=true" };
                assertEquals(0, new OozieCLI().run(args));
                assertEquals(MockCoordinatorEngineService.did, RestConstants.JOB_ACTION_DRYRUN);
                assertFalse(MockCoordinatorEngineService.started.get(1));
                return null;
            }
        });
    }

    public void testUpdate() throws Exception {
        runTest(END_POINTS, SERVLET_CLASSES, false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HeaderTestingVersionServlet.OOZIE_HEADERS.clear();
                String oozieUrl = getContextURL();
                String[] args = new String[] { "job", "-update", "aaa", "-oozie", oozieUrl };
                assertEquals(-1, new OozieCLI().run(args));
                assertEquals(MockCoordinatorEngineService.did, RestConstants.JOB_COORD_UPDATE );
                assertFalse(MockCoordinatorEngineService.started.get(1));
                return null;
            }
        });

    }

    public void testUpdateWithDryrun() throws Exception {
        runTest(END_POINTS, SERVLET_CLASSES, false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HeaderTestingVersionServlet.OOZIE_HEADERS.clear();

                String oozieUrl = getContextURL();
                String[] args = new String[] { "job", "-update", "aaa", "-dryrun", "-oozie", oozieUrl };
                assertEquals(-1, new OozieCLI().run(args));
                assertEquals(MockCoordinatorEngineService.did, RestConstants.JOB_COORD_UPDATE + "&"
                        + RestConstants.JOB_ACTION_DRYRUN);
                assertFalse(MockCoordinatorEngineService.started.get(1));
                return null;
            }
        });

    }

    public void testFailNoArg() throws Exception {
        runTest(END_POINTS, SERVLET_CLASSES, false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                HeaderTestingVersionServlet.OOZIE_HEADERS.clear();

                String oozieUrl = getContextURL();
                String[] args = new String[] { "job", "-oozie", oozieUrl };
                assertEquals(-1, new OozieCLI().run(args));
                return null;
            }
        });

    }

 }
diff --git a/core/src/test/java/org/apache/oozie/servlet/MockCoordinatorEngineService.java b/core/src/test/java/org/apache/oozie/servlet/MockCoordinatorEngineService.java
index 80ab512f5..638c28d17 100644
-- a/core/src/test/java/org/apache/oozie/servlet/MockCoordinatorEngineService.java
++ b/core/src/test/java/org/apache/oozie/servlet/MockCoordinatorEngineService.java
@@ -37,6 +37,8 @@ import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.CoordinatorJob;
 import org.apache.oozie.client.CoordinatorJob.Execution;
 import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordUpdateXCommand;
 import org.apache.oozie.service.CoordinatorEngineService;
 import org.apache.oozie.util.DateUtils;
 
@@ -60,6 +62,7 @@ public class MockCoordinatorEngineService extends CoordinatorEngineService {
     public static List<Boolean> started;
     public static final int INIT_COORD_COUNT = 4;
 

     static {
         reset();
     }
@@ -226,6 +229,19 @@ public class MockCoordinatorEngineService extends CoordinatorEngineService {
             writer.write(LOG);
         }
 
        @Override
        public String updateJob(Configuration conf, String jobId, boolean dryrun, boolean showDiff)
                throws CoordinatorEngineException {
            if (dryrun) {
                did = RestConstants.JOB_COORD_UPDATE + "&" + RestConstants.JOB_ACTION_DRYRUN;
            }
            else {
                did = RestConstants.JOB_COORD_UPDATE;
            }
            validateCoordinatorIdx(jobId);
            return "";
        }

         private int validateCoordinatorIdx(String jobId) throws CoordinatorEngineException {
             int idx = -1;
             try {
diff --git a/release-log.txt b/release-log.txt
index d1b209c43..fef34534e 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1878 Can't execute dryrun on the CLI (puru via rohini)
 OOZIE-1741 Add new coord EL function to get input partitions value string (satish.mittal via rohini) 
 OOZIE-1817 Oozie timers are not biased (rkanter)
 OOZIE-1807 Make bundle change command synchronous (puru via rohini)
- 
2.19.1.windows.1

