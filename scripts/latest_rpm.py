#!/usr/lib/python
# Latest Brew RPM Finder
# Digs through Brewroot directories to find the latest release of an RPM
# place in /var/www/python/
# Author: Steve Salevan <ssalevan@redhat.com>
# Author: James Molet <jmolet@redhat.com> (current)
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Library General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

import urllib2
import re
import sys
import pdb

from itertools import groupby
from BeautifulSoup import BeautifulSoup

VERSION_REGEX = "[0-9]+\.[0-9]+/"
EPOCH_REGEX   = "\d+\.\w+/"

def get_links_matching_regex(regex, page):
    soup = BeautifulSoup(page)
    links = soup.findAll('a')
    matches = []
    for link in links:
        if (re.search(regex, link.string)):
            matches.append(link)
    return matches

def rchop(thestring, ending):
  if thestring.endswith(ending):
    newstring = thestring[:-len(ending)]
    if newstring.endswith(ending):
      rchop(newstring, ending)
    else:
      return newstring
  return thestring

def sort_by_version(links):
    versions = []
    for item in links: versions.append(str(item['href'].replace('/','')))

    demarcations=0
    for item in versions:
        fields = len(item.split("."))
        if fields > demarcations:
            demarcations = fields

    version_list = []
    for item in versions:
        fields = len(item.split("."))
        version_list.append(item + ('.0')*(demarcations-fields))

    cur_list = version_list

    n=1
    while n < demarcations:
        new_list = []
        group_func = lambda x: ".".join(x.split(".")[0:-n])
        sort_key_func = lambda x: int(x.split(".")[demarcations - n])

        cur_list.sort(key=group_func)

        for key, group in groupby(cur_list, group_func):
            group_list = list(group)
            group_list.sort(key=sort_key_func)
            new_list += group_list

        cur_list = new_list
        n += 1

    for item in cur_list:
        #cur_list[cur_list.index(item)] = str(rchop(item, ".0") + '/')
        cur_list[cur_list.index(item)] = str(item + '/')

    return cur_list


def find_latest_rpm_url(baseurl, arch, rpm_name, version='', release='', regress=False):
    version_page = urllib2.urlopen(baseurl)
    if version == '':
        VREGEX = VERSION_REGEX
    else:
        VREGEX = version + "[\.\d+]*/"
    if release == '':
        EREGEX = EPOCH_REGEX
    else:
        EREGEX = "\d+\." + release + "/"
    version_links = get_links_matching_regex(VREGEX, version_page)
    version_links = sort_by_version(version_links)
    version_links.reverse()
    for i in xrange(len(version_links)):
        version_append = version_links[i]
        epoch_page = urllib2.urlopen(baseurl+version_append)
        epoch_links = get_links_matching_regex(EREGEX, epoch_page)
        if epoch_links or not regress:
            break
    epoch_append = epoch_links[len(epoch_links)-1]['href']
    rpm_page = urllib2.urlopen(baseurl+version_append+epoch_append+arch+'/')
    rpm_links = get_links_matching_regex(rpm_name, rpm_page)
    rpm_append = rpm_links[0]['href']
    return baseurl+version_append+epoch_append+arch+'/'+rpm_append

if __name__ in "__main__":
    regress = sys.argv[6]
    if regress.lower() == 'true':
        regress=True
    else:
        regress=False
    print find_latest_rpm_url(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], regress)

