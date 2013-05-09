package rhsm.testng;


import java.util.logging.Handler;
import java.util.logging.Logger;

import org.testng.ITestResult;

import com.redhat.qe.auto.testng.TestNGListener;

public class TestNGListenerForRHSM extends TestNGListener {
	
<<<<<<< HEAD
	protected static TestNGReportHandlerForRHSM testNGReportHandlerForRHSM;
=======
	protected TestNGReportHandlerForRHSM testNGReportHandlerForRHSM;
>>>>>>> 01425a8ce0a0aca9fba563e941800c89c877d21e
	public TestNGListenerForRHSM() {
		super();
		// TODO Auto-generated constructor stub
		for (Handler handler : log.getHandlers()) {
<<<<<<< HEAD
			if (handler.getClass().isInstance(TestNGReportHandlerForRHSM.class)) {
				testNGReportHandlerForRHSM = (TestNGReportHandlerForRHSM) handler;
			}
		}
		System.out.print("testNGReportHandlerForRHSM= "+testNGReportHandlerForRHSM);
=======
			if (handler instanceof TestNGReportHandlerForRHSM) {
				testNGReportHandlerForRHSM = (TestNGReportHandlerForRHSM) handler;
			}
		}
>>>>>>> 01425a8ce0a0aca9fba563e941800c89c877d21e
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
