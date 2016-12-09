(ns rhsm.errors.implementations
  (:require [rhsm.errors.protocols :as protocols]
            [clojure.core.match :refer [match]])
  (:import org.testng.SkipException))

(extend-protocol protocols/IFailureClassifier
  SkipException
  (failure-level [e] :skip-exception)

  Exception
  (failure-level [e] :unknown-exception)

  clojure.lang.PersistentArrayMap
  (failure-level [e]
    (match [e (:type e)]
           [_ :network-error] :network-error
           :else :unknown))

  java.lang.AssertionError
  (failure-level [e] :verification-failure)

  java.lang.Exception
  (failure-level [e] :unknown-exception)

  Object
  (failure-level [o] :unknown))

(gen-class)
