From 8103901558938b017d06a03010b485818d3f8d0f Mon Sep 17 00:00:00 2001
From: Andras Piros <andras.piros@cloudera.com>
Date: Tue, 31 Jul 2018 10:53:29 +0200
Subject: [PATCH] OOZIE-2942 [examples] Fix Findbugs warnings (Jan Hentschel,
 kmarton via andras.piros)

--
 .../org/apache/oozie/example/DateList.java    | 118 +++---
 .../oozie/example/LocalOozieExample.java      |   5 +-
 .../org/apache/oozie/example/Repeatable.java  | 347 +++++++++---------
 release-log.txt                               |   1 +
 4 files changed, 234 insertions(+), 237 deletions(-)

diff --git a/examples/src/main/java/org/apache/oozie/example/DateList.java b/examples/src/main/java/org/apache/oozie/example/DateList.java
index 7e574cbec..731fe4130 100644
-- a/examples/src/main/java/org/apache/oozie/example/DateList.java
++ b/examples/src/main/java/org/apache/oozie/example/DateList.java
@@ -29,72 +29,64 @@ import java.util.Properties;
 import java.util.TimeZone;
 
 public class DateList {
	private static final TimeZone UTC = getTimeZone("UTC");
	private static String DATE_LIST_SEPARATOR = ",";
    private static final TimeZone UTC = getTimeZone("UTC");
    private static String DATE_LIST_SEPARATOR = ",";
 
	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.out
					.println("Usage: java DateList <start_time>  <end_time> <frequency> <timeunit> <timezone>");
			System.out
					.println("Example: java DateList 2009-02-01T01:00Z 2009-02-01T02:00Z 15 MINUTES UTC");
			System.exit(1);
		}
		Date startTime = parseDateUTC(args[0]);
		Date endTime = parseDateUTC(args[1]);
		Repeatable rep = new Repeatable();
		rep.setBaseline(startTime);
		rep.setFrequency(Integer.parseInt(args[2]));
		rep.setTimeUnit(TimeUnit.valueOf(args[3]));
		rep.setTimeZone(getTimeZone(args[4]));
		Date date = null;
		int occurrence = 0;
		StringBuilder dateList = new StringBuilder();
		do {
			date = rep.getOccurrenceTime(startTime, occurrence++, null);
			if (!date.before(endTime)) {
				break;
			}
			if (occurrence > 1) {
				dateList.append(DATE_LIST_SEPARATOR);
			}
			dateList.append(formatDateUTC(date));
		} while (date != null);
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out
                    .println("Usage: java DateList <start_time>  <end_time> <frequency> <timeunit> <timezone>");
            System.out
                    .println("Example: java DateList 2009-02-01T01:00Z 2009-02-01T02:00Z 15 MINUTES UTC");
            System.exit(1);
        }
        Date startTime = parseDateUTC(args[0]);
        Date endTime = parseDateUTC(args[1]);
        Repeatable rep = new Repeatable();
        rep.setBaseline(startTime);
        rep.setFrequency(Integer.parseInt(args[2]));
        rep.setTimeUnit(TimeUnit.valueOf(args[3]));
        rep.setTimeZone(getTimeZone(args[4]));
        int occurrence = 0;
        Date date = rep.getOccurrenceTime(startTime, occurrence++, null);
        StringBuilder dateList = new StringBuilder();
        while (date != null && date.before(endTime)) {
            date = rep.getOccurrenceTime(startTime, occurrence++, null);
            if (occurrence > 1) {
                dateList.append(DATE_LIST_SEPARATOR);
            }
            dateList.append(formatDateUTC(date));
        }
 
		System.out.println("datelist :" + dateList+ ":");
		//Passing the variable to WF that could be referred by subsequent actions
		File file = new File(System.getProperty("oozie.action.output.properties"));
		Properties props = new Properties();
		props.setProperty("datelist", dateList.toString());
		OutputStream os = new FileOutputStream(file);
        	props.store(os, "");
        	os.close();
	}
        System.out.println("datelist :" + dateList+ ":");
        //Passing the variable to WF that could be referred by subsequent actions
        File file = new File(System.getProperty("oozie.action.output.properties"));
        Properties props = new Properties();
        props.setProperty("datelist", dateList.toString());
        try (OutputStream os = new FileOutputStream(file)) {
            props.store(os, "");
        }
    }
    //Utility methods
    private static DateFormat getISO8601DateFormat() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(UTC);
        return dateFormat;
    }
 
	//Utility methods
	private static DateFormat getISO8601DateFormat() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		dateFormat.setTimeZone(UTC);
		return dateFormat;
	}
    private static TimeZone getTimeZone(String tzId) {
        TimeZone tz = TimeZone.getTimeZone(tzId);
        if (!tz.getID().equals(tzId)) {
            throw new IllegalArgumentException("Invalid TimeZone: " + tzId);
        }
        return tz;
    }
 
	private static TimeZone getTimeZone(String tzId) {
		TimeZone tz = TimeZone.getTimeZone(tzId);
		if (!tz.getID().equals(tzId)) {
			throw new IllegalArgumentException("Invalid TimeZone: " + tzId);
		}
		return tz;
	}

	private static Date parseDateUTC(String s) throws Exception {
		return getISO8601DateFormat().parse(s);
	}
	private static String formatDateUTC(Date d) throws Exception {
		return (d != null) ? getISO8601DateFormat().format(d) : "NULL";
	}

	private static String formatDateUTC(Calendar c) throws Exception {
		return (c != null) ? formatDateUTC(c.getTime()) : "NULL";
	}
    private static Date parseDateUTC(String s) throws Exception {
        return getISO8601DateFormat().parse(s);
    }
 
    private static String formatDateUTC(Date d) throws Exception {
        return (d != null) ? getISO8601DateFormat().format(d) : "NULL";
    }
 }
diff --git a/examples/src/main/java/org/apache/oozie/example/LocalOozieExample.java b/examples/src/main/java/org/apache/oozie/example/LocalOozieExample.java
index c9f5697c2..7cb8ed253 100644
-- a/examples/src/main/java/org/apache/oozie/example/LocalOozieExample.java
++ b/examples/src/main/java/org/apache/oozie/example/LocalOozieExample.java
@@ -69,7 +69,9 @@ public class LocalOozieExample {
             conf.setProperty(OozieClient.APP_PATH, new Path(appUri, "workflow.xml").toString());
             // load additional workflow job parameters from properties file
             if (propertiesFile != null) {
                conf.load(new FileInputStream(propertiesFile));
                try (FileInputStream properties = new FileInputStream(propertiesFile)) {
                    conf.load(properties);
                }
             }
 
             // submit and start the workflow job
@@ -112,5 +114,4 @@ public class LocalOozieExample {
         }
         System.out.println();
     }

 }
diff --git a/examples/src/main/java/org/apache/oozie/example/Repeatable.java b/examples/src/main/java/org/apache/oozie/example/Repeatable.java
index ee8632518..198f3873f 100644
-- a/examples/src/main/java/org/apache/oozie/example/Repeatable.java
++ b/examples/src/main/java/org/apache/oozie/example/Repeatable.java
@@ -23,177 +23,180 @@ import java.util.TimeZone;
 import java.util.Calendar;
 
 public class Repeatable {
	private String name;
	private Date baseline;
	private TimeZone timeZone;
	private int frequency;
	private TimeUnit timeUnit;
	public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	/**
	 * Compute the occurrence number for the given nominal time using a TZ-DST
	 * sensitive frequency If nominal time is before baseline return -1
	 *
	 * @param nominalTime
	 *            :baseline time
	 * @param timeLimit
	 *            : Max end time
	 * @return occurrence number
	 */
	int getOccurrence(Date nominalTime, Date timeLimit) {
		int occurrence = -1;
		// ensure nominal time is greater than initial-instance
		long positiveDiff = nominalTime.getTime() - getBaseline().getTime();
		if (positiveDiff >= 0) {
			Calendar calendar = Calendar.getInstance(getTimeZone());
			calendar.setLenient(true);
			calendar.setTime(getBaseline());
			occurrence = 0;
			// starting from initial instance increment frequencies until
			// passing nominal time
			while (calendar.getTime().compareTo(nominalTime) < 0) {
				if (timeLimit != null
						&& calendar.getTime().compareTo(timeLimit) > 0) {
					return -1;
				}
				calendar.add(getTimeUnit().getCalendarUnit(), getFrequency());
				occurrence++;
			}
			// compute reminder delta between nominal time and closest greater
			// frequency tick time
			long nominalCurrentDelta = nominalTime.getTime()
					- calendar.getTime().getTime();
			// ensure that computed current is greater than initial-instance
			// the nominalCurrentDelta has to be used to cover the case when the
			// computed current
			// falls between (-1*f ... 0*f)
			positiveDiff = calendar.getTime().getTime()
					- getBaseline().getTime() + nominalCurrentDelta;
			if (positiveDiff < 0) {
				occurrence = -1;
			}
		}
		return occurrence;
	}

	/**
	 * Compute the occurrence number for the given nominal time using a TZ-DST
	 * sensitive frequency If nominal time is before baseline return -1
	 *
	 * @param nominalTime
	 *            :baseline time
	 * @return occurrence number
	 */
	public int getOccurrence(Date nominalTime) {
		return getOccurrence(nominalTime, null);
	}

	/**
	 * Compute the occurrence nominal time for the given nominal-time and
	 * occurrence-offset using a TZ-DST sensitive frequency If the computed
	 * occurrence is before baseline time returns NULL
	 *
	 * @param nominalTime
	 *            :baseline time
	 * @param occurrenceOffset
	 *            : offset
	 * @param timeLimit
	 *            : Max end time
	 * @return Date after 'occurrenceOffset' instance
	 */
	Date getOccurrenceTime(Date nominalTime, int occurrenceOffset,
			Date timeLimit) {
		Date date = null;
		int occurrence = getOccurrence(nominalTime, timeLimit);
		if (occurrence > -1) {
			occurrence += occurrenceOffset;
			occurrence = (occurrence >= 0) ? occurrence : -1;
		}
		if (occurrence > -1) {
			Calendar calendar = Calendar.getInstance(getTimeZone());
			calendar.setLenient(true);
			calendar.setTime(getBaseline());
			calendar.add(getTimeUnit().getCalendarUnit(), getFrequency()
					* occurrence);
			date = calendar.getTime();

		}
		return date;
	}

	/**
	 * Compute the occurrence nominal time for the given nominal-time and
	 * occurrence-offset using a TZ-DST sensitive frequency If the computed
	 * occurrence is before baseline time returns NULL
	 *
	 * @param nominalTime
	 *            :baseline time
	 * @param occurrenceOffset
	 *            : offset
	 * @return Date after 'occurrenceOffset' instance
	 */
	public Date getOccurrenceTime(Date nominalTime, int occurrenceOffset) {
		return getOccurrenceTime(nominalTime, occurrenceOffset, null);
	}

	/**
	 * computes the nominal time for the Nth occurrence of the Repeatable
	 *
	 * @param occurrence
	 *            : instance numbner
	 * @return TimeStamp of the Nth instance
	 */
	public Date getTime(int occurrence) {
		if (occurrence < 0) {
			throw new IllegalArgumentException("occurrence cannot be <0");
		}
		Calendar calendar = Calendar.getInstance(getTimeZone());
		calendar.setLenient(true);
		calendar.setTime(getBaseline());
		calendar.add(getTimeUnit().getCalendarUnit(), getFrequency()
				* occurrence);
		return calendar.getTime();
	}

	// Setters and getters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getBaseline() {
		return baseline;
	}

	public void setBaseline(Date baseline) {
		this.baseline = baseline;
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}
    private String name;
    private Date baseline;
    private TimeZone timeZone;
    private int frequency;
    private TimeUnit timeUnit;
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Compute the occurrence number for the given nominal time using a TZ-DST
     * sensitive frequency If nominal time is before baseline return -1
     *
     * @param nominalTime
     *            :baseline time
     * @param timeLimit
     *            : Max end time
     * @return occurrence number
     */
    int getOccurrence(Date nominalTime, Date timeLimit) {
        int occurrence = -1;
        // ensure nominal time is greater than initial-instance
        long positiveDiff = nominalTime.getTime() - getBaseline().getTime();
        if (positiveDiff >= 0) {
            Calendar calendar = Calendar.getInstance(getTimeZone());
            calendar.setLenient(true);
            calendar.setTime(getBaseline());
            occurrence = 0;
            // starting from initial instance increment frequencies until
            // passing nominal time
            while (calendar.getTime().compareTo(nominalTime) < 0) {
                if (timeLimit != null
                        && calendar.getTime().compareTo(timeLimit) > 0) {
                    return -1;
                }
                calendar.add(getTimeUnit().getCalendarUnit(), getFrequency());
                occurrence++;
            }
            // compute reminder delta between nominal time and closest greater
            // frequency tick time
            long nominalCurrentDelta = nominalTime.getTime()
                    - calendar.getTime().getTime();
            // ensure that computed current is greater than initial-instance
            // the nominalCurrentDelta has to be used to cover the case when the
            // computed current
            // falls between (-1*f ... 0*f)
            positiveDiff = calendar.getTime().getTime()
                    - getBaseline().getTime() + nominalCurrentDelta;
            if (positiveDiff < 0) {
                occurrence = -1;
            }
        }
        return occurrence;
    }

    /**
     * Compute the occurrence number for the given nominal time using a TZ-DST
     * sensitive frequency If nominal time is before baseline return -1
     *
     * @param nominalTime
     *            :baseline time
     * @return occurrence number
     */
    public int getOccurrence(Date nominalTime) {
        return getOccurrence(nominalTime, null);
    }

    /**
     * Compute the occurrence nominal time for the given nominal-time and
     * occurrence-offset using a TZ-DST sensitive frequency If the computed
     * occurrence is before baseline time returns NULL
     *
     * @param nominalTime
     *            :baseline time
     * @param occurrenceOffset
     *            : offset
     * @param timeLimit
     *            : Max end time
     * @return Date after 'occurrenceOffset' instance
     */
    Date getOccurrenceTime(Date nominalTime, int occurrenceOffset,
            Date timeLimit) {
        Date date = null;
        int occurrence = getOccurrence(nominalTime, timeLimit);
        if (occurrence > -1) {
            occurrence += occurrenceOffset;
            occurrence = (occurrence >= 0) ? occurrence : -1;
        }
        if (occurrence > -1) {
            Calendar calendar = Calendar.getInstance(getTimeZone());
            calendar.setLenient(true);
            calendar.setTime(getBaseline());
            calendar.add(getTimeUnit().getCalendarUnit(), getFrequency()
                    * occurrence);
            date = calendar.getTime();

        }
        return date;
    }

    /**
     * Compute the occurrence nominal time for the given nominal-time and
     * occurrence-offset using a TZ-DST sensitive frequency If the computed
     * occurrence is before baseline time returns NULL
     *
     * @param nominalTime
     *            :baseline time
     * @param occurrenceOffset
     *            : offset
     * @return Date after 'occurrenceOffset' instance
     */
    public Date getOccurrenceTime(Date nominalTime, int occurrenceOffset) {
        return getOccurrenceTime(nominalTime, occurrenceOffset, null);
    }

    /**
     * computes the nominal time for the Nth occurrence of the Repeatable
     *
     * @param occurrence
     *            : instance numbner
     * @return TimeStamp of the Nth instance
     */
    public Date getTime(int occurrence) {
        if (occurrence < 0) {
            throw new IllegalArgumentException("occurrence cannot be <0");
        }
        Calendar calendar = Calendar.getInstance(getTimeZone());
        calendar.setLenient(true);
        calendar.setTime(getBaseline());
        calendar.add(getTimeUnit().getCalendarUnit(), getFrequency()
                * occurrence);
        return calendar.getTime();
    }

    // Setters and getters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBaseline() {
        if (this.baseline == null) {
            this.baseline = new Date();
        }
        return new Date(baseline.getTime());
    }

    public void setBaseline(Date baseline) {
        this.baseline = baseline;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
 
 }
diff --git a/release-log.txt b/release-log.txt
index fb0e020e7..bb98c3d38 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 5.1.0 release (trunk - unreleased)
 
OOZIE-2942 [examples] Fix Findbugs warnings (Jan Hentschel, kmarton via andras.piros)
 OOZIE-2718 Improve -dryrun for bundles (zhengxb2005, asalamon74 via andras.piros)
 OOZIE-3156 amend Retry SSH action check when cannot connect to remote host (txsing, matijhs via andras.piros)
 OOZIE-3303 Oozie UI does not work after Jetty 9.3 upgrade (asalamon74 via gezapeti)
- 
2.19.1.windows.1

