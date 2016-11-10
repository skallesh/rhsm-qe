(ns rhsm.errors.classification
  (:use [slingshot.slingshot :only (try+ throw+)]
        [clojure.core.match :only (match)])
  (:require [clojure.tools.logging :as log]))

(defprotocol IFailureClassifier
  "Methods related to classifying of exceptions mainly.
  It is important to distinguish errors and exceptions
  from TestNG report results perspective."
  (failure-level [a]
    "returns:
      -  :testware-problem
      -  :verification-failure
      -  :skip-exception
      -  :network-error
      -  :unknown-exception
      -  :unknown"))

(defmacro normalize-exception-types
  "This macro catches an exception
  and tries to find out 'failure-level' of the exception.
  After that it raises the proper exception when 'failure-level'
  requires so."
  [& body]
  `(try+
    ~@body
    (catch Object e#
      (-> (failure-level e#)
          (match :verification-failure :re-throw
                 :network-error        :throw-as-skipped
                 :else                 :no-throw)
          (match :throw-as-skipped (throw+ (SkipException.
                                            (format "Trapped exception: %s" e#)))
                 :re-throw         (throw+ e#))))))
