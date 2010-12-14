(ns sm.gui.errors)

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


(defmacro assert
  "Evaluates expr and throws an exception if it does not evaluate to logical true."
  {:added "1.0"}
  [x]
  (when *assert*
    (let [bindings (local-bindings &env)]
      `(when-not ~x
         (let [sep#  (System/getProperty "line.separator")
               form# '~x]
           (throw (AssertionError. (apply str "Assert failed: " (pr-str form#) sep#
                                          (map (fn [[k# v#]] 
                                                 (when (some #{k#} form#) 
                                                   (str "\t" k# " : " v# sep#))) 
                                               ~bindings)))))))))