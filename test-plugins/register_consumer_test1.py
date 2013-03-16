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


class RegisterConsumerTestPlugin(SubManPlugin):
    """Plugin triggered when a consumer registers"""
    name = "register_consumer_test"

    def pre_register_consumer_hook(self, conduit):
        """`pre_register_consumer` hook

        Args:
            conduit: A RegistrationConduit()
        """
        conduit.log.info("Running pre_register_consumer_hook 1: system name %s is about to be registered." % conduit.name)
        conduit.log.info("Running pre_register_consumer_hook 1: consumer facts count is %s" % len(conduit.facts))

    def post_register_consumer_hook(self, conduit):
        """`post_register_consumer` hook

        Args:
            conduit: A RegistrationConduit()
        """
        conduit.log.info("Running post_register_consumer_hook 1: consumer uuid %s is now registered." % conduit.consumer['uuid'])

