(ns everflow.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[everflow started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[everflow has shut down successfully]=-"))
   :middleware identity})
