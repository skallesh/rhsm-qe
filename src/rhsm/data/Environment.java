package rhsm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class Environment extends AbstractCommandLineData {
	
	// abstraction fields
	public String envDescription;
	public String envName;

		
	public Environment(Map<String, String> environmentData) {
		super(environmentData);
	}
	
	public Environment(String envName, String envDescription) {
		super(null);
		this.envName = envName;
		this.envDescription = envDescription;
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (envName != null)			string += String.format(" %s='%s'", "envName",envName);
		if (envDescription != null)		string += String.format(" %s='%s'", "envDescription",envDescription);

		return string.trim();
	}
	
	
	/**
	 * @param stdoutOrgsList - stdout from "subscription-manager orgs --username=testuser1 --password=password"
	 * @return
	 */
	static public List<Environment> parse(String stdoutEnvList) {
		/* [root@jsefler-7 ~]# subscription-manager environments --username=admin --password=admin --org ACME_Corporation
		+-------------------------------------------+
		          Environments
		+-------------------------------------------+
		Name:        Library
		Description: None
		*/
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("envName",				"Name:(.*)");
		regexes.put("envDescription",		"Description:(.*)");
		
		List<Map<String,String>> envList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutEnvList, envList, field);
		}
		
		List<Environment> environments = new ArrayList<Environment>();
		for(Map<String,String> envMap : envList)
			environments.add(new Environment(envMap));
		return environments;
	}
}
