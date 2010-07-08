(ns snakejure.level-loader
    (:use [clojure.contrib.find-namespaces :only (find-namespaces-in-dir)])
    (:import (java.io File)))

(defn- extract-description 
  "Finds in ns namespace 'level-description function and call it."
  [ns]
  (require ns)
  ((intern ns 'level-description)))

(defn- load-levels 
  "Loads all levels from levels directory"
  []
  (map extract-description (find-namespaces-in-dir (File. "src/snakejure/levels"))))

(defn- assoc-level [map {:keys [name level-creator]}]
  "Adds level description to map, with name as a key and 
  level-creator as a value."
  (assoc map name level-creator))

(defn get-levels-map
  "Returns map, containing levels. Keys of the map are level names,
  values are level creators functions."
  []
  (reduce assoc-level {} (load-levels)))