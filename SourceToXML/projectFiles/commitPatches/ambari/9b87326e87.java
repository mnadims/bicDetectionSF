From 9b87326e877311927b5d49999b76d7c5733b50ae Mon Sep 17 00:00:00 2001
From: Jayush Luniya <jluniya@hortonworks.com>
Date: Mon, 2 Nov 2015 22:20:21 -0800
Subject: [PATCH] AMBARI-13687: Express Upgrade: Install Packages is throwing
 exception Caught a system exception while attempting to create a resource
 (jluniya)

--
 .../ambari/server/api/handlers/CreateHandler.java    |  2 +-
 .../ClusterStackVersionResourceProvider.java         | 12 +++++++-----
 2 files changed, 8 insertions(+), 6 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/api/handlers/CreateHandler.java b/ambari-server/src/main/java/org/apache/ambari/server/api/handlers/CreateHandler.java
index c1bd91b011..9690bcd013 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/api/handlers/CreateHandler.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/api/handlers/CreateHandler.java
@@ -54,7 +54,7 @@ public class CreateHandler extends BaseManagementHandler {
       result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, e.getMessage()));
     } catch (SystemException e) {
       if (LOG.isErrorEnabled()) {
        LOG.error("Caught a system exception while attempting to create a resource", e.getMessage());
        LOG.error("Caught a system exception while attempting to create a resource: {}", e.getMessage());
       }
       result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.SERVER_ERROR, e.getMessage()));
     } catch (ResourceAlreadyExistsException e) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
index adae1055de..22212bfcb6 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/ClusterStackVersionResourceProvider.java
@@ -383,10 +383,10 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
       // Create next stage
       String stageName;
       if (batchCount > 1) {
        stageName = INSTALL_PACKAGES_FULL_NAME;
      } else {
         stageName = String.format(INSTALL_PACKAGES_FULL_NAME + ". Batch %d of %d", batchId,
             batchCount);
      } else {
        stageName = INSTALL_PACKAGES_FULL_NAME;
       }
 
       Stage stage = stageFactory.createNew(req.getId(), "/tmp/ambari", cluster.getClusterName(),
@@ -414,7 +414,6 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
         } else {
           directTransitions.add(host);
         }

       }
     }
 
@@ -550,8 +549,11 @@ public class ClusterStackVersionResourceProvider extends AbstractControllerResou
         componentInfo = ami.getComponent(stackId.getStackName(),
                 stackId.getStackVersion(), component.getServiceName(), component.getServiceComponentName());
       } catch (AmbariException e) {
        throw new SystemException(String.format("Exception while accessing component %s of service %s for stack %s",
                component.getServiceName(), component.getServiceComponentName(), stackId));
        // It is possible that the component has been removed from the new stack
        // (example: STORM_REST_API has been removed from HDP-2.2)
        LOG.warn(String.format("Exception while accessing component %s of service %s for stack %s",
            component.getServiceComponentName(), component.getServiceName(), stackId));
        continue;
       }
       if (componentInfo.isVersionAdvertised()) {
         return true;
- 
2.19.1.windows.1

