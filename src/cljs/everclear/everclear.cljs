(ns everclear.everclear ; .cljs
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require
   ;; <other stuff>
   [clojure.pprint :as pp]
   [re-frame.core :as rf]
   [everclear.events :as reframe-events]
   [cljs.core.async :as async :refer (<! >! put! chan)]
   [taoensso.sente  :as sente :refer (cb-success?)] ; <--- Add this
   ))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(if ?csrf-token
  (js/console.log "CSRF token detected in HTML, great!")
  (js/alert "CSRF token NOT detected in HTML, default Sente config will reject requests"))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
       "/chsk" ; Note the same path as before
       ?csrf-token
       {:type :auto ; e/o #{:auto :ajax :ws}
        })]

  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (rf/dispatch [:save-jobinfo ?data])
  (js/console.log (str "Push event from server woop wopp: " ?data)))

(defmethod -event-msg-handler :h/h
  [{:as ev-msg :keys [?data]}]
  (js/alert (str "correctamundo " ?data)))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: %s" event))

(defn everclear-page []
  (let []
    [:div
     [:h1 "Hiya there! Great start! I'm here! Party time! Excellentio!"]
     [:input
      {:type "button" :value "Click me!"
       :on-click #(chsk-send! [:everclear/button "Hooo!"])}]
     (let [jobinfo @(rf/subscribe [:jobinfo])]
       [:div
        [:pre (with-out-str (pp/pprint jobinfo))]
        ])]))

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler)))

(defn start! [] (start-router!))

(defonce _start-once (start!))

;; lein repl :connect 7000
;; Connecting to nREPL at 127.0.0.1:7000
;; REPL-y 0.4.3, nREPL 0.6.0
;; Clojure 1.10.1
;; OpenJDK 64-Bit Server VM 1.8.0_152-release-1056-b12
;; Docs: (doc function-name-here)
;; (find-doc "part-of-name-here")
;; Source: (source function-name-here)
;; Javadoc: (javadoc java-object-or-class-here)
;; Exit: Control+D or (exit) or (quit)
;; Results: Stored in vars *1, *2, *3, an exception in *e

;; (require 'everclear.routes.home)
;; (in-ns 'everclear.routes.home)
;; @connected-uids
;; (chsk-send! :sente/all-users-without-uid [:h/h "Hooooo!"])
