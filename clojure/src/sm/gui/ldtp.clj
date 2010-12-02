(ns sm.gui.ldtp
  (:import [org.apache.xmlrpc.client XmlRpcClient XmlRpcClientConfigImpl]))


(def client 
  (let [config (XmlRpcClientConfigImpl.)
        xclient (XmlRpcClient.)]
    (comment (.setServerURL config (java.net.URL. url)))
    (.setConfig xclient config)
    xclient))

(defn xmlrpcmethod-arity [fnname argsyms n]
  (let [theseargs (take n argsyms)]
    `(~(vec theseargs)
     (clojure.lang.Reflector/invokeInstanceMethod
      client "execute" (to-array (list ~fnname ~(concat '(list) theseargs)))))) )

(defmacro defxmlrpc
    [specfile]
    (let [methods (with-in-str (slurp specfile) (read))
          defs (map
		(fn [[fnname [args num-optional-args]]]
		  (let [argsyms (map symbol args)
			num-required-args (- (count args) num-optional-args)
			arity-arg-counts (range num-required-args (inc (count args)))
			arities (for [arity arity-arg-counts] (xmlrpcmethod-arity fnname argsyms arity))]
			
		    `(defn ~(symbol fnname)
		        ~@arities
		       )))
		methods)]
      `(do
         ~@defs)))

(defn set-url [url]
  (.setServerURL (.getConfig client) (java.net.URL. url)))

(defxmlrpc "clojure/src/sm/gui/ldtp_api.txt")


(comment 
  "stuff i used to debug at the REPL"
  (.execute client "click" (to-array '("hi" "there")))
  (javacall client "execute" "click"  (to-array '("hi" "there")))
  (javacall client "execute" "click"  "hi" "there")
  (let [config (XmlRpcClientConfigImpl.)]
    (.setServerURL config (java.net.URL. "http://localhost:4118"))
    (.setConfig client config))
  (.printStackTrace *e)
  (click "hi" "there")
  (macroexpand-1 '(defxmlrpc "clojure/src/sm/gui/ldtp_api.txt"))
  
  (clojure.lang.Reflector/invokeInstanceMethod
                              client "execute" (to-array (list "click" (list "blah" "blaH"))))

  (defn javacall "call a method on a java object, given an arbitrary number of args" 
    [obj name & args]
    (pprint args)
    (clojure.lang.Reflector/invokeInstanceMethod obj (str name)
						 (if args (to-array args) clojure.lang.RT/EMPTY_ARRAY)))

  (defn click
    ([list] (apply click list))
    ([window-name obj-name] )
    )
  )
