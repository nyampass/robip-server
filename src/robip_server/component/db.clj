(ns robip-server.component.db
  (:refer-clojure :exclude [select find sort hash])
  (:require [com.stuartsierra.component :as comp]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer :all]
            [monger.gridfs :refer [store-file make-input-file filename content-type metadata]]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-date from-date]]
            [clj-time.local :refer [to-local-date-time]]
            [clj-time.format :as cf]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :refer :all])
  (:import [java.io File]
           [com.mongodb DuplicateKeyException]))

(def file-coll :files)

(def user-coll :users)

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

(defn update-user-info [db email attr-key attr-value]
  (mc/update db user-coll
             {:_id email}
             {:$set {attr-key attr-value}}))

(defn fetch-wifi-settings [db email]
  (-> (mc/find-one-as-map db user-coll
                          {:_id email})
      :wifi))

(defn update-robip-id [db email robip-id]
  (mc/update db user-coll
             {:_id email}
             {:robip-id robip-id}))

(defn fetch-robip-id [db email]
  (-> (mc/find-one-as-map db user-coll
                          {:_id email})
      :robip-id))

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

(defn encrypted-password [password]
  (-> (hash/sha256 password) bytes->hex))

(defn fix-user [{:keys [_id] :as user}]
   (-> user
       (dissoc :password :_id)
       (assoc :id _id)))

(defn find-user-by-email [db email]
 (some-> (mc/find-one-as-map
          db user-coll {:_id email})
         fix-user))

(defn login [db email password]
  (if-let [user
           (mc/find-one-as-map
            db user-coll {:_id email})]
    (if (= (encrypted-password password) (:password user))
      (fix-user user))))

(defn signup [db email name password]
  (if (and (seq email)
           (seq name)
           (seq password))
    (try
      (mc/insert db user-coll
                 {:_id email
                  :name name
                  :password (encrypted-password password)
                  :at (to-date (t/now))})
      (catch DuplicateKeyException e))))

(defn social-signup [db media email name id-in-media]
  (if-let [user (signup db email name
                        (apply str (take 10 (repeatedly #(-> (range (int \a) (inc (int \z))) rand-nth char)))))]
    (mc/update db user-coll
               {:_id email}
               {:$set {:media media
                       :id-in-media id-in-media}})))

(defrecord DbComponent [uri]
  comp/Lifecycle
  (start [this]
    (assoc this :db (:db (mg/connect-via-uri uri))))
  (stop [this]
    (dissoc this :db)))

(defn db-component [config]
  (map->DbComponent config))





