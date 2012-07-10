package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.data.Translation;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 * @References
 *   Engineering Localization Services: https://home.corp.redhat.com/node/53593
 *   http://git.fedorahosted.org/git/?p=subscription-manager.git;a=blob;f=po/pt.po;h=0854212f4fab348a25f0542625df343653a4a097;hb=RHEL6.3
 *   Here is the raw rhsm.po file for LANG=pt
 *   http://git.fedorahosted.org/git/?p=subscription-manager.git;a=blob;f=po/pt.po;hb=RHEL6.3
 *   
 *   https://engineering.redhat.com/trac/LocalizationServices
 *   https://engineering.redhat.com/trac/LocalizationServices/wiki/L10nRHEL6LanguageSupportCriteria
 *   
 *   https://translate.zanata.org/zanata/project/view/subscription-manager/iter/0.99.X/stats
 *   
 *   https://fedora.transifex.net/projects/p/fedora/
 *   
 *   http://translate.sourceforge.net/wiki/
 *   http://translate.sourceforge.net/wiki/toolkit/index
 *   http://translate.sourceforge.net/wiki/toolkit/pofilter
 *   http://translate.sourceforge.net/wiki/toolkit/pofilter_tests
 *   http://translate.sourceforge.net/wiki/toolkit/installation
 *   
 *   https://github.com/translate/translate
 **/
@Test(groups={"PofilterTranslationTests"})
public class PofilterTranslationTests extends TranslationTests{
	
	
	// Test Methods ***********************************************************************

	@Test(	description="run pofilter translate tests on the translation file",
			groups={},
			dataProvider="getTranslationFilePofilterTestData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void pofilter_Test(Object bugzilla, String pofilterTest, File translationFile) {
		log.info("For an explanation of pofilter test '"+pofilterTest+"', see: http://translate.sourceforge.net/wiki/toolkit/pofilter_tests");
		File translationPoFile = new File(translationFile.getPath().replaceFirst(".mo$", ".po"));
		
		// execute the pofilter test
		String pofilterCommand = "pofilter --gnome -t "+pofilterTest;
		SSHCommandResult pofilterResult = client.runCommandAndWait(pofilterCommand+" "+translationPoFile);
		Assert.assertEquals(pofilterResult.getExitCode(), new Integer(0), "Successfully executed the pofilter tests.");
		
		// convert the pofilter test results into a list of failed Translation objects for simplified handling of special cases 
		List<Translation> pofilterFailedTranslations = Translation.parse(pofilterResult.getStdout());
		
		// remove the first translation which contains only meta data
		if (!pofilterFailedTranslations.isEmpty() && pofilterFailedTranslations.get(0).msgid.equals("")) pofilterFailedTranslations.remove(0);
		
		// ignore the following special cases of acceptable results..........
		List<String> ignorableMsgIds = Arrays.asList();
		if (pofilterTest.equals("accelerators")) {
			if (translationFile.getPath().contains("/hi/")) ignorableMsgIds = Arrays.asList("proxy url in the form of proxy_hostname:proxy_port");
			if (translationFile.getPath().contains("/ru/")) ignorableMsgIds = Arrays.asList("proxy url in the form of proxy_hostname:proxy_port");
		}
		if (pofilterTest.equals("newlines")) {
			ignorableMsgIds = Arrays.asList(
					"Optional language to use for email notification when subscription redemption is complete. Examples: en-us, de-de",
					"\n"+"Unable to register.\n"+"For further assistance, please contact Red Hat Global Support Services.",
					"Tip: Forgot your login or password? Look it up at http://red.ht/lost_password",
					"Unable to perform refresh due to the following exception: %s",
					""+"This migration script requires the system to be registered to RHN Classic.\n"+"However this system appears to be registered to '%s'.\n"+"Exiting.",
					"The tool you are using is attempting to re-register using RHN Certificate-Based technology. Red Hat recommends (except in a few cases) that customers only register with RHN once.",
					// bug 825397	""+"Redeeming the subscription may take a few minutes.\n"+"Please provide an email address to receive notification\n"+"when the redemption is complete.",	// the Subscription Redemption dialog actually expands to accommodate the message, therefore we could ignore it	// bug 825397 should fix this
					// bug 825388	""+"We have detected that you have multiple service level\n"+"agreements on various products. Please select how you\n"+"want them assigned.", // bug 825388 or 825397 should fix this
					"\n"+"This machine appears to be already registered to Certificate-based RHN.  Exiting.");	
		}
		if (pofilterTest.equals("unchanged")) {
			ignorableMsgIds = Arrays.asList("close_button","facts_view","register_button","register_dialog_main_vbox","registration_dialog_action_area\n","prod 1, prod2, prod 3, prod 4, prod 5, prod 6, prod 7, prod 8");
		}

		
		// pluck out the ignorable pofilter test results
		for (String msgid : ignorableMsgIds) {
			Translation ignoreTranslation = Translation.findFirstInstanceWithMatchingFieldFromList("msgid", msgid, pofilterFailedTranslations);
			if (ignoreTranslation!=null) {
				log.info("Ignoring result of pofiliter test '"+pofilterTest+"' for msgid: "+ignoreTranslation.msgid);
				pofilterFailedTranslations.remove(ignoreTranslation);
			}
		}
		
		// assert that there are no failed pofilter translation test results
		Assert.assertEquals(pofilterFailedTranslations.size(),0, "Discounting the ignored test results, the number of failed pofilter '"+pofilterTest+"' tests for translation file '"+translationFile+"'.");
	}
	
	
	
	
	
	// Candidates for an automated Test:
	
	// Configuration Methods ***********************************************************************
	
	
	// Protected Methods ***********************************************************************

	
	// Data Providers ***********************************************************************
	@DataProvider(name="getTranslationFilePofilterTestData")
	public Object[][] getTranslationFilePofilterTestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFilePofilterTestDataAsListOfLists());
	}
	protected List<List<Object>> getTranslationFilePofilterTestDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// see http://translate.sourceforge.net/wiki/toolkit/pofilter_tests
		//	Critical -- can break a program
		//    	accelerators, escapes, newlines, nplurals, printf, tabs, variables, xmltags, dialogsizes
		//	Functional -- may confuse the user
		//    	acronyms, blank, emails, filepaths, functions, gconf, kdecomments, long, musttranslatewords, notranslatewords, numbers, options, purepunc, sentencecount, short, spellcheck, urls, unchanged
		//	Cosmetic -- make it look better
		//    	brackets, doublequoting, doublespacing, doublewords, endpunc, endwhitespace, puncspacing, simplecaps, simpleplurals, startcaps, singlequoting, startpunc, startwhitespace, validchars
		//	Extraction -- useful mainly for extracting certain types of string
		//    	compendiumconflicts, credits, hassuggestion, isfuzzy, isreview, untranslated

		List<String> pofilterTests = Arrays.asList(
				//	Critical -- can break a program
				"accelerators","escapes","newlines","nplurals","printf","tabs","variables","xmltags",
				//	Functional -- may confuse the user
				"blank","emails","filepaths","gconf","long","notranslatewords","numbers","options","short","urls","unchanged",
				//	Cosmetic -- make it look better
				"doublewords",
				//	Extraction -- useful mainly for extracting certain types of string
				"untranslated");
// debugTesting
pofilterTests = Arrays.asList("escapes");

		for (File translationFile : translationFileMap.keySet()) {
			for (String pofilterTest : pofilterTests) {
				BlockedByBzBug bugzilla = null;
				// Bug 825362	[es_ES] failed pofilter accelerator tests for subscription-manager translations 
				if (pofilterTest.equals("accelerators") && translationFile.getPath().contains("/es_ES/")) bugzilla = new BlockedByBzBug("825362");
				// Bug 825367	[zh_CN] failed pofilter accelerator tests for subscription-manager translations 
				if (pofilterTest.equals("accelerators") && translationFile.getPath().contains("/zh_CN/")) bugzilla = new BlockedByBzBug("825367");
				// Bug 825397	Many translated languages fail the pofilter newlines test
				if (pofilterTest.equals("newlines") && !(translationFile.getPath().contains("/zh_CN/")||translationFile.getPath().contains("/ru/")||translationFile.getPath().contains("/ja/"))) bugzilla = new BlockedByBzBug("825397");			
				// Bug 825393	[ml_IN][es_ES] translations should not use character ¶ for a new line. 
				if (pofilterTest.equals("newlines") && translationFile.getPath().contains("/ml/")) bugzilla = new BlockedByBzBug("825393");
				if (pofilterTest.equals("newlines") && translationFile.getPath().contains("/es_ES/")) bugzilla = new BlockedByBzBug("825393");

				ll.add(Arrays.asList(new Object[] {bugzilla,	pofilterTest,	translationFile}));
			}
		}
		return ll;
	}
	
	

}
