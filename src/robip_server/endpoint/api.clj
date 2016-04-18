(ns robip-server.endpoint.api
  (:require [compojure.core :refer [routes context GET POST PUT]]
            [compojure.route :as route]
            [ring.util.response :as res]
            [ring.middleware.format :refer [wrap-restful-format]]
            [robip-server.builder :as builder]
            [robip-server.component.db :as db]
            [robip-server.config :as config]
            [robip-server.sign :as sign])
  (:import java.io.File))

(defn response [status opts]
  (res/response (merge {:status status} opts)))

(defn ok [& {:as opts}]
  (response :ok opts))

(defn error [msg & {:as opts}]
  (response :error (merge {:message msg} opts)))

(defn session-user-id-by-req [req]
  (-> req :session :id))

(defn session-user-by-req [req db]
  (if-let [id (session-user-id-by-req req)]
    (db/find-user-by-email db id)))

(defn build [{{:keys [code wifi]} :params :as req} db]
  (if-let [user (session-user-by-req req (:db db))]
    (if (and code
             (:robip-id user))
      (let [id (:robip-id user)
            prev-build (or (:build (db/peek-latest db id)) 0)
            result (builder/build code {:robip-id id :build (inc prev-build) :wifi wifi})
            {bin-file :bin-file {:keys [out err exit]} :result} result]
        (if bin-file
          (let [build (db/save-file db id bin-file)]
            (ok :build build :out out :err err :exit exit))
          (error "build failed" :out out :err err :exit exit)))
      (error "invalid code or robip-id"))
    (error "not login")))

(defn fetch-user-info [req db]
  (if-let [id (-> req :session :id)]
    (ok :user (db/find-user-by-email (:db db) id))
    (error "not login")))

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

(defn signup [{{:keys [email name password]} :params} db]
  (if-let [user (db/signup (:db db) email name password)]
    (-> (ok :message "登録しました！")
        (assoc :session {:id email}))
    (error "登録情報を確認してください.すでに同一のメールアドレスが登録されている可能性があります")))

(defn login [{{:keys [email password]} :params} db]
  (or (if-let [user (db/login (:db db) email password)]
        (-> (ok :id (:id user) :name (:name user))
            (assoc :session {:id email})))
      (error "ログインに失敗しました")))

(defn logout [req]
  (-> (ok)
      (assoc :session nil)))

(defn update-user-info [{params :params :as req} key db]
  (do
    (db/update-user-info (:db db) (session-user-id-by-req req)
                       key (key params))
    (ok)))

(defn update-file [{{:keys [index name xml]} :params :as req} db]
  (db/update-user-info (:db db) (session-user-id-by-req req)
                       (keyword (str "files." index))
                       {:name name, :xml xml})
  (ok))

(defn facebook-login [{:keys [query-params] :as req} db]
  (let [redirect-fn #(assoc (res/redirect (if (= (:type query-params) "app")
                                            "/app.html"
                                            "/editor.html"))
                            :session {:id (:id %)})]
    (if-let [{:keys [id name email]} (sign/facebook-user-info (config/facebook-config
                                                               (-> req :params :type)) query-params)]
      (if-let [user (db/social-signup (:db db) :facebook email name id)]
        (redirect-fn user)
        (if-let [user (db/find-user-by-email (:db db) email)]
          (redirect-fn user))))))

(defn api-endpoint [{db :db http :http :as config}]
  (-> (routes
       (GET "/" []
            (-> "index.html"
                (res/file-response {:root "resources/public"
                                    :allow-symlinks? true})
                (res/content-type "text/html")))
       (GET "/login/facebook/:type" req
            (res/redirect (sign/auth-reqeuest-uri
                           (config/facebook-config (-> req :params :type)))))
       (GET "/login/facebook-auth/:type" req
            (facebook-login req db))
       (context "/api" []
                (GET "/:id/latest" req ;; for halake board
                     (fetch-latest req db))
                (POST "/board/build" req
                      (build req db))
                (GET "/board/logs" req
                     (logs req db))
                (POST "/users" req
                      (signup req db))
                (GET "/users/me" req
                     (fetch-user-info req db))
                (POST "/users/me/wifi" req
                     (update-user-info req :wifi db))
                (POST "/users/me/robip-id" req
                     (update-user-info req :robip-id db))
                (PUT "/users/me/files/:index" req
                     (update-file req db))
                (POST "/login" req
                      (login req db))
                (GET "/logout" req
                      (logout req)))
       (route/resources "/"))
      (wrap-restful-format :formats [:json-kw])))
