(ns
  mongodb.driver-tests
  "Tests for mongodb.driver"
  (:use mongodb.driver)
  (:use clojure.test))

(def db (struct db-config "TEST_DB" "localhost" 27017))
(def collection "test_collection")
(defstruct blog-post :title :content)

(defn cleanup [f]
  (do
    (f)
    (drop-collection db collection)
    (drop-db db)))

(use-fixtures :each cleanup)

(deftest collection-names-test
  (is (empty? (collection-names db))))

(deftest save-and-find-test
  (let [to-save (struct blog-post "Techcrunch sucks." "This is why.")
        saved-doc (save-doc db collection to-save)
        found-by-title
          (single-doc db collection {:title "Techcrunch sucks."})
        found-by-id
          (single-doc db collection {:_id (:_id saved-doc)})]
    (is (= (select-keys found-by-title (keys to-save)) to-save))
    (is (= found-by-title saved-doc))
    (is (= found-by-id saved-doc))))

(deftest save-and-update-test
  (let [to-save (struct blog-post "Techcrunch sucks." "This is why.")
        saved-doc (save-doc db collection to-save)
        to-update (assoc saved-doc :title "Techcrunch doesn't suck.")
        updated-doc (save-doc db collection to-update)
        all-docs (find-docs db collection {})]
    (is (= (count all-docs) 1))
    (is (= to-update (first all-docs)))))

(deftest save-and-delete-individual-test
  (do
    (map #(save-doc db collection (struct blog-post % %)) (range 10))
    (map #(delete-docs db collection %) (find-docs db collection {}))
    (is (= (count (find-docs db collection {})) 0))))

(deftest save-and-delete-bulk-test
  (do
    (map #(save-doc db collection (struct blog-post % 1)) (range 10))
    (delete-docs db collection (struct blog-post nil 1))
    (is (= (count (find-docs db collection {}))) 0)))

(deftest create-indices-test
  (do
    (create-index db collection {:title 1})
    (some #(= (:key %) {:title 1}) (indices db collection))))
