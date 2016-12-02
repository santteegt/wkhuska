
wkhomeControllers.controller('map', ['$routeParams', '$scope', '$window', 'globalData', 'sparqlQuery', 'searchData',
    function ($routeParams, $scope, $window, globalData, sparqlQuery, searchData) {

        //if click in pie-chart
        $scope.ifClick = function (value)
        {
            searchData.genericData = value;
            $window.location.hash = "/" + $routeParams.lang + "/w/cloud?" + "datacloud";
        };
        $scope.themes = [];
        if (!searchData.allkeywords)
        {
            waitingDialog.show("Loading Research Areas");
            var queryKeywords = globalData.PREFIX
                    + ' CONSTRUCT { ?keyword rdfs:label ?key } '
                    + ' WHERE { '
                    + '     SELECT  (count(?pubs) as ?total) ' //(SAMPLE(?keyword) as ?keywordp) (SAMPLE(?key) as ?keyp)  '
                    + '     WHERE { '
                    + '         graph <'+globalData.centralGraph+'> {'
                    + '             ?subject foaf:publications ?pubs. '
                    //+ '           ?subject dct:subject ?key. '
                    + '             ?pubs bibo:Quote ?key. '
                    + '             BIND(REPLACE(?key, " ", "_", "i") AS ?unickey). '
                    + '             BIND(IRI(?unickey) as ?keyword) '
                    + '         }'
                    + '     } '
                    + '     GROUP BY ?keyword  ?key '
                    //+ '     GROUP BY ?subject'
                    
                    + '     HAVING(?total > 4) ' //si la keyword aparece en mas de 5 publicaciones
                    + '}';
            sparqlQuery.querySrv({query: queryKeywords}, function (rdf) {
                jsonld.compact(rdf, globalData.CONTEXT, function (err, compacted) {
                    _.map(compacted["@graph"], function (pub) {
                        var model = {};
                        model["id"] = pub["@id"];
                        model["tag"] = pub["rdfs:label"];
                        $scope.themes.push({tag: model["tag"]});
                    });
                    $scope.$apply(function () {
                        $scope.relatedtags = $scope.themes;
                        $scope.selectedTagItem = 'Semantic Web';
                        searchData.allkeywords = $scope.themes;
                    });
                    waitingDialog.hide();
                });
            });
        }
        else
        {
            $scope.relatedtags = searchData.allkeywords;
            $scope.selectedTagItem = 'Semantic Web';
        }



        //default selectedTagItem =  Semantic Web  - > see in app.js
        $scope.$watch('selectedTagItem', function () {
            //alert($scope.selectedItem);
            $scope.selectedTagItem = $scope.selectedTagItem ? $scope.selectedTagItem : "Semantic Web";
            waitingDialog.show("Consultando Ubicacion de Autores Relacionados con:  \"" + $scope.selectedTagItem + "\"");
            var queryBySource = globalData.PREFIX
                    + ' CONSTRUCT { '
                    + '         ?urikeyword bibo:Quote "' + $scope.selectedTagItem + '". '
                    + '         ?urikeyword uc:totalpublications ?cont. '
                    + '         ?urikeyword uc:name ?sourcename.  '
                    + '         ?urikeyword uc:lat ?lat. '
                    + '         ?urikeyword uc:long ?long. '
                    + '         ?urikeyword uc:province ?province. '
                    + '         ?urikeyword uc:city ?city. '
                    + '         ?urikeyword uc:fullname ?fullname. '
                    + ' } '
                    + 'WHERE {'
                    + '     SELECT (COUNT( DISTINCT ?object) as ?cont) ?provenance  ?urikeyword ?provenance ?sourcename ?lat ?long ?province ?city ?fullname '
                    + '     WHERE {'
                    + '         GRAPH <' + globalData.centralGraph + '>  {'
                    + '             ?subject foaf:publications ?object.'
                    //+ '             ?object bibo:Quote "' + $scope.selectedTagItem + '".'
                    //+ '             ?subject dct:subject ?key.'
                    + '             ?object bibo:Quote ?key.'
                    + '             FILTER (mm:fulltext-search(?key, "' + $scope.selectedTagItem + '")) .'
                    + '             ?subject dct:provenance ?provenance.'
                    + '             { '
                    + '                 SELECT DISTINCT ?sourcename ?lat ?long ?province ?city (STR(?name) as ?fullname) '
                    + '                 WHERE { '
                    + '                     GRAPH <' + globalData.endpointsGraph + '>  { '
                    + '                         ?provenance uc:name ?sourcename. '
                    + '                         ?provenance  uc:latitude ?lat. '
                    + '                         ?provenance uc:longitude ?long. '
                    + '                         ?provenance uc:province ?province. '
                    + '                         ?provenance uc:city ?city. '
                    + '                         ?provenance uc:fullName ?name.'
                    + '                         FILTER (lang(?name) = "es").'
                    + '                     } '
                    + '                 } '
                    + '             } '
                    + '             BIND(REPLACE("' + $scope.selectedTagItem + '"," ","_","i") + "_" + ?sourcename  as ?iduri). '
                    + '             BIND(IRI(?iduri) as ?urikeyword) '
                    + '         } '
                    + '     } '
                    + '     GROUP BY ?sourcename ?provenance ?lat ?long ?province ?city ?fullname  ?urikeyword '
                    + ' } ';
            $scope.publicationsBySource = [];
            sparqlQuery.querySrv({query: queryBySource},
            function (rdf) {
                jsonld.compact(rdf, globalData.CONTEXT, function (err, compacted) {
                    if (compacted["@graph"])
                    {
                        waitingDialog.hide();
                        var model = [];
                        _.map(compacted["@graph"], function (resource) {
                            var model = {};
                            model["id"] = resource["@id"];
                            model["name"] = resource["uc:name"];
                            model["fullname"] = resource["uc:fullname"];
                            model["total"] = resource["uc:totalpublications"]["@value"];
                            model["lat"] = resource["uc:lat"];
                            model["long"] = resource["uc:long"];
                            model["keyword"] = resource["bibo:Quote"];
                            model["city"] = resource["uc:city"];
                            model["province"] = resource["uc:province"];
                            if (model["id"])
                            {
                                $scope.publicationsBySource.push({id: model["id"], name: model["name"], fullname: model["fullname"], total: model["total"], latitude: model["lat"]
                                    , longitude: model["long"], city: model["city"], province: model["province"], keyword: model["keyword"]});
                            }
                        });
                        $scope.$apply(function () {
                            $scope.data = $scope.publicationsBySource;
                        });
                    }
                    else
                    {
                        alert("Informacion no encontrada");
                        waitingDialog.hide();

                    }
                });
            });
        });
    }]);