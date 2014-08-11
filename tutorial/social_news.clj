;; work through at the REPL, evaulating each form

(use 'datomic.samples.repl)

(easy!)
(doc easy!)

(doc scratch-conn)

(def conn (scratch-conn))

(doc transact-all)
(source transact-all)
(transact-all conn (io/resource "day-of-datomic/social-news.edn"))

(doc qes)
(source qes)
(defpp all-stories
  "All stories"
  (qes '[:find ?e :where [?e :story/url]] (d/db conn)))

(defpp all-stories-names
  "All stories"
  (d/q '[:find ?n :where [?e :story/title ?n]] (d/db conn)))

(defpp new-user-id (d/tempid :db.part/user))

(defpp upvote-all-stories
  "The new user will upvote every story in the database.
   Transaction data for new-user-id to upvote all stories"
  #_"Q. What am I doing here?
      A. I am creating a transaction for the attribute :user/upVotes. The entity
      ID for this transaction is 'new-user-id'."
  (mapv
   (fn [[story]] [:db/add new-user-id :user/upVotes (:db/id story)])
   all-stories))

(defpp new-user
  "Transaction data for a new user"
  #_"I am UPDATING (using the same id (new-user-id) the 'new-user-id' with:
     first-name
     last-name
     email."
  [{:db/id new-user-id
    :user/email "john@example.com"
    :user/firstName "John"
    :user/lastName "Doe"}])

(defpp upvote-tx-result
  "In a single transaction, create new user and upvote all stories"
  (->> (concat upvote-all-stories new-user)
       (d/transact conn)))

(defpp all-users
  "All users"
  (d/q '[:find ?k ?t ?e,
         :where
         [?e :user/firstName ?k]
         [?e :user/upVotes ?n]
         [?n :story/title ?t]
         [?e :user/email ?em]]
       (d/db conn)))

(defpp change-user-name-result
  "Demonstrates upsert. Tempid will resolve to existing id to
   match specified :user/email."
  (d/transact
   conn
   [{:user/email "john@example.com" ;; this finds the existing entity
     :db/id #db/id [:db.part/user]  ;; will be replaced by existing id
     :user/firstName "Johnathan"}]))

(defpp change-user-name-result
  "Demonstrates upsert. Tempid will resolve to existing id to
   match specified :user/email."
  (d/transact
   conn
   [{:user/email "john@example.com" ;; this finds the existing entity
     :db/id #db/id [:db.part/user]  ;; will be replaced by existing id
     :user/firstName "Johnathan Haywire"}]))

(doc qe)
(source qe)

(defpp john
  (qe '[:find ?e :where [?e :user/email "john@example.com"]]
      (d/db conn)))

(defpp johns-upvote-for-pg
  (qe '[:find ?story
        :in $ ?e
        :where
        [?e :user/upVotes ?story]
        [?story :story/url "http://www.paulgraham.com/avg.html"]]
      (d/db conn)
      (:db/id john)))

;; Damn it John, why are you up-voting shit you don't like?!!
(defpp damnit-john
  (d/transact
   [[:db/retract (:db/id john) :user/upVotes (:db/id johns-upvote-for-pg)]]))


(defpp john
  (find-by (d/db conn) :user/email "john@example.com"))

;; should now be only two, since one was retracted
(get john :user/upVotes)

(defpp data-that-retracts-johns-upvotes
  "You can use/format the query result to perform more transactions!! (beautiful)"
  (let [db (d/db conn)]
    (->> (d/q '[:find ?op ?e ?a ?v
                :in $ ?op ?e ?a
                :where [?e ?a ?v]]
              db
              :db/retract
              (:db/id john)
              :user/upVotes)
         (into []))))

(d/transact conn data-that-retracts-johns-upvotes)

(defpp john
  (find-by (d/db conn) :user/email "john@example.com"))

;; all gone
(get john :user/upVotes)

(doc gen-users-with-upvotes)

(defpp ten-new-users
  (gen-users-with-upvotes (mapv first all-stories) "user" 10))

(def add-ten-new-users-result
  (d/transact conn ten-new-users))

;; how many users are there?
(count (d/q '[:find ?e ?v
              :where
              [?e :user/email ?v]]
            (d/db conn)))

;; how many users have upvoted something?
(count (d/q '[:find ?e
              :where
              [?e :user/email]
              [?e :user/upVotes]]
            (d/db conn)))

;; count how many users have not upvoted anything
;; To do this, exclude the query of upvotes and use filter instead
;; (:upvotes keyword added just to ensure that query is correct.)
(defpp users-did-not-vote
  #_("This (d/q) can be replaced by 'find-all-by. But it's here for fun")
  (->> (d/db conn)
       (d/q '[:find ?e
              :where
              [?e :user/email]])
       (map (comp (partial d/entity (d/db conn)) first))
       (filter #(nil? (:user/upVotes %)))
       (map #(hash-map :email (:user/email %)
                       :name (:user/firstName %)
                       :upvotes (:user/upVotes %)))))

;; Datomic does not need a left join to keep entities missing
;; some attribute. Just leave that attribute out of the query,
;; and then ask for it on the entity.
(defpp users-with-emails-and-upvotes
  (->> (find-all-by (d/db conn) :user/email)
       (mapv
        (fn [[ent]]
          {:email (:user/email ent)
           :upvoted (mapv :story/url (:user/upVotes ent))}))))

;; Query for all the users without a first name
(defpp users-without-first-name
  (->> (find-all-by (d/db conn) :user/email)
       (filter (fn [[ent]]
                 (nil? (:user/firstName ent))))
       (mapv #(:user/email (first %)))
       sort))

;; Find the cardinality of user/upvotes
(d/q '[:find ?v
       :where
       [:user/upVotes :db/cardinality ?card]
       [?card :db/ident ?v]]
     (d/db conn))

;; find all users and their upvotes, using data function maybe
;; to simulate outer join
(d/q '[:find ?email ?upvote
       :where
       [?e :user/email ?email]
       [(datomic.samples.query/maybe $ ?e :user/upVotes :none) ?upvote]]
     (d/db conn))
