(ns snakejure.ui
      (:use (clojure.contrib import-static)
	    (snakejure core))
      (:require   (snakejure.levels basic))
      (:import (java.awt Dimension)
	       (java.awt.event ActionListener KeyListener)
	       (javax.swing JPanel Timer JFrame JOptionPane)))

(import-static java.awt.event.KeyEvent VK_SPACE VK_LEFT VK_RIGHT VK_UP VK_DOWN)

(def key-to-dir {VK_RIGHT :right
                 VK_LEFT :left
		 VK_UP :up
		 VK_DOWN :down})
(def size 20)
(def speed 100)
(def dir-keys (set (keys key-to-dir)))

(defn reset
  "Take level ref and set new level to it."
  [level]
  (dosync (ref-set level (snakejure.levels.basic/create-level))))

(defn draw-point 
  "Draws point on given graphics g."
  [g [y x]]
  (.fillRect g (* size x) (* size y) size size))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :snake [g {:keys [body color]}]
  (.setColor g color)
  (doseq [part body] (draw-point g part)))

(defmethod paint :apple [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :wall [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :noiser [g {:keys [location color-noise color-silence]} snake]
  (if (overlaps-body? snake location)
    (.setColor g color-noise)
    (.setColor g color-silence))
  (draw-point g location))

(defn switch-timer [timer]
  "Stop timer, if it's running.
  Start otherwise."
  (if (.isRunning timer) 
    (.stop timer)
    (.restart timer)))

(defn game-panel 
  "Creates proxy...
  See source :)"
  [frame level]
  (proxy [JPanel ActionListener] []
	 (paintComponent [g]
			 (let-map [@level snake noisers walls apple]
				  (proxy-super paintComponent g)
				  (paint g apple)
				  (paint g snake)
				  (doseq [noiser noisers] (paint g noiser snake))
				  (doseq [wall walls] (paint g wall))))
	 (actionPerformed [e]
			  (let-map [@level snake noisers walls] 
				   (update-snake-position level)
				   (when (lose? snake walls) 
				     (JOptionPane/showMessageDialog frame "You lose...")
				     (reset level))
				   (when (win? snake noisers)
				     (JOptionPane/showMessageDialog frame "You win!")
				     (reset level))
				   (.repaint this)))
	 (getPreferredSize []
			   (Dimension. (* (inc (dec width)) size) 
				       (* (inc (dec height)) size)))))

(defn create-key-listener [level timer]
  (proxy [KeyListener] []
	 (keyPressed [e]
		     (if (= (.getKeyCode e) VK_SPACE)
		       (switch-timer timer))
		     (if (dir-keys (.getKeyCode e))
		       (update-snake-direction level (key-to-dir (.getKeyCode e)))))
	 (keyReleased [e])
	 (keyTyped [e])))

(defn game []
  "Starts game. It will create frame with walls, snake, apple and noisers in it."
  (let [level (ref nil)
	frame (JFrame. "Snake")
	panel (game-panel frame level)
	timer (Timer. speed panel)
	key-listener (create-key-listener level timer)]
    (reset level)
    (doto panel
      (.setFocusable true)
      (.addKeyListener key-listener))
    (doto frame
      (.add panel)
      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setVisible true))
    (.start timer)))
