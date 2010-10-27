(ns sm.gui.ldtp
  (:import java.util.NoSuchElementException
    [org.apache.xmlrpc.client XmlRpcClient XmlRpcClientConfigImpl]))

(defn element-getter "Returns a function that, given a keyword, retrieves 
the window id and element id from the given element map, and return those 2 items in a vector" 
  [elem-map]
  (fn [name-kw] 
    (let [window (first (filter #(get-in % [:elements name-kw]) (vals elem-map)))
        elem (get-in window [:elements name-kw])]
    (if elem [(:id window) elem]
      (throw (NoSuchElementException. (format "%s not found in ui mapping." name-kw)))))))

(defn javacall "call a method on a java object, given an arbitrary number of args" 
  [obj name & args]
  (println args)
  (clojure.lang.Reflector/invokeInstanceMethod obj (str name)
					       (if args (to-array args) clojure.lang.RT/EMPTY_ARRAY)))

(defn def-ldtp-method [name args client]
  (intern *ns* (symbol name) 
    (fn [& args] 
      (apply javacall client "execute" name args))))

(defn init-xmlrpc-client [url specfile]
  (let [config (XmlRpcClientConfigImpl.)
        client (XmlRpcClient.)]
    (.setServerURL config (java.net.URL. url))
    (.setConfig client config)
    (let [methods (with-in-str (slurp specfile) (read))]
      (doall 
        (map (fn [method] 
               (def-ldtp-method (first method) (second method) client)) 
             methods)))
    client))

(def client 
  (let [config (XmlRpcClientConfigImpl.)
        xclient (XmlRpcClient.)]
    (comment (.setServerURL config (java.net.URL. url)))
    (.setConfig xclient config)
    xclient))

(defmacro defxmlrpc
    [specfile]
    (let [methods (with-in-str (slurp specfile) (read))
          defs (map (fn [[fnname args]]
                      (let [argsyms (map symbol args)]
                        `(defn ~(symbol fnname) ~(vec argsyms)
                           (apply javacall client "execute" ~fnname (vec ~argsyms)))))
                 methods)]
      `(do
         ~@defs)))

(defxmlrpc "clojure/src/sm/gui/ldtp_api.txt")
