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

# imported to support logging in subscription-manager >= 1.17.10-1 from Bug 1334916: Move logging configuration to rhsm.conf
import logging
log = logging.getLogger('rhsm-app.' + __name__)


class ProductIdInstallTestPlugin(SubManPlugin):
    """Plugin triggered when product id certs are installed"""
    name = "product_install_test"

    def pre_product_id_install_hook(self, conduit):
        """pre_product_id_install hook

        Args:
            conduit: A ProductConduit()
        """
        #print dir(conduit)
        #print conduit.consumer_uuid
        conduit.log.info("Running pre_product_id_install_hook: yum product-id plugin is about to install a product cert")
        log.info        ("Running pre_product_id_install_hook: yum product-id plugin is about to install a product cert")
        #conduit.log.info("Running pre_product_id_install_hook: %s product_ids are about to be installed" % len(conduit.product_list))
        #log.info        ("Running pre_product_id_install_hook: %s product_ids are about to be installed" % len(conduit.product_list))

    def post_product_id_install_hook(self, conduit):
        """`post_product_id_install` hook

        Args:
            conduit: A ProductConduit()
        """
        #print dir(conduit)
        #print conduit.consumer_uuid
        conduit.log.info("Running post_product_id_install_hook: yum product-id plugin just installed a product cert")
        log.info        ("Running post_product_id_install_hook: yum product-id plugin just installed a product cert")
        conduit.log.info("Running post_product_id_install_hook: %s product_ids were just installed" % len(conduit.product_list))
        log.info        ("Running post_product_id_install_hook: %s product_ids were just installed" % len(conduit.product_list))

        # print out the product cert list
        for product_cert in conduit.product_list:
            #print "product_cert ", product_cert
            #print "dir(product_cert) ", dir(product_cert)
            #print "products ", product_cert.products
            #print "products length", len(product_cert.products)
            #print product_cert.products[0]
            #print dir(product_cert.products[0])
            for product in product_cert.products:
                conduit.log.info("Running post_product_id_install_hook: product_id %s was just installed" % product.id)
                log.info        ("Running post_product_id_install_hook: product_id %s was just installed" % product.id)
        
