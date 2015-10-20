'use strict';

var cloudGroup = angular.module('cloudGroup', []);
//	D3	Factory
cloudGroup.factory('d3', function () {
    return	d3;
});
cloudGroup.directive('cloudGroup', ["d3", 'sparqlQuery',
    function (d3, sparqlQuery) {

        function create(element, data) {
    var colors = {
        exchange: {
            NYSE: 'red',
            NASDAQ: 'orange',
            TSX: 'blue',
            'TSX-V': 'green'
        },
        volumeCategory: {
            Top: 'mediumorchid',
            Middle: 'cornflowerblue',
            Bottom: 'gold'
        },
        lastPriceCategory: {
            Top: 'aqua',
            Middle: 'chartreuse',
            Bottom: 'crimson'
        },
        standardDeviationCategory: {
            Top: 'slateblue',
            Middle: 'darkolivegreen',
            Bottom: 'orangered'
        },
        default: '#4CC1E9'
    };

    var radius = 50;
    var width = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    var height = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
    var fill = d3.scale.ordinal().range(['#FF00CC', '#FF00CC', '#00FF00', '#00FF00', '#FFFF00', '#FF0000', '#FF0000', '#FF0000', '#FF0000', '#7F0000']);
    var svg = d3.select("#chart").append("svg")
            .attr("width", width)
            .attr("height", height);

    data = getDataMapping(data, size);

    var padding = 5;
    var maxRadius = d3.max(_.pluck(data, 'radius'));

    var maximums = {
        volume: d3.max(_.pluck(data, 'volume')),
        lasPrice: d3.max(_.pluck(data, 'lastPrice')),
        standardDeviation: d3.max(_.pluck(data, 'standardDeviation'))
    };

    var getCenters = function (vname, size) {
        var centers, map;
        centers = _.uniq(_.pluck(data, vname)).map(function (d) {
            return {name: d, value: 1};
        });

        map = d3.layout.treemap().size(size).ratio(1 / 1);
        map.nodes({children: centers});

        return centers;
    };

    var nodes = svg.selectAll("circle")
            .data(data);

    nodes.enter().append("circle")
            .attr("class", "node")
            .attr("cx", function (d) {
                return d.x;
            })
            .attr("cy", function (d) {
                return d.x;
            })
            .attr("r", function (d) {
                return d.radius;
            })
            .style("fill", function (d, i) {
                return colors['default'];
            })
            .on("mouseover", function (d) {
                showPopover.call(this, d);
            })
            .on("mouseout", function (d) {
                removePopovers();
            });

    function getDataMapping(data, vname) {
        var max = d3.max(_.pluck(data, vname));

        for (var j = 0; j < data.length; j++) {
            data[j].radius = (vname != '') ? radius * (data[j][vname] / max) : 15;
            data[j].x = data[j].x ? data[j].x : Math.random() * width;
            data[j].y = data[j].y ? data[j].y : Math.random() * height;
            data[j].volumeCategory = getCategory('volume', data[j]);
            data[j].lastPriceCategory = getCategory('lastPrice', data[j]);
            data[j].standardDeviationCategory = getCategory('standardDeviation', data[j]);
        }

        return data;
    }

    function getCategory(type, d) {
        var max = d3.max(_.pluck(data, type));
        var val = d[type] / max;

        if (val > 0.4)
            return 'Top';
        else if (val > 0.1)
            return 'Middle';
        else
            return 'Bottom';
    }

//        $('#board').change(function() {
//          $('#chart').empty();
//
//          start(this.value);
//        });

    $('#group').change(function () {
        group = this.value;
        draw(group);
    });

    $('#size').change(function () {
        var val = this.value;
        var max = d3.max(_.pluck(data, val));

        d3.selectAll("circle")
                .data(getDataMapping(data, this.value))
                .transition()
                .attr('r', function (d, i) {
                    return val ? (radius * (data[i][val] / max)) : 15
                })
                .attr('cx', function (d) {
                    return d.x
                })
                .attr('cy', function (d) {
                    return d.y
                })
                .duration(1000);

        size = this.value;

        force.start();
    });

    $('#color').change(function () {
        color = this.value;
        changeColor(this.value);
    });


    function changeColor(val) {
        console.log(val);
        d3.selectAll("circle")
                .transition()
                .style('fill', function (d) {
                    return val ? colors[val][d[val]] : colors['default']
                })
                .duration(1000);

        $('.colors').empty();
        if (val) {
            for (var label in colors[val]) {
                $('.colors').append('<div class="col-xs-1 color-legend" style="background:' + colors[val][label] + ';">' + label + '</div>')
            }
        }
    }


    var force = d3.layout.force();

    changeColor(color);
    draw(group);

    function draw(varname) {
        var centers = getCenters(varname, [width, height]);
        force.on("tick", tick(centers, varname));
        labels(centers)
        force.start();
    }

    function tick(centers, varname) {
        var foci = {};
        for (var i = 0; i < centers.length; i++) {
            foci[centers[i].name] = centers[i];
        }
        return function (e) {
            for (var i = 0; i < data.length; i++) {
                var o = data[i];
                var f = foci[o[varname]];
                o.y += ((f.y + (f.dy / 2)) - o.y) * e.alpha;
                o.x += ((f.x + (f.dx / 2)) - o.x) * e.alpha;
            }
            nodes.each(collide(.11))
                    .attr("cx", function (d) {
                        return d.x;
                    })
                    .attr("cy", function (d) {
                        return d.y;
                    });
        }
    }

    function labels(centers) {
        svg.selectAll(".label").remove();

        svg.selectAll(".label")
                .data(centers).enter().append("text")
                .attr("class", "label")
                .attr("fill", "red")
                .text(function (d) {
                    return d.name
                })
                .attr("transform", function (d) {
                    return "translate(" + (d.x + (d.dx / 2)) + ", " + (d.y + 20) + ")";
                });
    }

    function removePopovers() {
        $('.popover').each(function () {
            $(this).remove();
        });
    }

    function showPopover(d) {
        $(this).popover({
            placement: 'auto top',
            container: 'body',
            trigger: 'manual',
            html: true,
            content: function () {
                return "Title: " + d.title + "<br />" +
                        //"Uri: " + d.title + "<br />" +
                        "Keyword: " + d.keyword + "<br />" +
                        "Abstract: " + d.abstract.substring(0,50) + "<br />" 
//                        "Country: " + d.country + "<br />" +
//                        "SIC Sector: " + d.sicSector + "<br />" +
//                        "Last: " + d.lastPrice + " (" + d.pricePercentChange + "%)<br />" +
//                        "Volume: " + d.volume + "<br />" +
//                        "Standard Deviation: " + d.standardDeviation
                            ;
            }
        });
        $(this).popover('show')
    }

    function collide(alpha) {
        var quadtree = d3.geom.quadtree(data);
        return function (d) {
            var r = d.radius + maxRadius + padding,
                    nx1 = d.x - r,
                    nx2 = d.x + r,
                    ny1 = d.y - r,
                    ny2 = d.y + r;
            quadtree.visit(function (quad, x1, y1, x2, y2) {
                if (quad.point && (quad.point !== d)) {
                    var x = d.x - quad.point.x,
                            y = d.y - quad.point.y,
                            l = Math.sqrt(x * x + y * y),
                            r = d.radius + quad.point.radius + padding;
                    if (l < r) {
                        l = (l - r) / l * alpha;
                        d.x -= x *= l;
                        d.y -= y *= l;
                        quad.point.x += x;
                        quad.point.y += y;
                    }
                }
                return x1 > nx2 || x2 < nx1 || y1 > ny2 || y2 < ny1;
            });
        };
    }
}
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
//        var chart, clear, click, collide, collisionPadding, connectEvents, data, force, gravity, hashchange, height, idValue, jitter, label, margin, maxRadius, minCollisionRadius, mouseout, mouseover, node, rScale, rValue, textValue, tick, transformData, update, updateActive, updateLabels, updateNodes, width;
//        var scope;
//        var attrs;
//        var ctrlFn;
//        width;// = 980;
//        height;// = 510;
//        data = [];
//        node = null;
//        label = null;
//        margin = {
//            top: 5,
//            //top: 1350,
//            right: 0,
//            bottom: 0,
//            left: 0
//        };
//        maxRadius = 65;
//        rScale = d3.scale.sqrt().range([0, maxRadius]);
//        rValue = function (d) {
//            return parseInt(d.value);
//        };
//        idValue = function (d) {
//            return d.label;
//        };
//        textValue = function (d) {
//            return d.label;
//        };
//        collisionPadding = 10;
//        minCollisionRadius = 12;
//        jitter = 0.1;  
//        transformData = function (rawData) {
//            rawData.forEach(function (d) {
//                d.value = parseInt(d.value);
//            });
//            return rawData;
//        };
//        tick = function (e) {
//            var dampenedAlpha;
//            dampenedAlpha = e.alpha * 0.1;
//            node.each(gravity(dampenedAlpha)).each(collide(jitter)).attr("transform", function (d) {
//                return "translate(" + d.x + "," + d.y + ")";
//            });
//            return label.style("left", function (d) {
//                /*if(d.label == "Amino Acid") { 
//                 d = d;
//                 }*/      //POSITION OF LABEL
//                return (17 + (margin.left + d.x) - d.dx / 2) + "px";
//            }).style("top", function (d) {
//                return (50 + (margin.top + d.y) - d.dy / 2) + "px";
//            });
//        };
//
//        //main method to plot 
//        chart = function (selection) {
//            return selection.each(function (rawData) {
//                var maxDomainValue, svg, svgEnter;
//                data = transformData(rawData);
//                maxDomainValue = d3.max(data, function (d) {
//                    return rValue(d);
//                });
//                rScale.domain([0, maxDomainValue]);
//                svg = d3.select(this).selectAll("svg").data([data]);
//                svgEnter = svg.enter().append("svg");
//                svg.attr("width", width + margin.left + margin.right);
//                svg.attr("height", height + margin.top + margin.bottom);
//                node = svgEnter.append("g").attr("id", "bubble-nodes").attr("transform", "translate(" + margin.left + "," + margin.top + ")");
//                node.append("rect").attr("id", "bubble-background").attr("width", width).attr("height", height).on("click", clear);
//                label = d3.select(this).selectAll("#bubble-labels").data([data]).enter().append("div").attr("id", "bubble-labels");
//                update();
//                /*hashchange();
//                 return d3.select(window).on("hashchange", hashchange);*/
//                return d3.select(window);
//            });
//        };
//        update = function () {
//            data.forEach(function (d, i) {
//                return d.forceR = Math.max(minCollisionRadius, rScale(rValue(d)));
//            });
//            force.nodes(data).start();
//            updateNodes();
//            return updateLabels();
//        };
//        updateNodes = function () {
//            node = node.selectAll(".bubble-node").data(data, function (d) {
//                return idValue(d);
//            });
//            node.exit().remove();
//            return node.enter().append("a").attr("class", "bubble-node").attr("xlink:href", function (d) {
//                return "#" + (encodeURIComponent(idValue(d)));
//            }).call(force.drag).call(connectEvents).append("circle").attr("r", function (d) {
//                return rScale(rValue(d));
//            });
//        };
//        updateLabels = function () {
//            var labelEnter;
//            label = label.selectAll(".bubble-label").data(data, function (d) {
//                return idValue(d);
//            });
//            label.exit().remove();
//            labelEnter = label.enter().append("a").attr("class", "bubble-label").attr("href", function (d) {
//                return "#" + (encodeURIComponent(idValue(d)));
//            }).call(force.drag).call(connectEvents);
//            labelEnter.append("div").attr("class", "bubble-label-name").text(function (d) {
//                return textValue(d);
//            });
//            labelEnter.append("div").attr("class", "bubble-label-value").text(function (d) {
//                return rValue(d);
//            });
//            label.style("font-size", function (d) {
//                return Math.max(8, rScale(rValue(d) / 5)) + "px";
//            }).style("width", function (d) {
//                return 0.1 * rScale(rValue(d)) + "px";
//            });
//            label.append("span").text(function (d) {
//                return textValue(d);
//            }).each(function (d) {
//                return d.dx = Math.max(2.5 * rScale(rValue(d)), this.getBoundingClientRect().width);
//            }).remove();
//            label.style("width", function (d) {
//                return d.dx + "px";
//            });
//            return label.each(function (d) {
//                return d.dy = this.getBoundingClientRect().height;
//            });
//        };
//        gravity = function (alpha) {
//            var ax, ay, cx, cy;
//            cx = width / 2;
//            cy = height / 2;
//            ax = alpha / 8;
//            ay = alpha;
//            return function (d) {
//                d.x += (cx - d.x) * ax;
//                return d.y += (cy - d.y) * ay;
//            };
//        };
//        collide = function (jitter) {
//            return function (d) {
//                return data.forEach(function (d2) {
//                    var distance, minDistance, moveX, moveY, x, y;
//                    if (d !== d2) {
//                        x = d.x - d2.x;
//                        y = d.y - d2.y;
//                        distance = Math.sqrt(x * x + y * y);
//                        minDistance = d.forceR + d2.forceR + collisionPadding;
//                        if (distance < minDistance) {
//                            distance = (distance - minDistance) / distance * jitter;
//                            moveX = x * distance;
//                            moveY = y * distance;
//                            d.x -= moveX;
//                            d.y -= moveY;
//                            d2.x += moveX;
//                            return d2.y += moveY;
//                        }
//                    }
//                });
//            };
//        };
//        connectEvents = function (d) {
//            d.on("click", click);
//            d.on("mouseover", mouseover);
//            return d.on("mouseout", mouseout);
//        };
//        clear = function () {
//            return location.replace("#");
//        };
//        click = function (d) {
//            //   location.replace("#" + encodeURIComponent(idValue(d)));
//
//
//            //adding information about publications of THIS keyword into "tree-node-info"   DIV
//            var infoBar = $('div.tree-node-info');
//            var model = {"dcterms:title": {label: "Title", containerType: "div"},
//                "bibo:uri": {label: "URL", containerType: "a"},
//                "dcterms:contributor": {label: "Contributor", containerType: "a"},
//                "dcterms:isPartOf": {label: "Is Part Of", containerType: "a"},
//                "dcterms:license": {label: "License", containerType: "a"},
//                "dcterms:provenance": {label: "Source", containerType: "div"},
//                "dcterms:publisher": {label: "Publisher", containerType: "div"},
//                "bibo:numPages": {label: "Pages", containerType: "div"}
//            };
//            if (infoBar) {
//                var keyword = d.label;
//                //var sparqlDescribe = "DESCRIBE <" + id + ">";
//                var sparqlPublications = 'PREFIX dct: <http://purl.org/dc/terms/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> '
//                        + ' PREFIX bibo: <http://purl.org/ontology/bibo/> '
//                        + ' PREFIX uc: <http://ucuenca.edu.ec/wkhuska/resource/> '
//                        + " CONSTRUCT { ?keyword uc:publication ?publicationUri. "
//                        + " ?publicationUri dct:title ?title. "
//                        + " ?publicationUri bibo:abstract ?abstract "   
//                        + " } "
//                        + " WHERE {"
//                        + " GRAPH <http://ucuenca.edu.ec/wkhuska>"
//                        + " {"
//                        + " ?publicationUri dct:title ?title . "
//                        + " ?publicationUri bibo:abstract  ?abstract. "
//                        + " ?publicationUri bibo:Quote \"" + keyword + "\" ."
//                        + "  BIND(REPLACE( \"" + keyword + "\", \" \", \"_\", \"i\") AS ?key) ."
//                        + "  BIND(IRI(?key) as ?keyword)"
//                        + " }"
//                        + "}";
//
//                sparqlQuery.querySrv({query: sparqlPublications}, function (rdf) {
//                    var context = {
//                        "foaf": "http://xmlns.com/foaf/0.1/",
//                        "dc": "http://purl.org/dc/elements/1.1/",
//                        "dcterms": "http://purl.org/dc/terms/",
//                        "bibo": "http://purl.org/ontology/bibo/",
//                        "uc": "http://ucuenca.edu.ec/wkhuska/resource"
//                    };
//                    jsonld.compact(rdf, context, function (err, compacted) {
//                        //var entity = _.findWhere(compacted["@graph"], {"@id": id, "@type": "bibo:Document"});
//                        var entity = compacted["@graph"];
//
//                        infoBar.find('h4').text("Publication Info");
////                        infoBar.find('div#title').text("Title: " + entity["dcterms:title"]);
////                        infoBar.find('a').attr('href', "http://190.15.141.85:8080/marmottatest/meta/text/html?uri=" + entity["@id"])
////                                .text("More Info...");
//
//
//
//                      //  var pubInfo = $('div.tree-node-info .entityInfo');
//                      //  pubInfo.html('');
//                        var values = entity.length ? entity : [entity];
//
//
////                        _.map(values, function (value) {
////
////                            var div = $('<div>');
////                            var lblTitle = $('<span class="label label-primary">').text(model["dcterms:title"].label);
////                            div.append(lblTitle);
////                            div.append("</br>");
////                            pubInfo.append(div);
////                            var span = $('<span class="field-value"> ng-model="titulo"').text(value["dcterms:title"]);
////                            div.append(span);
////                            div.append("</br>");
////                            //adding url
////                            var div = $('<div>');
////                            var lblUrl = $('<span class="label label-primary">').text(model["bibo:uri"].label)
////                            div.append(lblUrl);
////                            div.append("</br>");
////                            pubInfo.append(div);
//////                            var spanId = $('<span class="field-value">').text(value["@id"]);
//////                            div.append(spanId);
//////                            div.append("</br>");
////                            var anchor = $("<a target='blank'>").attr('href', value["@id"].replace('/xr/', '/')/*SOLO DBLP*/).text(value["@id"]);
////                            div.append(anchor);
////                            div.append("</br>");
////
////                        });
//                        //send data to getKeywordTag Controller
//                        scope.ctrlFn({value: values});
//
//                    });
//                });
//            }
//            return d3.event.preventDefault();
//        };
//        hashchange = function () {
//            var id;
//            id = decodeURIComponent(location.hash.substring(1)).trim();
//            return updateActive(id);
//        };
//        updateActive = function (id) {
//            node.classed("bubble-selected", function (d) {
//                return id === idValue(d);
//            });
//            if (id.length > 0) {
//                return d3.select("#status").html("<h3>The word <span class=\"active\">" + id + "</span> is now active</h3>");
//            } else {
//                return d3.select("#status").html("<h3>No word is active</h3>");
//            }
//        };
//        mouseover = function (d) {
//            return node.classed("bubble-hover", function (p) {
//                return p === d;
//            });
//        };
//        mouseout = function (d) {
//            return node.classed("bubble-hover", false);
//        };
//        chart.jitter = function (_) {
//            if (!arguments.length) {
//                return jitter;
//            }
//            jitter = _;
//            force.start();
//            return chart;
//        };
//        chart.height = function (_) {
//            if (!arguments.length) {
//                return height;
//            }
//            height = _;
//            return chart;
//        };
//        chart.width = function (_) {
//            if (!arguments.length) {
//                return width;
//            }
//            width = _;
//            return chart;
//        };
//        chart.r = function (_) {
//            if (!arguments.length) {
//                return rValue;
//            }
//            rValue = _;
//            return chart;
//        };
//
//        var draw = function draw(element, widthEl, heightEl, data, scopeEl, attrsEl, ctrlFnEl) {
//            width = widthEl;
//            height = heightEl;
//            scope = scopeEl;
//            attrs = attrsEl;
//            ctrlFn = ctrlFnEl;
//            force = d3.layout.force().gravity(0).charge(0).size([width, height]).on("tick", tick);
//            element.datum(data).call(chart);
//
//        }

        return {
            restrict: 'E',
            scope: {
               
                data: '='
            },
            compile: function (element, attrs, transclude) {
                //	Create	a	SVG	root	element
                /*var	svg	=	d3.select(element[0]).append('svg');
                 svg.append('g').attr('class', 'data');
                 svg.append('g').attr('class', 'x-axis axis');
                 svg.append('g').attr('class', 'y-axis axis');*/
                //	Define	the	dimensions	for	the	chart
                //var width = 960, height = 500;
                var elementWidth = parseInt(element.css('width'));
                var elementHeight = parseInt(element.css('height'));
                var width = attrs.ctWidth ? attrs.ctWidth : elementWidth;
                var height = attrs.ctHeight ? attrs.ctHeight : elementHeight;


                var svg = d3.select(element[0]);

                //	Return	the	link	function
                return	function (scope, element, attrs, ctrlFn) {
                    //	Watch	the	data	attribute	of	the	scope
                    /*scope.$watch('$parent.logs', function(newVal, oldVal, scope) {
                     //	Update	the	chart
                     var data = scope.$parent.logs.map(function(d) {
                     return {
                     x: d.time,
                     y: d.visitors
                     }
                     
                     });
                     
                     draw(svg, width, height, data);
                     },	true);*/
                    scope.$watch('data', function (newVal, oldVal, scope) {
                        //	Update	the	chart
                        var data = scope.data;
                        if (data) {
                            var jsonld = data.data;
                            var schema = data.schema;
                            var fields = schema.fields;
                            var mappedData = [];
//                            _.each(jsonld['@graph'], function (keyword, idx) {
//                                mappedData.push({label: keyword[fields[0]], value: keyword[fields[1]]["@value"]});
//                            });
                            create(svg, data);
                        }
                    }, true);

                };
            }
        };
    }]);
