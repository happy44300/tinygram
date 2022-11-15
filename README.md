# TinyGram
Bilal Molli & Erwan Boisteau-Desdevises


TinyGram is a University Project realized during the first year of the ALMA Master's Degree at Nantes Université.

## Objectives

TinyGram is a simplistic replication of a social media network, made using Google Cloud Services, meant to implement scalable features. 
In particular, the following features were implemented: 
- Following a user
- Posting a picture
- Liking a picture
- Retrieving recent posts

## Product

This project relies on the services provided by Google Cloud, and as such uses the Google Datastore. Listed below are the different Kinds involved in the structure of TinyGram: 
* User
![UserKindPicture](./other/user_kind_picture.png)

* FollowerShard
![FollowerShard](./other/follower_shard_kind_picture.png)

* Post
![UserKindPicture](./other/post_kind_picture.png)

* ReceiverShard
![UserKindPicture](./other/receiver_shard_kind_picture.png)

* LikerShard
![UserKindPicture](./other/liker_shard_kind_picture.png)

# fast install

* precondition: you have a GCP project selected with billing activated. 
* go to GCP console and open a cloud shell
* git clone https://github.com/momo54/webandcloud.git
* cd webandcloud
* mvn appengine:deploy (to deploy)
* mvn appengine:run (to debug in dev server)

# webandcloud from the lab

**Be sure your maven has access to the web**
* you should have file ~/.m2/settings.xml
* otherwise cp ~molli-p/.m2/settings.xml ~/.m2/

```
molli-p@remote:~/.m2$ cat settings.xml
<settings>
 <proxies>
 <proxy>
      <active>true</active>
      <protocol>https</protocol>
      <host>proxy.ensinfo.sciences.univ-nantes.prive</host>
      <port>3128</port>
    </proxy>
  </proxies>
</settings>
```

## import and run in eclipse
* install the code in your home:
```
 cd ~
 git clone https://github.com/momo54/webandcloud.git
 cd webandcloud
 mvn install
```
* Change "sobike44" with your google project ID in pom.xml
* Change "sobike44" with your google project ID in src/main/webapp/WEB-INF/appengine-web.xml

## Run in eclipse

* start an eclipse with gcloud plugin
```
 /media/Enseignant/eclipse/eclipse
 or ~molli-p/eclipse/eclipse
 ```
* import the maven project in eclipse
 * File/import/maven/existing maven project
 * browse to ~/webandcloud
 * select pom.xml
 * Finish and wait
 * Ready to deploy and run...
 ```
 gcloud app create error...
 ```
 Go to google cloud shell console (icon near your head in google console)
 ```
 gcloud app create
 ```


## Install and Run 
* (gcloud SDK must be installed first. see https://cloud.google.com/sdk/install)
 * the gcloud command should be in your path. Run the following command to initialize your local install of gcloud.
```
gcloud init
```
* git clone https://github.com/momo54/webandcloud.git
* cd webandcloud
* running local (http://localhost:8080):
```
mvn package
mvn appengine:run
```
* Deploying at Google (need gcloud configuration, see error message -> tell you what to do... 
)
```
mvn appengine:deploy
gcloud app browse
```

# Access REST API
* (worked before) 
```
https://<yourapp>.appstpot.com/_ah/api/explorer
```
* New version of endpoints (see https://cloud.google.com/endpoints/docs/frameworks/java/adding-api-management?hl=fr):
```
mvn clean package
mvn endpoints-framework:openApiDocs
gcloud endpoints services deploy target/openapi-docs/openapi.json 
mvn appengine:deploy
```
