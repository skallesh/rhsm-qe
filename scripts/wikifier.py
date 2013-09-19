#!/usr/bin/python
#Author: J.C. Molet
#Example usage:
#python wikifier.py --link="http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/RHSM-qe/job/subscription-manager-1.9-AcceptanceTests-OnRHEL6.5-OnAllDistros-UsingStage/23/PLATFORM=RedHatEnterpriseLinux6@Server@i386,label=rhsm/TestNG_Report/" --build="blah-1.342.3"

import sys
import re
import urllib2
from BeautifulSoup import BeautifulSoup
from optparse import OptionParser
from pprint import pprint

if __name__ == "__main__":
    usage = "Usage: %prog [options]"
    parser = OptionParser(usage=usage)
    parser.add_option("-l", "--link", action = "store", type = "string", dest = "link",
                      help = "Link to the test report.")
    parser.add_option("-b", "--build", action = "store", type = "string", dest = "build",
                      help = "Build number of the project.")
    (options, args) = parser.parse_args()
    if not options.link:
        parser.print_help()
        sys.exit(1)
    
    url = options.link + "overview.html"
    page = urllib2.urlopen(url)
    soup = BeautifulSoup(page)
    allentries = []
    for table in soup.findAll('table'):
        for i in range(2,(len(table.findAll('tr'))-1)):
            entry = table.findAll('tr')[i]
            thisentry = {}

            href = entry.td.a
            href['href'] = options.link + href['href']
            test = href.prettify().replace('\n', '')
                
            rate = filter(lambda x: x['class'] == 'passRate', entry.findAll('td'))[0].getText()
            
            failed = filter(lambda x: x['class'] == 'failed number', entry.findAll('td'))
            if failed:
                fail = failed[0].getText()
                note = "Failures: " + fail
            else:
                note = ""
            
            thisentry['test'] = test
            thisentry['rate'] = rate
            thisentry['note'] = note
            allentries.append(thisentry)
        
    if options.build:
        build = options.build
    else:
        build = ""
        
    print '<!-- START OF MY TABLE -->'
    for i in allentries:
        print '<tr>'
        print '<td style="padding: 2px;">' + i['test'] + '</td>'
        print '<td style="padding: 2px;">' + build + '</td>'
        print '<td style="padding: 2px;">' + i['rate'] + '</td>'
        print '<td style="padding: 2px;">' + i['note'] + '</td>'
        print '</tr>'
    print '<!-- END OF MY TABLE -->'

       
        
        
        
