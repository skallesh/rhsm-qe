#!/usr/bin/python3
#
# This script is based on the test scenario from https://github.com/candlepin/subscription-manager/issues/1006
# This purpose of this script is to verify bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1195446
# Bug 1195446 - python-rhsm sets socket.setdefaulttimeout(60) _always_
#
import socket
from dnf.cli import main

print(socket.getdefaulttimeout())
main.user_main(['repolist'])
print(socket.getdefaulttimeout())
