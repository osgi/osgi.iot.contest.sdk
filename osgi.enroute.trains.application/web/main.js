'use strict';

(function() {

	var MODULE = angular.module('osgi.enroute.trains', [ 'ngRoute',
			'ngResource', 'enJsonrpc', 'enEasse', 'enMarkdown', 'ui.bootstrap']);

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
		events : [],
		passengers: {}
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

	MODULE.run(function($rootScope, $location, en$easse, en$jsonrpc, $modal) {
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
					
				if ( e.train && e.segment  ) {
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
					
				case "BLOCKED": {
						var s = track[ e.segment ];
						if ( angular.isDefined(s) )
							s.symbol = e.blocked ? "BLOCKED" : "PLAIN";
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
					break;
				}
				
				trains.ep.getPassengersInStation(e.station).then( function(p) {
					stations.passengers[e.station] = p;
				});
				
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
					
					trains.ep.getPassengersInStation(station.name).then( function(p) {
						stations.passengers[station.name] = p;
					});
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
		
		var $ctrl = this;
		$rootScope.showCheckInDialog = function(station){
			$modal.open({
				templateUrl: 'main/htm/checkin.htm',
				controller: 'checkinCtrl',
				resolve: {
					endpoint: function(){
						return trains.ep;
					},
					station: function () {
						return station;
					},
					destinations: function(){
						var destinations = Object.keys($rootScope.stations.passengers);
						console.log(destinations+" "+station+" "+destinations.indexOf(station));
						destinations.splice(destinations.indexOf(station), 1);
						return destinations;
					}
				}
			});
		}
	});

	var mainProvider = function($scope) {
		$scope.assign = trains.ep.assign;
		$scope.rfid= function(segment,train) {
			console.log(segment + " " + train);
			trains.ep.setPosition( train, segment);

		}
	}
	
	angular.module('osgi.enroute.trains').controller('checkinCtrl', function ($scope, $modalInstance, endpoint, station, destinations) {
		
		  $scope.station = station;
		  $scope.destinations = destinations;
		  $scope.destination;
		  $scope.firstName;
		  $scope.lastName;
		  $scope.persons;
		  
		  
		  $scope.getFirstNames = function(firstName, lastName){
			  return endpoint.getFirstNames(firstName, lastName);
		  };

		  $scope.getLastNames = function(firstName, lastName){
			  return endpoint.getLastNames(firstName, lastName);
		  };
		  
		  $scope.checkin = function () {
			  endpoint.checkIn($scope.firstName, $scope.lastName, $scope.station, $scope.destination);
			  $modalInstance.dismiss('close');
		  };
	
		  $scope.cancel = function () {
			  $modalInstance.dismiss('cancel');
		  };
	});

})();
