From 644548f201743408904dfe24b9f5b515b2c96713 Mon Sep 17 00:00:00 2001
From: Gera Shegalov <gera@apache.org>
Date: Fri, 30 Jan 2015 23:18:03 -0800
Subject: [PATCH] HADOOP-11506. Configuration variable expansion regex
 expensive for long values. (Gera Shegalov via gera)

--
 .../hadoop-common/CHANGES.txt                 |   3 +
 .../org/apache/hadoop/conf/Configuration.java | 124 +++++++++++++++---
 .../apache/hadoop/conf/TestConfiguration.java |  54 +++++++-
 3 files changed, 159 insertions(+), 22 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 44adf7f79a5..66c2cba70a5 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -573,6 +573,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11188. hadoop-azure: automatically expand page blobs when they become
     full. (Eric Hanson via cnauroth)
 
    HADOOP-11506. Configuration variable expansion regex expensive for long
    values. (Gera Shegalov via gera)

   BUG FIXES
 
     HADOOP-11488. Difference in default connection timeout for S3A FS
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
index 8f98d0a89e3..ea0d3a65880 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/conf/Configuration.java
@@ -187,7 +187,7 @@
     "testingforemptydefaultvalue";
 
   private boolean allowNullValueProperties = false;
  

   private static class Resource {
     private final Object resource;
     private final String name;
@@ -845,31 +845,106 @@ private synchronized void addResourceObject(Resource resource) {
     resources.add(resource);                      // add to resources
     reloadConfiguration();
   }
  
  private static final Pattern VAR_PATTERN =
      Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");
 
   private static final int MAX_SUBST = 20;
 
  private static final int SUB_START_IDX = 0;
  private static final int SUB_END_IDX = SUB_START_IDX + 1;

  /**
   * This is a manual implementation of the following regex
   * "\\$\\{[^\\}\\$\u0020]+\\}". It can be 15x more efficient than
   * a regex matcher as demonstrated by HADOOP-11506. This is noticeable with
   * Hadoop apps building on the assumption Configuration#get is an O(1)
   * hash table lookup, especially when the eval is a long string.
   *
   * @param eval a string that may contain variables requiring expansion.
   * @return a 2-element int array res such that
   * eval.substring(res[0], res[1]) is "var" for the left-most occurrence of
   * ${var} in eval. If no variable is found -1, -1 is returned.
   */
  private static int[] findSubVariable(String eval) {
    int[] result = {-1, -1};

    int matchStart;
    int leftBrace;

    // scanning for a brace first because it's less frequent than $
    // that can occur in nested class names
    //
    match_loop:
    for (matchStart = 1, leftBrace = eval.indexOf('{', matchStart);
         // minimum left brace position (follows '$')
         leftBrace > 0
         // right brace of a smallest valid expression "${c}"
         && leftBrace + "{c".length() < eval.length();
         leftBrace = eval.indexOf('{', matchStart)) {
      int matchedLen = 0;
      if (eval.charAt(leftBrace - 1) == '$') {
        int subStart = leftBrace + 1; // after '{'
        for (int i = subStart; i < eval.length(); i++) {
          switch (eval.charAt(i)) {
            case '}':
              if (matchedLen > 0) { // match
                result[SUB_START_IDX] = subStart;
                result[SUB_END_IDX] = subStart + matchedLen;
                break match_loop;
              }
              // fall through to skip 1 char
            case ' ':
            case '$':
              matchStart = i + 1;
              continue match_loop;
            default:
              matchedLen++;
          }
        }
        // scanned from "${"  to the end of eval, and no reset via ' ', '$':
        //    no match!
        break match_loop;
      } else {
        // not a start of a variable
        //
        matchStart = leftBrace + 1;
      }
    }
    return result;
  }

  /**
   * Attempts to repeatedly expand the value {@code expr} by replacing the
   * left-most substring of the form "${var}" in the following precedence order
   * <ol>
   *   <li>by the value of the Java system property "var" if defined</li>
   *   <li>by the value of the configuration key "var" if defined</li>
   * </ol>
   *
   * If var is unbounded the current state of expansion "prefix${var}suffix" is
   * returned.
   *
   * If a cycle is detected: replacing var1 requires replacing var2 ... requires
   * replacing var1, i.e., the cycle is shorter than
   * {@link Configuration#MAX_SUBST} then the original expr is returned.
   *
   * @param expr the literal value of a config key
   * @return null if expr is null, otherwise the value resulting from expanding
   * expr using the algorithm above.
   * @throws IllegalArgumentException when more than
   * {@link Configuration#MAX_SUBST} replacements are required
   */
   private String substituteVars(String expr) {
     if (expr == null) {
       return null;
     }
    Matcher match = VAR_PATTERN.matcher("");
     String eval = expr;
    Set<String> evalSet = new HashSet<String>();
    for(int s=0; s<MAX_SUBST; s++) {
      if (evalSet.contains(eval)) {
        // Cyclic resolution pattern detected. Return current expression.
    Set<String> evalSet = null;
    for(int s = 0; s < MAX_SUBST; s++) {
      final int[] varBounds = findSubVariable(eval);
      if (varBounds[SUB_START_IDX] == -1) {
         return eval;
       }
      evalSet.add(eval);
      match.reset(eval);
      if (!match.find()) {
        return eval;
      }
      String var = match.group();
      var = var.substring(2, var.length()-1); // remove ${ .. }
      final String var = eval.substring(varBounds[SUB_START_IDX],
          varBounds[SUB_END_IDX]);
       String val = null;
       try {
         val = System.getProperty(var);
@@ -882,8 +957,23 @@ private String substituteVars(String expr) {
       if (val == null) {
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
        return expr; // return original expression if there is a loop
      }

       // substitute
      eval = eval.substring(0, match.start())+val+eval.substring(match.end());
      eval = eval.substring(0, dollar)
             + val
             + eval.substring(afterRightBrace);
     }
     throw new IllegalStateException("Variable substitution depth too large: " 
                                     + MAX_SUBST + " " + expr);
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
index 7b4fbb5ba34..b84045d25eb 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/conf/TestConfiguration.java
@@ -1271,12 +1271,56 @@ public void testSettingKeyNull() throws Exception {
   }
 
   public void testInvalidSubstitutation() {
    final Configuration configuration = new Configuration(false);

    // 2-var loops
    //
    final String key = "test.random.key";
    for (String keyExpression : Arrays.asList(
        "${" + key + "}",
        "foo${" + key + "}",
        "foo${" + key + "}bar",
        "${" + key + "}bar")) {
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
    Configuration configuration = new Configuration(false);
     String key = "test.random.key";
    String keyExpression = "${" + key + "}";
    Configuration configuration = new Configuration();
    configuration.set(key, keyExpression);
    String value = configuration.get(key);
    assertTrue("Unexpected value " + value, value.equals(keyExpression));
    for (String keyExpression : Arrays.asList(
        "{}",
        "${}",
        "{" + key,
        "${" + key,
        "foo${" + key,
        "foo${" + key + "bar",
        "foo{" + key + "}bar",
        "${" + key + "bar")) {
      configuration.set(key, keyExpression);
      String value = configuration.get(key);
      assertTrue("Unexpected value " + value, value.equals(keyExpression));
    }
   }
 
   public void testBoolean() {
- 
2.19.1.windows.1

