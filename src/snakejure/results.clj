(ns snakejure.results
  (:import (java.io File)))

(defn- get-results-file
  "Returns file .snakejure, which will be looked for in home directory."
  []
  (File. (str (System/getProperty "user.home") "/.snakejure")))

(defn get-results-map
  "Return map with results. If ~/.snakejure file doesn't exist return empty map."
  []
  (let [file (get-results-file)]
    (if (.exists file)
      (load-file (.getAbsolutePath file))
      {})))

(defn- save-results-map
  "Writes results to ~/.snakejure file."
  [results]
  (spit (get-results-file) results))

(defn update-results
  "Loads results, updated them and saves."
  [level-name result]
  (-> (get-results-map)
      (assoc level-name result)
      (save-results-map)))

