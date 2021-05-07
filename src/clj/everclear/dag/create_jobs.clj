(ns everclear.dag.create-jobs
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [clojure.string :as str]
            [com.stuartsierra.dependency :as dep]
            [selmer.parser :refer [render]])
  (:use [hashp.core]))

(defmacro timed [expr]
  (let [sym (= (type expr) clojure.lang.Symbol)]
    `(let [start# (. System (nanoTime))
           return# ~expr
           res# (if ~sym
                    (resolve '~expr)
                    (resolve (first '~expr)))]
       (prn (str "Timed "
           (:name (meta res#))
           ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
       return#)))
(defn flatten-rules [rules]
  (for [rule (vals rules)
        outfile (or (vals (:output rule)) [nil])
        infile (or (seq (vals (:input rule))) [nil])]
    {:infile    infile
     :rulename  (rule :rulename)
     :outfile   outfile
     :wildcards (rule :wildcards)}))

(defn connect-rules [flattened-rules]
  (distinct
   (for [[r1 r2] (combo/cartesian-product flattened-rules flattened-rules)
         :let [;; this rule has no dependencies - i.e. is a source.
               is-source (and
                          (nil? (r1 :infile))
                          (= r1 r2))]
         :when (or
                ; include source rules (which have no infile)
                is-source
                ; these rules depend on each other
                (= (r1 :infile) (r2 :outfile)))]
     {:in-file       (r1 :infile)
      :out-file      (r1 :outfile)
      :in-wildcards  (r2 :wildcards)
      :out-wildcards (r1 :wildcards)
      :in-rule       (r2 :rulename)
      :out-rule      (r1 :rulename)})))

(defn add-rule-info [merged-connected-rules rules]
  (for [r merged-connected-rules
        :let [rulename (r :out-rule)
              rule (rules rulename)]]
    (merge rule r)))

(defn merge-connected-rules [connected-rules]
  (for [[k vs] (group-by (juxt :in-rule :out-rule) connected-rules)]
    (assoc (first vs)
           :in-file (-> (map :in-file vs) distinct vec)
           :out-file (-> (map :out-file vs) distinct vec))))

(defn fetch-external [external wildcards]
  (into {}
        (for [[external-kw wildcard->external-file] external
              :let [external-keys (-> wildcard->external-file keys first keys)
                    wildcard-keys (distinct (map #(select-keys % external-keys) wildcards))]]
          [external-kw (mapv wildcard->external-file wildcard-keys)])))

(defn add-external-paths [jobs rules externals]
  (for [job jobs
        :let [rule (rules (job :out-rule))
              externals-in-rule (select-keys externals (rule :external))]]
    (if (nil? externals-in-rule)
      job
      (let [external-files (fetch-external externals-in-rule (job :wildcards))
            external nil]
        (assoc job :external-files external-files
               :external (into {} (for [[k files] external-files] [k (str/join " " files)])))))))

(defn add-dep [graph [node dep]] (dep/depend graph node dep))

(defn to-path [file rule wildcards]
  (let [rulename (name rule)
        wc (zipmap (map name (keys wildcards)) (vals wildcards))
        wc (->> wc sort flatten (clojure.string/join "/"))]
    (clojure.string/join "/" [rulename wc file])))

(defn add-paths [created-jobs]
  (for [{:keys [in-file out-file in-wildcards out-wildcards in-rule out-rule wildcards] :as cj} created-jobs]
    (assoc cj
           :in-files (for [wc in-wildcards if in-file] (to-path if in-rule wc))
           :out-files (for [wc out-wildcards of out-file] (to-path of out-rule wc)))))

(defn- intersect [c1 c2]
  (sort (set/intersection (set c1) (set c2))))

(defn- map-select [ms ks]
  (distinct (map #(select-keys % ks) ms)))

;;
;; TODO: find out how to treat case when there are no
;;       wildcards in common between :in and :out.
;;       Should it even be allowed?
(defn compute-wildcards [merged-connected-rules wildcards]
  (into {}
        (for [{:keys [in-wildcards out-wildcards]} merged-connected-rules
              :let [wildcard-subset (concat in-wildcards out-wildcards)
                    applicable-wildcards (map #(select-keys % wildcard-subset) wildcards)
                    wildcard-core (intersect in-wildcards out-wildcards)
                    get-wc-core #(select-keys % wildcard-core)
                    core-groupby (group-by get-wc-core applicable-wildcards)
                    wildcard-groups (zipmap (keys core-groupby)
                                            (->> core-groupby
                                                 vals
                                                 (mapv #(-> % distinct vec))))]]
          [[in-wildcards out-wildcards] wildcard-groups])))

(defn create-jobs [merged-connected-rules wildcards]
  (let [precomputed-wildcards (compute-wildcards merged-connected-rules wildcards)]
    (for [cr merged-connected-rules
          [core wc] (precomputed-wildcards [(cr :in-wildcards) (cr :out-wildcards)])]
      (assoc cr
             :wildcards wc :core core
             :in-wildcards (map-select wc (cr :in-wildcards))
             :out-wildcards (map-select wc (cr :out-wildcards))))))


;
 ;(defn add-rule-info-sources [merged-connected-rules]           (for [r merged-connected-rules    :when (r :)    :let [rulename (r :out-rule)             rule (rules rulename)]]   (merge rule r)))(defn add-rule-info [merged-connected-rulesules] (for [r merged-connected-rules    :let [rulename (r :out-rule)          rule (rules rulename)]](merge rule r)))fn- intersect [c1 c2]ort (set/intersection (set c1) (set c2))))  - map-select [ms ks]stinct (mapect-keys % ks) ms)))ODO: find outo treat case when there are no    wildcards in common between :in and :out.
 ;;;       Should it even be allowed?         (defn compute-wildcards [merged-connected-rules wildcards]nto {}    (for [{:keys [in-wildcards out-wildcards]} merged-connected-rules             :let [wildcard-subset (cont in-wildcards out-wdcards)                   applicablwildcards (map #(select-keys % wildcard-subset) wildcards)                wildcard-core (inct in-wildcards out-wildcards)                get-wc-core #(select-keys % wildcard-core)                core-groupby (group-by get-wc-core applicable-wildcards)                wildcard-groups (zipmap (keys core-groupby)                                              (->> core-groupby                                                                          (mapv #(-> % distinct vec))))]]     [[in-wildcards out-wildcards] wildcard-groups])))
 ;         (defn create-jobs [merged-connected-rules wildcards]et [precomputed-wildcards (compute-wildcards mergedected-rules wildcards)]   (for [cr merged-connected-rules         [core wc] (ecomputed-wildcards [(cr :in-wildcards) (crout-wildcards)])]     (assoc cr    :wildcards wc :core core    :in-wildcards (map-select wc (cr :in-wildcards))    :out-wildcards (map-select wc (cr :out-wildcards))))))fn to-path [file rule wildcards]et [rulename (name rule)          wc (zipmap (map name (keys wildcards)) (vals wildcards))   wc (->> wc sort flattenure.string//"))]clojure.strin "/" [rulename wc file])))n add-paths [created-jobs]
 ;  (for [{:keys [in-file out-file in-wildcards out-wildcards in-rule out-rule wildcards] :as cj} created-jobs]             (assoc cj  :in-files (for [wc in-wildcards if in-file] (to-pf in-rule wc))     :out-files (for [wc out-wildcards  out-file] (to-path  out-rule wc)))))(defn csv-data->maps [csv-da]ap zipmap   (->> (first csv-data)                                ;st row is the header        (map keyword)                                   ;; Drop if you wantng keys instead              repeat)  (rest csv-data)))n fetch-ext[external wildcards]to {}   (for [[external-kw wildcard->exterle] external
 ;              :let [external-keys (-> wildcard->external-file keys first keys)                             wildcard-keys (distinct (map #(select-keys % external-keys) wids))]]      [external-kw (mapv wildcard->external-file wildcard-keys)])))(defn add-external-paths [jobs rules exrnals] (for [job jobs       :let [rule (rules (jo:out-rule))          externals-in-rule (seleys externals (rule :external))]](if (nil? externals-in-rule)  job  (let [external-files (fetch-external externals-in-rule (job :wildcards))]          (assoc job :external-files external-files              :external (i (for [[k fexternal-files] [k (str/ " files)])))
 ;


(defn add-dep [graph [node dep]]
  (dep/depend graph node dep))

(defn create-rulegraph-pairs [connected-rules]
  (for [{:keys [in-rule out-rule]} connected-rules
        :when (not= in-rule out-rule)]
    [in-rule out-rule]))

(defn create-jobgraph-pairs [merged-connected-rules]
  (for
   [{:keys [in-rule out-rule in-wildcards out-wildcards]} merged-connected-rules
    :when (not= in-rule out-rule)]
    [[in-rule in-wildcards] [out-rule out-wildcards]]))

(defn invert-filemap [fs]
  (zipmap (vals fs) (keys fs)))

(defn paths-into-map [added-paths rules]
  (for [jobinfo added-paths
        :let [rule (rules (jobinfo :out-rule))
              inv-infilemap (invert-filemap (rule :input))
              inv-outfilemap (invert-filemap (rule :output))]]
    (assoc jobinfo
           :in-files {(inv-infilemap (first (jobinfo :in-file))) (jobinfo :in-files)}
           :out-files {(inv-outfilemap (first (jobinfo :out-file))) (jobinfo :out-files)})))

(defn merge-paths [paths-in-maps]
  (for [js (vals (group-by (juxt :core :in-rule :out-rule) paths-in-maps))
        :let [in-files (apply merge (map :in-files js))
              out-files (apply merge (map :out-files js))]]
    (assoc (first js)
           :in-files in-files :out-files out-files
           :input (into {} (for [[k files] in-files] [k (str/join " " files)]))
           :output (into {} (for [[k files] out-files] [k (str/join " " files)])))))

(defn fill-params [external-paths-added]
  (for [{:keys [params in-files out-files wildcards core] :as job} external-paths-added
        :let [filled-in-params
              (into {}
                    (for [[param param-string] params]
                      [param (render param-string {:input in-files :output out-files :wildcards core})]))]]
    (assoc job :params filled-in-params)))

(defn fill-shell [code jobinfo]
  (render code jobinfo))

(defn fill-script []
  ;TODO: MISSING
  )

(defn fill-code [external-paths-added rules]
  (for [jobinfo external-paths-added
        :let [rule (rules (jobinfo :out-rule))
              runs (if (contains? rule :shell) :shell :script)
              code (if (= runs :shell)
                     (fill-shell (rule :shell) (assoc jobinfo :wildcards (first (jobinfo :wildcards))))
                     (fill-script))]]
    (assoc jobinfo runs code)))

(defn create-graph [pairs]
  (reduce add-dep (dep/graph) pairs))

(defn create-jobgraph [merged-connected-rules]
  (-> (create-jobgraph-pairs merged-connected-rules)
      create-graph))

(defn create-rulegraph [merged-connected-rules]
  (-> (create-rulegraph-pairs merged-connected-rules)
      create-graph))

;; (defn create-jobinfo [rules wildcards external]
;;   (-> rules
;;       flatten-rules
;;       connect-rules
;;       merge-connected-rules
;;       (add-rule-info rules)
;;       (create-jobs wildcards)
;;       add-paths
;;       (paths-into-map rules)
;;       merge-paths
;;       (add-external-paths rules external)
;;       (fill-params)
;;       (fill-code rules)))

(defn create-jobinfo [rules wildcards external]
  (try
    (let [merged-connected-rules (-> rules
                                     flatten-rules
                                     connect-rules
                                     merge-connected-rules)

          ;; _ #p (distinct (map (juxt :in-rule :out-rule) merged-connected-rules))
          ;; _ #p merged-connected-rules
          ;; _ #p (map :out-rule  merged-connected-rules)
          rulegraph (create-rulegraph merged-connected-rules)

          jobinfo (-> merged-connected-rules
                      (create-jobs wildcards)
                      add-paths
                      (paths-into-map rules)
                      merge-paths
                      (add-external-paths rules external)
                      fill-params
                      (fill-code rules))
          jobgraph (create-jobgraph jobinfo)
          ]

      ;; (distinct (map (juxt :in-rule :out-rule) jobinfo))
      {:jobinfo jobinfo})
    (catch Exception e
      {:exception-message (str e)})))



(defn create-jobinfo-timed [rules wildcards external]
  (let [flattened-rules (timed (flatten-rules rules))
        connected-rules (timed (connect-rules flattened-rules))
        merged-connected-rules (timed (merge-connected-rules connected-rules))

        ;; _ #p (distinct (map (juxt :in-rule :out-rule) merged-connected-rules))
        ;; _ #p merged-connected-rules
        ;; _ #p (map :out-rule  merged-connected-rules)
        rulegraph (timed (create-rulegraph merged-connected-rules))

        created-jobs (timed (create-jobs merged-connected-rules wildcards))
        added-paths (timed (add-paths created-jobs))
        paths-in-maps (timed (paths-into-map added-paths rules))
        merged-paths (timed (merge-paths paths-in-maps))
        added-external-paths (timed (add-external-paths merged-paths rules external))
        filled-params (timed (fill-params added-external-paths))
        jobinfo (timed (fill-code filled-params rules))

        ;; jobinfo (-> merged-connected-rules
        ;;             (create-jobs wildcards)
        ;;             add-paths
        ;;             (paths-into-map rules)
        ;;             merge-paths
        ;;             (add-external-paths rules external)
        ;;             fill-params
        ;;             (fill-code rules))
        jobgraph (timed (create-jobgraph jobinfo))]

    ;; (distinct (map (juxt :in-rule :out-rule) jobinfo))

    (println (java.time.LocalDateTime/now))
    jobinfo))

    ;; ))
    ;;
;; [:bwa-map :samtools-sort]
;; [:samtools-sort :samtools-index]
;; [:bwa-map :bwa-map]
;; [:bcftools-call :plot-quals]
;; [:samtools-sort :bcftools-call]
;; MISSING! [:samtools-index :bcftools-call]

;;   ;; (-> rules
;;   ;;     flatten-rules
;;   ;;     connect-rules
;;   ;;     merge-connected-rules
;;   ;;     (add-rule-info rules)
;;   ;;     (create-jobs wildcards)
;;   ;;     add-paths
;;   ;;     (paths-into-map rules)
;;   ;;     merge-paths
;;   ;;     (add-external-paths rules external)
;;   ;;     (fill-params)
;;   ;;     (fill-code rules)))
