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


class AutoAttachTestPlugin(SubManPlugin):
    """Plugin triggered when a consumer auto_attaches entitlements"""
    name = "auto_attach_test"

    def pre_auto_attach_hook(self, conduit):
        """`pre_auto_attach` hook

        Args:
            conduit: An AutoAttachConduit()
        """
        #print dir(conduit)
        #print conduit.consumer_uuid
        conduit.log.info("Running pre_auto_attach_hook: system is about to auto-attach")
        conduit.log.info("Running pre_auto_attach_hook: auto-attaching consumer is %s" % conduit.consumer_uuid)

    def post_auto_attach_hook(self, conduit):
        """`post_auto_attach` hook

        Args:
            conduit: An AutoAttachConduit()
        """
        #print dir(conduit)
        #print conduit.consumer_uuid
        conduit.log.info("Running post_auto_attach_hook: system just auto-attached")
        conduit.log.info("Running post_auto_attach_hook: auto-attached consumer is %s" % conduit.consumer_uuid)
        if conduit.entitlement_data is None:
            conduit.log.info("Running post_auto_attach_hook: auto-attached %d entitlements" % 0)
        else:
            conduit.log.info("Running post_auto_attach_hook: auto-attached %d entitlements" % len(conduit.entitlement_data))

