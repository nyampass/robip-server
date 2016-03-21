(ns robip-server.component.db
  (:refer-clojure :exclude [select find sort])
  (:require [com.stuartsierra.component :as comp]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer :all]
            [monger.gridfs :refer [store-file make-input-file filename content-type metadata]]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-date from-date]]
            [clj-time.local :refer [to-local-date-time]]
            [clj-time.format :as cf])
  (:import [java.io File]))

(def file-coll :files)

(def user-coll :users)

(def wifi-setting-coll :wifi-settings)

(def log-coll :logs)

(defn append-log [db id action]
  (assert (#{:update :online} action))
  (mc/insert db log-coll
             {:robip-id id :action (name action)
              :at (to-date (t/now))}))

(defn logs [db id count]
  (->> (with-collection db (name log-coll)
         (find {:robip-id id})
         (limit count)
         (sort (sorted-map :at -1)))
       (map #(dissoc % :_id))))

(def datetime-formatter (cf/with-zone
                          (cf/formatter "yyyy/MM/dd HH:mm:ss")
                          (t/default-time-zone)))

(defn formatted-logs [db id limit]
  (let [logs (map (fn [{:keys [action at] :as log}]
                    (str (cf/unparse datetime-formatter(from-date at)) ": "
                         (condp = (keyword action)
                           :update "HaLakeボードのプログラミングを更新しました!"
                           :online "HaLakeボードがインターネットに接続しました!"
                           "-")))
                  (logs db id limit))]
    (if (> (count logs) 0)
      (clojure.string/join "\n" logs)
      "HaLakeボードからのアクセスログが表示されます")))

(defn update-wifi-settings [db id wifi-settings]
  (mc/update db wifi-setting-coll
             {:_id id}
             {:wifi wifi-settings}
             {:upsert true}))

(defn fetch-wifi-settings [db id]
  (-> (mc/find-one-as-map db wifi-setting-coll
                          {:_id id})
      :wifi))

(defn peek-latest [db-component id]
  (mc/find-one-as-map (:db db-component)
                      file-coll
                      {:_id id}))

(defn fetch-latest [db-component id]
  (mc/find-and-modify (:db db-component) file-coll
                      {:_id id}
                      {$inc {:downloads 1}}
                      {:return-new true}))

(defn- update-file [db id new-path]
  (mc/find-and-modify db file-coll
                      {:_id id}
                      {$set {:path new-path, :downloads 0}
                       $inc {:build 1}}
                      {:upsert true :return-new true}))

(defn save-file [db-component id ^File file]
  (:build (update-file (:db db-component) id (.getPath file))))

(defn login [db email password]
  (mc/find-one-as-map db
                      user-coll
                      {:_id email}))

(defn signup [db email username password]
  (if (and (seq email)
           (seq username)
           (seq password))
    (mc/insert db log-coll
               {:_id email
                :username username
                :at (to-date (t/now))})))

(defrecord DbComponent [uri]
  comp/Lifecycle
  (start [this]
    (prn :uri uri)
    (assoc this :db (:db (mg/connect-via-uri uri))))
  (stop [this]
    (dissoc this :db)))

(defn db-component [config]
  (map->DbComponent config))
