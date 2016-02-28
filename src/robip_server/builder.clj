(ns robip-server.builder
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [me.raynes.fs :as fs])
  (:import [java.net URLEncoder]))

(def command-run-platformio "/usr/local/bin/platformio run")
(def base-project "./resources/platformio-project")

(defn- project-config []
  (let [project-dir (fs/file (fs/tmpdir) (fs/temp-name "robip-" ""))]
    {:base-dir project-dir
     :source-file (fs/file project-dir "src" "main.ino")
     :settings-file (fs/file project-dir "src" "settings.h")
     :command (str "cd " (.getPath project-dir) "; " command-run-platformio)
     :bin-file (fs/file project-dir ".pioenvs" "esp12e" "firmware.bin")}))

(def settings-template
  (str "#ifndef _SETTINGS_H_\n"
       "#define _SETTINGS_H_\n"
       "\n"
       "#define ROBIP_ID \"%s\"\n"
       "#define ROBIP_BUILD %d\n"
       "#define ROBIP_WIFI_COUNT %d\n"
       "#define ROBIP_WIFI_SSID (const char*[%d]) {%s}\n"
       "#define ROBIP_WIFI_PASS (const char*[%d]) {%s}\n"
       "\n"
       "#endif\n"))

(defn wifi-ssids [wifi]
  (join ", " (for [{ssid :ssid} wifi]
               (str "\"" (URLEncoder/encode ssid) "\""))))

(defn wifi-passwords [wifi]
  (join ", " (for [{password :password} wifi]
               (str "\"" password "\""))))

(defn write-settings [writer {:keys [robip-id build wifi]}]
  (.write writer (format settings-template
                         robip-id build
                         (count wifi)
                         (count wifi) (wifi-ssids wifi)
                         (count wifi) (wifi-passwords wifi))))

(defn build [code settings]
  (let [config (project-config)]
    (fs/copy-dir (io/file base-project) (:base-dir config))
    (with-open [writer (io/writer (:source-file config))]
      (.write writer code))
    (with-open [writer (io/writer (:settings-file config))]
      (write-settings writer settings))
    (let [command-result (sh "/bin/sh" "-c" (:command config))
          result (assoc config
                        :result command-result)]
      (if (= (:exit command-result) 0)
        result
        (dissoc result :bin-file)))))

(defn clean [{:keys [base-dir] :as build-result}]
  (when (and base-dir
             (.startsWith (.getPath base-dir) (fs/tmpdir)))
    (fs/delete-dir base-dir)))










