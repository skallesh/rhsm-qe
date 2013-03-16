#
# Copyright (c) 2013 Red Hat, Inc.
#
# This software is licensed to you under the GNU General Public License,
# version 2 (GPLv2). There is NO WARRANTY for this software, express or
# implied, including the implied warranties of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
# along with this software; if not, see
# http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
#
# Red Hat trademarks are not licensed under GPLv2. No permission is
# granted to use or replicate Red Hat trademarks that are incorporated
# in this software or its documentation.
#

from subscription_manager.base_plugin import SubManPlugin
requires_api_version = "1.0"

import subprocess
import simplejson as json


class FactsCollectionTestPlugin(SubManPlugin):
    """Plugin for adding additional facts to subscription-manager facts"""
    name = "facts_collection_test"

    def post_facts_collection_hook(self, conduit):
        """'post_facts_collection' hook to add facter facts

        Args:
            conduit: A FactsConduit()
        """
        conduit.log.info("Running post_facts_collection_hook: consumer facts count is %s" % len(conduit.facts))
