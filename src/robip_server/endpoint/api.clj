(ns robip-server.endpoint.api
  (:require [compojure.core :refer [routes context GET POST]]
            [compojure.route :as route]
            [ring.util.response :as res]
            [ring.middleware.format :refer [wrap-restful-format]]
            [robip-server.builder :as builder]
            [robip-server.component.db :as db])
  (:import java.io.File))

(defn response [status opts]
  (res/response (merge {:status status} opts)))

(defn ok [& {:as opts}]
  (response :ok opts))

(defn error [msg & {:as opts}]
  (response :error (merge {:message msg} opts)))

(defn build [{{:keys [id code wifi]} :params} db]
  (if code
    (let [prev-build (or (:build (db/peek-latest db id)) 0)
          result (builder/build code {:robip-id id :build (inc prev-build) :wifi wifi})
          {bin-file :bin-file {:keys [out err exit]} :result} result]
      (db/update-wifi-settings (:db db) id wifi)
      (if bin-file
        (let [build (db/save-file db id bin-file)]
          (ok :build build :out out :err err :exit exit))
        (error "build failed" :out out :err err :exit exit)))
    (error "invalid request")))

(defn fetch-wifi [req db]
  (ok :wifi (or (db/fetch-wifi-settings (:db db) (-> req :params :id))
                [])))

(defn- user-agent [{headers :headers :as params}]
  (get headers "user-agent"))

(defn- robip-boad-request? [params]
  (= (user-agent params)
     "ESP8266-http-Update"))

(defn fetch-latest [{{:keys [id since]} :params :as params} db]
  (if-let [{:keys [build path]} (db/fetch-latest db id)]
    (let [with-append-log
          (fn [log-action fn]
            (let [ret (fn)]
              (if (robip-boad-request? params)
                (db/append-log (:db db) id log-action))
              ret))]
      (if (or (not since) (< (Integer. since) build))
        (with-append-log :update
          #(res/file-response path))
        (with-append-log :online
          (constantly {:status 404 :headers {} :body ""}))))
    (error "invalid id")))

(defn logs [{{:keys [id]} :params} db]
  (ok :logs (db/formatted-logs (:db db) id 10)))

(defn signup [{{:keys [email username password]} :params} db]
  (if-let [user (db/signup (:db db) email username password)]
    (ok :message "登録しました！")
    (error "登録情報を確認してください.すでに同一のメールアドレスが登録されている可能性があります")))

(defn login [{{:keys [email password]} :params} db]
  (or (if-let [user (db/login (:db db) email password)]
        (ok :id (:id user) :name (:username user)))
      (error "ログインに失敗しました")))

(defn api-endpoint [{db :db}]
  (-> (routes
       (GET "/" []
            (-> "index.html"
                (res/file-response {:root "resources/public"
                                    :allow-symlinks? true})
                (res/content-type "text/html")))
       (context "/api" []
                (GET "/:id/wifi" req
                     (fetch-wifi req db))
                (POST "/:id/build" req
                      (build req db))
                (GET "/:id/latest" req
                     (fetch-latest req db))
                (GET "/:id/logs" req
                     (logs req db))
                (POST "/users" req
                      (signup req db))
                (POST "/login" req
                      (login req db)))
       (route/resources "/"))
      (wrap-restful-format :formats [:json-kw])))
