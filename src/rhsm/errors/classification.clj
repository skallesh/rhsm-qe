(ns rhsm.errors.classification
  (:use [slingshot.slingshot :only [try+
                                    throw+]])
  (:require [clojure.tools.logging :as log]))

(defprotocol AnUrgenceClassifier
  "Methods related to classifying of exceptions mainly.
  It is important to distinguish errors and exceptions
  from TestNG report results perspective."
  (urgency_level [a]
    "returns:
      -  :testware-problem
      -  :verification-failure
      -  :skip-exception
      -  :unknown-exception
      -  :unknown"))

(gen-class)
