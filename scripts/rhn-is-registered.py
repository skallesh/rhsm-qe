#!/usr/bin/python

# Author jsefler
# Given credentials and a hosted RHN server, this script is used to report if a systemid is currently registered.

import sys, time
from xmlrpclib import Server
from optparse import OptionParser

usage = "Usage %prog [OPTIONS] systemid"
parser = OptionParser(usage=usage)
parser.add_option("-u", "--username", dest="username", help="Username")
parser.add_option("-p", "--password", dest="password", help="Password")
#parser.add_option("-i", "--systemid", dest="systemid", help="System id to check for registration")
parser.add_option("-s", "--server", dest="server", help="Server hostname rhn.redhat.com", default="rhn.redhat.com")
(options, args) = parser.parse_args()

if not options.username or not options.password or not args:
   parser.print_usage()
   sys.exit(1) 

systemid = args[0];



# create an api connection to the server
# RHN API documentation: https://access.stage.redhat.com/knowledge/docs/Red_Hat_Network/
client = Server("https://%s/rpc/api/" % options.server)
# sessionKey = client.auth.login(options.username, options.password)
sessionKey = None
count = 0
while (sessionKey == None):
    if count > 10:
        print "Giving up trying to authenticate to RHN API..."
        sys.exit(-1)
    try:
        sessionKey = client.auth.login(options.username, options.password)
    except Exception, e:
        print "Unexpected error:", e
        count += 1
        time.sleep(10)

# loop through all of the user's systems looking for systemid
system_list = client.system.listUserSystems(sessionKey)
registered = False;
for system in system_list:
    if system["id"] == systemid:
        registered = True;
        break;

print registered;
