(ns sm.gui.condition
  (:import [javax.naming NamingException]))

(defprotocol Raisable
  (raise [this])
  (msg [this])
  (errdata [this]))

(defrecord NumberError [n]
  Raisable
  (msg [this] (str "Number blah blah error! " n))
  (raise [this] (raise (Exception. (msg this)))))
  
(extend-protocol Raisable
  clojure.lang.IPersistentMap
  (raise [this] (let [e (NamingException.)] (.setResolvedObj e this) (throw e)))

  java.lang.Throwable
  (raise [this] (let [e (NamingException. (.getMessage this))] (.setRootCause e this) (throw e))))

(defprotocol Handler
  (handle [this raisable])
  (handles [this raisable]))

(defrecord TypeHandler [type raisable myfn] Handler
  (handle [this raisable] (myfn raisable))
  (handles [this raisable] (instance? type raisable)))

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