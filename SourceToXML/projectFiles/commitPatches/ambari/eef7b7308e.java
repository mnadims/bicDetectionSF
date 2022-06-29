From eef7b7308e33a9da3639611d2e4d47e52f25671c Mon Sep 17 00:00:00 2001
From: Nishant <nishant.monu51@gmail.com>
Date: Wed, 30 Aug 2017 23:43:56 +0530
Subject: [PATCH] AMBARI-21076. ADDENDUM. Delete the remaining DRUID files
 after moving superset as an independent project. (Nishant Bangarwa via Swapan
 Shridhar).

--
 .../0.15.0/configuration/superset-env.xml}                        | 0
 .../0.15.0/configuration/superset.xml}                            | 0
 .../{DRUID/0.9.2 => SUPERSET/0.15.0}/package/scripts/superset.py  | 0
 .../0.9.2 => SUPERSET/0.15.0}/package/templates/superset.sh       | 0
 4 files changed, 0 insertions(+), 0 deletions(-)
 rename ambari-server/src/main/resources/common-services/{DRUID/0.9.2/configuration/druid-superset-env.xml => SUPERSET/0.15.0/configuration/superset-env.xml} (100%)
 rename ambari-server/src/main/resources/common-services/{DRUID/0.9.2/configuration/druid-superset.xml => SUPERSET/0.15.0/configuration/superset.xml} (100%)
 rename ambari-server/src/main/resources/common-services/{DRUID/0.9.2 => SUPERSET/0.15.0}/package/scripts/superset.py (100%)
 rename ambari-server/src/main/resources/common-services/{DRUID/0.9.2 => SUPERSET/0.15.0}/package/templates/superset.sh (100%)

diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset-env.xml b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/configuration/superset-env.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset-env.xml
rename to ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/configuration/superset-env.xml
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset.xml b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/configuration/superset.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/DRUID/0.9.2/configuration/druid-superset.xml
rename to ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/configuration/superset.xml
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/superset.py b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/superset.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/scripts/superset.py
rename to ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/scripts/superset.py
diff --git a/ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/templates/superset.sh b/ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/templates/superset.sh
similarity index 100%
rename from ambari-server/src/main/resources/common-services/DRUID/0.9.2/package/templates/superset.sh
rename to ambari-server/src/main/resources/common-services/SUPERSET/0.15.0/package/templates/superset.sh
- 
2.19.1.windows.1

