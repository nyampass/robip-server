(ns robip-server.component.db
  (:require [com.stuartsierra.component :as comp]
            [pandect.algo.sha1 :as sha1]))

(defn peek-latest [db-component id]
  (get @(:db db-component) id))

(defn fetch-latest [db-component id]
  (let [res (peek-latest db-component id)]
    (swap! (:db db-component) update-in [id :downloads] (fnil inc 0))
    res))

(defn update-file [db id new-file]
  (let [build (get-in db [id :build])]
    (update db id assoc :file new-file :build (inc (or build 0)) :downloads 0)))

(defn save-file [db-component id file]
  (let [db (update-file @(:db db-component) id file)]
    (reset! (:db db-component) db)
    (get-in db [id :build])))

(defrecord DbComponent [db]
  comp/Lifecycle
  (start [this]
    (if db
      this
      (assoc this :db (atom {}))))
  (stop [this]
    (if-not db
      this
      (assoc this :db nil))))

(defn db-component [config]
  (map->DbComponent {}))
