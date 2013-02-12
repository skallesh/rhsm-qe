"""
A Python interface to a Hudson instance.

Please contribute!
 * submit bugs/patches/requests via issues.hudson-ci.org
 * email mcrooney@dev.java.net with bugs/patches/requests
 * fork the Gist and request a pull: http://gist.github.com/510467

TODO:
 * remove dep on BeautifulSoup in getTestFailuresForJob, use /api/python instead
 * eliminate wget shell call in createJob via native urllib[2]
"""
import re
import urllib
import urllib2
import commands

def sanitize(arg):
    """Sanitize an argument to put in a shell command."""
    return arg.replace("\"", "\\\"").replace("$", r"\$").replace("`", r"\`")

class HudsonInstance:
    def __init__(self, hudsonUrl):
        self.URL = hudsonUrl

    def escapeUrl(self, url):
        for char in ":# ":
            url = url.replace(char, "_")
        return url

    def getAbsoluteUrl(self, relativeUrl):
        # Take care of proper url joining.
        if not (self.URL.endswith("/") or relativeUrl.startswith("/")):
            relativeUrl = "/" + relativeUrl
        return self.URL + self.escapeUrl(relativeUrl)

    def read(self, url, postVarDict=None):
        if postVarDict:
            postVarDict = urllib.urlencode(postVarDict)
        fullUrl = self.getAbsoluteUrl(url)
        return urllib2.urlopen(fullUrl, postVarDict).read()

    def readJob(self, job, url, postVarDict=None):
        return self.read("/job/%s/%s" % (job, url), postVarDict)

    def build(self, job, parameters="", token=None):
        url = "/build"
        if parameters:
            url += "WithParameters?%s" % parameters
            if token:
                url += "&token=%s" % token
        elif token:
            url += "?token=%s" % token
        self.readJob(job, url)

    def isJobBuilding(self, job):
        return "<building>true</building>" in self.readJob(job, "/lastBuild/api/xml")

    def getLastBuildNumberForJob(self, job):
        contents = self.readJob(job, "/lastBuild/")
        last = re.search("Build \#(\d+)", contents).groups()[0]
        last = int(last)
        return last

    def getTestFailuresForJob(self, job, buildNumber=None):
        from BeautifulSoup import BeautifulStoneSoup
        if buildNumber is None:
            buildNumber = self.getLastBuildNumberForJob(job)

        try:
            testResultXml = self.readJob(job, "/%s/testReport/api/xml" % buildNumber)
        except urllib2.HTTPError:
            return None

        soup = BeautifulStoneSoup(testResultXml)
        failures = []
        for test in soup.findAll("case"):
            if test.status.contents[0] not in ["PASSED", "FIXED"]:
                test.AGE = int(test.age.contents[0])
                test.CLASS = test.classname.contents[0]
                test.NAME = test.find("name").contents[0]
                failures.append(test)
        return failures

    def getConsoleTextForJob(self, job, build="lastBuild"):
        return self.readJob(job, "/%s/consoleText" % build)

    def getConfigForJob(self, job):
        """Read and return the config.xml of the specified job."""
        return self.readJob(job, "/config.xml")

    def getDescriptionForJob(self, job):
        return self.readJob(job, "/description")

    def setDescriptionForJob(self, job, description):
        return self.readJob(job, "/description", {"description": description})

    def createJob(self, job, config, build=False, buildToken=None):
        """Given a name and a config.xml, create a new Hudson job, optionally triggering a build."""
        url = self.URL + "/createItem?name=%s" % job
        cmd = 'wget --header="Content-Type: text/xml" --post-data="%s" -O - "%s"' % (sanitize(config), url)
        status, output = commands.getstatusoutput(cmd)
        if status or "200 OK" not in output:
            raise Exception("wget failed for %s: %s" % (job, output))
        if build:
            self.build(job, token=buildToken)

