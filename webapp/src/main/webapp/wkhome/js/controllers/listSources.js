/* 
 * Controller to load/list Authors Sources
 */

wkhomeControllers.controller('listSources', ['sparqlQuery', '$scope', 'globalData',
    function (sparqlQuery, $scope, globalData) {


        /*************************************************************/
        /*query to get the sources in memory */
        /*************************************************************/
        // loadAllKeyword();
        $scope.datasources = [];

        //only keywords that appear in more than 2 articles
        var queryKeywords = globalData.PREFIX
                + ' Construct { '
                + '?subject uc:name ?name. '
                + '?subject uc:fullName ?fullName. '
                + '?subject uc:city ?city. '
                + '?subject uc:province ?province. '
                + '} '
                + 'WHERE { '
                + 'graph <' + globalData.endpointsGraph + '> '
                + '{ '
                + '?subject uc:name ?name. '
                + '?subject uc:fullName ?fullName. '
                + '?subject uc:city ?city. '
                + '?subject uc:province ?province. '
                + 'FILTER (lang(?fullName) = "es").'
                + '}'
                + '} ';
        sparqlQuery.querySrv({query: queryKeywords}, function (rdf) {

            jsonld.compact(rdf, globalData.CONTEXT, function (err, compacted) {
                _.map(compacted["@graph"], function (pub) {
                    var model = {};
                    model["id"] = pub["@id"];
                    model["name"] = pub["uc:name"];
                    model["fullName"] = pub["uc:fullName"];
                    model["city"] = pub["uc:city"];
                    model["url"] = "http://www." + pub["uc:name"].toLowerCase() + ".edu.ec/";
                    $scope.datasources.push({name: model["name"], fullName: model["fullName"]["@value"], url: model["url"], city: model["city"]});
                });
                $scope.$apply(function () {
                    $scope.sources = $scope.datasources;
                });
                waitingDialog.hide();
            });
        });

        /***********************************/
        /***********************************/







    }]); //end controller

