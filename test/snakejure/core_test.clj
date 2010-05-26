(ns snakejure.core-test
  (:use [snakejure.core] :reload-all)
  (:use [clojure.test]))

(deftest add-points-t
    (is (= [1 1] (add-points [0 25] [1 -3] [0 -21]))))

(deftest overlaps-body?-t
  (let [snake {:body [ [0 0] [0 1] [1 1] [2 1] [3 1] [3 2] ]}] 
    (is (true? (overlaps-body? snake [1 1])))
    (is (false? (overlaps-body? snake [3 3])))))

(defn point? [point]
  (and (< (point 1) width)
       (< (point 0) height)
       (>= (point 1) 0)
       (>= (point 0) 0)))

(defn apple? [{:keys [location color type]}]
  (and (= :apple type)
       (point? location)
       (instance? java.awt.Color color)))

(defn create-full-table-snake[]
  (for [y (range height) x (range width)] [y x]))

(deftest create-apple-t 
  (is (every? apple?  (take 20 (repeatedly create-apple))))
  (is (let [snake {:body (rest (create-full-table-snake))}
	    apple (create-apple snake)]
	(and (apple? apple)
	     (= (:location apple) [0 0])))))

(defn noiser? [{:keys [location color-silence color-noise type] }]
  (and (point? location)
       (instance? java.awt.Color color-silence)
       (instance? java.awt.Color color-noise)
       (= :noiser type)))

(deftest create-noiser-t
  (is (every? noiser? (take 20 (repeatedly create-noiser)))))

