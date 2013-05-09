package rhsm.testng;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

import rhsm.base.SubscriptionManagerBaseTestScript;

import com.redhat.qe.auto.testng.TestNGReportHandler;

/**
 * This class is an attempt to avoid The following exception which we have observed after a
 * very large job completes.
    <br>Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	<br>at java.util.Arrays.copyOf(Arrays.java:2367)
	<br>at java.lang.AbstractStringBuilder.expandCapacity(AbstractStringBuilder.java:130)
	<br>at java.lang.AbstractStringBuilder.ensureCapacityInternal(AbstractStringBuilder.java:114)
	<br>at java.lang.AbstractStringBuilder.append(AbstractStringBuilder.java:480)
	<br>at java.lang.StringBuffer.append(StringBuffer.java:309)
	<br>at java.util.regex.Matcher.appendReplacement(Matcher.java:839)
	<br>at java.util.regex.Matcher.replaceAll(Matcher.java:906)
	<br>at org.testng.reporters.XMLStringBuffer.toXML(XMLStringBuffer.java:327)
	<br>at org.testng.reporters.XMLReporter.generateReport(XMLReporter.java:66)
	<br>at org.testng.TestNG.generateReports(TestNG.java:1089)
	<br>at org.testng.TestNG.run(TestNG.java:1048)
	<br>at org.testng.TestNG.privateMain(TestNG.java:1338)
	<br>at org.testng.TestNG.main(TestNG.java:1307)
	<br>
 * The solution used in this class attempts reduce the amount of logging by excluding log records
 * published from within a test that PASSED.  Afterall, the logs are useful when a test
 * does not PASS.
 *
 * @author jsefler
 *
 */
public class TestNGReportHandlerForRHSM extends TestNGReportHandler {
	
	// used as a state machine
	private List<LogRecord> logRecordBuffer = new ArrayList<LogRecord>();
	private boolean buffering = false;
	
	@Override
	public void publish(LogRecord logRecord) {
		
		// when the user does not want to use TestNGReportHandlerForRHSM, simply publish the logRecord and return
		if (!Boolean.valueOf(SubscriptionManagerBaseTestScript.getProperty("sm.conservativeTestNGReporting", "false"))) {
			super.publish(logRecord);
			return;
		}
		
		// Note: The success of the following logic stack depends on the fixed strings below
		// matching the fixed strings used in com.redhat.qe.auto.testng.TestNGListener.java 
		
		if (logRecord.getMessage().startsWith("Test Passed")) { // must match string used in com.redhat.qe.auto.testng.TestNGListener.onTestSuccess(ITestResult result)
			logRecordBuffer.clear();	// this is key call that purges all the logs from a passed test
			buffering = false;
		}
		
		if (logRecord.getMessage().startsWith("Test Failed")) { // must match string used in com.redhat.qe.auto.testng.TestNGListener.onTestFailure(ITestResult result)  com.redhat.qe.auto.testng.TestNGListener.onTestFailedButWithinSuccessPercentage(ITestResult result)
			buffering = false;
		}
		
		if (logRecord.getMessage().startsWith("Skipping Test") ||
			logRecord.getMessage().startsWith("Skipping test")) { // must match string used in com.redhat.qe.auto.testng.TestNGListener.onTestSkipped(ITestResult result)
			buffering = false;
		}
		
		logRecordBuffer.add(logRecord);
		if (!buffering) { // publish the entire logRecordBuffer
			for (LogRecord record : logRecordBuffer) super.publish(record);
			logRecordBuffer.clear();
		}
		
		if (logRecord.getMessage().startsWith("Starting Test:")) { // must match string used in com.redhat.qe.auto.testng.TestNGListener.onTestStart(ITestResult result)
			buffering = true;
		}
	}
}
