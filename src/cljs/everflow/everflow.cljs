(ns everflow.everflow ; .cljs
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require
   ;; <other stuff>
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


(defn everflow-page []
  (let []
    [:div
     [:h1 "Hiya there! Great start! I'm here! Party time! Excellentio!"]
     [:input
      {:type "button" :value "Click me!"
       :on-click #(chsk-send! [:everflow/button "Hooo!"])}]]))

