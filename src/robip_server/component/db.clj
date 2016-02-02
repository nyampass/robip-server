(ns robip-server.component.db
  (:require [com.stuartsierra.component :as comp]
            [pandect.algo.sha1 :as sha1]))

(defn fetch-latest [db id]
  (let [res (get @(:db db) id)]
    (swap! (:db db) update-in [id :downloads] (fnil inc 0))
    res))

(defn update-file [db id new-file]
  (prn db id new-file)
  (let [build (get-in db [id :build])]
    (update db id assoc :file new-file :build (inc (or build 0)))))

(defn save-file [db-component id file]
  (prn db-component id file)
  (swap! (:db db-component) update-file id file))

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
