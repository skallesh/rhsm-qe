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
public class Org extends AbstractCommandLineData {
	
	// abstraction fields
	public String orgKey;
	public String orgName;

		
	public Org(Map<String, String> orgData) {
		super(orgData);
	}
	
	public Org(String orgKey, String orgName) {
		super(null);
		this.orgKey = orgKey;
		this.orgName = orgName;
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (orgKey != null)		string += String.format(" %s='%s'", "orgKey",orgKey);
		if (orgName != null)	string += String.format(" %s='%s'", "orgName",orgName);

		return string.trim();
	}
	
	@Override
	public boolean equals(Object obj){

		Org that = (Org) obj;
		
		if (that.orgKey!=null && !that.orgKey.equals(this.orgKey)) return false;
		if (this.orgKey!=null && !this.orgKey.equals(that.orgKey)) return false;
				
		if (that.orgName!=null && !that.orgName.equals(this.orgName)) return false;
		if (this.orgName!=null && !this.orgName.equals(that.orgName)) return false;
		
		return true;
	}
	
	/**
	 * @param stdoutOrgsList - stdout from "subscription-manager orgs --username=testuser1 --password=password"
	 * @return
	 */
	static public List<Org> parse(String stdoutOrgsList) {
		/* # subscription-manager orgs --username=testuser1 --password=password
		+-------------------------------------------+
		          testuser1 Organizations
		+-------------------------------------------+
		
		OrgName: 	Snow White               
		OrgKey: 	snowwhite                
		
		OrgName: 	Admin Owner              
		OrgKey: 	admin    
		*/
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("orgKey",				"Key:(.*)");
		regexes.put("orgName",				"Name:(.*)");
		
		List<Map<String,String>> orgsList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutOrgsList, orgsList, field);
		}
		
		List<Org> orgs = new ArrayList<Org>();
		for(Map<String,String> orgMap : orgsList)
			orgs.add(new Org(orgMap));
		return orgs;
	}
}
