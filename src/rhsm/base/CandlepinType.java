package rhsm.base;

/**
 * hosted (indicative of testing against subscription.rhn.redhat.com / subscription.rhn.stage.redhat.com; candlepin.conf candlepin.standalone=false) <BR>
 * standalone (indicative of an onPremise deployment of candlepin; candlepin.conf candlepin.standalone=true) <BR>
 * katello <BR>
 * sam <BR>
 * 
 * @author jsefler
 */
public enum CandlepinType  { hosted, standalone, katello, sam }


//hosted (indicative of testing against subscription.rhn.redhat.com / subscription.rhn.stage.redhat.com; candlepin.conf candlepin.standalone=false)
//standalone (indicative of an onPremise deployment of candlepin; candlepin.conf candlepin.standalone=true)
//katello
//sam