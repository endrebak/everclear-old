(ns everclear.state.filewatch
  (:require
   [hawk.core :as hawk]
   [clojure.pprint :as p]
   [everclear.routes.home :as routes]
   [everclear.state.state :as state]))

(defn initiate-watch! [n fs fn]
  (swap! state/watches
         assoc n (hawk/watch! {:watcher :polling :sensitivity :high} [{:paths fs :handler fn}])))

(defn end-watch! [n]
  (hawk/stop! (@state/watches n))
  (swap! state/watches dissoc n))

(add-watch state/jobinfo :jobinfo-watcher
           (fn [key atom old-state new-state]
             (prn "-- Atom Changed --")
             (prn new-state)
             (routes/chsk-send! :sente/all-users-without-uid [:h/h new-state])
             ;; (p/pprint (vec new-state))
             ))
