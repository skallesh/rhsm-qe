(ns sm.gui.condition
  (:import [javax.naming NamingException]))


(defn wrap [raisable msg]
  (let [e (NamingException. msg)]
    (.setResolvedObj e raisable)
    e))

(defprotocol Raisable
  (raise [this])
  (msg [this]))

(defprotocol Condition
  (data [this]))

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



(defrecord NumberError [number]
  Condition 
  (data [this] number)
  Raisable
  (raise [this] (throw (wrap this (msg this))))
  (msg [this] (str "Number blah blah error! " number)))

(defn myrun [handler myfn]
  (try
    (myfn)
    (catch Throwable t (if (handles handler t)
			 (handle handler t)))))


(comment

  
  
  (defprotocol Handleable
   (handle [this]))

	 (extend-protocol Handleable
	   java.lang.Throwable
	   (handle [this] )))