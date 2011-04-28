package com.redhat.qe.sm.data;

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
public class YumRepo extends AbstractCommandLineData {
	
	// abstraction fields
	public String id;	// repo id/label
	public String name;
	public String baseurl;
	public Boolean enabled;

	
	public YumRepo(Map<String, String> repoData) {
		super(repoData);
	}
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (id != null)			string += String.format(" %s='%s'", "id",id);
		if (name != null)		string += String.format(" %s='%s'", "name",name);
		if (baseurl != null)	string += String.format(" %s='%s'", "baseurl",baseurl);
		if (enabled != null)	string += String.format(" %s='%s'", "enabled",enabled);

		return string.trim();
	}
	
	
	/**
	 * @param stdoutCatOfRedhatRepoFile - stdout from "cat /etc/yum.repos.d/redhat.repo"
	 * @return
	 */
	static public List<YumRepo> parse(String stdoutCatOfRedhatRepoFile) {
		/* # cat /etc/yum.repos.d/redhat.repo
		#
		# Red Hat Repositories
		# Managed by (rhsm) subscription-manager
		#
		
		[awesomeos-workstation-scalable-fs]
		name = awesomeos-workstation-scalable-fs
		baseurl = https://cdn.redhat.com/path/to/awesomeos-workstation-scalable-fs
		enabled = 1
		gpgcheck = 1
		gpgkey = https://cdn.redhat.com/path/to/awesomeos/gpg/
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/key.pem
		sslclientcert = /etc/pki/entitlement/8367634176913608279.pem
		metadata_expire = 3600
		
		[awesomeos-modifier]
		name = awesomeos-modifier
		baseurl = http://example.com/awesomeos-modifier
		enabled = 1
		gpgcheck = 1
		gpgkey = http://example.com/awesomeos-modifier/gpg
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/key.pem
		sslclientcert = /etc/pki/entitlement/834137930840854788.pem
		
		[awesomeos-server-scalable-fs]
		name = awesomeos-server-scalable-fs
		baseurl = https://cdn.redhat.com/path/to/awesomeos-server-scalable-fs
		enabled = 1
		gpgcheck = 1
		gpgkey = https://cdn.redhat.com/path/to/awesomeos/gpg/
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/key.pem
		sslclientcert = /etc/pki/entitlement/8367634176913608279.pem
		metadata_expire = 3600
		
		[always-enabled-content]
		name = always-enabled-content
		baseurl = https://cdn.redhat.com/foo/path/always
		enabled = 1
		gpgcheck = 1
		gpgkey = https://cdn.redhat.com/foo/path/always/gpg
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/key.pem
		sslclientcert = /etc/pki/entitlement/7590966368525320704.pem
		metadata_expire = 200
		
		[content-label]
		name = content
		baseurl = https://cdn.redhat.com/foo/path
		enabled = 1
		gpgcheck = 1
		gpgkey = https://cdn.redhat.com/foo/path/gpg/
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/key.pem
		sslclientcert = /etc/pki/entitlement/7590966368525320704.pem
		metadata_expire = 0
		
		[never-enabled-content]
		name = never-enabled-content
		baseurl = https://cdn.redhat.com/foo/path/never
		enabled = 0
		gpgcheck = 1
		gpgkey = https://cdn.redhat.com/foo/path/never/gpg
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/key.pem
		sslclientcert = /etc/pki/entitlement/7590966368525320704.pem
		*/
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("id",					"\\[(.*)\\]");
		regexes.put("name",					"name = (.*)");
		regexes.put("baseurl",				"baseurl = (.*)");
		regexes.put("enabled",				"enabled = (.*)");
		
		List<Map<String,String>> yumRepoList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutCatOfRedhatRepoFile, yumRepoList, field);
		}
		
		List<YumRepo> yumRepos = new ArrayList<YumRepo>();
		for(Map<String,String> yumRepoMap : yumRepoList)
			yumRepos.add(new YumRepo(yumRepoMap));
		return yumRepos;
	}
}
