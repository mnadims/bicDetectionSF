From 03060075c53a2cecfbf5f60b6fc77afecf64ace5 Mon Sep 17 00:00:00 2001
From: Andrew Wang <wang@apache.org>
Date: Mon, 10 Oct 2016 12:19:26 -0700
Subject: [PATCH] HADOOP-13699. Configuration does not substitute multiple
 references to the same var.

--
 .../org/apache/hadoop/conf/Configuration.java | 23 +++++++++---------
 .../apache/hadoop/conf/TestConfiguration.java | 24 ++++---------------
 2 files changed, 16 insertions(+), 31 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
index 1e8ed503c39..dbbc8ff20e8 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
@@ -943,10 +943,15 @@ private synchronized void addResourceObject(Resource resource) {
    *
    * If var is unbounded the current state of expansion "prefix${var}suffix" is
    * returned.
   *
   * If a cycle is detected: replacing var1 requires replacing var2 ... requires
   * replacing var1, i.e., the cycle is shorter than
   * {@link Configuration#MAX_SUBST} then the original expr is returned.
   * <p>
   * This function also detects self-referential substitutions, i.e.
   * <pre>
   *   {@code
   *   foo.bar = ${foo.bar}
   *   }
   * </pre>
   * If a cycle is detected then the original expr is returned. Loops
   * involving multiple substitutions are not detected.
    *
    * @param expr the literal value of a config key
    * @return null if expr is null, otherwise the value resulting from expanding
@@ -959,7 +964,6 @@ private String substituteVars(String expr) {
       return null;
     }
     String eval = expr;
    Set<String> evalSet = null;
     for(int s = 0; s < MAX_SUBST; s++) {
       final int[] varBounds = findSubVariable(eval);
       if (varBounds[SUB_START_IDX] == -1) {
@@ -1004,15 +1008,12 @@ private String substituteVars(String expr) {
         return eval; // return literal ${var}: var is unbound
       }
 
      // prevent recursive resolution
      //
       final int dollar = varBounds[SUB_START_IDX] - "${".length();
       final int afterRightBrace = varBounds[SUB_END_IDX] + "}".length();
       final String refVar = eval.substring(dollar, afterRightBrace);
      if (evalSet == null) {
        evalSet = new HashSet<String>();
      }
      if (!evalSet.add(refVar)) {

      // detect self-referential values
      if (val.contains(refVar)) {
         return expr; // return original expression if there is a loop
       }
 
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
index 917ccbce29c..17112f5c9f6 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
@@ -168,6 +168,9 @@ public void testVariableSubstitution() throws IOException {
     declareProperty("my.fullfile", "${my.base}/${my.file}${my.suffix}", "/tmp/hadoop_user/hello.txt");
     // check that undefined variables are returned as-is
     declareProperty("my.failsexpand", "a${my.undefvar}b", "a${my.undefvar}b");
    // check that multiple variable references are resolved
    declareProperty("my.user.group", "${user.name} ${user.name}",
        "hadoop_user hadoop_user");
     endConfig();
     Path fileResource = new Path(CONFIG);
     mock.addResource(fileResource);
@@ -1508,7 +1511,7 @@ public void testSettingKeyNull() throws Exception {
     }
   }
 
  public void testInvalidSubstitutation() {
  public void testInvalidSubstitution() {
     final Configuration configuration = new Configuration(false);
 
     // 2-var loops
@@ -1522,25 +1525,6 @@ public void testInvalidSubstitutation() {
       configuration.set(key, keyExpression);
       assertEquals("Unexpected value", keyExpression, configuration.get(key));
     }

    //
    // 3-variable loops
    //

    final String expVal1 = "${test.var2}";
    String testVar1 = "test.var1";
    configuration.set(testVar1, expVal1);
    configuration.set("test.var2", "${test.var3}");
    configuration.set("test.var3", "${test.var1}");
    assertEquals("Unexpected value", expVal1, configuration.get(testVar1));

    // 3-variable loop with non-empty value prefix/suffix
    //
    final String expVal2 = "foo2${test.var2}bar2";
    configuration.set(testVar1, expVal2);
    configuration.set("test.var2", "foo3${test.var3}bar3");
    configuration.set("test.var3", "foo1${test.var1}bar1");
    assertEquals("Unexpected value", expVal2, configuration.get(testVar1));
   }
 
   public void testIncompleteSubbing() {
- 
2.19.1.windows.1

