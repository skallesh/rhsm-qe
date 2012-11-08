package rhsm.data;

import java.io.File;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class CertStatistics extends AbstractCommandLineData {

	// abstraction fields
	public String type;
	public String version;
	public String derSize;
	public String subjectKeyIdSize;
	public Integer contentSets;



	public CertStatistics(Map<String, String> certData) {
		super(certData);
	}
	
	@Override
	public String toString() {
		String string = "";
		if (type != null)				string += String.format(" %s='%s'", "type",type);
		if (version != null)			string += String.format(" %s='%s'", "version",version);
		if (derSize != null)			string += String.format(" %s='%s'", "derSize",derSize);
		if (subjectKeyIdSize != null)	string += String.format(" %s='%s'", "subjectKeyIdSize",subjectKeyIdSize);
		if (contentSets != null)		string += String.format(" %s='%s'", "contentSets",contentSets);
		
		return string.trim();
	}
	
	
	/**
	 * @param rawStatistics - stdout from: # rct stat-cert /etc/pki/entitlement/SERIAL.pem"
	 * @return
	 */
	static public CertStatistics parse(String rawStatistics) {
		
		//	[root@jsefler-6 ~]# rct stat-cert /etc/pki/entitlement/7296015097057939603.pem 
		//	Type: Entitlement Certificate
		//	Version: 3.0
		//	DER size: 948b
		//	Subject Key ID size: 20b
		//	Content sets: 6
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("type",					"Type: (.+)");
		regexes.put("version",				"Version: (.+)");
		regexes.put("derSize",				"DER size: (.+)");
		regexes.put("subjectKeyIdSize",		"Subject Key ID size: (.+)");
		regexes.put("contentSets",			"Content sets: (.+)");
		
		List<Map<String,String>> statisticsDataList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, rawStatistics, statisticsDataList, field);
		}
		
		// assert that there is only one group of certData found in the list
		if (statisticsDataList.size()!=1) Assert.fail("Error when parsing raw cert statitics.  Expected to parse only one group of certificate data.");
		Map<String,String> statisticsData = statisticsDataList.get(0);
	
		return new CertStatistics(statisticsData);
	}
}
