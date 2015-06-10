#!/usr/bin/python
#
# This script was downloaded from https://bugzilla.redhat.com/attachment.cgi?id=1027300 and slightly modified
# This purpose of this script is to verify bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1223038
# Bug 1223038 - RepoActionInvoker.is_managed() broken in subscription-manager-1.14.5-1.el6.x86_64 /usr/share/rhsm/subscription_manager/repolib.py
#
import sys
import logging
LOG_FILENAME = 'ismanagedtest.log'
logging.basicConfig(filename=LOG_FILENAME,level=logging.DEBUG)

# read argument to this script
some_repo = str(sys.argv[1])

_LIBPATH = "/usr/share/rhsm"
# add to the path if need be
if _LIBPATH not in sys.path:
    sys.path.append(_LIBPATH)

from subscription_manager.injectioninit import init_dep_injection
init_dep_injection()
from subscription_manager.repolib import RepoActionInvoker
rai = RepoActionInvoker()

# Should print "False" if is_managed is working
print rai.is_managed(some_repo)
