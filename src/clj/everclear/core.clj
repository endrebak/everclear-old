(ns everclear.core
  (:require
   [clojure.java.io :as io]
   [everclear.handler :as handler]
   [everclear.nrepl :as nrepl]
   [luminus.http-server :as http]
   [everclear.config :refer [env]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [everclear.dag.core :as dag]
   [mount.core :as mount]))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  ;; An option with a required argument
  [["-c" "--config-file FILE" "Config file to use."
    :validate [#(and
                 (not (nil? %))
                 (.exists (io/file %)))]]
   ["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (-> env
       (assoc  :handler (handler/app))
       (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
       (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (start-app opts)
    (dag/start-everclear opts)))
