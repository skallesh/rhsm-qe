#!/usr/bin/python

import socket
import sys, time
from xmlrpclib import Server
from optparse import OptionParser
from pprint import pprint

usage = "Usage: %prog [options] <IDs>"
parser = OptionParser(usage=usage)
parser.add_option("-u", "--username", dest="username", help="Username")
parser.add_option("-p", "--password", dest="password", help="Password")
parser.add_option("-s", "--serverurl", dest="serverurl", help="URL of Satellite Server", default="http://rhsm-sat5.usersys.redhat.com")
parser.add_option("-n", "--delete-by-name", dest="name", action="store_true", default=False, help="Delete systems by name instead of ID")
parser.add_option("-d", "--dry-run", dest='dryrun', action="store_true", default=False, help="Dry run; doesn't actually delete anything")

(options, args) = parser.parse_args()

if not options.username or not options.password or len(args) < 1:
   parser.print_usage()
   sys.exit(1)

# set a socket timeout of 3 minutes; attempt to avoid a 502 Proxy Error after 2 minutes
socket.setdefaulttimeout(60*3)

# create an api connection to the server
# RHN API documentation: https://access.stage.redhat.com/knowledge/docs/Red_Hat_Network/
client = Server("%s/rpc/api/" % options.serverurl)
#sessionKey = client.auth.login(options.username, options.password)
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

systems = client.system.listSystems(sessionKey)

if not options.name:
    system_ids = [x['id'] for x in systems if str(x['id']) in args]
else:
    system_ids = [x['id'] for x in systems if x['name'] in args]

print "Deleting the following systems:"
psystems = [x for x in systems if x['id'] in system_ids]
pprint(psystems)

if not options.dryrun:
    client.system.deleteSystems(sessionKey, system_ids)

client.auth.logout(sessionKey)

