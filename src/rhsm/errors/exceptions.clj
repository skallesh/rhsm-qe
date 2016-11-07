(ns rhsm.errors.exceptions
  (:require [rhsm.errors.classification :as classification]
            [clojure.core.match :refer [match]])

  (:import org.testng.SkipException))

(extend-protocol classification/IFailureClassifier
  SkipException
  (failure-level [e] :skip-exception)

  Exception
  (failure-level [e] :unknown-exception)

  clojure.lang.PersistentArrayMap
  (failure-level [e]
    (match [e (:type e)]
           [_ :network-error] :network-error
           :else :unknown))

  Object
  (failure-level [o] :unknown))
