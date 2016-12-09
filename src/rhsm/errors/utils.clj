(ns rhsm.errors.utils
  (:use [slingshot.slingshot :only (try+ throw+)]
        [rhsm.errors.protocols :as protocols]
        [clojure.core.match :only (match)])
  (:require [clojure.tools.logging :as log]))

(defmacro normalize-exception-types
  "This macro catches an exception
  and tries to find out 'failure-level' of the exception.
  After that it raises the proper exception when 'failure-level'
  requires so."
  [& body]
  `(try+
    ~@body
    (catch Object e#
      (-> (protocols/failure-level e#)
          (match :verification-failure :re-throw
                 :network-error        :throw-as-skipped
                 :else                 :no-throw)
          (match :throw-as-skipped (throw+ (SkipException.
                                            (format "Trapped exception: %s" e#)))
                 :re-throw         (throw+ e#))))))

