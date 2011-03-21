#!/usr/bin/python

import json
import urllib
import urllib2
import commands
import time
from pprint import pprint

def sanitize(arg):
    """Sanitize an argument to put in a shell command."""
    return arg.replace("\"", "\\\"").replace("$", r"\$").replace("`", r"\`")

url = "http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/rhsm-beaker-on-premises"
json_data = urllib.urlopen(url + "/api/json").read()
data = json.loads(json_data)

value_list = []
for value in data['property'][1]['parameterDefinitions']:
  if value['type'] == 'PasswordParameterDefinition':
    continue
  if value['defaultParameterValue'] == None:
    continue
  else:
    value_list.append(value['defaultParameterValue'])

variants = [['x86_64','Server'],
            ['x86_64','Client'],
            ['x86_64','Workstation'],
            ['x86_64','ComputeNode'],
            ['i386','Server'],
            ['i386','Workstation'],
            ['i386','Client'],
            ['ppc64','Server'],
            ['s390x','Server']]

for variant in variants:
  print variant
  for item in value_list:
    if item['name'] == 'CLIENT1_ARCH':
      item['value'] = variant[0]
    if item['name'] == 'CLIENT1_VARIANT':
      item['value'] = variant[1]  
  output_dict = dict()
  output_dict['parameter'] = value_list
  outputdata = json.dumps(output_dict)
  cmd = 'curl -X POST %s -d token=hudsonbeaker-remote --data-urlencode json="%s"' % (url + "/build", sanitize(outputdata))
  print cmd
  print
  status, output = commands.getstatusoutput(cmd)
  print status
  print output
  time.sleep(10)

#urllib2.urlopen(url + "/build?token=hudsonbeaker-remote", outputdata, {'Content-Type': 'application/json'}).read()


