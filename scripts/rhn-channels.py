#!/usr/bin/python

# Authors jsherril and jsefler
# Given a credentials and a hosted RHN server, this script is used to list the
# RHN Classic channels available.

import socket
import sys, time
from xmlrpclib import Server
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-u", "--username", dest="username", help="Username")
parser.add_option("-p", "--password", dest="password", help="Password")
parser.add_option("-b", "--basechannel", dest="basechannel", help="List child channels for only this base channel")
parser.add_option("-n", "--no-custom", dest="nocustom", action="store_true", default=False, help="Attempt to filter out custom channels (identified by no gpgkey)")
parser.add_option("-a", "--available", dest="available", action="store_true", default=False, help="Only list the child channels that currently have available entitlements")
parser.add_option("-s", "--serverurl", dest="serverurl", help="Server URL https://rhn.redhat.com", default="https://rhn.redhat.com")
(options, args) = parser.parse_args()

if not options.username or not options.password:
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

# find all the available parent/base channels and their child channels
parents = []
child_map = {}
channel_ = "channel_"
chan_list = client.channel.listSoftwareChannels(sessionKey)
for chan in chan_list:
    # satellite and rhn hosted keys differ by a "channel_" prefix  (rhn hosted uses "channel_parent_label")
    if channel_+"parent_label" not in chan:
        channel_ = ""
    
    if chan[channel_+"parent_label"] == "":
        parents.append(chan[channel_+"label"])
    else:
       if not child_map.has_key(chan[channel_+"parent_label"]):
           child_map[chan[channel_+"parent_label"]] = []
       child_map[chan[channel_+"parent_label"]].append(chan[channel_+"label"])


# print a tree view of the channels
for parent in parents:
    
    if options.basechannel and options.basechannel != parent:
        continue
    
    if options.nocustom:
        details = client.channel.software.getDetails(sessionKey, parent)
        if details[channel_+"gpg_key_url"] == "":
            continue
    print parent
    
    if child_map.has_key(parent):
        for child in child_map[parent]:        
            
            if options.available:
                availableEntitlements = client.channel.software.availableEntitlements(sessionKey, child)
                if availableEntitlements <= 0:
                    continue
            
            if options.nocustom:
                details = client.channel.software.getDetails(sessionKey, child)
                if details[channel_+"gpg_key_url"] == "":
                    continue
            
            print "  " + child
