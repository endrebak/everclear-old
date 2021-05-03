(ns everclear.dag.parse-rulefiles
  (:import [java.io PushbackReader])
  (:require
   [clojure.java.io :as io]))

(defn handle-docs [& body]
  (if (string? (first body))
    (assoc (second body) :doc (first body))
    (first body)))

(defn files-to-map [files]
  (cond
    (or (map? files) (nil? files)) files
    (vector? files) (into {} (map-indexed vector files))
    (string? files) {0 files}
    :else (throw (Exception. ":input/:output must be map, string or vector"))))

(defn read-all
  [file]
  (let [rdr (-> file io/file io/reader PushbackReader.)]
    (loop [forms []]
      (let [form (try (read rdr) (catch Exception e nil))]
        (if form
          (recur (conj forms form))
          forms)))))

(defn parse-rule
  [[_ name & body]]
  (let [name (keyword name)]
    (if (= 2 (count body))
      (assoc (second body) :doc (first body) :rulename name)
      (assoc (first body) :rulename name))))

(defn fix-rule [rule]
  (assoc rule
         :input (files-to-map (rule :input))
         :output (files-to-map (rule :output))))

(defn read-rules
  [rule-file]
  (let [rules (read-all rule-file)]
    (into {}
          (for [rule rules
                :let [rulemap (-> (parse-rule rule) fix-rule)
                      name (:rulename rulemap)]]
            [name rulemap]))))
