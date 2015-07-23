#!/usr/bin/python
#
# This script is based on the test scenario from https://github.com/candlepin/subscription-manager/issues/1006
# This purpose of this script is to verify bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1195446
# Bug 1195446 - python-rhsm sets socket.setdefaulttimeout(60) _always_
#
import socket
import yum

yb = yum.YumBase()

print socket.getdefaulttimeout()
yb.getReposFromConfig()
print socket.getdefaulttimeout()
