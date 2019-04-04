(ns terraform
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.xml :as xml]
            [cuerdas.core :as str]))

(defn copy [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn xml-content-parser
  ([{:keys [tag] :as el}] (xml-content-parser tag el)) 
  ([k {:keys [content]}]
   (hash-map k (first content))))

(defn itunes-image-parser [els]
  (when-let [href (get-in (first els) [:attrs :href])]
    {:image (last (str/split href #"/"))
     :image-url href}))

(def channel-tag-parsers
  {:title #(xml-content-parser (first %))
   :description #(xml-content-parser (first %))
   :link #(xml-content-parser :url (first %))
   :itunes:image itunes-image-parser
   :itunes:category (fn [els]
                      {:categories (into [] (map (fn [{:keys [attrs]}]
                                                   (get attrs :text))
                                                 els))})
   :itunes:owner (fn [els]
                   (xml-content-parser :email (first (get (first els) :content))))
   :itunes:explicit (fn [els]
                      {:explicit? (= (first (get (first els) :content)) "Yes")})})

(defn parse-config [tag-parsers content]
  (reduce
    (fn [config tag]
      (let [tag-parser (get tag-parsers tag)]
        (merge config (tag-parser (filter #(= tag (get % :tag)) content)))))
    {}
    (keys tag-parsers)))

(defn parse-site-config! [{:keys [content] :as rss}]
  (parse-config channel-tag-parsers (get (first content) :content)))

(def episode-tag-parsers
  {:title #(xml-content-parser (first %))
   :description #(xml-content-parser (first %))
   :enclosure (fn [els]
                {:url (get-in (first els) [:attrs :url])})
   :pubDate #(xml-content-parser :published-at (first %))
   :itunes:image itunes-image-parser
   :itunes:explicit (fn [els]
                      {:explicit? (= (first (get (first els) :content)) "Yes")})})

(defn parse-episode-config! [{:keys [content] :as item}]
  (parse-config episode-tag-parsers content))

(defn parse-episodes! [rss]
  (map parse-episode-config!
       (filter #(= :item (get % :tag))
               (get (first (get rss :content)) :content))))

(defn -main [url & args]
  (let [out-dir (or (first args) "out")
        rss (-> url xml/parse)
        site-config (parse-site-config! rss)
        site-config-filepath (str out-dir "/config.edn")
        site-image-filepath (when-let [cover (get site-config :image)]
                              (str out-dir "/" cover))
        episodes-output-dir (str out-dir "/episodes")]
    (io/make-parents site-config-filepath)
    (when (some? site-image-filepath)
      (copy (get site-config :image-url) site-image-filepath))
    (pprint/pprint site-config (io/writer site-config-filepath))
    (doseq [episode-config (parse-episodes! rss)
            :let [{:keys [image image-url title url]} episode-config
                  episode-output-dir (str episodes-output-dir "/" (str/uslug title))
                  episode-config-filepath (str episode-output-dir "/config.edn")
                  episode-image-filepath (when (some? image)
                                           (str episode-output-dir "/" image))
                  episode-audio-filepath (str episode-output-dir "/" (last (str/split url #"/")))]]
      (io/make-parents episode-config-filepath)
      (when (some? episode-image-filepath)
        (copy image-url episode-image-filepath))
      (pprint/pprint episode-config (io/writer episode-config-filepath))
      (copy url episode-audio-filepath))))
