#!/usr/bin/python

test_output = """{
  "tag" : {
    "maven_support" : false,
    "locked" : false,
    "name" : "trashcan",
    "perm" : "admin",
    "perm_id" : 1,
    "arches" : null,
    "maven_include_all" : false,
    "id" : 1187
  },
  "force" : true,
  "build" : {
    "owner_name" : "jesusr",
    "package_name" : "subscription-manager",
    "task_id" : 6163658,
    "volume_name" : "DEFAULT",
    "owner_id" : 330,
    "creation_event_id" : 7261744,
    "creation_time" : "2013-08-13 14:02:45.372464",
    "state" : 1,
    "nvr" : "subscription-manager-1.8.20-1.el6_3",
    "completion_time" : "2013-08-13 14:10:32.782029",
    "epoch" : null,
    "version" : "1.8.20",
    "creation_ts" : 1.37641696537246E9,
    "volume_id" : 0,
    "release" : "1.el6_3",
    "package_id" : 19375,
    "completion_ts" : 1.37641743278203E9,
    "id" : 286661,
    "name" : "subscription-manager"
  },
  "user" : {
    "status" : 0,
    "usertype" : 0,
    "krb_principal" : "host/x86-013.build.bos.redhat.com@REDHAT.COM",
    "id" : 1181,
    "name" : "host/x86-013.build.bos.redhat.com"
  },
  "rpms" : {
    "s390x" : [ "subscription-manager-1.8.20-1.el6_3.s390x.rpm", "subscription-manager-debuginfo-1.8.20-1.el6_3.s390x.rpm", "subscription-manager-firstboot-1.8.20-1.el6_3.s390x.rpm", "subscription-manager-gui-1.8.20-1.el6_3.s390x.rpm", "subscription-manager-migration-1.8.20-1.el6_3.s390x.rpm" ],
    "i686" : [ "subscription-manager-1.8.20-1.el6_3.i686.rpm", "subscription-manager-debuginfo-1.8.20-1.el6_3.i686.rpm", "subscription-manager-firstboot-1.8.20-1.el6_3.i686.rpm", "subscription-manager-gui-1.8.20-1.el6_3.i686.rpm", "subscription-manager-migration-1.8.20-1.el6_3.i686.rpm" ],
    "ppc64" : [ "subscription-manager-1.8.20-1.el6_3.ppc64.rpm", "subscription-manager-debuginfo-1.8.20-1.el6_3.ppc64.rpm", "subscription-manager-firstboot-1.8.20-1.el6_3.ppc64.rpm", "subscription-manager-gui-1.8.20-1.el6_3.ppc64.rpm", "subscription-manager-migration-1.8.20-1.el6_3.ppc64.rpm" ],
    "x86_64" : [ "subscription-manager-1.8.20-1.el6_3.x86_64.rpm", "subscription-manager-debuginfo-1.8.20-1.el6_3.x86_64.rpm", "subscription-manager-firstboot-1.8.20-1.el6_3.x86_64.rpm", "subscription-manager-gui-1.8.20-1.el6_3.x86_64.rpm", "subscription-manager-migration-1.8.20-1.el6_3.x86_64.rpm" ],
    "s390" : [ "subscription-manager-1.8.20-1.el6_3.s390.rpm", "subscription-manager-debuginfo-1.8.20-1.el6_3.s390.rpm", "subscription-manager-firstboot-1.8.20-1.el6_3.s390.rpm", "subscription-manager-gui-1.8.20-1.el6_3.s390.rpm", "subscription-manager-migration-1.8.20-1.el6_3.s390.rpm" ],
    "ppc" : [ "subscription-manager-1.8.20-1.el6_3.ppc.rpm", "subscription-manager-debuginfo-1.8.20-1.el6_3.ppc.rpm", "subscription-manager-firstboot-1.8.20-1.el6_3.ppc.rpm", "subscription-manager-gui-1.8.20-1.el6_3.ppc.rpm", "subscription-manager-migration-1.8.20-1.el6_3.ppc.rpm" ],
    "src" : [ "subscription-manager-1.8.20-1.el6_3.src.rpm" ]
  },
  "tags" : [ "trashcan" ]
}"""

import json
import os
import sys

ci_message = os.getenv('CI_MESSAGE')
if not ci_message: #for testing
  ci_message = test_output

myjson = json.loads(ci_message)

arch = os.getenv('DISTRO_ARCH')
arches = myjson['rpms'].keys()
if arch and arch not in arches:
  print "DISTRO_ARCH not found in availabe arches!"
  sys.exit(1)

if not arch and 'x86_64' in arches:
  arch = 'x86_64'
elif not arch and 'noarch' in arches:
  arch = 'noarch'
else:
  print "No arches found!"
  sys.exit(1)

build = myjson['build']
url = 'http://download.devel.redhat.com/brewroot/packages'
path = "%s/%s/%s/%s/%s" % (url, build['name'], build['version'], build['release'], arch)

urls = []
for i in myjson['rpms'][arch]:
  urls.append("%s/%s" % (path, i))

print ', '.join(urls)