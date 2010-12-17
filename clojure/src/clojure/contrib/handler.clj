(ns clojure.contrib.handler
  (:import [javax.naming NamingException]))

(def *handlers* [])

(defn- rewrap [e addmap]
  (let [m (unwrap e)
        m (merge addmap m)]
    (.setResolvedObj e m)
    e))

(defn- e-to-map [e]
  {:msg (.getMessage e) :type (class e) :exception e})

(defn- unwrap [e]
  (if (instance? NamingException e)
    (let [r (.getResolvedObj e)]
      (if (map? r) r
          (throw (IllegalStateException.
                  "Wrapped object is not a map - must be a real NamingException?"))))
    (e-to-map e)))

(defn- is-type [m ptype]
  (isa? (:type this) ptype))
  
(defprotocol Raisable
  (raise [this])
  (wrap [this]))

(extend-protocol Raisable
  clojure.lang.IPersistentMap
  (raise [this] (throw (wrap this)))
  (wrap [this] (let [e (NamingException. (or (:msg this) ""))]
               (.setResolvedObj e this)
               e)))

  java.lang.Throwable
  (raise [this] (raise (e-to-map this)))
  (wrap [this] (wrap (e-to-map this))))


(defn- equal-or-more? [m1 m2]
  (cond (= m1 m2) true
        (not (and (map? m1) (map? m2))) false
        (= m1 (select-keys m2 (keys m1))) true))


(defn- recover [recovery err]
  (let [recoveryfn (recovery err)]
    (cond (nil? recoveryfn) (throw (IllegalStateException.
                                    (str "Recovery chosen that does not exist: " recovery)))
          (fn? recoveryfn) (recoveryfn err)
          :else (throw (IllegalArgumentException.
                        (format "Recovery %s needs to be a function with one argument, instead got: %s"
                                recovery recoveryfn))))))

(defn- dispatch "Thread the map m through all the handler functions
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
an error as an argument, and returns one of the following:

  1) A value which will be returned as the value of the whole form

  2) The original error, if the handler doesn't handle this kind of
  error.

  3) Finally the handler can choose a pre-defined recovery by
  returning a map with a single entry like {:recovery :abort} where
  the value is the recovery name to invoke."  [hlist & body]
  (if-not (and (coll? hlist) (every? coll? hlist))
    (throw (IllegalArgumentException.
            "First argument to with-handler must be a collection of handlers")))
  `(binding [*handlers* (concat ~hlist *handlers*) ] ;chain handlers together
     (try ~@body
          (catch Throwable ne#
            (let [unwrapped# (unwrap ne#)
                  handler-result# (dispatch unwrapped# *handlers*)
                  unhandled# (or (:exception unwrapped#) ne#)]  ;if the original error was an exception, retrieve it to throw if it is not handled.
              (cond (equal-or-more? unwrapped# handler-result#) (throw unhandled#) ;returning the original map means unhandled
                    (and (map? handler-result#) (:recovery handler-result#))
                    (recover (:recovery handler-result#) unwrapped#)
                    :else handler-result#))))))

(defmacro add-recoveries [m & body]
  `(try ~@body
        (catch Throwable ne#
          (throw (rewrap ne# ~m)))))

(defmacro handle-type "A convenience macro that creates an error
handler by error type. It will match any error e where (isa? (:type e)
type). The rest of the error map entries are ignored.  If you need
more access to the error map, you shouldn't use this."
  [type & body]
  `(fn [e#] (if (isa? (:type e#) ~type)
            (do ~@body)
            e#)))




(comment

(defn error-prone [n]
  (cond 
        (> n 200) (raise (IllegalStateException. "Wayy Too big!"))
        (> n 100) (throw (IllegalArgumentException. "Too big!"))
        (> n 0) (inc n)
        :else (raise {:msg "Negative number!" :number n :type :NumberError})))
(defn do-stuff [n]
  (add-recoveries {:zero (fn [e] 0)
                   :retry (fn [e] (error-prone (Math/abs (:number e))))}
                  (error-prone n)))

(with-handlers (fn [e] (if (= (:type e) :NumberError) {:recovery :retry} )) (do-stuff -5))

(with-handlers
  [ (handle-type :NumberError {:recovery :one})
    (handle-type :OtherError 0)
    (handle-type IllegalStateException 201)]
  (add-recoveries {:one (fn [e] 1)} (do-stuff 205))))
