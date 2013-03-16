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


class SubscribeTestPlugin(SubManPlugin):
    """Plugin triggered when a consumer subscribes to an entitlement"""
    name = "subscribe_test"

    def pre_subscribe_hook(self, conduit):
        """`pre_subscribe` hook

        Args:
            conduit: A SubscriptionConduit()
        """
        conduit.log.info("Running pre_subscribe_hook: system is about to subscribe.")

    def post_subscribe_hook(self, conduit):
        """`post_subscribe` hook

        Args:
            conduit: A PostSubscriptionConduit()
        """
        conduit.log.info("Running post_subscribe_hook: system just subscribed.")


