package rhsm.testng;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.testng.Reporter;

import rhsm.base.SubscriptionManagerBaseTestScript;

import com.redhat.qe.auto.testng.TestNGReportHandler;
import com.redhat.qe.jul.TestRecords;

public class TestNGReportHandlerForRHSM extends TestNGReportHandler {
	@Override
	public void publish(LogRecord record) {
		if (Boolean.valueOf(SubscriptionManagerBaseTestScript.getProperty("sm.conservativeLogging", "true"))) {
			super.publish(new LogRecord(Level.WARNING,"Conservative Logging is ON"));
		}
		if (!Boolean.valueOf(SubscriptionManagerBaseTestScript.getProperty("sm.conservativeLogging", "true"))) {
			super.publish(new LogRecord(Level.WARNING,"Conservative Logging is OFF"));
		}
		super.publish(new LogRecord(Level.INFO,"Hello World... About to log:"+record.getMessage()));
		super.publish(record);
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
}
