From c5f5f9d6bae8ab44b54f728ebee15851478fa272 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Fri, 27 May 2016 13:07:27 -0700
Subject: [PATCH] OOZIE-2475 Oozie does not cleanup action dir of killed
 actions (satishsaley via rohini)

--
 .../oozie/command/wf/ActionKillXCommand.java  | 46 +++++++++++++++----
 release-log.txt                               |  1 +
 2 files changed, 39 insertions(+), 8 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
index 33498bfc7..ac096cc49 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/ActionKillXCommand.java
@@ -22,33 +22,37 @@ import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.SLAEventBean;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.XException;
import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutor.Context;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.action.control.ControlNodeActionExecutor;
 import org.apache.oozie.client.SLAEvent.SlaAppType;
 import org.apache.oozie.client.SLAEvent.Status;
 import org.apache.oozie.client.rest.JsonBean;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.action.control.ControlNodeActionExecutor;
 import org.apache.oozie.service.ActionService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.UUIDService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.util.LogUtils;
import org.apache.oozie.service.UUIDService;
 import org.apache.oozie.util.Instrumentation;
import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.util.db.SLADbXOperations;
 
 /**
@@ -129,10 +133,11 @@ public class ActionKillXCommand extends ActionXCommand<Void> {
         if (wfAction.isPending()) {
             ActionExecutor executor = Services.get().get(ActionService.class).getExecutor(wfAction.getType());
             if (executor != null) {
                ActionExecutorContext context = null;
                 try {
                     boolean isRetry = false;
                     boolean isUserRetry = false;
                    ActionExecutorContext context = new ActionXCommand.ActionExecutorContext(wfJob, wfAction,
                    context = new ActionXCommand.ActionExecutorContext(wfJob, wfAction,
                             isRetry, isUserRetry);
                     incrActionCounter(wfAction.getType(), 1);
 
@@ -179,6 +184,7 @@ public class ActionKillXCommand extends ActionXCommand<Void> {
                 }
                 finally {
                     try {
                        cleanupActionDir(context);
                         BatchQueryExecutor.getInstance().executeBatchInsertUpdateDelete(insertList, updateList, null);
                         if (!(executor instanceof ControlNodeActionExecutor) && EventHandlerService.isEnabled()) {
                             generateEvent(wfAction, wfJob.getUser());
@@ -194,4 +200,28 @@ public class ActionKillXCommand extends ActionXCommand<Void> {
         return null;
     }
 
    /*
     * Cleans up the action directory
     */
    private void cleanupActionDir(Context context) {
        try {
            FileSystem actionFs = context.getAppFileSystem();
            Path actionDir = context.getActionDir();
            Path jobDir = actionDir.getParent();
            if (!context.getProtoActionConf().getBoolean("oozie.action.keep.action.dir", false)
                    && actionFs.exists(actionDir)) {
                actionFs.delete(actionDir, true);
            }
            if (actionFs.exists(jobDir) && actionFs.getFileStatus(jobDir).isDir()) {
                FileStatus[] statuses = actionFs.listStatus(jobDir);
                if (statuses == null || statuses.length == 0) {
                    actionFs.delete(jobDir, true);
                }
            }
        }
        catch (Exception e) {
            LOG.warn("Exception while cleaning up action dir. Message[{1}]", e.getMessage(), e);
        }
    }

 }
diff --git a/release-log.txt b/release-log.txt
index 1c8d11b41..02b3bc87f 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2475 Oozie does not cleanup action dir of killed actions (satishsaley via rohini)
 OOZIE-2535 User can't disable uber mode (puru)
 OOZIE-2482 Pyspark job fails with Oozie (satishsaley and gezapeti via rkanter)
 OOZIE-2467 Oozie can shutdown itself on long GC pause (puru)
- 
2.19.1.windows.1

