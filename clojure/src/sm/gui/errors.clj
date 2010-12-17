(ns sm.gui.errors
  (:require [clojure.contrib.logging :as log])
  (:import [javax.naming NamingException]))

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {:invalid-credentials #"Invalid username"
		   :wrong-consumer-type #"Consumers of this type are not allowed" })

(defn matching-error "Returns a keyword of known error, if the message matches any of them."
  [message]
  (let [matches-message? (fn [key] (let [re (known-errors key)]
				       (if (re-find re message) key false)))]
    (or (some matches-message? (keys known-errors))
	:sm-error)))

(defn handle [e handler recoveries]
  (let [result (handler (matching-error (or (.getMessage e) "")))
	rk (:recovery result)]
    (if rk (let [recfn (recoveries rk)]
	     (if recfn (recfn)
		 (throw (IllegalStateException. (str "Unknown error recovery strategy: " rk) e)))))
    (if-not (:handled result) 
      (throw e))))

(defn ^{:private true} local-bindings
  "Produces a map of the names of local bindings to their values."
  [env]
  (let [symbols (map key env)]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))


(defmacro verify
  "Evaluates expr and either logs what was evaluated, or throws an exception if it does not evaluate to logical true."
  [x]
  (let [bindings (local-bindings &env)]
    `(let [res# ~x
           sep#  (System/getProperty "line.separator")
           form# '~x
           msg# (apply str (if res# "Verified: " "Verification failed: ") (pr-str form#) sep#
                       (map (fn [[k# v#]] 
                              (when (some #{k#} (flatten form#)) 
                                (str "\t" k# " : " v# sep#))) 
                            ~bindings))]
       (if res# (log/info msg#) (throw (AssertionError. msg#))))))


(def *handlers* [])
(defn wrap [m]
  (let [e (NamingException. (or (:msg m) ""))]
    (.setResolvedObj e m)
    e))

(defn rewrap [e addmap]
  (let [m (unwrap e)
        m (merge addmap m)]
    (.setResolvedObj e m)
    e))

(defn unwrap [e]
  (let [r (.getResolvedObj e)]
    (if (map? r) r (throw (IllegalStateException. "Wrapped object is not a map - must be a real NamingException?")))))

(defn equal-or-more? [m1 m2]
  (cond (= m1 m2) true
        (not (and (map? m1) (map? m2))) false
        (= m1 (select-keys m2 (keys m1))) true))


(defn recover [recovery err]
  (let [recoveryfn (recovery err)]
    (cond (nil? recoveryfn) (throw (IllegalStateException. (str "Recovery chosen that does not exist: " recovery)))
          (fn? recoveryfn) (recoveryfn err)
          :else (throw (IllegalArgumentException. (format "Recovery %s needs to a function with one argument, instead got: %s" recovery recoveryfn))))))

(defn dispatch "Thread the map m through all the handler functions
in hlist, until one of them returns something other than m.
Return the first non-m value, or m if the end of
the list is hit."
  [m hlist]
  (println hlist)
  (let [handled (drop-while  #(= m %)
                             (reductions #(%2 %1) m hlist))]
    (if (> (count handled) 0)
      (first handled) m)))

(defmacro with-handlers [hlist & body]
  (println (coll? hlist) hlist)
  (if-not (and (coll? hlist) (every? coll? hlist)) (throw (IllegalArgumentException. "First argument to with-handler must be a collection of handlers")))
  `(binding [*handlers* (concat ~hlist *handlers*) ] ;chain handlers together
     (try ~@body
          (catch NamingException ne#
            (let [unwrapped# (unwrap ne#)
                  handler-result# (dispatch unwrapped# *handlers*)]
              (cond (equal-or-more? unwrapped# handler-result#) (throw ne#) ;returning the original map means unhandled
                    (and (map? handler-result#) (:recovery handler-result#))
                     (recover (:recovery handler-result#) unwrapped#)
                    :else handler-result#))))))

(defmacro add-recoveries [m & body]
  `(try ~@body
        (catch NamingException ne#
          (throw (rewrap ne# ~m)))))

(defmacro handle-type [type-kw & body]
  `(fn [e#] (if (= (:type e#) ~type-kw)
            (do ~@body)
            e#)))

(defn error-prone [n]
  (if (> n 0) (inc n) (throw (wrap {:msg "Negative number!" :number n :type :NumberError}))))

(comment (defn do-stuff [n]
           (add-recoveries {:zero (fn [e] 0)
                            :retry (fn [e] (error-prone (Math/abs (:number e))))}
                            (error-prone n)))

         (with-handlers (fn [e] (if (= (:type e) :NumberError) {:recovery :retry} )) (do-stuff -5))
         (with-handlers
           [ (handle-type :NumberError {:recovery :one})
             (handle-type :OtherError 0)]
           (add-recoveries {:one (fn [e] 1)} (do-stuff -5)))
         )

