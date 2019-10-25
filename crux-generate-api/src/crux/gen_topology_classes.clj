(ns crux.gen-topology-classes
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [crux.topology-info :as ti]))

(import (com.squareup.javapoet MethodSpec TypeSpec FieldSpec JavaFile)
        javax.lang.model.element.Modifier
        java.util.Properties)

(defn format-topology-key [key]
  (-> (name key)
      (string/replace "-" "_")
      (string/upper-case)))

(defn format-topology-key-method [key]
  (->> (name key)
       (#(string/split % #"-"))
       (map string/capitalize )
       (string/join "")
       (str "With")))

(defn add-properties-field [class properties-name]
  (let [field (FieldSpec/builder Properties properties-name
                                 (into-array ^Modifier [Modifier/PUBLIC]))]
    (.addField class (.build field))))

(defn add-constructor [class properties-name]
  (let [constructor (MethodSpec/constructorBuilder)]
    (.addModifiers constructor (into-array ^Modifier [Modifier/PUBLIC]))
    (.addStatement constructor (str properties-name " = new $T()") (into-array [Properties]))
    (.addMethod class (.build constructor))))

(defn build-key-field[[key value]]
  (let [field (FieldSpec/builder String (format-topology-key key)
                                 (into-array ^Modifier [Modifier/PUBLIC Modifier/FINAL Modifier/STATIC]))]
    (.initializer field "$S" (into-array [(str key)]))
    (.build field)))

(defn build-key-default-field[[key val]]
  (let [field (FieldSpec/builder (type val) (str (format-topology-key key) "_DEFAULT")
                                 (into-array ^Modifier [Modifier/PUBLIC Modifier/FINAL Modifier/STATIC]))]
    (if (string? val)
      (.initializer field "$S" (into-array [val]))
      (.initializer field "$L" (into-array [val])))
    (.build field)))

(defn build-topology-key-setter [key properties-name]
  (let [set-property (MethodSpec/methodBuilder (format-topology-key-method key))]
    (.addModifiers set-property (into-array ^Modifier [Modifier/PUBLIC]))
    (.addParameter set-property String "val" (make-array Modifier 0))
    (.addStatement set-property (str properties-name ".put($L, val)")
                   (into-array [(format-topology-key key)]))
    (.build set-property)
    ))

(defn add-topology-key-code [class properties-name [key value]]
  (do
    (.addField class (build-key-field [key value]))
    (when (:default value)
      (.addField class (build-key-default-field [key (:default value)])))
    (.addMethod class (build-topology-key-setter key properties-name))))

(defn build-java-class [class-name topology-info]
  (let [class (TypeSpec/classBuilder class-name)
        properties-name (str class-name "Properties")]
    (.addModifiers class (into-array ^Modifier [Modifier/PUBLIC]))
    (add-properties-field class properties-name)
    (add-constructor class properties-name)
    (doall (map #(add-topology-key-code class properties-name %) (seq topology-info)))
    (.build class)))

(defn build-java-file [class-name topology-info]
  (let [javafile (build-java-class class-name topology-info)
        output (io/file class-name)]
    (-> (JavaFile/builder "crux.api" javafile)
        (.build)
        (.writeTo output))))

(defn gen-topology-file [class-name topology]
  (let [topology-info (ti/get-topology-info topology)]
    (build-java-file class-name topology-info)))

;;Currently fails, as keyword cannot be turned into valid variable
;(gen-topology-file "StandaloneNode" 'crux.standalone/topology)


                                        ;(gen-topology-file "KafkaNode" 'crux.kafka/topology)