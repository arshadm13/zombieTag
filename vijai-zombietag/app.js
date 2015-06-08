/*
 * Copyright 2014 IBM Corp. All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var express = require('express'),
    bodyparser = require('body-parser'),
    ibmbluemix = require('ibmbluemix'),
    ibmpush = require('ibmpush'),
    ibmdata = require('ibmdata');

//configuration for application
var appConfig = {
    applicationId: "085c1092-7f59-4d95-bec3-17dbf530c015",
    applicationRoute: "vijai-zombietag.mybluemix.net",
	applicationSecret: "5ea91f57f46ce0b0bac8ec81905d4542c680b78e"
};
// create an express app
var app = express();
app.use(bodyparser.json());
app.use(bodyparser.urlencoded({
  extended: true
}));

//uncomment below code to protect endpoints created afterwards by MAS
//var mas = require('ibmsecurity')();
//app.use(mas);

//initialize mbaas-config module
ibmbluemix.initialize(appConfig);
var logger = ibmbluemix.getLogger();
var data = ibmdata.initializeService();
var data = ibmdata.getService();
var push = ibmpush.initializeService();

console.log('Done initializing bluemix services.');

app.use(function(req, res, next) {
	req.ibmpush = ibmpush.initializeService(req);
	req.logger = logger;
	next();
});

//initialize ibmconfig module
var ibmconfig = ibmbluemix.getConfig();

//get context root to deploy your application
//the context root is '${appHostName}/v1/apps/${applicationId}'
var contextRoot = ibmconfig.getContextRoot();
appContext=express.Router();
app.use(contextRoot, appContext);

console.log("contextRoot: " + contextRoot);

// log all requests
app.all('*', function(req, res, next) {
	console.log("Received request to " + req.url);
	next();
});

// create resource URIs
// endpoint: https://mobile.ng.bluemix.net/${appHostName}/v1/apps/${applicationId}/notifyOtherDevices/
appContext.post('/notifyOtherDevices', function(req,res) {
	var results = 'Sent notification to all registered devices successfully.';

	console.log("Trying to send push notification via JavaScript Push SDK");
	var message = { "alert" : "The BlueList has been updated.",
					"url": "http://www.google.com"
	};

	req.ibmpush.sendBroadcastNotification(message,null).then(function (response) {
		console.log("Notification sent successfully to all devices.", response);
		res.send("Sent notification to all registered devices.");
	}, function(err) {
		console.log("Failed to send notification to all devices.");
		console.log(err);
		res.send(400, {reason: "An error occurred while sending the Push notification.", error: err});
	});
});

// host static files in public folder
// endpoint:  https://mobile.ng.bluemix.net/${appHostName}/v1/apps/${applicationId}/static/
appContext.use('/static', express.static('public'));

//redirect to cloudcode doc page when accessing the root context
app.get('/', function(req, res){
	res.sendfile('public/index.html');
});

app.listen(ibmconfig.getPort());
console.log('Server started at port: '+ibmconfig.getPort());

function updatePlayersData() {
	console.log('Updating player data....');
	// Loop through this logic every 15 seconds

	// Get a list of all the players in the game
	var players = data.Query.ofType("Player");
	var playersAttributes = []; 

	players.find().then(function(objects){
		// Create a list of the actual players
		objects.forEach(function(obj){
			playersAttributes.push(obj.attributes);
		});
	}).done(function(){
		var foundZombie = false;
		var zombieCandidate = '';
		// Check if a player is a zombie
		playersAttributes.forEach(function(mainPlayer){
			if(mainPlayer.state == "ZOMBIE") {
				console.log('Player ' + mainPlayer.id + ' is a zombie.');
				foundZombie = true;
				console.log('Checking if any player is close to ' + mainPlayer.id);
				playersAttributes.forEach(function(player)	{
					// Ignore self
					if(mainPlayer.id != player.id) {
						if(player.state != "ZOMBIE") {
							// Calculate distance between the 2 players
							var lat1 = mainPlayer.latitude;
							var lat2 = player.latitude;
							
							var lon1 = mainPlayer.longitude;
							var lon2 = player.longitude;
							
							decimals = 8 || 8;
							var earthRadius = 6371; // km	 
							var dLat = (lat2 - lat1) * Math.PI / 180;
							var dLon = (lon2 - lon1) * Math.PI / 180;
							var lat1 = lat1 * Math.PI / 180;
							var lat2 = lat2 * Math.PI / 180;
						 
							var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
									Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
							var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
							var d = earthRadius * c;
							var distance = 1000 * Math.round(d * Math.pow(10, decimals)) / Math.pow(10, decimals);
				
							console.log('Distance between = ' + mainPlayer.id + ' and ' + player.id + ' is ' + distance + ' meters.');
							
							console.log('passInt: ' + numTagged + 1);
						
						

							// If less than 10 meters then we have some work to do
							if (distance <= 10) {
								console.log('Time to tag!');
								
								// Update state of infected player to zombie in the DB
								var query = data.Query.ofType("Player");

								query.find({id: player.id}).done(function(matchedPlayers){
									matchedPlayers.forEach(function(matchedPlayer){
										matchedPlayer.set("state", "ZOMBIE");
										matchedPlayer.save().done(function(saved){
										console.log('numTagged' + parseInt(matchedPlayer.numTagged + 1));
										console.log(player.id + ' has been tagged and state has been updated to a zombie!');
	
										}, function(err){
											console.error(err);
										});
									});
								});
								
								// Update tag count of mainPlayer
								query.find({id: mainPlayer.id}).done(function(matchedPlayers){
									matchedPlayers.forEach(function(matchedPlayer){
										matchedPlayer.set("numTagged", parseInt(matchedPlayer.numTagged + 01));
										
										matchedPlayer.save().done(function(saved){
										console.log(mainPlayer.id + ' has been credited with creating a new zombie!');
										}, function(err){
											console.error(err);
										});
									});
								});
																
								// Send notification to all players that there is a new zombie.
								var results = 'Sent notification to all registered devices successfully.';

								console.log("Trying to send push notification via JavaScript Push SDK");
								var message = { "alert" : player.id + " has been transformed into a zombie by " + mainPlayer.id + "! Beware!!",
												"url": "http://www.google.com"
								};

								push.sendBroadcastNotification(message,null).then(function (response) {
									console.log("Notification sent successfully to all devices.", response);
								}, function(err) {
									console.log("Failed to send notification to all devices.");
									console.log(err);
								});	
							}
						}
					}
				});
			}
			else {
				zombieCandidate = mainPlayer;
			}
		});
			
		if ((foundZombie == false) && (zombieCandidate != '')) {
			console.log('No zombies found. Randomly tag a player as a zombie.');
			// Update state of mainPlayer in db as a zombie.

			var query = data.Query.ofType("Player");

			query.find({id: zombieCandidate.id}).done(function(matchedPlayers){
				matchedPlayers.forEach(function(matchedPlayer){
					matchedPlayer.set("state", "ZOMBIE");
					matchedPlayer.save().done(function(saved){
					console.log(zombieCandidate.id + ' has been elected to be a zombie!');
					}, function(err){
						console.error(err);
					});
				});
			});
			
			// Send notification to all players that there is a new zombie elected.
			var results = 'Sent notification to all registered devices successfully.';

			console.log("Trying to send push notification via JavaScript Push SDK");
			var message = { "alert" : zombieCandidate.id + " has been transformed into a zombie! Beware!!",
							"url": "http://www.google.com"
			};

			push.sendBroadcastNotification(message,null).then(function (response) {
				console.log("Notification sent successfully to all devices.", response);
			}, function(err) {
				console.log("Failed to send notification to all devices.");
				console.log(err);
			});			
		}
	});
}

function infinite() {
    updatePlayersData();
    setTimeout(infinite, 15000);
}

infinite()
