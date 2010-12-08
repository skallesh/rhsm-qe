(ns sm.gui.errors
  (:require [clojure.contrib.error-kit :as errkit]))

;;parent error
(errkit/deferror *sm-error* [] "Indicates an error dialog has appeared in the application." [s]
  {:msg (str "Error dialog is present with message: " s)
   :unhandled (errkit/throw-msg RuntimeException)})

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {'*invalid-login* #"Invalid username"})

(defmacro define-all-errors
  "Expands to create an error type for each of the pre-defined known RHSM
error types."
  []
 `(do  ~@(for [errname (keys known-errors)]
     `(errkit/deferror ~errname [*sm-error*] [s#]
	{:msg (str "Error " ~errname s#)
	 :unhandled (errkit/throw-msg RuntimeException)}))))

(define-all-errors)

(defn raise-matching-error "Calls error-kit's raise macro with the error type that matches the text of the error presented in the gui."
  [message]
  (let [matches-message? (fn [key] (let [re (known-errors key)]
				       (if (re-find re message) key false)))
	  name (or (some matches-message? (keys known-errors))
		   '*sm-error*)
	  err (ns-resolve 'sm.gui.errors name)]
    (errkit/raise err message)))
