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
public class Repo extends AbstractCommandLineData {
	
	// abstraction fields
	public String repoName;
	public String repoId;	// repoLabel
	public String repoUrl;
	public Boolean enabled;

	
	public Repo(Map<String, String> repoData) {
		super(repoData);
	}
	
	public Repo(String repoName, String repoId, String repoUrl, Boolean enabled) {
		super(null);
		this.repoName = repoName;
		this.repoId = repoId;
		this.repoUrl = repoUrl;
		this.enabled = enabled;
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (repoName != null)	string += String.format(" %s='%s'", "repoName",repoName);
		if (repoId != null)		string += String.format(" %s='%s'", "repoId",repoId);
		if (repoUrl != null)	string += String.format(" %s='%s'", "repoUrl",repoUrl);
		if (enabled != null)	string += String.format(" %s='%s'", "enabled",enabled);

		return string.trim();
	}
	
	@Override
	public boolean equals(Object obj){

		Repo that = (Repo) obj;
		
		if (that.repoName!=null && !that.repoName.equals(this.repoName)) return false;
		if (this.repoName!=null && !this.repoName.equals(that.repoName)) return false;
		
		if (that.repoId!=null && !that.repoId.equals(this.repoId)) return false;
		if (this.repoId!=null && !this.repoId.equals(that.repoId)) return false;
		
		if (that.repoUrl!=null && !that.repoUrl.equals(this.repoUrl)) return false;
		if (this.repoUrl!=null && !this.repoUrl.equals(that.repoUrl)) return false;
		
		if (that.enabled!=null && !that.enabled.equals(this.enabled)) return false;
		if (this.enabled!=null && !this.enabled.equals(that.enabled)) return false;
		
		return true;
	}
	
	/**
	 * @param stdoutReposList - stdout from "subscription-manager repos --list"
	 * @return
	 */
	static public List<Repo> parse(String stdoutReposList) {
		/* # subscription-manager repos --list
		+----------------------------------------------------------+
		    Entitled Repositories in /etc/yum.repos.d/redhat.repo
		+----------------------------------------------------------+
		
		RepoName:            	awesomeos-server-scalable-fs
		RepoId:              	awesomeos-server-scalable-fs
		RepoUrl:             	https://mockamai.devlab.phx1.redhat.com/path/to/awesomeos-server-scalable-fs
		Enabled:             	1                        
		
		
		RepoName:            	never-enabled-content    
		RepoId:              	never-enabled-content    
		RepoUrl:             	https://mockamai.devlab.phx1.redhat.com/foo/path/never
		Enabled:             	0                        
		
		
		RepoName:            	always-enabled-content   
		RepoId:              	always-enabled-content   
		RepoUrl:             	https://mockamai.devlab.phx1.redhat.com/foo/path/always
		Enabled:             	1                        
		
		
		RepoName:            	content                  
		RepoId:              	content-label            
		RepoUrl:             	https://mockamai.devlab.phx1.redhat.com/foo/path
		Enabled:             	1   
		*/
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("repoName",				"Repo Name:(.*)");
		regexes.put("repoId",				"Repo ID:(.*)");	// "Repo Id:(.*)"); changed by Bug 878634
		regexes.put("repoUrl",				"Repo URL:(.*)");	// "Repo Url:(.*)"); changed by Bug 878634
		regexes.put("enabled",				"Enabled:(.*)");
		
		List<Map<String,String>> reposList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutReposList, reposList, field);
		}
		
		List<Repo> repos = new ArrayList<Repo>();
		for(Map<String,String> repoMap : reposList)
			repos.add(new Repo(repoMap));
		return repos;
	}
}
