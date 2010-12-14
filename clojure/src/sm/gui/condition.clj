(ns sm.gui.condition
  (:import [javax.naming NamingException]))


(defn wrap [raisable msg]
  (let [e (NamingException. msg)]
    (.setResolvedObj e raisable)
    e))

(defprotocol Raisable
  (raise [this]))

(defprotocol Condition
  (data [this])
  (is-instance? [this otherclass]))

(defrecord Err [msg data]
  Raisable
  (raise [this] (throw (wrap this msg)))
  Condition
  (data [this] data))

(extend-protocol Raisable
  clojure.lang.IPersistentMap
  (raise [this] (raise (Err. (:msg this) (dissoc this :msg))))

  java.lang.Throwable
  (raise [this] (throw (wrap this))))

(extend-protocol Condition)


(defn unwrap [e]
  (let [r (.getResolvedObj e)]
    (if (satisfies? Condition r) r (throw (IllegalStateException. "Wrapped object is not a Condition - must be a real NamingException?")))))


(defprotocol Handler
  (handle [this raisable])
  (handles [this raisable]))

(defrecord TypeHandler [type myfn] Handler
  (handle [this throwable] (myfn (unwrap throwable)))
  (handles [this throwable]
	   (instance? type (try
			     (instance? type (unwrap throwable))
			     (catch IllegalStateException ise false)))))

(defrecord Error [type parent] Condition
  )


(defrecord NumberError [number]
  Condition 
  (data [this] number)
  (is-instance? [this otherclass] ())
  Raisable
  (raise [this msg data] (throw (wrap this msg data))
	 )
  (msg [this] (str "Number blah blah error! " number)))

(defn myrun [handler myfn]
  (try
    (myfn)
    (catch Throwable t (if (handles handler t)
			 (handle handler t)))))


(comment

  (with-handlers [( TypeHandler. MyType (fn [e] (println e)))
		  (OtherHandler )])
  
  (defprotocol Handleable
   (handle [this]))

	 (extend-protocol Handleable
	   java.lang.Throwable
	   (handle [this] )))