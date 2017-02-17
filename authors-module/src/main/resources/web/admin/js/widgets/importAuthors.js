/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//Module
var myApp = angular.module('myApp', ['ngResource']);

//Directive
myApp.directive('fileModel', ['$parse', function ($parse) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                element.bind('change', function () {
                    $parse(attrs.fileModel).assign(scope, element[0].files)
                    scope.$apply();
                });
            }
        };
    }]);

myApp.service('fileUpload', ['$http', function ($http) {
        this.uploadFileToUrl = function (file, selectedEndpoint, uploadUrl) {
            var fd = new FormData();
            //file["0"].append('endpoint', selectedEndpoint.id) ;
            fd.append('endpointName', selectedEndpoint.name);
            fd.append('file', file["0"]);
            fd.append('endpoint', selectedEndpoint.id);
            $http.post(uploadUrl, fd, {
                transformRequest: angular.identity,
                headers: {'Content-Type': undefined, 'Accept': 'text/plain'}
            }).then(function successCallback(response) {
                // this callback will be called asynchronously
                // when the response is available
                alert("Success at uploading the file! " + response.data);
            }, function errorCallback(response) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
                alert("Error" + response.data);
            });

        }
    }]);

myApp.controller('myCtrl', ['$scope', 'fileUpload', function ($scope, fileUpload) {
        
        $scope.endpoints = [];
        loadEndpoints();
        
        $scope.uploadFile = function () {
            var file = $scope.myFile;
            if (file == null || file.lenght < 1) {
                alert("Select a file!");
            } else if ($scope.selectedEndpoint == null) {
                alert("Select an endpoint!");
            } else {
                console.log('file is ');
                console.dir(file);
                var uploadUrl = "/authors-module/upload";
                fileUpload.uploadFileToUrl(file, $scope.selectedEndpoint, uploadUrl);  
            }
        };
        
        function loadEndpoints() {
            $.getJSON( "http://redi.cedia.org.ec/authors-module/endpoint/list", function (data) {
                for (var e in data) {
                    $scope.endpoints.push({id: data[e].id, name: data[e].name});
                }
            });
            
        }
        

    }]);



        