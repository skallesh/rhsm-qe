(ns sm.gui.ldtp
  (:import java.util.NoSuchElementException
	   [org.apache.xmlrpc.client XmlRpcClient XmlRpcClientConfigImpl]))

(defn element-getter "Returns a function that, given a keyword, retrieves 
the window id and element id from the given element map, and return those 2 items in a vector" 
  [elem-map]
  (fn [name-kw] 
    (let [window (first (filter #(get-in % [:elements name-kw])
				(vals elem-map)))
	  elem (get-in window [:elements name-kw])]
      (if elem [(:id window) elem]
	  (throw (NoSuchElementException.
		  (format "%s not found in ui mapping." name-kw)))))))

(def client 
  (let [config (XmlRpcClientConfigImpl.)
        xclient (XmlRpcClient.)]
    (comment (.setServerURL config (java.net.URL. url)))
    (.setConfig xclient config)
    xclient))

(defmacro defxmlrpc
    [specfile]
    (let [methods (with-in-str (slurp specfile) (read))
          defs (map
		(fn [[fnname args]]
		  (let [argsyms (map symbol args)]
		    `(defn ~(symbol fnname) ~(vec argsyms)
		       (clojure.lang.Reflector/invokeInstanceMethod
			client "execute" (to-array (list ~fnname ~(concat '(list) argsyms)))))))
		methods)]
      `(do
         ~@defs)))

(defn set-url [url]
  (.setConfig client 
	      (.setServerURL (.getConfig client) (java.net.URL. url))))

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
      (if args (to-array args) clojure.lang.RT/EMPTY_ARRAY))))
