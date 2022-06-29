From dce5964d8a19c288b34b7671bcfeb2844d83d882 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sat, 26 Nov 2016 12:24:45 +0530
Subject: [PATCH] SOLR-9784: removed unused method

--
 .../client/solrj/impl/CloudSolrClient.java    | 30 -------------------
 1 file changed, 30 deletions(-)

diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
index 241e2a145f1..6e4a256e8d5 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
@@ -1566,37 +1566,7 @@ public class CloudSolrClient extends SolrClient {
     
     return lbClient;
   }
  
  private static String buildZkHostString(Collection<String> zkHosts, String chroot) {
    if (zkHosts == null || zkHosts.isEmpty()) {
      throw new IllegalArgumentException("Cannot create CloudSearchClient without valid ZooKeeper host; none specified!");
    }
    
    StringBuilder zkBuilder = new StringBuilder();
    int lastIndexValue = zkHosts.size() - 1;
    int i = 0;
    for (String zkHost : zkHosts) {
      zkBuilder.append(zkHost);
      if (i < lastIndexValue) {
        zkBuilder.append(",");
      }
      i++;
    }
    if (chroot != null) {
      if (chroot.startsWith("/")) {
        zkBuilder.append(chroot);
      } else {
        throw new IllegalArgumentException(
            "The chroot must start with a forward slash.");
      }
    }
 
    /* Log the constructed connection string and then initialize. */
    final String zkHostString = zkBuilder.toString();
    log.debug("Final constructed zkHost string: " + zkHostString);
    return zkHostString;
  }
  
   /**
    * Constructs {@link CloudSolrClient} instances from provided configuration.
    */
- 
2.19.1.windows.1

