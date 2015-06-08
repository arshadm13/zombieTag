bluelist-push-node sample
===

The bluelist-push-node code is the Node.js runtime code used for both the Android and iOS samples to add Cloud Code notifications. 

This sample works with the Mobile Cloud, an application boilerplate that is available on [IBM Bluemix](https://www.ng.bluemix.net).  With this boilerplate, you can quickly incorporate pre-built, managed, and scalable cloud services into your mobile applications without relying on IT involvement. You can focus on building your mobile applications rather than the complexities of managing the back end infrastructure.

Downloading this sample
---

You can clone the samples from IBM DevOps Services with the following command:

    git clone https://hub.jazz.net/git/mobilecloud/bluelist-push

The bluelist-push-node code is the Node.js runtime code used with both the bluelist-push-android and bluelist-push-iOS samples.

Prerequisite's
---
Before you can run the sample you need to install the prerequisite software components.

Download the [Cloud Foundry CLI version 6](https://github.com/cloudfoundry/cli/releases), and choose the installer appropriate for the system from which you will run the CLI.

Running this sample
---

To test the Node.js code you need to have created a Mobile Cloud Boilerplate application with [IBM Bluemix](http://bluemix.net) and you need to make a note of your application id.

### Configuration

You need to modify the ```app.js``` file with your corresponding application id and application route.

```javascript
//configuration for application
var appConfig = {
    applicationId: "<INSERT_APP_ID>",
    applicationRoute: "<INSERT_APP_ROUTE>"
};
```

### Deploy to Bluemix
1. Open a command prompt and go to the directory containing the bluelist-push-node code.
```bash
cd <git repository>/bluelist-push/bluelist-push-node
```
2. Verify the Cloud Foundry CLI is installed.
```bash
cf --version 
```
3. Log in to Bluemix from the CLI
```bash
cf login -a https://ace.ng.bluemix.net
```
4. Push (upload) the bluelist-push-node application up to the Bluemix Node.js runtime.
```bash
cf push ${yourAppName} -p . -m 512M
```