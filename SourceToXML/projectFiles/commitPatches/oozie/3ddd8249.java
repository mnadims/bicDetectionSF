From 3ddd8249c04f29e66074da1f856f8057867f0bdd Mon Sep 17 00:00:00 2001
From: Bowen Zhang <bowenzhangusa@yahoo.com>
Date: Fri, 1 Aug 2014 10:02:17 -0700
Subject: [PATCH] OOZIE-1930 oozie coordinator "-info desc" returns earliest
 instead of latest actions when specifying "len" after oozie-1532

--
 .../org/apache/oozie/CoordinatorEngine.java   |  4 +--
 .../oozie/command/coord/CoordJobXCommand.java | 14 ++++----
 .../CoordJobGetActionsSubsetJPAExecutor.java  | 33 ++++---------------
 .../apache/oozie/servlet/V1JobServlet.java    |  6 ++--
 release-log.txt                               |  1 +
 5 files changed, 19 insertions(+), 39 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
index 1011b9e66..dd5c70328 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
@@ -158,11 +158,11 @@ public class CoordinatorEngine extends BaseEngine {
      * @see org.apache.oozie.BaseEngine#getCoordJob(java.lang.String, java.lang.String, int, int)
      */
     @Override
    public CoordinatorJobBean getCoordJob(String jobId, String filter, int start, int length, boolean desc)
    public CoordinatorJobBean getCoordJob(String jobId, String filter, int offset, int length, boolean desc)
             throws BaseEngineException {
         Map<String, List<String>> filterMap = parseStatusFilter(filter);
         try {
            return new CoordJobXCommand(jobId, filterMap, start, length, desc)
            return new CoordJobXCommand(jobId, filterMap, offset, length, desc)
                     .call();
         }
         catch (CommandException ex) {
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordJobXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordJobXCommand.java
index c872c4849..5eaf06214 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordJobXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordJobXCommand.java
@@ -18,7 +18,6 @@
 package org.apache.oozie.command.coord;
 
 import java.util.ArrayList;
import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 
@@ -41,7 +40,7 @@ import org.apache.oozie.util.ParamChecker;
 public class CoordJobXCommand extends CoordinatorXCommand<CoordinatorJobBean> {
     private final String id;
     private final boolean getActionInfo;
    private int start = 1;
    private int offset = 1;
     private int len = Integer.MAX_VALUE;
     private boolean desc = false;
     private Map<String, List<String>> filterMap;
@@ -59,16 +58,17 @@ public class CoordJobXCommand extends CoordinatorXCommand<CoordinatorJobBean> {
      * Constructor for loading a coordinator job information
      *
      * @param id coord jobId
     * @param start starting index in the list of actions belonging to the job
     * @param offset starting index in the list of actions belonging to the job
      * @param length number of actions to be returned
     * @param filetrList
     * @param filterMap
     * @param desc boolean for whether the actions returned are in descending order
      */
    public CoordJobXCommand(String id, Map<String, List<String>> filterMap, int start, int length, boolean desc) {
    public CoordJobXCommand(String id, Map<String, List<String>> filterMap, int offset, int length, boolean desc) {
         super("job.info", "job.info", 1);
         this.id = ParamChecker.notEmpty(id, "id");
         this.getActionInfo = true;
         this.filterMap = filterMap;
        this.start = start;
        this.offset = offset;
         this.len = length;
         this.desc = desc;
     }
@@ -132,7 +132,7 @@ public class CoordJobXCommand extends CoordinatorXCommand<CoordinatorJobBean> {
                         coordActions = new ArrayList<CoordinatorActionBean>();
                     }
                     else {
                        coordActions = jpaService.execute(new CoordJobGetActionsSubsetJPAExecutor(id, filterMap, start,
                        coordActions = jpaService.execute(new CoordJobGetActionsSubsetJPAExecutor(id, filterMap, offset,
                                 len, desc));
                     }
                     coordJob.setActions(coordActions);
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java
index 420a466ae..a531798f0 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsSubsetJPAExecutor.java
@@ -35,12 +35,12 @@ import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ParamChecker;
 
 /**
 * Load coordinator actions by start and len (a subset) for a coordinator job.
 * Load coordinator actions by offset and len (a subset) for a coordinator job.
  */
 public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<CoordinatorActionBean>> {
 
     private String coordJobId = null;
    private int start = 1;
    private int offset = 1;
     private int len = 50;
     private boolean desc = false;
     private Map<String,List<String>> filterMap;
@@ -51,10 +51,10 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
     }
 
     public CoordJobGetActionsSubsetJPAExecutor(String coordJobId, Map<String, List<String>> filterMap,
            int start, int len, boolean desc) {
            int offset, int len, boolean desc) {
         this(coordJobId);
         this.filterMap = filterMap;
        this.start = start;
        this.offset = offset;
         this.len = len;
         this.desc = desc;
     }
@@ -101,7 +101,6 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
             StringBuilder statusClause = new StringBuilder();
             getStatusClause(statusClause, filterMap.get(CoordinatorEngine.POSITIVE_FILTER), true);
             getStatusClause(statusClause, filterMap.get(CoordinatorEngine.NEGATIVE_FILTER), false);
            getIdClause(statusClause);
             // Insert 'where' before 'order by'
             sbTotal.insert(offset, statusClause);
             q = em.createQuery(sbTotal.toString());
@@ -109,7 +108,8 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
         if (desc) {
             q = em.createQuery(q.toString().concat(" desc"));
         }
        q.setParameter("jobId", coordJobId);;
        q.setParameter("jobId", coordJobId);
        q.setFirstResult(offset - 1);
         q.setMaxResults(len);
         return q;
     }
@@ -140,27 +140,6 @@ public class CoordJobGetActionsSubsetJPAExecutor implements JPAExecutor<List<Coo
         return sb;
     }
 
    // Form the where clause for coord action ids
    private StringBuilder getIdClause(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.append("and a.id IN (");
        boolean isFirst = true;
        for (int i = start; i < start + len; i++) {
            if (isFirst) {
                sb.append("\'").append(coordJobId).append("@").append(i).append("\'");
                isFirst = false;
            }
            else {
                sb.append(", \'").append(coordJobId).append("@").append(i).append("\'");
            }
        }
        sb.append(") ");

        return sb;
    }

     private CoordinatorActionBean getBeanForRunningCoordAction(Object arr[]) {
         CoordinatorActionBean bean = new CoordinatorActionBean();
         if (arr[0] != null) {
diff --git a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
index 8a3476324..855fabcb1 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
@@ -820,14 +820,14 @@ public class V1JobServlet extends BaseJobServlet {
         String filter = request.getParameter(RestConstants.JOB_FILTER_PARAM);
         String orderStr = request.getParameter(RestConstants.ORDER_PARAM);
         boolean order = (orderStr != null && orderStr.equals("desc")) ? true : false;
        int start = (startStr != null) ? Integer.parseInt(startStr) : 1;
        start = (start < 1) ? 1 : start;
        int offset = (startStr != null) ? Integer.parseInt(startStr) : 1;
        offset = (offset < 1) ? 1 : offset;
         // Get default number of coordinator actions to be retrieved
         int defaultLen = Services.get().getConf().getInt(COORD_ACTIONS_DEFAULT_LENGTH, 1000);
         int len = (lenStr != null) ? Integer.parseInt(lenStr) : 0;
         len = getCoordinatorJobLength(defaultLen, len);
         try {
            CoordinatorJobBean coordJob = coordEngine.getCoordJob(jobId, filter, start, len, order);
            CoordinatorJobBean coordJob = coordEngine.getCoordJob(jobId, filter, offset, len, order);
             jobBean = coordJob;
         }
         catch (CoordinatorEngineException ex) {
diff --git a/release-log.txt b/release-log.txt
index 869931427..6462556f0 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -5,6 +5,7 @@ OOZIE-1943 Bump up trunk to 4.2.0-SNAPSHOT (bzhang)
 
 -- Oozie 4.1.0 release (4.1 - unreleased)
 
OOZIE-1930 oozie coordinator "-info desc" returns earliest instead of latest actions when specifying "len" after oozie-1532 (bzhang)
 OOZIE-1944 Recursive variable resolution broken when same parameter name in config-default and action conf (mona)
 OOZIE-1906 Service to periodically remove ZK lock (puru via rohini)
 OOZIE-1812 Bulk API with bundle Id should relax regex check for Id (puru via rohini)
- 
2.19.1.windows.1

