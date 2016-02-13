(ns robip-server.component.db
  (:require [com.stuartsierra.component :as comp]
            [pandect.algo.sha1 :as sha1]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.gridfs :refer [store-file make-input-file filename content-type metadata]])
  (:import [java.io File]))

(def coll "files")

(defn peek-latest [db-component id]
  (mc/find-one-as-map (:db db-component)
                      coll
                      {:_id id}))

(defn fetch-latest [db-component id]
  (mc/find-and-modify (:db db-component) coll
                      {:_id id}
                      {$inc {:downloads 1}}
                      {:return-new true}))

(defn- update-file [db id new-path]
  (mc/find-and-modify db coll
                      {:_id id}
                      {$set {:path new-path, :downloads 0}
                       $inc {:build 1}}
                      {:upsert true :return-new true}))

(defn save-file [db-component id ^File file]
  (:build (update-file (:db db-component) id (.getPath file))))

(defrecord DbComponent [uri]
  comp/Lifecycle
  (start [this]
    (prn :uri uri)
    (assoc this :db (:db (mg/connect-via-uri uri))))
  (stop [this]
    (dissoc this :db)))

(defn db-component [config]
  (map->DbComponent config))
