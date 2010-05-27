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

(defn snake? [{:keys [body type dir color]}]
  (and (point? (first body))
       (= type :snake)
       (instance? java.awt.Color color)))

(deftest create-snake-t
  (is (snake? (create-snake))))

(deftest normalize-point-t
  (let [w (dec width)
	h (dec height)]
    (are [p1 p2] (= p1 (normalize-point p2))
	 [0 0] [height width]
	 [h w] [-1 -1])))

(deftest move-snake-t
  (let [snake (create-snake)]
    (is (= 2 (count (:body (move-snake snake :grow)))))))

(deftest eats?-t
  (let [snake (create-snake)
	location (first (:body snake))]
    (are [pred apple] (pred (eats? snake {:location apple}))
	 true? location
	 false? (add-points location [1 0]))))

(deftest lose?-t 
  (let [snake {:body (list [0 0] [1 0] [0 0])}]
    (are [pred sn] (pred (lose? sn))
	 true? snake
	 false? (create-snake))))

(deftest win?-t
  (let [snake {:body (for [a (range 10)] [a 0])}
	noisers (for [a (range 1 12 2)] {:location [a 0]})]
    (are [pred nsz] (pred (win? snake nsz))
	 true? (butlast noisers)
	 false? noisers)))

	  