'use strict';

(function() {

	var MODULE = angular.module('osgi.enroute.trains', [ 'ngRoute',
			'ngResource', 'enJsonrpc', 'enEasse', 'enMarkdown' ]);

	var resolveBefore;
	var alerts = [];
	var trains = {
		events : [],
		segments : {},
		locations: {},
		passengers: {},
		rfids: {},
		ep : null,
		destinations : []
	};
	var stations = {
		events : []
	}

	function error(msg) {
		alerts.push({
			msg : msg,
			type : 'danger'
		});
	}

	MODULE.config(function($routeProvider, en$jsonrpcProvider) {
		en$jsonrpcProvider.setNotification({
			error : error
		})
		resolveBefore = {
			trainsEndpoint : en$jsonrpcProvider.endpoint("trains")
		};

		$routeProvider.when('/', {
			controller : mainProvider,
			templateUrl : '/osgi.enroute.trains/main/htm/home.htm',
			resolve : resolveBefore
		});
		$routeProvider.when('/about', {
			templateUrl : '/osgi.enroute.trains/main/htm/about.htm',
			resolve : resolveBefore
		});
		$routeProvider.otherwise('/');

	});

	MODULE.run(function($rootScope, $location, en$easse, en$jsonrpc) {
		var track = {};
		
		$rootScope.alerts = alerts;
		$rootScope.trains = trains;
		$rootScope.track = track;
		$rootScope.stations = stations;

		en$easse.handle("osgi/trains/observation", function(e) {
			$rootScope.$applyAsync(function() {
				trains.events.push(e);
				if ( trains.events.length > 10)
					trains.events.splice(0,1);
					
				if ( e.train  ) {
					var set = trains.rfids[ e.train ] || {};
					set.train = e.train;
					set.segment= e.segment;
					set.speed=  e.speed;
					trains.rfids[ e.train ] = set;
				}
				
				switch(e.type) {
				case "LOCATED":
					trains.locations[e.train]=e.segment;
					break;
					
				case "SWITCH": {
						var s = track[ e.segment ];
						if ( angular.isDefined(s) )
							s.alt = e.alternate;
					}
					break;
					
				case "SIGNAL": {
						var s = track[ e.segment ];
						if ( angular.isDefined(s) )
							s.color = e.signal;
					}
				case "ASSIGNMENT": {
						if ( trains.rfids[ e.train ] )
							trains.rfids[ e.train  ].assignment = e.assignment;
					}
					break;
					
					
				}
			});
		}, function(e) {
			alerts.push({
				type : 'error',
				msg : e
			});
		});

		en$easse.handle("osgi/trains/station", function(e) {
			$rootScope.$applyAsync(function() {
				
				switch(e.type) {
				case "CHECK_IN":
					trains.ep.getPerson(e.personId).then( function(person) {
						e.message = person.firstName+" "+person.lastName+" checked in at "+e.station;
						e.profilepic = person.picture ? person.picture : "img/user.jpg";
						e.website = person.website;
						stations.events.push(e);
						if ( stations.events.length > 13)
							stations.events.splice(0,1);
					});
					break;
				case "CHECK_OUT":
					trains.ep.getPerson(e.personId).then( function(person) {
						e.message = person.firstName+" "+person.lastName+" checked out at "+e.station;
						e.profilepic = person.picture ? person.picture : "img/user.jpg";
						e.website = person.website;
						stations.events.push(e);
						if ( stations.events.length > 13)
							stations.events.splice(0,1);
					});
					break;
				case "ARRIVAL":
				case "DEPARTURE":
					trains.ep.getTrains().then( function(t) {
						t.forEach( function(d) {
							trains.ep.getPassengersOnTrain(d).then( function(p) {
								trains.passengers[d] = p;
							});
						});
					});
					break;
				default:
					return;
				}
				
				
			});
		}, function(e) {
			alerts.push({
				type : 'error',
				msg : e
			});
		});
		
		
		resolveBefore.trainsEndpoint().then(function(ep) {
			trains.ep = ep;
			trains.ep.getPositions().then( function(pos) {
				angular.copy(pos, track);
			});
			
			trains.ep.getStations().then( function(s) {
				s.forEach( function(station) {
					trains.destinations.push(station);
				});
			});
			
			trains.ep.getTrains().then( function(t) {
				t.forEach( function(d) {
					trains.rfids[d] = { train: d };
					
					trains.ep.getPassengersOnTrain(d).then( function(p) {
						trains.passengers[d] = p;
					});
				});
			});
		});

		$rootScope.closeAlert = function(index) {
			alerts.splice(index, 1);
		};
		$rootScope.page = function() {
			return $location.path();
		}

		$rootScope.getPassengersOnTrain = function(train){
			return trains.passengers[train];
		}
	});

	var mainProvider = function($scope) {
		$scope.assign = trains.ep.assign;
		$scope.rfid= function(segment,train) {
			console.log(segment + " " + train);
			trains.ep.setPosition( train, segment);

		}
	}

})();
