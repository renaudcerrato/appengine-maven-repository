# appengine-maven-repository

Private Maven repositories hosted on Google App-Engine, backed by Google Cloud Storage, supporting HTTP Basic authentication and minimalistic user access control deployed in less than 5 minutes.

   * [Why ?](#why-)
   * [Installation](#installation)
      * [Prerequisites](#prerequisites)
      * [Configuration](#configuration)
      * [Deployment](#deployment)
   * [Usage](#usage)
   * [Limitations](#limitations)
   * [License](#license)
   
# Why ?

Private Maven repositories shouldn't cost you [an arm and a leg](https://www.cloudrepo.io/pricing.html), nor requires you to become a [Linux Sys-Admin](https://inthecheesefactory.com/blog/how-to-setup-private-maven-repository/en) to setup, and should ideally be **zero maintenance** and **costs nothing**.

Thanks to Google App-Engine's [free quotas](https://cloud.google.com/appengine/docs/quotas), you'll benefits for free of:

* 5GB of storage
* 1GB of daily incoming bandwidth
* 1GB of daily outgoing bandwidth
* 20,000+ storage ops per day

Moreover, no credit card is required to benefits of the quotas above.

# Installation

## Prerequisites

### Create a new Project
First of all, you'll need to go to your [Google Cloud console](https://console.cloud.google.com/projectselector/appengine/create?lang=java&st=true) to create a new App Engine application: 

![](https://i.imgur.com/SD1WwP3.png)

As soon as your project is created, a default [Google Cloud storage bucket](https://console.cloud.google.com/storage/browser) has been automatically created for you which provides the first 5GB of storage for free.

### Setup the Google Cloud SDK

Follow the [official documentation](https://cloud.google.com/sdk/docs/) to install the latest Google Cloud SDK. As a shorthand, you'll find below the Ubuntu/Debian instructions:


```bash
$ export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)"
$ echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
$ curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
$ sudo apt-get update && sudo apt-get install google-cloud-sdk
```

Do not forget to install the `app-engine-java` [component](https://cloud.google.com/sdk/docs/components#external_package_managers). If you installed the Google Cloud SDK using the instructions above:

```bash
$ sudo apt-get install google-cloud-sdk-app-engine-java
```

As a last step, configure the `gcloud` command line environment and select your newly created App Engine project when requested to do so:

```bash
$ gcloud init
$ gcloud auth application-default login
```

## Configuration

Clone (or [download](https://github.com/renaudcerrato/appengine-maven-repository/archive/master.zip)) the source code:

```bash
$ git clone https://github.com/renaudcerrato/appengine-maven-repository.git
```

Update [`WEB-INF/users.txt`](src/main/webapp/WEB-INF/users.txt) to declare users, passwords and permissions:

```ini
# That file declares your users - using basic authentication.
# Minimalistic access control is provided through the following permissions: write, read, or list.
# Syntax is:
# <username>:<password>:<permission>
# (use '*' as username and password to declare anonymous users)
admin:l33t:write
john:j123:read
donald:coolpw:read
guest:guest:list
```
> The `list` permission allows users to list the content of the repository but prohibits downloads. The `read` permission allows downloads (and implies `list`). The `write` permission allows uploads (and implies `read`).

> Anonymous users are supported by using "*" for both username and password. For example, `*:*:read` will allow anonymous read access. 

## Deployment

Once you're ready to go live, just push the application to Google App-Engine:

```bash
$ cd appengine-maven-repository
$ ./gradlew appengineDeploy
```

And voilà! Your private Maven repository is hosted and running at the following address:

`https://<your-project-id>.appspot.com`

# Usage

You'll find some usage examples in the [examples](examples) folder. There's absolutely no extra steps required to fetch and/or deploy Maven artifacts to your repository: simply use your favorite Maven tools as you're used to do. 

An example deploying artifacts using the maven plugin for Gradle:

```gradle
apply plugin: 'java'
apply plugin: 'maven'

...

uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: "https://<your-project-id>.appspot.com") {
                authentication(userName: "admin", password: "s3curepa55w0rd")
            }
            pom.version = "1.0.0"
            pom.artifactId = "test"
            pom.groupId = "com.example"
        }
    }
}
```

Using the above plugin, deploying artifacts to your repository is as simple as:

```bash
$ ./gradlew upload
```

In the other way, accessing password protected Maven repositories using Gradle only requires you to specify the `credentials` closure:

```gradle
repositories {
    ...
    maven {
        credentials {
            username 'user'
            password 'YouCantGuess'
        }
        url "https://<your-project-id>.appspot.com"
    }
}
```

> Ensure you do NOT commit credentials with your code. With Gradle, you can achieve this by amending the above examples using the approach specified [here](http://stackoverflow.com/a/12751665/752167) of moving your creds to `~/.gradle/gradle.properties` and only referring to the variable names within your build.

# Limitations

Google App-Engine HTTP requests are limited to 32MB - and thus, any artifacts above that limit can't be hosted.

# License

```
Copyright 2018 Cerrato Renaud

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
