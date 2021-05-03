(ns everclear.dag.core
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   ;; [dag.checks :as checks]
   ;;
   [everclear.dag.parse-rulefiles :as parse]
   [everclear.state.filewatch :as filewatch]
   [everclear.state.state :as state]
   [everclear.dag.create-jobs :as cj :refer [flatten-rules connect-rules merge-connected-rules add-paths]]))

(defn file-to-map [f]
  (-> (slurp f) edn/read-string))

(defn start-everclear [opts]
  (let [config-file (get-in opts [:options :config-file])
        config (file-to-map config-file)

        {:keys [wildcards-file rule-file externals-file]} config
        ;; datastructures

        config (file-to-map config-file)
        wildcards (file-to-map wildcards-file)
        rules (parse/read-rules rule-file)
        external (file-to-map externals-file)

        ;; watchers
        jobinfo (cj/create-jobinfo rules wildcards external)
        ;; rulegraph (cj/create-rulegraph jobinfo)
        ;; jobgraph (cj/create-jobgraph jobinfo)
        ;; missing-file-dependencies (checks/all-file-dependencies-met? rulegraph rules)
        ]
    (filewatch/initiate-watch!
     ;; create the jobinfo anew each time
     ;; one of the underlying files is changed
     :jobinfo-watch
     [wildcards-file rule-file externals-file]
     (fn [ctx e]
       (println (str "ctx: " ctx))
       (println (str "e: " e))
       (reset! state/jobinfo
               (cj/create-jobinfo rules wildcards external))))))
