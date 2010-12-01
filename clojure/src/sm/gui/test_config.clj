(ns sm.gui.test-config
  (:import [com.redhat.qe.auto.testng TestScript]))

;;Sets up logging
(def testscript (TestScript.) )



(defn property-map [map]
  (zipmap (keys map) (for [v (vals map) ]
		       (System/getProperty (cond (string? v) v
						 (vector? v) (first v)
						 :else (throw new IllegalArgumentException (str "Value must be string or vector, got " v)))
					   ()))))

(def properties (property-map {:binary-path "sm.gui.binary"
			       :client-hostname "sm.client1.hostname"}))

(defn default-property-map [map]
  )
(def default-properties {:ldtp-url ["sm.ldtp.url"
				    (str "http://" (properties :client-hostname) ":4118")]
			 })

(def config (merge properties
		   {:mystuff :goes-here}))