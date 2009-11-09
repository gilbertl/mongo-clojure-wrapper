(ns
  mongodb.driver
  "Wrapper around Mongo Java driver."
  (:import
    (com.mongodb
      Mongo BasicDBObject BasicDBList DBObject ObjectId))
  (:use [clojure.walk :only (keywordize-keys stringify-keys)])
  (:use [clojure.test :only (is with-test)]))

(defstruct db-config :name :host :port)

(defn- db
  "returns mongodb object"
  [{n :name h :host p :port}]
  (.getDB (new Mongo h p) n))
(def #^{:private true} db (memoize db))

(defn- coll
  "returns collection object"
  [db-config coll-name]
  (.getCollection (db db-config) coll-name))
(def #^{:private true} coll (memoize coll))

(defn collection-names
  "sorted set of db's existing collections"
  [db-config]
  (.getCollectionNames (db db-config)))

(defn- to-clojure-map
  "converts BasicDBObject to clojure map"
  [obj]
  (cond
    (instance? BasicDBObject obj)
      (let [m (keywordize-keys (into {} obj))]
        (zipmap (keys m) (map to-clojure-map (vals m))))
    (instance? BasicDBList obj)
      (vec (map to-clojure-map obj))
    (instance? ObjectId obj)
      (str obj)
    :else obj))

(defn- to-db-obj
  "converts clojure map to BasicDBObject"
  [m]
  (let [id-converted
          (if (contains? m :_id)
            (assoc m :_id (new ObjectId (:_id m)))
            m)]
  (new BasicDBObject (stringify-keys id-converted))))

(defn save-doc
  "insert document into given db and collection"
  [db-config coll-name m]
  (let [c (coll db-config coll-name)
        dbo (to-db-obj m)]
    (to-clojure-map (.save c dbo))))

(defn delete-docs
  "remove document from given db and collection"
  [db-config coll-name m]
  ; TODO: throw exception if no _id
  (let [c (coll db-config coll-name)
        dbo (to-db-obj m)]
     (.remove c dbo)))

(defn count-docs
  "number of docs in collection satisfying criteria"
  [db-config coll-name m]
  (.getCount (coll db-config coll-name) (to-db-obj m)))

(defn find-docs
  "docs in collection satisfying criteria"
  [db-config coll-name m]
  (let [dbos 
         (.find (coll db-config coll-name) (to-db-obj m))]
    (map to-clojure-map dbos)))

(def single-doc (comp first find-docs))

(defn update-docs
  "update the first document found by 'query' with 'update'"
  ; for some reason, the updateMulti method doesn't upload more than one row
  ; hopefully, future releases will fix that
  [db-config coll-name query update]
  (let [c (coll db-config coll-name)
        query (to-db-obj query)
        update (to-db-obj update)]
    (to-clojure-map (.updateMulti c query update))))

(defn create-index
  "creates index on given attributes; 1 for ascending; -1 for descending"
  [db-config coll-name m]
  (.createIndex (coll db-config coll-name) (to-db-obj m)))

(defn indices
  "sorted set of db collection's indices"
  [db-config coll-name]
  (map to-clojure-map (.getIndexInfo (coll db-config coll-name))))

(defn drop-collection
  "drop collection in given db"
  [db-config coll-name]
  (.drop (coll db-config coll-name)))

(defn drop-db
  [db-config]
  (.dropDatabase (db db-config)))
