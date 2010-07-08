(ns snakejure.menu-gui
    (:import (javax.swing JFrame JList)
	     (java.awt.event KeyAdapter KeyEvent))
    (:use [snakejure.level-loader :only (get-levels-map)]
	  [snakejure.game-gui :only (create-game-panel)]))


(defn- create-poor-jlist []
  "Creates Jlist without any listeners."
  (let [level-names (apply sorted-set (keys (get-levels-map)))]
    (JList. (to-array level-names))))

(defn- create-key-listener [jframe]
  (proxy [KeyAdapter] []
	 (keyPressed [e] 
		   (when (= (.getKeyCode e) (KeyEvent/VK_ENTER))
			  (let [name (.getSelectedValue (.getSource e))
				levels-map (get-levels-map)
				game-panel (create-game-panel ((levels-map name)))]
			    (doto jframe
			      (.remove (.getSource e))
			      (.add game-panel)
			      (.revalidate))
			    (.requestFocus game-panel))))))

(defn- create-jlist [frame]
  (let [jlist (create-poor-jlist)
	key-listener (create-key-listener frame)]
    (doto jlist
      (.addKeyListener key-listener))))


(defn show-menu []
  (let [jframe (JFrame. "Snakejure")
	jlist (create-jlist (.getContentPane jframe))]
    (doto jframe
      (.add jlist)
      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setVisible true))))
	 
  