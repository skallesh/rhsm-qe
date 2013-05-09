package rhsm.testng;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

import rhsm.base.SubscriptionManagerBaseTestScript;

import com.redhat.qe.auto.testng.TestNGReportHandler;

public class TestNGReportHandlerForRHSM extends TestNGReportHandler {
	@Override
	public void publish(LogRecord logRecord) {
		if (buffering) {
			logRecordBuffer.add(logRecord);
		} else {
			super.publish(logRecord);
		}
//		if (Boolean.valueOf(SubscriptionManagerBaseTestScript.getProperty("sm.conservativeTestNGReporting", "false"))) {
//			super.publish(record);
//			return;
//		}
//		
//		
//		if (!Boolean.valueOf(SubscriptionManagerBaseTestScript.getProperty("sm.conservativeTestNGReporting", "true"))) {
//			super.publish(new LogRecord(Level.WARNING,"Conservative Logging is OFF"));
//		}
//		super.publish(new LogRecord(Level.INFO,"Hello World... About to log:"+record.getMessage()));
//		super.publish(record);
//		String css_class = record.getLevel().toString();
//		if (record.getParameters() != null)
//		for (Object param: record.getParameters()){
//		if (param.equals(TestRecords.Style.Banner))
//		css_class += " banner";
//		if (param.equals(TestRecords.Style.StartTest))
//		css_class += " startTest";
//		if (param.equals(TestRecords.Style.Action))
//		css_class += " ACTION";
//		if (param.equals(TestRecords.Style.Asserted))
//		css_class += " ASSERT";
//		if (param.equals(TestRecords.Style.AssertFailed))
//		css_class += " ASSERTFAIL";
//		}
//		//Reporter.log("<div class='" + css_class + "'>"+record.getMessage() + "</div>");
//		Reporter.log("<div class='" + css_class + "'>"+tagAllUrls(addLineBreaks(escapeAllTags(record.getMessage()))) + "</div>");
	}
	
	
	
	private List<LogRecord> logRecordBuffer = new ArrayList<LogRecord>();
	private boolean buffering=false;
	
	public void clearBuffer() {
		// clear all log records pending in the buffer (these will never be logged)
		logRecordBuffer.clear();
	}
	public void stopBuffer() {
		// stop the buffer
		buffering=false;
		// and publish what has accumulated
		for (LogRecord logRecord : logRecordBuffer) {
			this.publish(logRecord);
		}
	}
	public void startBuffer() {
		// when the user does not want to use TestNGReportHandlerForRHSM, simply don't start buffering
		if (!Boolean.valueOf(SubscriptionManagerBaseTestScript.getProperty("sm.conservativeTestNGReporting", "false"))) {
			return;
		}
		
		buffering=true;
	}
}
