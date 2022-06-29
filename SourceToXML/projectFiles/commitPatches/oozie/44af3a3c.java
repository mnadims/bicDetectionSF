From 44af3a3cbeacd2d5cc602a39556c53c375f4747b Mon Sep 17 00:00:00 2001
From: Shwetha GS <sshivalingamurthy@hortonworks.com>
Date: Mon, 27 Apr 2015 14:19:11 +0530
Subject: [PATCH] OOZIE-2129 fixed build failure

--
 .../apache/oozie/action/hadoop/ShellMain.java | 28 -------------------
 1 file changed, 28 deletions(-)

diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/ShellMain.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/ShellMain.java
index e1c5a166e..3f5391579 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/ShellMain.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/ShellMain.java
@@ -290,32 +290,4 @@ public class ShellMain extends LauncherMain {
         }
         return exec;
     }

    /**
     * Read action configuration passes through action xml file.
     *
     * @return action  Configuration
     * @throws IOException
     */
    protected Configuration loadActionConf() throws IOException {
        System.out.println();
        System.out.println("Oozie Shell action configuration");
        System.out.println("=================================================================");

        // loading action conf prepared by Oozie
        Configuration actionConf = new Configuration(false);

        String actionXml = System.getProperty("oozie.action.conf.xml");

        if (actionXml == null) {
            throw new RuntimeException("Missing Java System Property [oozie.action.conf.xml]");
        }
        if (!new File(actionXml).exists()) {
            throw new RuntimeException("Action Configuration XML file [" + actionXml + "] does not exist");
        }

        actionConf.addResource(new Path("file:///", actionXml));
        logMasking("Shell configuration:", new HashSet<String>(), actionConf);
        return actionConf;
    }
 }
- 
2.19.1.windows.1

