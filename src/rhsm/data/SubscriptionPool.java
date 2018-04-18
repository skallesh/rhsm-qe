package rhsm.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class SubscriptionPool extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MM/dd/yyyy";	// 04/24/2011 https://bugzilla.redhat.com/show_bug.cgi?id=695234  https://bugzilla.redhat.com/show_bug.cgi?id=699442

	public SubscriptionPool() {
		super(null);
	}



	// abstraction fields
	public String productId;	// public String productSku;	// productSku was replaced by productId in subscription-manager-0.68-1.el6.i686  jsefler 7/13/2010
	public String subscriptionName;
	public List<String> provides;	// list of provided product names	// added bug RFE Bug 996993
	public String poolId;
	public String contract;	// added by Bug 1007580 - RFE: list-available output should include contract number and not sku
	public String quantity;	// public Integer quantity;	// can be "unlimited"
	public Integer suggested;	// introduced by Bug 1008557 - [RFE] CLI list --available should include a "Quantity Needed" field to facilitate compliance and provide parity with GUI; subscription-manager commit 411764095e1f3468381b0b27448e849876f0da66
	public Boolean multiEntitlement;	// replaced by subscriptionType
	public String subscriptionType;	// introduced by Bug 1029968 - [RFE] request for new subscription-manager list installed/consumed field called "Subscription Type"
	public Calendar endDate;
	public String machineType;
	public String serviceLevel;
	public String serviceType;
	
	public Calendar startDate;
	public Boolean activeSubscription;
	public Integer consumed;
	public ArrayList<ProductSubscription> associatedProductIDs;
	
	
	
	public Boolean isConsumed(){
		return (consumed > 0);
	}
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	public void addProductID(String productID){
		associatedProductIDs.add(new ProductSubscription(productID, this));
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj==null) return false;
		
		// Only consider a unique pool identifier when comparing two pools to see if they are equal (the same pool).

		// assumes productName is unique across all SubscriptionPools
		// return ((SubscriptionPool)obj).subscriptionName.contains(this.subscriptionName);	// jsefler 7/13/2010: this is not correct  

		// assumes productId is unique across all SubscriptionPools
		// return ((SubscriptionPool)obj).productId.equals(this.productId);	// jsefler 7/16/2010: this is not correct when is more than contract/serial has been issued for the same productId so as to increase the customers total quantity of available entitlements
		
		// assumes poolId is unique across all SubscriptionPools
		return ((SubscriptionPool)obj).poolId.equals(this.poolId);
	}
	
	public SubscriptionPool(String subscriptionName,
			Calendar startDate,
			Calendar endDate,
			Boolean activeSubscription,
			Integer consumed,
			String quantity,	//Integer quantity,
			Integer suggested,
			String id,
			String productId){
		super(null);
		
		this.subscriptionName = subscriptionName;
		this.startDate = startDate;
		this.endDate = endDate;
		this.activeSubscription = activeSubscription;
		this.consumed = consumed;
		this.quantity = quantity;
		this.suggested = suggested;
		this.poolId = id;
		this.productId = productId;

		associatedProductIDs = new ArrayList<ProductSubscription>();
	}
	
	public SubscriptionPool(
			String subscriptionName,
			String productId,
			String poolId,
			String quantity,
			Integer suggested,
			Boolean multiEntitlement,
			String endDate) {
		super(null);
		
		this.subscriptionName = subscriptionName;
		this.productId = productId;
		this.poolId = poolId;
		this.quantity = quantity;
		this.suggested = suggested;
		this.multiEntitlement = multiEntitlement;
		this.endDate = parseDateString(endDate);
		associatedProductIDs = new ArrayList<ProductSubscription>();
	}
	
	public SubscriptionPool(
			String productId,
			String poolId) {
		super(null);
		
		this.productId = productId;
		this.poolId = poolId;
		associatedProductIDs = new ArrayList<ProductSubscription>();
	}

	public SubscriptionPool(Map<String, String> poolMap) {
		super(poolMap);
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (productId != null)			string += String.format(" %s='%s'", "productId",productId);
		if (subscriptionName != null)	string += String.format(" %s='%s'", "subscriptionName",subscriptionName);
		if (provides != null)			string += String.format(" %s='%s'", "provides",provides);
		if (poolId != null)				string += String.format(" %s='%s'", "poolId",poolId);
		if (contract != null)			string += String.format(" %s='%s'", "contract",contract);
		if (quantity != null)			string += String.format(" %s='%s'", "quantity",quantity);
		if (suggested != null)			string += String.format(" %s='%s'", "suggested",suggested);
		if (consumed != null)			string += String.format(" %s='%s'", "consumed",consumed);
		if (multiEntitlement != null)	string += String.format(" %s='%s'", "multiEntitlement",multiEntitlement);
		if (subscriptionType != null)	string += String.format(" %s='%s'", "subscriptionType",subscriptionType);
		if (activeSubscription != null)	string += String.format(" %s='%s'", "activeSubscription",activeSubscription);
		if (startDate != null)			string += String.format(" %s='%s'", "startDate",formatDateString(startDate));
		if (endDate != null)			string += String.format(" %s='%s'", "endDate",formatDateString(endDate));
		if (machineType != null)		string += String.format(" %s='%s'", "machineType",machineType);
		if (serviceLevel != null)		string += String.format(" %s='%s'", "serviceLevel",serviceLevel);
		if (serviceType != null)		string += String.format(" %s='%s'", "serviceType",serviceType);

		return string.trim();
	}
	
	@Override
	protected Calendar parseDateString(String dateString){
		return parseDateString(dateString, simpleDateFormat);
	}
	
	//@Override
	public static String formatDateString(Calendar date){
		DateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
		return dateFormat.format(date.getTime());
	}
	
	/**
	 * @param stdoutListingOfAvailableSubscriptions - stdout from "subscription-manager-cli list --available"
	 * @return
	 */
	static public List<SubscriptionPool> parse(String stdoutListingOfAvailableSubscriptions) {
		/*
		# subscription-manager-cli list --available
		+-------------------------------------------+
    		Available Subscriptions
		+-------------------------------------------+
		
		Name:              	Basic RHEL Server        
		ProductId:         	MKT-simple-rhel-server-mkt
		PoolId:            	2                        
		quantity:          	10                       
		Expires:           	2011-07-01     
		
		ProductName:       	Scalable File System (1-2 sockets)
		ProductId:         	RH1414165                
		PoolId:            	8a8aa80d2be34ec0012be505f5a20600
		Quantity:          	1                        
		Expires:           	2011-10-24  
		
		ProductName:       	Red Hat Enterprise Linux Entitlement Alpha (1-2 Sockets)
                        	(Unlimited Virtualization)
		ProductId:         	RH1050830                
		PoolId:            	8a8aa80d2be34ec0012be505f34d05f9
		Quantity:          	1                        
		Expires:           	2011-01-24   
		
		ProductName:       	Red Hat Enterprise Linux Server Entitlement Beta for
                        	Certified Engineers and System Administrators - NOT FOR SALE
		ProductId:         	RH3036913                
		PoolId:            	8a9b90882da9ac9f012da9e5e991000e
		Quantity:          	9                        
		Expires:           	2011-07-19   
		


		ProductName:       	Awesome OS Scalable Filesystem
		ProductId:         	awesomeos-scalable-fs    
		PoolId:            	8a90f8c632878dd10132878f98390702
		Quantity:          	10                       
		Multi-Entitlement: 	Yes                      
		Expires:           	09/18/2012               
		MachineType:       	physical                 
		
		
		ProductName:       	Awesome OS Modifier      
		ProductId:         	awesomeos-modifier       
		PoolId:            	8a90f8c632878dd10132878f99020719
		Quantity:          	10                       
		Multi-Entitlement: 	No                       
		Expires:           	09/18/2012               
		MachineType:       	physical  
		
		
		Product Name:         	Awesome OS Server Bundled (2 Sockets, Standard Support)
		Product Id:           	awesomeos-server-2-socket-std
		Pool Id:              	8a90f814362d6d6d01362d6e90e702fa
		Quantity:             	5                        
		Service Level:        	Standard                 
		Service Type:        	L1-L3                    
		Multi-Entitlement:    	No                       
		Expires:              	04/18/2013               
		Machine Type:         	physical                 
		
		Product Name:         	Awesome OS with up to 4 virtual guests
		Product Id:           	awesomeos-virt-4         
		Pool Id:              	8a90f814362d6d6d01362d6e9607044d
		Quantity:             	5                        
		Service Level:        	                         
		Service Type:        	                         
		Multi-Entitlement:    	Yes                      
		Expires:              	04/18/2013               
		Machine Type:         	physical  
		
		
		// after changes from: Bug 806986 - Subscription-Manager should refer to subscription name and product name
		Subscription Name:    	Awesome OS Workstation Basic
		SKU:                  	awesomeos-workstation-basic
		Pool Id:              	8a90f8753859805a01385981646002d4
		Quantity:             	10
		Service Level:        	Standard
		Service Type:         	L1-L3
		Multi-Entitlement:    	No
		Ends:                 	07/04/2013
		Machine Type:         	physical
		
		// after changes from: Bug 1029968 - [RFE] request for new subscription-manager list installed/consumed field called "Subscription Type"
		Subscription Name: Awesome OS with up to 4 virtual guests
		Provides:          Awesome OS Server Bits
		SKU:               awesomeos-virt-4
		Contract:          0
		Pool ID:           8a90874042fc428e0142fc4369000f29
		Available:         5
		Suggested:         1
		Service Level:     
		Service Type:      
		Subscription Type: Multi-Entitleable
		Ends:              12/15/2014
		System Type:       Physical
		*/
		
		

		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field					regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("subscriptionName",			"Subscription Name:(.*(\\n.*?)+)^\\w+\\s?\\w+:");	// was "Product Name"; changed by Bug 806986	// this assumes that ProductName is NOT last in its subscription grouping since ^\w+\s?\w+: represents the start of the next property so as to capture a multi-line value
		regexes.put("provides",					"Provides:(.*(\\n.*?)+)^\\w+\\s?\\w+:");	// this assumes that Provides is NOT last in its subscription grouping since ^\w+\s?\w+: represents the start of the next property so as to capture a multi-line value	// added by RFE Bug 996993
		regexes.put("productId",				"SKU:(.*)");		// "Product Id"; changed by Bug 806986
		regexes.put("contract",					"Contract:(.*)");	// "Contract"; added by Bug 1007580
		regexes.put("poolId",					"Pool ID:(.*)");	// "Pool Id:(.*)"); changed by Bug 878634
		regexes.put("quantity",					"(?:Quantity:|Available:)(.*)");	// "Quantity:(.*)"); changed by Bug 986971	// "quantity:(.*)"); changed by Bug 612730	// may eventually be changed by Bug 963874
		regexes.put("suggested",				"Suggested:(.*)");	// added by Bug 1008557 and Bug 1088372
		regexes.put("serviceLevel",				"Service Level:(.*)");
		regexes.put("serviceType",				"Service Type:(.*)");
		regexes.put("multiEntitlement",			"Multi-Entitlement:(.*)");
		regexes.put("subscriptionType",			"Subscription Type:(.*)");	// added by Bug 1029968 (replaces multiEntitlement)	// for possible values see https://bugzilla.redhat.com/show_bug.cgi?id=1029968#c2
		regexes.put("startDate",				"Starts:(.*)");	// added by commit 42004d4d5402802a42f0763caa6615948621dadc	// subscription-manager-1.21.2-1	// for the benefit of RFE Bug 1479353
		regexes.put("endDate",					"Ends:(.*)");
		regexes.put("machineType",				"System Type:(.*)");	// changed by bug 874760	"Machine Type:(.*)");
	
		List<Map<String,String>> listOfAvailableSubscriptionMaps = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutListingOfAvailableSubscriptions, listOfAvailableSubscriptionMaps, field);
		}
		
		// assemble a new List of SubscriptionPool from the List of Available Subscription Maps
		List<SubscriptionPool> subscriptionPools = new ArrayList<SubscriptionPool>();
		for(Map<String,String> poolMap : listOfAvailableSubscriptionMaps) {
			// normalize newlines from subscriptionName when it spans multiple lines
			String key = "subscriptionName", subscriptionName = poolMap.get(key);
			if (subscriptionName!=null) {
				poolMap.remove(key);
				subscriptionName = subscriptionName.replaceAll("\\s*\\n\\s*", " ");
				poolMap.put(key, subscriptionName);
			}
			subscriptionPools.add(new SubscriptionPool(poolMap));
		}
		return subscriptionPools;
	}

}
