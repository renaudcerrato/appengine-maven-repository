# appengine-maven-repository

Private Maven repositories hosted on Google App-Engine, backed by Google Cloud Storage. 

# Why ?

Private Maven repositories shouldn't cost you [an arm and a leg](https://bintray.com/account/pricing?tab=account&type=pricing), nor requires you to become a [Linux SysAdmin](https://inthecheesefactory.com/blog/how-to-setup-private-maven-repository/en) to setup, and should ideally be **zero maintenance** and **cost nothing**.

Thanks to Google App-Engine's [free quotas](https://cloud.google.com/appengine/docs/quotas), you'll benefits for free:

* 5GB of storage
* 1GB of daily incoming bandwidth
* 1GB of daily outgoing bandwidth
* 20,000+ storage ops per day
* No maintenance

Moreover, no credit card are required to benefit of those free quotas.

# Limitations

Google App-Engine HTTP requests are limited to 32MB - and thus, any artifacts above that limit can't be hosted.

# Installation

## Prerequisites

First of all, you'll need to go to your [Google Cloud console](https://console.cloud.google.com) and create a new project: 

![](http://i.imgur.com/iSt98wWl.png)

Once your project is created be sure you activated the default Cloud Storage bucket for your app: click *Create* under *Default Cloud Storage Bucket* in the App Engine settings page for your project. 


## Configuration

Clone (or [download](https://github.com/renaudcerrato/appengine-maven-repository/archive/master.zip)) the source code:

```bash
$ git clone https://github.com/renaudcerrato/appengine-maven-repository.git
```

Edit [`appengine-web.xml`](src/main/webapp/WEB-INF/appengine-web.xml#L3), and replace the default application ID with the one you choosed previously:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appengine-web-app xmlns="http://appengine.google.com/ns/1.0">
    <application>your-app-id</application>
    ...
```

Finally, update [`users.txt`](src/main/webapp/WEB-INF/users.txt) to declare users, passwords and roles:

```ini
# That file declares repositories users - using basic authentication.
# Minimal access control is provided through roles: write, read, list.
# Syntax is:
# <username>:<password>:<role>[,<role>]

admin:l33t:write,read,list
john:j123:read,list
donald:coolpw:read,list
guest:guest:list
```
> The `list` role allows you to list/browse the content of your repository, but download is prohibited. Both the `list` and `read` roles are required to be able to fetch artifacts.

## Deployment

Once you're ready to go live, just push the application to Google App-Engine:

```bash
$ cd appengine-maven-repository
$ ./gradlew appengineUpdate
```

> Please note that the very first time you'll run the commands above, a browser page will open - asking you to authorize to Gradle App-Engine plugin to access your Google Cloud account. Just copy the returned authorization code, paste it into your console and press [Enter].

And voilà! Your private Maven repository can be accessed at the following address:

`https://<yourappid>.appspot.com`

# Artifacts

There's no special configuration required to deploy artifacts, just upload your artifacts the way you're used to do. An example using Gradle:

```gradle
apply plugin: 'java'
apply plugin: 'maven'

...

uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: "https://<yourappid>.appspot.com") {
                authentication(userName: "admin", password: "password")
            }
            pom.version = "1.0-SNAPSHOT"
            pom.artifactId = "test"
            pom.groupId = "com.example"
        }
    }
}
```
Using the above, deploying artifacts to your brand new private repository is as simple as:

```bash
$ ./gradlew upload
```

[Accessing artifacts](https://docs.gradle.org/current/userguide/dependency_management.html#sec:accessing_password_protected_maven_repositories) doesn't require any special configuration neither:

```gradle
repositories {
    maven {
        credentials {
            username 'user'
            password 'password'
        }
        url "https://<yourappid>.appspot.com"
    }
}

```

# License

```
Copyright 2016 Cerrato Renaud

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
