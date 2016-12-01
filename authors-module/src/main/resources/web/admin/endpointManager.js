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
/**
 * Created by . User: Thomas Kurz Date: 18.02.11 Time: 18:46 To change this
 * template use File | Settings | File Templates.
 */
(function ($) {

    var contentBox;

    var programs;
    var buttonset;

    var loadS;

    // new program
    var name;
    var kind;
    var prefix;
    var endpoint;
    var mime;
    var expiry;

    var samples;
    var samp;

    var loader;

    $.fn.endpointsManager = function (options) {
        var settings = {
            host: 'http://localhost:8080/LMF/'
        }

        function addProgram() {

            if (name.val() == '') {
                alert("name may not be empty!");
                return;
            }
            if (prefix.val() == '') {
                alert("prefix may not be empty!");
                return;
            }
            /*if (endpoint.val() == '') {
             alert("endpoint may not be empty!");
             return;
             }
             if (mime.val() == '') {
             alert("mimetype may not be empty!");
             return;
             }*/
            if (expiry.val() == '') {
                alert("expiry may not be empty!");
                return;
            }
            var url = settings.host + "cache/endpoint?name=" + encodeURIComponent(name.val()) + '&prefix='
                    + encodeURIComponent(prefix.val()) + '&endpoint=' + encodeURIComponent(endpoint.val()) + '&mimetype='
                    + encodeURIComponent(mime.val()) + '&kind=' + encodeURIComponent(kind.val()) + '&expiry='
                    + encodeURIComponent(expiry.val());
            loader.html("<span>Save Values</span>");
            // upload
            $.ajax({
                type: "POST",
                url: url,
                contentType: "text/plain",
                success: function () {
                    loader.html("");
                    $.getJSON(settings.host + "cache/endpoint/list", function (data) {
                        writePrograms(data);
                    });
                    alert("success");
                },
                error: function (jXHR) {
                    loader.html("");
                    alert("Error: " + jXHR.responseText);
                }
            });
        }

        function removeProgram(b) {
            var url = settings.host + "authors-module/endpoint/delete?id=" + b.attr('data-id');
            $.ajax({
                type: "DELETE",
                url: url,
                success: function () {
                    $.getJSON(settings.host + "authors-module/endpoint/list", function (data) {
                        writePrograms(data);
                    });
                    alert("success");
                },
                error: function (jXHR, textStatus) {
                    alert("Error: " + jXHR.responseText);
                }
            });
        }
        function activateProgram(b, active) {
            var url = settings.host + "authors-module/endpoint/updatestatus?id=" + b.attr('data-id') + "&oldstatus=" + b.attr('data-oldstatus') + "&newstatus=" + b.attr('data-newstatus');
            $.ajax({
                type: "POST",
                url: url,
                success: function () {
                    $.getJSON(settings.host + "authors-module/endpoint/list", function (data) {
                        writePrograms(data);
                    });
                    alert("success");
                },
                error: function (jXHR, textStatus) {
                    alert("Error: " + jXHR.responseText);
                }
            });
        }

        function writeSample(data) {
            // contains method
            var contains = function (array, obj) {
                var i = array.length;
                while (i--) {
                    if (array[i] == obj) {
                        return true;
                    }
                }
                return false;
            }
            var sorted = [];
            for (property in data) {
                property = property.substring(15);
                property = property.substring(0, property.indexOf('.'));
                if (!contains(sorted, property)) {
                    sorted.push(property);
                }
            }
            sorted.sort();
            samples = data;
            samp = $('<select/>').change(function () {
                loadSample(this);
            });
            var d = $("<div></div>");
            loadS.append(d);
            d.append('<br>Load Sample: ');
            d.append(samp);
            samp.append('<option>---select---</option>');
            for (property in sorted) {
                samp.append('<option>' + sorted[property] + '</option>');
            }
        }

        function loadSample() {
            if (samp.val() != '---select---') {
                var x = samp.val();
                name.val(samples['ldcache.sample.' + x + '.name'].value);
                kind.val(samples['ldcache.sample.' + x + '.kind'].value);
                prefix.val(samples['ldcache.sample.' + x + '.prefix'].value);
                endpoint.val(samples['ldcache.sample.' + x + '.endpoint'].value);
                mime.val(samples['ldcache.sample.' + x + '.mimetype'].value);
                expiry.val(samples['ldcache.sample.' + x + '.expiry'].value);
            } else {
                var x = samp.val();
                name.val('');
                kind.val('');
                prefix.val('');
                endpoint.val('');
                mime.val('');
                expiry.val('');
            }
        }

        function writePrograms(ps) {
            if (ps.length == 0) {
                programs.text("no endpoints defined");
                return;
            }
            var table = $("<table class='simple_table'/>");
            var tr = $("<tr class='title' valign='top' style='font-weight:bold;color: white;background-color:gray;'/>");
            //tr.append($("<td/>").html("&nbsp;"));
            tr.append($("<td/>").text("Enable"));
            tr.append($("<td/>").text("Name"));
            tr.append($("<td/>").text("Endpoint URL"));
            tr.append($("<td/>").text("Graph URI"));
            tr.append($("<td/>").text("Full Name"));
            tr.append($("<td/>").text("City"));
            tr.append($("<td/>").text("Province"));
            tr.append($("<td/>").text("Latitude"));
            tr.append($("<td/>").text("Longitude"));
            //tr.append($("<td/>").text("Endpoint"));
            //tr.append($("<td/>").text("Mimetype"));
            //tr.append($("<td/>").text("Expiry"));
            tr.append($("<td/>").text("Actions"));
            table.append(tr);

            function buildIcon(enabled) {
                var icon = $("<span>", {'class': "endpoint_status"});
                if (enabled)
                    icon.addClass("on");
                return icon;
            }

            function ctToString(obj) {
                var ret = "";
                for (var i = 0; i < obj.length; i++) {
                    ret += obj[i].mime;
                    if (i != obj.length - 1)
                        ret += ",";
                }
                return ret;
            }

            for (var i = 0; i < ps.length; i++) {
                if (!ps[i].volatile) {
                    var delBtn = $("<button/>").text("delete");
                    delBtn.attr('data-id', ps[i].id);
                    delBtn.click(function () {
                        removeProgram($(this));
                    });
                    var modBtn = $("<button>", {text: (ps[i].status === 'true' ? "deactivate" : "activate"), 'data-id': ps[i].id, 'data-oldstatus': (ps[i].status === 'false' ? "false" : "true"), 'data-newstatus': (ps[i].status === 'false' ? "true" : "false")})
                    modBtn.click(function () {
                        activateProgram($(this));
                    });
                }
                var col = "";
                if (i % 2) {
                    col = "even";
                } else {
                    col = "odd";
                }
                var tr = $("<tr class='" + col + "'/>");
                $("<td/>").text(ps[i].status || '').appendTo(tr);
                $("<td/>").text(ps[i].name || '').appendTo(tr);
                $("<td/>").text(ps[i].url || '').appendTo(tr);
                $("<td/>").text(ps[i].graph || '').appendTo(tr);
                $("<td/>").text(ps[i].fullName || '').appendTo(tr);
                $("<td/>").text(ps[i].city || '').appendTo(tr);
                $("<td/>").text(ps[i].province || '').appendTo(tr);
                $("<td/>").text(ps[i].latitude || '').appendTo(tr);
                $("<td/>").text(ps[i].longitude || '').appendTo(tr);
                //$("<td/>").text(ps[i].endpoint || '').appendTo(tr);
                //$("<td/>").text(ps[i].mimetype != undefined ? ctToString(ps[i].mimetype) : '').appendTo(tr);
                //$("<td/>").text(ps[i].expiry || '').appendTo(tr);
                //   if(!ps[i].volatile) {

                $("<td/>").append(modBtn).append(delBtn).appendTo(tr);
                //$("<td/>").append(delBtn).appendTo(tr);

                //   } else {
                //       $("<td>autoregistered</td>").appendTo(tr);
                //   }
                table.append(tr);
            }
            programs.empty().append(table);
        }



        function writeContent() {
            // set Buttons
            var button = $("<button/>", {
                disabled: false
            }).text("Add Endpoint");

            programs = $("<div/>").text('loading');
            var newprogram = $("<table/>");
            buttonset = $("<div/>");
            button.click(function () {
                addEndpoint();
            });

            loader = $("<div/>");
            buttonset.append(button);
            buttonset.append(loader);

            loadS = $("<div style='float: right;margin-top: -60px;'></div>");

            //    contentBox.html("<h4 style='margin-bottom: 10px;'>Authors-Module Endpoints List</h4>");
            contentBox.append(buttonset);
            contentBox.append(programs);
            //    contentBox.append("<h4 style='margin-bottom: 10px;margin-top: 25px'>Add LD-Cache Endpoint</h4>");
            //    contentBox.append(loadS);
            //    contentBox.append(newprogram);

            //    $.getJSON(settings.host + "config/list?prefix=ldcache.sample", function(data) {
            //        writeSample(data);
            //    });
            $.getJSON(settings.host + "authors-module/endpoint/list", function (data) {
                writePrograms(data);
            });

            // add new program stuff

            //    name = $("<input style='width: 100%;' type='text' size='100'>");
            //    kind = $("<select>");
            //    $.getJSON(settings.host + "cache/provider/list", function(data) {
            //        kind.empty();
            //        for ( var d in data) {
            //            $("<option/>", {
            //                value : data[d],
            //                text : data[d]
            //            }).appendTo(kind);
            //        }
            //        $("<option/>", {
            //            value : "NONE",
            //            text : "NONE (Blacklist)"
            //        }).appendTo(kind);
            //    }).complete(function() {
            //        button.removeAttr("disabled");
            //    });
            prefix = $("<input style='width: 100%;' type='text' size='100'>");
            endpoint = $("<input style='width: 100%;' type='text' size='100'>");
            mime = $("<input style='width: 100%;' type='text' size='100'>");
            expiry = $("<input style='width: 100%;' type='text' size='100'>");

            newprogram.append("<tr class='title'><td>Name</td><td>Value</td></tr>")

            var tr1 = $("<tr></tr>");
            tr1.append("<td>Name</td>");
            var td1 = $("<td></td>").append(name);
            tr1.append(td1);
            newprogram.append(tr1);

            var tr2 = $("<tr></tr>");
            tr2.append("<td>Kind / Provider</td>");
            var td2 = $("<td></td>").append(kind);
            tr2.append(td2);
            newprogram.append(tr2);

            var tr3 = $("<tr></tr>");
            tr3.append("<td>Prefix</td>");
            var td3 = $("<td></td>").append(prefix);
            tr3.append(td3);
            newprogram.append(tr3);

            var tr4 = $("<tr></tr>");
            tr4.append("<td>Enpoint</td>");
            var td4 = $("<td></td>").append(endpoint);
            tr4.append(td4);
            newprogram.append(tr4);

            var tr5 = $("<tr></tr>");
            tr5.append("<td>Mimetype</td>");
            var td5 = $("<td></td>").append(mime);
            tr5.append(td5);
            newprogram.append(tr5);

            var tr6 = $("<tr></tr>");
            tr6.append("<td>Expiry Time</td>");
            var td6 = $("<td></td>").append(expiry);
            tr6.append(td6);
            newprogram.append(tr6);
        }


        function addEndpoint() {

            var name = document.getElementById('txtname').value;
            var endpoint = document.getElementById('txtendpoint').value;
            var graphuri = document.getElementById("txtgraphuri").value;
            var fullname = document.getElementById("txtfullname").value;
            var engname = document.getElementById("txtengname").value;
            var city = document.getElementById("txtcity").value;
            var province = document.getElementById("txtprovince").value;
            var latitude = document.getElementById("txtlatitude").value;
            var longitude = document.getElementById("txtlongitude").value;

            if (name == '') {
                alert("name may not be empty!");
                return;
            }
            if (endpoint == '') {
                alert("endpoint may not be empty!");
                return;
            }
            if (graphuri == '') {
                alert("graphuri may not be empty!");
                return;
            }
            if (fullname == '') {
                alert("fullname may not be empty!");
                return;
            } 
            if (engname == '') {
                alert("Name in English may not be empty!");
                return;
            }
            if (city == '') {
                alert("city may not be empty!");
                return;
            }
            if (province == '') {
                alert("province may not be empty!");
                return;
            }
            if (latitude == '') {
                alert("latitude may not be empty!");
                return;
            }
            if (longitude == '') {
                alert("longitude may not be empty!");
                return;
            }
            var dataT = {
                "State": "true",
                "Name": name,
                "Endpoint": endpoint,
                "GraphUri": graphuri,
                "FullName": fullname,
                "NameEnglish": engname,
                "City": city,
                "Province": province,
                "Latitude": latitude,
                "Longitude": longitude

            };

            $.ajax({
                type: "POST",
                data: JSON.stringify(dataT),
                dataType: "text", //result data type
                contentType: "application/json", // send data type
                url: settings.host + "authors-module/addendpoint",
                success: function (Result) {
                    $.getJSON(settings.host + "authors-module/endpoint/list", function (Data) {
                        writePrograms(Data);
                    });
                    alert("Success");
                },
                error: function (data) {
                    alert("Error" + data.responseText);
                }
            });
        }


        return this.each(function () {
            // merge options
            if (options) {
                $.extend(settings, options);
            }
            contentBox = $(this);
            // build skeleton

            writeContent();

            // $.getJSON(settings.host+"import/types",
            // function(data){write(data);});
        });
    };
})(jQuery);



function runGetAuthorsFromUTPL(host) {

    document.getElementById("imgloading").style.visibility = "visible";

    var endpoint = document.getElementById('txtendpointutpl').value;
    var graphuri = document.getElementById('txtgraphuriutpl').value;

    var settings = {
        host: host
    };


    $.ajax({
        type: "POST",
        dataType: "text", //result data type
        contentType: "application/json", // send data type
        url: settings.host + "authors-module/split?endpointuri="+endpoint+"&graphuri="+graphuri+"",
        //    url:  "http://localhost:8079/marmotta/authors-module/update",
        success: function (Result) {
            document.getElementById("imgloading").style.visibility = "hidden";
            alert("Correcto: " + Result);
        },
        error: function (data) {
            document.getElementById("imgloading").style.visibility = "hidden";
            alert("Error" + data.responseText);
        }
    });


}



