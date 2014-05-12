#!/usr/bin/python
channel_trees = {}
channel_trees['rhel-x86_64-client-7'] = [
'rhel-x86_64-client-7-debuginfo',
'rhel-x86_64-client-fastrack-7',
'rhel-x86_64-client-fastrack-7-debuginfo',
'rhel-x86_64-client-optional-7',
'rhel-x86_64-client-optional-7-debuginfo',
'rhel-x86_64-client-optional-fastrack-7',
'rhel-x86_64-client-optional-fastrack-7-debuginfo',
'rhel-x86_64-client-rh-common-7',
'rhel-x86_64-client-rh-common-7-debuginfo',
'rhel-x86_64-client-supplementary-7',
'rhel-x86_64-client-supplementary-7-debuginfo']

channel_trees['rhel-x86_64-hpc-node-7'] = [
'rhel-x86_64-hpc-node-7-debuginfo',
'rhel-x86_64-hpc-node-optional-7',
'rhel-x86_64-hpc-node-optional-7-debuginfo',
'rhel-x86_64-hpc-node-fastrack-7',
'rhel-x86_64-hpc-node-fastrack-7-debuginfo',
'rhel-x86_64-hpc-node-rh-common-7',
'rhel-x86_64-hpc-node-rh-common-7-debuginfo',
'rhel-x86_64-hpc-node-hpn-7',
'rhel-x86_64-hpc-node-hpn-7-debuginfo']

channel_trees['rhel-ppc64-server-7'] = [
'rhel-ppc64-server-7-debuginfo',
'rhel-ppc64-server-fastrack-7',
'rhel-ppc64-server-fastrack-7-debuginfo',
'rhel-ppc64-server-hpn-7',
'rhel-ppc64-server-hpn-7-debuginfo',
'rhel-ppc64-server-optional-7',
'rhel-ppc64-server-optional-7-debuginfo',
'rhel-ppc64-server-optional-fastrack-7',
'rhel-ppc64-server-optional-fastrack-7-debuginfo',
'rhel-ppc64-server-rh-common-7',
'rhel-ppc64-server-rh-common-7-debuginfo',
'rhel-ppc64-server-supplementary-7',
'rhel-ppc64-server-supplementary-7-debuginfo']

channel_trees['rhel-s390x-server-7'] = [
'rhel-s390x-server-7-debuginfo',
'rhel-s390x-server-fastrack-7',
'rhel-s390x-server-fastrack-7-debuginfo',
'rhel-s390x-server-optional-7',
'rhel-s390x-server-optional-7-debuginfo',
'rhel-s390x-server-optional-fastrack-7',
'rhel-s390x-server-optional-fastrack-7-debuginfo',
'rhel-s390x-server-rh-common-7',
'rhel-s390x-server-rh-common-7-debuginfo',
'rhel-s390x-server-supplementary-7',
'rhel-s390x-server-supplementary-7-debuginfo']

channel_trees['rhel-x86_64-server-7'] = [
'rhel-x86_64-server-7-debuginfo',
'rhel-x86_64-server-fastrack-7',
'rhel-x86_64-server-fastrack-7-debuginfo',
'rhel-x86_64-server-hpn-7',
'rhel-x86_64-server-hpn-7-debuginfo',
'rhel-x86_64-server-optional-7',
'rhel-x86_64-server-optional-7-debuginfo',
'rhel-x86_64-server-optional-fastrack-7',
'rhel-x86_64-server-optional-fastrack-7-debuginfo',
'rhel-x86_64-server-rh-common-7',
'rhel-x86_64-server-rh-common-7-debuginfo',
'rhel-x86_64-server-supplementary-7',
'rhel-x86_64-server-supplementary-7-debuginfo']

channel_trees['rhel-x86_64-workstation-7'] = [
'rhel-x86_64-workstation-7-debuginfo',
'rhel-x86_64-workstation-fastrack-7',
'rhel-x86_64-workstation-fastrack-7-debuginfo',
'rhel-x86_64-workstation-optional-7',
'rhel-x86_64-workstation-optional-7-debuginfo',
'rhel-x86_64-workstation-optional-fastrack-7',
'rhel-x86_64-workstation-optional-fastrack-7-debuginfo',
'rhel-x86_64-workstation-rh-common-7',
'rhel-x86_64-workstation-rh-common-7-debuginfo',
'rhel-x86_64-workstation-supplementary-7',
'rhel-x86_64-workstation-supplementary-7-debuginfo']

from xmlrpclib import Server
s = Server('https://sat-56-server.usersys.redhat.com/rpc/api')
token = s.auth.login('admin','nimda')

def create_channel(channel, parent = ''):
    print "Creating channel %s" % channel
    channel_list = channel.split('-')
    arch = channel_list[1]
    if arch == 'ppc64':
        arch = 'ppc'
    try:
        s.channel.software.create(token, channel,
                "Simulated channel for %s" % channel,
                'In leu of real RHEL 7 channels this channel has been created'
                + ' to simulate %s for RHSM transition testing' % channel,
                'channel-' + arch,
                parent, 'sha256',
                {'url': 'file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release'})
    except Exception as e:
        print "Warning, could not create channel %s" % channel
        print e

def create_dist_channel_map(channel):
    channel_list = channel.split('-')
    arch = channel_list[1]
    if arch == 'ppc64':
        arch = 'PPC'
    if channel_list[2] == 'server':
        os = 'RHEL Server'
        release = '7Server'
    elif channel_list[2] == 'client':
        os = 'RHEL Client'
        release = '7Client'
    elif channel_list[2] == 'workstation':
        os = 'RHEL Workstation'
        release = '7Workstation'
    elif channel_list[2] == 'hpc':
        os = 'RHEL Compute Node'
        release = '7ComputeNode'
    else:
        print "Error: could not determine dist channel map for %s" % channel
        return

    print "Creating dist channel map for %s" % channel
    print "  OS: %s, Release: %s, Arch: %s" % (os, release, arch)
    try:
        s.distchannel.setMapForOrg(token, os, release, arch, channel)
    except Exception as e:
        print "Warning, could not create dist channel map for %s" % channel
        print e

for parent in channel_trees:
    create_channel(parent)
    create_dist_channel_map(parent)
    for child in channel_trees[parent]:
        create_channel(child, parent)

print "Channel creation done, someone must log into the database and make the channels be owned by Red Hat."

s.auth.logout(token)
