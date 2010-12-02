(ns sm.gui.ldtp
  (:require [clojure.contrib.logging :as log])
  (:import [org.apache.xmlrpc.client XmlRpcClient XmlRpcClientConfigImpl]))


(def client 
  (let [config (XmlRpcClientConfigImpl.)
        xclient (XmlRpcClient.)]
    (comment (.setServerURL config (java.net.URL. url)))
    (.setConfig xclient config)
    xclient))

(defn- xmlrpcmethod-arity "Generate code for one arity of xmlrpc method."[fnname argsyms n]
  (let [theseargs (take n argsyms)]
    `(~(vec theseargs)
     (clojure.lang.Reflector/invokeInstanceMethod
      client "execute" (to-array (list ~fnname ~(concat '(list) theseargs)))))) )

(defmacro defxmlrpc
  "Generate functions corresponding to an xmlrpc API (in this case,
    LDTP). Generates arities for xmlrpc methods that have optional
    arguments. Reads a given spec file that lists the method name,
    arguments, and number of default args. See also
    http://ldtp.freedesktop.org/user-doc/index.html"
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

;;Generate all LDTP functions from the specfile.  Specfile is produced
;;by this python script:
;;https://github.com/weissjeffm/ldtp-server/blob/master/extract-api.py
(defxmlrpc "clojure/src/sm/gui/ldtp_api.txt")

(defn action-with-uimap
  "Take a getter function (which should take a keyword and return a
  locator list), returns a function with the following properties:
  Takes an action function, keyword, and args. When called, it
  retrieves an element (via the getter), and calls the action function
  with the element and args. Then it logs what it did."
  [getter]
 (fn [actionfn arg1 & args]
   (let [locator (if (keyword? arg1) (getter arg1) [arg1])
	 returnval (apply actionfn (concat locator args))]
     (log/info (str "Action: " (:name (meta actionfn)) locator " " args))
     returnval)))
