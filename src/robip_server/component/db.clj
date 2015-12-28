(ns robip-server.component.db
  (:require [com.stuartsierra.component :as comp]
            [pandect.algo.sha1 :as sha1]))

(defn fetch-file [db hash]
  (get @(:db db) hash))

(defn save-file [db file]
  (let [hash (sha1/sha1 file)]
    (swap! (:db db) assoc hash file)
    hash))

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
