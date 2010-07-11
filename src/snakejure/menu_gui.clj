(ns snakejure.menu-gui
    (:import (javax.swing JFrame JList JOptionPane)
	     (java.awt.event KeyAdapter KeyEvent))
    (:use [snakejure.level-loader :only (get-levels-map)]
	  [snakejure.game-gui :only (create-game-panel)]))


(defn- create-poor-jlist []
  "Creates Jlist without any listeners."
  (let [level-names (apply sorted-set (keys (get-levels-map)))]
    (doto (JList. (to-array level-names))
      (.setSelectedIndex 0))))

(defn- switch-panel [frame panel]
  (doto (.getContentPane frame)
    (.removeAll)
    (.add panel)
    (.revalidate))
  (.requestFocus panel))

(declare create-jlist)

(defn end-fn [frame name result]
  (case result
	:win (JOptionPane/showMessageDialog frame (str "You win level " name))
	:lose (JOptionPane/showMessageDialog frame (str "You lose level " name))
	nil)
  (switch-panel frame (create-jlist frame)))


(defn- create-key-listener [frame]
  (proxy [KeyAdapter] []
	 (keyPressed [e] 
		   (when (= (.getKeyCode e) (KeyEvent/VK_ENTER))
			  (let [name (.getSelectedValue (.getSource e))
				levels-map (get-levels-map)
				game-panel (create-game-panel ((levels-map name)) 
							      (partial end-fn frame name))]
			    (switch-panel frame game-panel))))))

(defn- create-jlist [frame]
  (let [jlist (create-poor-jlist)
	key-listener (create-key-listener frame)]
    (doto jlist
      (.addKeyListener key-listener))))

(defn show-menu []
  (let [frame (JFrame. "Snakejure")
	list (create-jlist frame)]
    (doto frame
      (.add list)
      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setVisible true))))
	 
  