package com.redhat.qe.sm.data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.qe.auto.testng.Assert;
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
	public Boolean gpgcheck;
	public String gpgkey;
	
//	public String rawYumRepo;

	
	public YumRepo(Map<String, String> yumRepoData) {
		super(yumRepoData);
	}
//	public YumRepo(String rawYumRepo, Map<String, String> yumRepoData){
//		super(yumRepoData);
//		this.rawYumRepo = rawYumRepo;
//	}
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (id != null)			string += String.format(" %s='%s'", "id",id);
		if (name != null)		string += String.format(" %s='%s'", "name",name);
		if (baseurl != null)	string += String.format(" %s='%s'", "baseurl",baseurl);
		if (enabled != null)	string += String.format(" %s='%s'", "enabled",enabled);
		if (gpgcheck != null)	string += String.format(" %s='%s'", "gpgcheck",gpgcheck);
		if (gpgkey != null)		string += String.format(" %s='%s'", "gpgkey",gpgkey);

		return string.trim();
	}
	
	//@Override
	public boolean equals(Object obj) {
		AbstractCommandLineData certObj = (AbstractCommandLineData)obj;
		for(Field certField:certObj.getClass().getDeclaredFields()){
			
			try {
				Field correspondingField = this.getClass().getField(certField.getName());
				if (correspondingField.get(this)==null && certField.get(certObj)==null) continue;
				if (!correspondingField.get(this).equals(certField.get(certObj))) return false;
			} catch (Exception e)  {
				log.warning("Exception caught while comparing abstraction fields: " + e.getMessage());
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param stdoutCatOfRedhatRepoFile - stdout from "cat /etc/yum.repos.d/redhat.repo"
	 * @return
	 */
	static public List<YumRepo> parse(String stdoutCatOfRedhatRepoFile) {
		/* [root@jsefler-onprem-5server ~]# cat /etc/yum.repos.d/redhat.repo
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
		
		*/
		
		/* [root@jsefler-onprem-5server ~]# grep rhel-5-server-rpms /etc/yum.repos.d/redhat.repo -A14
		[rhel-5-server-rpms]
		name = Red Hat Enterprise Linux 5 Server (RPMs)
		baseurl = https://cdn.redhat.com/content/dist/rhel/server/5/$releasever/$basearch/os
		enabled = 1
		gpgcheck = 1
		gpgkey = file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		sslverify = 1
		sslcacert = /etc/rhsm/ca/redhat-uep.pem
		sslclientkey = /etc/pki/entitlement/2882285410158247626-key.pem
		sslclientcert = /etc/pki/entitlement/2882285410158247626.pem
		metadata_expire = 86400
		proxy = https://auto-services.usersys.redhat.com:3128
		proxy_username = redhat
		proxy_password = redhat
		*/
		
		// begin by finding all the yumRepoIds
		List<String> yumRepoIds = new ArrayList<String>();
		Matcher m = Pattern.compile("^(\\[.*\\])", Pattern.MULTILINE).matcher(stdoutCatOfRedhatRepoFile);
		while (m.find()) yumRepoIds.add(m.group(1));
		
		List<YumRepo> yumRepos = new ArrayList<YumRepo>();
		
		
		// begin by splitting the yumRepos and processing each yumRepo individually

		// trim off leading redhat.repo comments from stdoutCatOfRedhatRepoFile
		stdoutCatOfRedhatRepoFile = Pattern.compile("^.*?(\\[.*?\\])", Pattern.DOTALL).matcher(stdoutCatOfRedhatRepoFile).replaceAll("$1");

		// split the stdoutCatOfRedhatRepoFile by the [id] and processing each yumRepo individually
		int y=0;
		for (String rawYumRepo : stdoutCatOfRedhatRepoFile.split("\\[.*\\]")) {
			if (rawYumRepo.trim().length()==0) continue;	// skip leading split
			rawYumRepo = yumRepoIds.get(y++)+rawYumRepo;	// tac the yumRepo [id] back on
	
			Map<String,String> regexes = new HashMap<String,String>();

			// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
			regexes.put("id",					"\\[(.*)\\]");
			regexes.put("name",					"name = (.*)");
			regexes.put("baseurl",				"baseurl = (.*)");
			regexes.put("enabled",				"enabled = (.*)");
			regexes.put("gpgcheck",				"gpgcheck = (.*)");
			regexes.put("gpgkey",				"gpgkey = (.*)");

			List<Map<String,String>> yumRepoDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawYumRepo, yumRepoDataList, field);
			}
			
			// assert that there is only one group of yumRepoData found in the map
			if (yumRepoDataList.size()!=1) Assert.fail("Error when parsing raw yumRepo.");
			Map<String,String> yumRepoData = yumRepoDataList.get(0);
			
			// create a new YumRepo
			yumRepos.add(new YumRepo(/*rawYumRepo,*/yumRepoData));
		}
		
		return yumRepos;
		
		
		
		
		
		
//		// OLD PARSING - fails when there is not a gpgkey on every yumRepo
//		
//		Map<String,String> regexes = new HashMap<String,String>();
//		
//		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
//		regexes.put("id",					"\\[(.*)\\]");
//		regexes.put("name",					"name = (.*)");
//		regexes.put("baseurl",				"baseurl = (.*)");
//		regexes.put("enabled",				"enabled = (.*)");
//		regexes.put("gpgcheck",				"gpgcheck = (.*)");
//		regexes.put("gpgkey",				"gpgkey = (.*)");
//		
//		List<Map<String,String>> yumRepoList = new ArrayList<Map<String,String>>();
//		for(String field : regexes.keySet()){
//			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
//			addRegexMatchesToList(pat, stdoutCatOfRedhatRepoFile, yumRepoList, field);
//		}
//		
//		List<YumRepo> yumRepos = new ArrayList<YumRepo>();
//		for(Map<String,String> yumRepoMap : yumRepoList)
//			yumRepos.add(new YumRepo(yumRepoMap));
//		return yumRepos;
	}
}
