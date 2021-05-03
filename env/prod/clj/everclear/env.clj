(ns everclear.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[everclear started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[everclear has shut down successfully]=-"))
   :middleware identity})
