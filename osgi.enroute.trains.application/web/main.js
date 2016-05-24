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
		rfids: {},
		ep : null,
		destinations : []
	};

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

		resolveBefore.trainsEndpoint().then(function(ep) {
			trains.ep = ep;
			trains.ep.getPositions().then( function(pos) {
				angular.copy(pos, track);
				for ( var i in track ) {
					var s = track[i];
					if ( s.segment.type == 'LOCATOR' ) {
						trains.destinations.push( i );
					}
				}
				trains.destinations.sort();
			});
			
			trains.ep.getTrains().then( function(t) {
				t.forEach( function(d) {
					trains.rfids[d] = { train: d };
				});
			});
		});

		$rootScope.closeAlert = function(index) {
			alerts.splice(index, 1);
		};
		$rootScope.page = function() {
			return $location.path();
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
