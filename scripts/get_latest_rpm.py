#!/usr/bin/python
# mod_python wrapper around latest_rpm.py
# place in same directory as latest_rpm.py

from latest_rpm import *
from mod_python import apache, util

BASE_URL = "http://download.devel.redhat.com/brewroot/packages/%s/"

def handler(request):
    request.headers_out.add("version", "0.1")
    request.send_http_header()
    
    params = util.FieldStorage(request)

    baseurl = params.get('baseurl', BASE_URL)
    rpmname = params.get('rpmname','')
    basegrp = params.get('basegrp',rpmname)
    getlink = params.get('getlink', 'false')
    arch    = params.get('arch', "")
    version = params.get('version','')

    rpmurl = find_latest_rpm_url(baseurl%basegrp, arch, rpmname, version)
    if 'true' in getlink:
        request.content_type = 'text/plain'
        request.send_http_header()
        request.write(rpmurl)
    else:
        rpm = urllib2.urlopen(rpmurl)
        request.content_type = 'application/x-rpm'
        request.send_http_header()
        request.write(rpm.read())
    return apache.OK
