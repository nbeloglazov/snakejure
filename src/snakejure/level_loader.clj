(ns snakejure.level-loader
    (:use [clojure.contrib.find-namespaces :only (find-namespaces-in-dir)])
    (:import (java.io File)))

(defn extract-description 
  "Finds in namespace ns function level-description and call it."
  [ns]
  (require ns)
  ((intern ns 'level-description)))

(defn load-levels 
  "Loads all levels from levels directory"
  []
  (map extract-description (find-namespaces-in-dir (File. "levels"))))