#!/usr/bin/python
# mod_python wrapper around latest_rpm.py
# place in same directory as latest_rpm.py
#
# To install:
# yum -y install mod_python python-BeautifulSoup
# mkdir /var/www/html/latestrpm
# rsync get_latest_rpm.py and latest_rpm.py to that folder
# chmod 755 those files
# edit /etc/httpd/conf.d/python.conf and add:
#<Directory /var/www/html/latestrpm>
#        AddHandler mod_python .py
#        PythonHandler get_latest_rpm
#        PythonDebug On
#</Directory>
#
# service httpd restart
# sample evocation:
# http://auto-services.usersys.redhat.com/latestrpm/get_latest_rpm.py?arch=noarch&basegrp=subscription-manager-migration-data&version=1.12.2&release=el6&rpmname=subscription-manager-migration-data
#




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
