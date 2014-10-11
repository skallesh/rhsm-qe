package rhsm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class OstreeRepo extends AbstractCommandLineData {
	
	// abstraction fields
	public String remote;	// remote id
	public String url;
	public Boolean gpg_verify;
	public String tls_client_cert_path;
	public String tls_client_key_path;
	public String tls_ca_path;
	
	
	
	
	public OstreeRepo(Map<String, String> ostreeRepoData) {
		super(ostreeRepoData);
	}
	
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (remote != null)					string += String.format(" %s='%s'", "remote",remote);
		if (url != null)					string += String.format(" %s='%s'", "url",url);
		if (gpg_verify != null)				string += String.format(" %s='%s'", "gpg_verify",gpg_verify);
		if (tls_client_cert_path != null)	string += String.format(" %s='%s'", "tls_client_cert_path",tls_client_cert_path);
		if (tls_client_key_path != null)	string += String.format(" %s='%s'", "tls_client_key_path",tls_client_key_path);
		if (tls_ca_path != null)			string += String.format(" %s='%s'", "tls_ca_path",tls_ca_path);
		
		return string.trim();
	}
	
	
	/**
	 * @param stdoutCatOfOstreeRepoConfigFile - stdout from "cat /ostree/repo/config"
	 * @return
	 */
	static public List<OstreeRepo> parse(String stdoutCatOfOstreeRepoConfigFile) {
		
		/* -bash-4.2# cat /ostree/repo/config 
		[core]
		repo_version=1
		mode=bare
		
		[remote "rhel-atomic-preview-ostree"]
		url = https://cdn.redhat.com/content/preview/rhel/atomic/7/x86_64/ostree/repo
		gpg-verify = false
		tls-client-cert-path = /etc/pki/entitlement/6474260223991696217.pem
		tls-client-key-path = /etc/pki/entitlement/6474260223991696217-key.pem
		tls-ca-path = /etc/rhsm/ca/redhat-uep.pem
		-bash-4.2# 
		*/
		
		
		List<OstreeRepo> ostreeRepos = new ArrayList<OstreeRepo>();
		
		// begin by finding all the ostreeRepoBlocks
		List<String> ostreeRepoHeadings = new ArrayList<String>();
		Matcher m = Pattern.compile("^(\\[.+\\])", Pattern.MULTILINE).matcher(stdoutCatOfOstreeRepoConfigFile);
		while (m.find()) ostreeRepoHeadings.add(m.group(1));
		
		// if no ostreeRepoHeadings, then no ostreeRepos
		if (ostreeRepoHeadings.isEmpty()) return ostreeRepos;
		
		
		// split the stdoutCatOfOstreeRepoConfigFile by the [remote "id"] and processing each ostreeRepo individually
		int y=0;
		for (String rawOstreeRepo : stdoutCatOfOstreeRepoConfigFile.split("\\[.+\\]")) {
			if (rawOstreeRepo.trim().length()==0) continue;	// skip leading split
			rawOstreeRepo = ostreeRepoHeadings.get(y++)+rawOstreeRepo;	// tac the ostree [remote "id"] back on
			
			Map<String,String> regexes = new HashMap<String,String>();
			
			// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
			regexes.put("remote",				"\\[remote\\s+\"(.+)\"\\]");
			regexes.put("url",					"url\\s*=\\s*(.*)");
			regexes.put("gpg_verify",			"gpg-verify\\s*=\\s*(.*)");
			regexes.put("tls_client_cert_path",	"tls-client-cert-path\\s*=\\s*(.*)");
			regexes.put("tls_client_key_path",	"tls-client-key-path\\s*=\\s*(.*)");
			regexes.put("tls_ca_path",			"tls-ca-path\\s*=\\s*(.*)");
			
			
			List<Map<String,String>> ostreeRepoDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawOstreeRepo, ostreeRepoDataList, field);
			}
			
			// continue when no ostreeRepoData was found in the map (likely the [core] block)
			if (ostreeRepoDataList.isEmpty()) continue;
			
			// assert that there is only one group of ostreeRepoData found in the map
			if (ostreeRepoDataList.size()>1) Assert.fail("Error when parsing raw ostreeRepo.");
			
			// create a new OstreeRepo
			Map<String,String> ostreeRepoData = ostreeRepoDataList.get(0);
			ostreeRepos.add(new OstreeRepo(ostreeRepoData));
		}
		
		return ostreeRepos;
	}
}