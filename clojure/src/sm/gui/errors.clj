(ns sm.gui.errors
  (:require [clojure.contrib.error-kit :as errkit]))

;;parent error
(errkit/deferror *sm-error* [] "Indicates an error dialog has appeared in the application." [s]
  {:msg (str "Error dialog is present with message: " s)
   :unhandled (errkit/throw-msg RuntimeException)})

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