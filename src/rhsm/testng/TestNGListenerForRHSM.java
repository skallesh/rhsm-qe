package rhsm.testng;


import java.util.logging.Handler;
import java.util.logging.Logger;

import org.testng.ITestResult;

import com.redhat.qe.auto.testng.TestNGListener;

public class TestNGListenerForRHSM extends TestNGListener {
	
	protected static TestNGReportHandlerForRHSM testNGReportHandlerForRHSM;
	public TestNGListenerForRHSM() {
		super();
		// TODO Auto-generated constructor stub
		for (Handler handler : log.getHandlers()) {
			if (handler.getClass().isInstance(TestNGReportHandlerForRHSM.class)) {
				testNGReportHandlerForRHSM = (TestNGReportHandlerForRHSM) handler;
			}
		}
		System.out.print("testNGReportHandlerForRHSM= "+testNGReportHandlerForRHSM);
	}
	
	
	@Override
	public void onTestStart(ITestResult result) {
		super.onTestStart(result);
		testNGReportHandlerForRHSM.startBuffer();
	}
	
	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		testNGReportHandlerForRHSM.stopBuffer();
		super.onTestFailedButWithinSuccessPercentage(result);
	}
	
	@Override
	public void onTestFailure(ITestResult result) {
		testNGReportHandlerForRHSM.stopBuffer();
		super.onTestFailure(result);
	}
	
	@Override
	public void onTestSkipped(ITestResult result) {
		testNGReportHandlerForRHSM.stopBuffer();
		super.onTestSkipped(result);
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		testNGReportHandlerForRHSM.clearBuffer();
		testNGReportHandlerForRHSM.stopBuffer();
		super.onTestSuccess(result);
	}
}
