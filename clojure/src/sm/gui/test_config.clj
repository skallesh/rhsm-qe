(ns sm.gui.test-config
  (:import [com.redhat.qe.auto.testng TestScript]))

;;Sets up logging
(def testscript (TestScript.) )

(defprotocol Defaultable
  (default [this] "returns the default value if the key is not found")
  (mapkey [this] "returns the key to get the value"))

(defrecord DefaultMapKey[key default]
  Defaultable
  (default [this] (:default this))
  (mapkey [this] (:key this)))

(extend-protocol Defaultable
  java.lang.String
  (default [this] nil)
  (mapkey [this] this))

(defn property-map [map]
  (zipmap (keys map)
	  (for [v (vals map) ]
	    (System/getProperty (mapkey v) (default v)))))

(def properties
     (let [m (property-map {:binary-path (DefaultMapKey. "sm.gui.binary" "subscription-manager-gui")
			    :client-hostname "sm.client1.hostname"
			    :username "sm.client1.username"
			    :password "sm.client1.password" })]
       (merge m (property-map
		 {:ldtp-url (DefaultMapKey. "sm.ldtp.url"
			      (str "http://" (m :client-hostname) ":4118"))}))))

 
(def config (merge properties
		   {:mystuff :goes-here}))

