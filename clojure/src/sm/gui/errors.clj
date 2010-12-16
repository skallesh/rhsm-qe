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
(def *error* nil)
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
  (let [recoveryval (recovery err)]
    (cond (nil? recoveryval) (throw (IllegalStateException. (str "Recovery chosen that does not exist: " recovery)))
          (fn? recoveryval) (recoveryval)
          :else recoveryval)))

(defn dispatch "Thread the map err through all the handler functions
in hlist, until one of them returns something other than err (or a
superset of err). Return the first non-err value, or err if the end of
the list is hit."
  [err hlist]
  (let [handled (drop-while  #(equal-or-more? err %)
                             (reductions #(%2 %1) err hlist ))]
    (if (> (count handled) 0) (first handled) err)))

(defmacro with-handler [hlist & body]
  (if-not (coll? list) throw (IllegalArgumentException. "First argument to with-handler must be a collection of handlers"))
  `(binding [*handler* (concat ~hlist *handler*) ] ;chain handlers together
     (try ~@body
          (catch NamingException ne#
            (let [unwrapped# (unwrap ne#)
                  handler-result# (dispatch unwrapped# *handlers*)]
              (cond (equal-or-more? unwrapped# handler-result#) (throw ne#) ;returning the original map means unhandled
                    (keyword? handler-result#) (binding [*error* unwrapped#]
                                                 (recover handler-result# unwrapped#))
                    :else handler-result#))))))


(defmacro add-recoveries [m & body]
  `(try ~@body
        (catch NamingException ne#
          (throw (rewrap ne# ~m)))))

(defmacro type-handler [type-kw & body]
  `(fn [e#] (if (= (:type e#) ~type-kw)
            (do ~@body)
            e#)))

(defn error-prone [n]
  (if (> n 0) (inc n) (throw (wrap {:msg "Negative number!" :number n :type :NumberError}))))

(comment (defn do-stuff [n]
           (add-recoveries {:zero 0
                            :retry (fn [] (error-prone (Math/abs (:number *error*))))}
                            (error-prone n)))

         (with-handler (fn [e] (if (= (:type e) :NumberError) :retry)) (do-stuff -5))
         
         )

