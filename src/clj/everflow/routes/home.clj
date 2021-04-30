(ns everflow.routes.home
  (:require
   [everflow.layout :as layout]
   [clojure.java.io :as io]
   [everflow.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]

   [taoensso.sente :as sente]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.params]
   [ring.middleware.keyword-params]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

;; from sente README example


(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  (println (str "@connected-uids: " @connected-uids)))

(add-watch connected-uids :connected-uids
           (fn [_ _ old new]
             (when (not= old new)
               (println "Connected uids change: %s" new))))

(defn home-page [request]
  (layout/render request "home.html"))

(defn login-handler
  "Here's where you'll add your server-side login/auth procedure (Friend, etc.).
  In our simplified example we'll just always successfully authenticate the user
  with whatever user-id they provided in the auth request."
  [ring-req]
  (let [{:keys [session params]} ring-req
        {:keys [user-id]} params]
    (println "Login request: %s" params)
    {:status 200 :session (assoc session :uid user-id)}))

(defn home-routes []
  [""
   {:middleware [ring.middleware.keyword-params/wrap-keyword-params
                 ring.middleware.params/wrap-params
                 middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/chsk" {:get ring-ajax-get-or-ws-handshake
             :post ring-ajax-post}]])

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :everflow/button
  [ev-msg] (println (:?data ev-msg)))


;; (defmethod -event-msg-handler :example/toggle-broadcast
;;   [{:as ev-msg :keys [?reply-fn]}]
;;   (let [loop-enabled? (swap! broadcast-enabled?_ not)]
;;     (?reply-fn loop-enabled?)))


(defn start-example-broadcaster!
  "Hio"
  []
  (while true
    (Thread/sleep 10000)
    (doseq [uid (:any @connected-uids)]
      (chsk-send! uid [:h/h (str "hello " uid "!!")]))))

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk event-msg-handler)))

(defn start! [] (start-router!))
  ;; (start-example-broadcaster!))

(defonce _start-once (start!))

;;; repl
; (require 'everflow.routes.home)
; (in-ns 'everflow.routes.home)
