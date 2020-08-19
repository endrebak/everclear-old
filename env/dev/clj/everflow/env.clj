(ns everflow.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [everflow.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[everflow started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[everflow has shut down successfully]=-"))
   :middleware wrap-dev})
