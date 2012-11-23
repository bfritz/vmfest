(ns vmfest.virtualbox.virtualbox-test
  (:use vmfest.virtualbox.virtualbox :reload)
  (:use clojure.test
        vmfest.virtualbox.session
        vmfest.fixtures)
  (:require [vmfest.virtualbox.machine :as machine])
  (:import vmfest.virtualbox.model.Server))

(def machine-name-1 "Test-1")
(def machine-name-bogus "bogus name")
(def valid-medium-path
  "/Users/tbatchelli/Library/VirtualBox/HardDisks/Test1.vdi")
(def bogus-medium-path "/path/to/nowhere")

(deftest ^{:integration true}
  find-a-machine
  (with-vbox *server* [_ vbox]
    (testing "finding a machine on a vbox (by name)"
      (is (not (nil? (find-vb-m vbox machine-name-1)))))
    (testing "find a machine on a vbox by id"
      (let [vb-m (find-vb-m vbox machine-name-1)
            id (.getId vb-m)]
        (is (not (nil? (find-vb-m vbox id))))))
    (testing "trying to find a non-existing machine returns null"
      (is (nil? (find-vb-m vbox machine-name-bogus))))))

(deftest ^{:integration true}
  find-a-medium
  (with-vbox *server* [_ vbox]
    (testing "finding a medium on a vbox (by name)"
      (is (not (nil? (find-medium vbox valid-medium-path :hard-disk)))))
    ;; from the SDK 4.2 documentation, it seems like using IDs has
    ;; been deprecated.
    #_(testing "find a medium on a vbox by id"
      (let [medium (find-medium vbox valid-medium-path)
            id (.getId medium)]
        (is (not (nil? (find-medium vbox id))))))
    (testing "trying to find a non-existing machine returns null"
      (is (nil? (find-medium vbox bogus-medium-path))))))

(deftest ^{:integration true}
  create-a-machine
  (with-vbox *server* [_ vbox]
    (let [dir "/tmp/vbox-tests"
          ;; for now, the group is hardcoded
          dest-dir (str dir "/vmfest")
          name "test-created-machine"]
      (let [machine (create-machine
                     vbox
                     name
                     "RedHat"
                     true
                     dir)]
        (testing "a machine can be created"
          (is (not (nil? machine))))
        (testing "a machine can be saved"
          (machine/save-settings machine)
          (is (.exists (clojure.java.io/file dest-dir name (str name ".vbox")))))
        (testing "a machine can be registered"
          (register-machine vbox machine)
          (is (not (nil? (find-vb-m vbox name)))))
        (testing "a machine can be unregistered and deleted"
          (let [media
                (machine/unregister machine :detach-all-return-hard-disks-only)]
            (machine/delete machine media)
            (is (nil? (find-vb-m vbox name)))))))))
