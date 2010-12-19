(ns com.redhat.qe.handler
  (:import [javax.naming NamingException]))

(def *handlers* [])
(def *error* nil)

(defn- e-to-map [e]
  {:msg (.getMessage e) :type (class e) :exception e})

(defn- wrapped? [e]
  (and (instance? NamingException e) (map? (.getResolvedObj e))))

(defn unwrap [e]
  (if (wrapped? e)
    (let [r (.getResolvedObj e)]
      (if (map? r) r
          (throw (IllegalStateException.
                  "Wrapped object is not a map - must be a real NamingException?"))))
    (e-to-map e)))

(defn is-type [m ptype]
  (isa? (:type m) ptype))
  
(defprotocol Raisable
  (raise [this])
  (wrap [this]))

(extend-protocol Raisable
  clojure.lang.IPersistentMap
  (raise [this] (throw (wrap this)))
  (wrap [this] (let [e (NamingException. (or (:msg this) ""))]
               (.setResolvedObj e this)
               e))

  java.lang.Throwable
  (raise [this] (raise (e-to-map this)))
  (wrap [this] (wrap (e-to-map this))))

(defn rewrap [e addmap]
  (let [m (unwrap e)
        m (merge addmap m)]
    (if (wrapped? e) (do (.setResolvedObj e m)
                         e)
        (wrap m))))

(defn equal-or-more? [m1 m2]
  (cond (= m1 m2) true
        (not (and (map? m1) (map? m2))) false
        (= m1 (select-keys m2 (keys m1))) true))


(defn recover [recovery err]
  (let [recoveryfn (recovery err)]
    (cond (nil? recoveryfn) (throw (IllegalStateException.
                                    (str "Recovery chosen that does not exist: " recovery)))
          (fn? recoveryfn) (recoveryfn)
          :else (throw (IllegalArgumentException.
                        (format "Recovery %s needs to be a function with one argument, instead got: %s"
                                recovery recoveryfn))))))

(defn dispatch "Thread the map m through all the handler functions
in hlist, until one of them returns something other than m.
Return the first non-m value, or m if the end of
the list is hit."
  [m hlist]
  (let [handled (drop-while  #(= m %)
                             (reductions #(%2 %1) m hlist))]
    (if (> (count handled) 0)
      (first handled) m)))

(defmacro with-handlers "Runs code in an error handling environment.

  Executes body, if an error is raised, pass it to each of the
handlers in hlist.  Each handler should be a function that should take
an error map as an argument, and returns one of the following:

  1) A value which will be returned as the value of the whole form

  2) The original error, if the handler doesn't handle this kind of
  error.

The error map will have whatever keys it was created with,
typically :msg will be the text of the error, and :type will be the
type.

Within the handler, you can also choose a pre-defined recovery by
calling the recover-by macro.  In most cases, that will be the entire
body of the handler."  [hlist & body]
  (if-not (and (coll? hlist) (every? coll? hlist))
    (throw (IllegalArgumentException.
            "First argument to with-handler must be a collection of handlers")))
  `(binding [*handlers* (concat ~hlist *handlers*) ] ;chain handlers together
     (try ~@body
          (catch Throwable ne#
            (let [unwrapped# (unwrap ne#)
                  handler-result# (binding [*error* unwrapped#]
                                    (dispatch unwrapped# *handlers*))
                  unhandled# (or (:exception unwrapped#) ne#)]  ;if the original error was an exception, retrieve it to throw if it is not handled.
              (if (equal-or-more? unwrapped# handler-result#)
                (throw unhandled#) ;returning the original map means unhandled
                handler-result#))))))

(defmacro add-recoveries "Executes body and attaches some pre-defined
recovery methods if an error occurs.  An error handler further down
the call stack can select the recovery by name.  The recovery methods
should be a map of keywords to functions.  Recovery functions should
not take any arguments, but can access the error in the *error* var."
[m & body]
  `(try ~@body
        (catch Throwable ne#
          (throw (rewrap ne# ~m)))))

(defmacro handle-type "A convenience macro that creates an error
handler by error type. It will be dispatched on any *error* where (isa? (:type *error*)
type)."
  [type & body]
  `(fn [e#] (if (is-type e# ~type)
            (do ~@body)
            e#)))

(defmacro recover-by [kw]
  `(recover ~kw *error*))

(defn expect [type]
  (handle-type type (constantly nil)))

(comment ;;examples of use

;; a low level fn that can cause errors
(defn error-prone [n]
  (cond 
        (> n 200) (raise (IllegalStateException. "Wayy Too big!"))  ;;java exceptions can participate normally
        (> n 100) (throw (IllegalArgumentException. "Too big!"))  ;;java exceptions can participate normally
        (> n 0) (inc n)
        :else (raise {:msg "Negative number!" :number n :type :NumberError})))  ;;clojure errors are just maps

;;a fn that adds recoveries in a middle layer
(defn do-stuff [n]
  (add-recoveries {:return-zero (fn [] 0)
                   :retry #(error-prone (Math/abs (:number *error*)))}
                  (error-prone n)))

;;define handler fn yourself and call middle layer
(with-handlers [ (fn [e] (if (is-type e :NumberError) (recover-by :retry) e))]
  (do-stuff -5)) ; --> 6

;;use macro to specify handlers, show that recoveries can be added at any level
(with-handlers
  [ (handle-type :NumberError (recover-by :return-zero)) ;;choose a predefined recovery
    (handle-type :OtherError 0)
    (handle-type IllegalStateException 201)]
  
  (do-stuff 105)))

