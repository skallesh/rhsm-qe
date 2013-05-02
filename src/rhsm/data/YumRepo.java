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
public class YumRepo extends AbstractCommandLineData {
	
	// abstraction fields
	public String id;	// repo id/label
	public String name;
	public String baseurl;
	public Boolean enabled;
	public Boolean gpgcheck;
	public String gpgkey;
	public String sslcacert;
	public Boolean sslverify;
	public String sslclientcert;
	public String sslclientkey;
	public String metadata_expire;	
	// more [repository] OPTIONS detailed at http://linux.die.net/man/5/yum.conf OR man yum.conf
	public String metalink;
	public String mirrorlist;
	public Boolean repo_gpgcheck;
	public String gpgcakey;
	public String exclude;
	public String includepkgs;
	public Boolean enablegroups;
	public String failovermethod;
	public Boolean keepalive;
	public String timeout;
	public String http_caching;
	public String retries;
	public String throttle;
	public String bandwidth;
	public String mirrorlist_expire;
	public String proxy;
	public String proxy_username;
	public String proxy_password;
	public String username;
	public String password;
	public String cost;
	public Boolean skip_if_unavailable;
	
	public String ui_repoid_vars;	// introduced by bug 906554
	
	// http://wiki.centos.org/PackageManagement/Yum/Priorities
	// http://docs.fedoraproject.org/en-US/Fedora/15/html/Musicians_Guide/sect-Musicians_Guide-CCRMA_Repository_Priorities.html
	public Integer priority;

	
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
		if (id != null)						string += String.format(" %s='%s'", "id",id);
		if (name != null)					string += String.format(" %s='%s'", "name",name);
		if (baseurl != null)				string += String.format(" %s='%s'", "baseurl",baseurl);
		if (enabled != null)				string += String.format(" %s='%s'", "enabled",enabled);
		if (gpgcheck != null)				string += String.format(" %s='%s'", "gpgcheck",gpgcheck);
		if (gpgkey != null)					string += String.format(" %s='%s'", "gpgkey",gpgkey);
		if (sslcacert != null)				string += String.format(" %s='%s'", "sslcacert",sslcacert);
		if (sslverify != null)				string += String.format(" %s='%s'", "sslverify",sslverify);
		if (sslclientcert != null)			string += String.format(" %s='%s'", "sslclientcert",sslclientcert);
		if (sslclientkey != null)			string += String.format(" %s='%s'", "sslclientkey",sslclientkey);
		if (metadata_expire != null)		string += String.format(" %s='%s'", "metadata_expire",metadata_expire);
		// more [repository] OPTIONS detailed at http://linux.die.net/man/5/yum.conf OR man yum.conf
		if (metalink != null)				string += String.format(" %s='%s'", "metalink",metalink);
		if (mirrorlist != null)				string += String.format(" %s='%s'", "mirrorlist",mirrorlist);
		if (repo_gpgcheck != null)			string += String.format(" %s='%s'", "repo_gpgcheck",repo_gpgcheck);
		if (gpgcakey != null)				string += String.format(" %s='%s'", "gpgcakey",gpgcakey);
		if (exclude != null)				string += String.format(" %s='%s'", "exclude",exclude);
		if (includepkgs != null)			string += String.format(" %s='%s'", "includepkgs",includepkgs);
		if (enablegroups != null)			string += String.format(" %s='%s'", "enablegroups",enablegroups);
		if (failovermethod != null)			string += String.format(" %s='%s'", "failovermethod",failovermethod);
		if (keepalive != null)				string += String.format(" %s='%s'", "keepalive",keepalive);
		if (timeout != null)				string += String.format(" %s='%s'", "timeout",timeout);
		if (http_caching != null)			string += String.format(" %s='%s'", "http_caching",http_caching);
		if (retries != null)				string += String.format(" %s='%s'", "retries",retries);
		if (throttle != null)				string += String.format(" %s='%s'", "throttle",throttle);
		if (bandwidth != null)				string += String.format(" %s='%s'", "bandwidth",bandwidth);
		if (mirrorlist_expire != null)		string += String.format(" %s='%s'", "mirrorlist_expire",mirrorlist_expire);
		if (proxy != null)					string += String.format(" %s='%s'", "proxy",proxy);
		if (proxy_username != null)			string += String.format(" %s='%s'", "proxy_username",proxy_username);
		if (proxy_password != null)			string += String.format(" %s='%s'", "proxy_password",proxy_password);
		if (username != null)				string += String.format(" %s='%s'", "username",username);
		if (password != null)				string += String.format(" %s='%s'", "password",password);
		if (cost != null)					string += String.format(" %s='%s'", "cost",cost);
		if (skip_if_unavailable != null)	string += String.format(" %s='%s'", "skip_if_unavailable",skip_if_unavailable);
		if (priority != null)				string += String.format(" %s='%s'", "priority",priority);
		if (ui_repoid_vars != null)			string += String.format(" %s='%s'", "ui_repoid_vars",ui_repoid_vars);

		return string.trim();
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
		
		List<YumRepo> yumRepos = new ArrayList<YumRepo>();
		
		// begin by finding all the yumRepoIds
		List<String> yumRepoIds = new ArrayList<String>();
		Matcher m = Pattern.compile("^(\\[.*\\])", Pattern.MULTILINE).matcher(stdoutCatOfRedhatRepoFile);
		while (m.find()) yumRepoIds.add(m.group(1));
		
		// if no yumRepoIds, then no yumRepos
		if (yumRepoIds.isEmpty()) return yumRepos;
		
		
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
			regexes.put("name",					"name\\s*=\\s*(.*)");
			regexes.put("baseurl",				"baseurl\\s*=\\s*(.*)");
			regexes.put("enabled",				"enabled\\s*=\\s*(.*)");
			regexes.put("gpgcheck",				"gpgcheck\\s*=\\s*(.*)");
			regexes.put("gpgkey",				"gpgkey\\s*=\\s*(.*)");
			regexes.put("sslcacert",			"sslcacert\\s*=\\s*(.*)");
			regexes.put("sslverify",			"sslverify\\s*=\\s*(.*)");
			regexes.put("sslclientcert",		"sslclientcert\\s*=\\s*(.*)");
			regexes.put("sslclientkey",			"sslclientkey\\s*=\\s*(.*)");
			regexes.put("metadata_expire",		"metadata_expire\\s*=\\s*(.*)");
			// more [repository] OPTIONS detailed at http://linux.die.net/man/5/yum.conf OR man yum.conf
			regexes.put("metalink",				"metalink\\s*=\\s*(.*)");
			regexes.put("mirrorlist",			"mirrorlist\\s*=\\s*(.*)");
			regexes.put("repo_gpgcheck",		"repo_gpgcheck\\s*=\\s*(.*)");
			regexes.put("gpgcakey",				"gpgcakey\\s*=\\s*(.*)");
			regexes.put("exclude",				"exclude\\s*=\\s*(.*)");
			regexes.put("includepkgs",			"includepkgs\\s*=\\s*(.*)");
			regexes.put("enablegroups",			"enablegroups\\s*=\\s*(.*)");
			regexes.put("failovermethod",		"failovermethod\\s*=\\s*(.*)");
			regexes.put("keepalive",			"keepalive\\s*=\\s*(.*)");
			regexes.put("timeout",				"timeout\\s*=\\s*(.*)");
			regexes.put("http_caching",			"http_caching\\s*=\\s*(.*)");
			regexes.put("retries",				"retries\\s*=\\s*(.*)");
			regexes.put("throttle",				"throttle\\s*=\\s*(.*)");
			regexes.put("bandwidth",			"bandwidth\\s*=\\s*(.*)");
			regexes.put("mirrorlist_expire",	"mirrorlist_expire\\s*=\\s*(.*)");
			regexes.put("proxy",				"proxy\\s*=\\s*(.*)");
			regexes.put("proxy_username",		"proxy_username\\s*=\\s*(.*)");
			regexes.put("proxy_password",		"proxy_password\\s*=\\s*(.*)");
			regexes.put("username",				"username\\s*=\\s*(.*)");
			regexes.put("cost",					"cost\\s*=\\s*(.*)");
			regexes.put("skip_if_unavailable",	"skip_if_unavailable\\s*=\\s*(.*)");
			regexes.put("priority",				"priority\\s*=\\s*(.*)");
			regexes.put("ui_repoid_vars",		"ui_repoid_vars\\s*=\\s*(.*)");
			
			
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
