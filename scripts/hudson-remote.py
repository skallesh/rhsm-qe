#!/usr/bin/python
# Copyright (c) 2010 Red Hat, Inc.
#
# This software is licensed to you under the GNU General Public License,
# version 2 (GPLv2). There is NO WARRANTY for this software, express or
# implied, including the implied warranties of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
# along with this software; if not, see
# http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
#
# Red Hat trademarks are not licensed under GPLv2. No permission is
# granted to use or replicate Red Hat trademarks that are incorporated
# in this software or its documentation.
#
# written by jmolet@redhat.com

import json
import urllib
import commands
import time
from pprint import pprint

def sanitize(arg):
    """Sanitize an argument to put in a shell command."""
    return arg.replace("\\", "\\\\")\
              .replace("\"", "\\\"")\
              .replace("$", r"\$")\
              .replace("`", r"\`")

def schedule_job(url, variants, token):
    """Schedules a beaker job in hudson.
    
    Arguements:
    url -- The url of the hudson job.
    variants -- A list of tests.  Each test is a list in this format:
                [['arch', 'variant', 'release'],
                 ['arch1', 'variant1', 'release1']]
    token -- The token used to gain remote access to the hudson job.
    
    """
    json_data = urllib.urlopen(url + "/api/json").read()
    data = json.loads(json_data)
    #pretty prints the jason data
    #print json.dumps(data, sort_keys=True, indent=4) 

    value_list = []

    #element data['property'][1] was valid in older version of hudson
    #for value in data['property'][1]['parameterDefinitions']:
    for value in data['property'][2]['parameterDefinitions']:
        if value['type'] == 'PasswordParameterDefinition':
            continue
        if value['defaultParameterValue'] == None:
            continue
        else:
            value_list.append(value['defaultParameterValue'])

    for variant in variants:
        print variant
        for item in value_list:
            if item['name'] == 'BEAKER_ARCH':
                item['value'] = variant[0]
            if item['name'] == 'BEAKER_VARIANT':
                item['value'] = variant[1]
            if item['name'] == 'BEAKER_DISTROFAMILY':
                item['value'] = variant[2]
        output_dict = dict()
        output_dict['parameter'] = value_list
        print "Parameters:"
        pprint(output_dict)
        outputdata = json.dumps(output_dict)
        cmd = 'curl -X POST %s -d token="%s" --data-urlencode json="%s"' % \
                  (url + "/build", token, sanitize(outputdata))  
        status, output = commands.getstatusoutput(cmd)
        print status
        print output
        time.sleep(10)


#### COMMENTING OUT THE LAUNCHING OF THE rhsm-beaker-on-premises-RHEL6.1 JOBS SINCE THIS PRODUCT HAS SHIPPED 5/18/2011
#URL = "http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/rhsm-beaker-on-premises-RHEL6.1"
#URL = "http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager%20%28OnPremises%29%20RHEL6.1%20Beaker"
#TOKEN = "hudsonbeaker-remote"
#VARIANTS = [['ppc64','Server','RedHatEnterpriseLinux6'],
#            ['s390x','Server','RedHatEnterpriseLinux6'],
#            ['x86_64','ComputeNode','RedHatEnterpriseLinux6'],
#            ['x86_64','Server','RedHatEnterpriseLinux6'],
#            ['x86_64','Client','RedHatEnterpriseLinux6'],
#            ['x86_64','Workstation','RedHatEnterpriseLinux6'],
#            ['i386','Client','RedHatEnterpriseLinux6'],
#            ['i386','Server','RedHatEnterpriseLinux6'],
#            ['i386','Workstation','RedHatEnterpriseLinux6']]
#schedule_job(URL, VARIANTS, TOKEN)


#### COMMENTING OUT THE LAUNCHING OF THE rhsm-beaker-on-premises-RHEL5.7 JOBS SINCE THIS PRODUCT HAS SHIPPED 7/21/2011
#URL = "http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/rhsm-beaker-on-premises-RHEL5.7"
#URL = "http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager%20%28OnPremises%29%20RHEL5.7%20Beaker"
#TOKEN = "hudsonbeaker-remote"
#VARIANTS = [['ppc64', '', 'RedHatEnterpriseLinuxServer5'],
#            ['s390x', '', 'RedHatEnterpriseLinuxServer5'],
#            ['ia64', '', 'RedHatEnterpriseLinuxServer5'],
#            ['i386', '', 'RedHatEnterpriseLinuxServer5'],
#            ['x86_64', '', 'RedHatEnterpriseLinuxServer5'],
#            ['i386', '', 'RedHatEnterpriseLinuxClient5'],
#            ['x86_64', '', 'RedHatEnterpriseLinuxClient5']]
#schedule_job(URL, VARIANTS, TOKEN)


URL = "http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager%20%28OnPremises%29%20RHEL6.2%20Beaker"
TOKEN = "hudsonbeaker-remote"
VARIANTS = [['ppc64','Server','RedHatEnterpriseLinux6'],
            ['s390x','Server','RedHatEnterpriseLinux6'],
            ['x86_64','ComputeNode','RedHatEnterpriseLinux6'],
            ['x86_64','Server','RedHatEnterpriseLinux6'],
            ['x86_64','Client','RedHatEnterpriseLinux6'],
            ['x86_64','Workstation','RedHatEnterpriseLinux6'],
            ['i386','Client','RedHatEnterpriseLinux6'],
            ['i386','Server','RedHatEnterpriseLinux6'],
            ['i386','Workstation','RedHatEnterpriseLinux6']]
schedule_job(URL, VARIANTS, TOKEN)

