# Realtime FCD Application with HERE Traffic API

## What is this?

This application is a real-time analytics use-case using HERE traffic API based on big data analytics component stack developed under Big Data Europe project. It fetches FCD data (Floating Car Data) continuously of a small area in Munich from HERE traffic API. A Flink Producer connects to HERE flow traffic API and sends the data into a Kafka topic. The data is consumed by a Flink job, aggregates data in given time window(default 5 minutes) and enriched using a function by calculating average speed then stores into Elasticsearch. A visualization based on Kibana is used to visualize the aggregated data in a map (average speed in road segments per time windows).
![Alt text](images/fcd-diag.png?raw=true "Architecture Diagram of Realtime FCD Application with HERE Traffic API")

This is application will be using following docker containers from big data europe project to create this pipeline,

bde2020/nginx-proxy-with-css
bde2020/flink-maven-template:1.4.0-hadoop2.7
bde2020/integrator-ui:1.8.13
bde2020/kafka:2.11-0.10.2.0
bde2020/flink-master:1.4.0-hadoop2.7
bde2020/flink-worker:1.4.0-hadoop2.7


### FlinkFcdEventProducer

This is a Java application and a flink job which fetches data from HERE traffic API in every one minute and match the TMC code with coordinates and insert the raw data in to Kafka topic. It will fetch data from following HERE traffic API,

https://traffic.cit.api.here.com/traffic/6.2/flow.json?app_id=***&app_code=***&bbox=***

For the bounding box it uses small area in Munich (48.160250,11.551678;48.159462,11.558652). It is fixed at the moment because the TMC codes retrieved with the response can only be converted to that specific area. This can be extended by adding more TMC conversions.

After processing the Json response from HERE traffic API the producer will serialize the data to following Avro schema in binary format to a topic called “fcd-messages” in Kafka.

    {"namespace": "org.tum.idp.fcd",
     "type": "record",
     "name": "FcdTaxiEvent",
     "fields": [
     	{"name": "mid", "type": "string"},
     	{"name": "timestamp", "type": "long"},
     	{"name": "lon", "type": "double"},
     	{"name": "lat", "type": "double"},
     	{"name": "speed", "type": "double"},
     	{"name": "tmc_code", "type": "int"}
     ]
    }

### FlinkFcdEventConsumer

This is Java application and a f flink job which aggregates events stored in Kafka in every 5 minutes by default and calculate the average speed of a TMC point coordinate and store the data in a index called “munich” in elasticsearch. “munich” index in elasticsearch has following shema.

     {
      "floating-cars" : {
    	"properties" : {
       	"tmc_code" : {"type": "integer"},
       	"location" : {"type": "geo_point"},
       	"speed" : {"type": "double"},
       	"timestamp" : {"type": "date",
                      	"format": "yyyy-MM-dd'T'HH:mm:ssZ"}
    	}
      }
    }

The time window is configurable, by default it is 5 minutes and you can change it with “--window” parameter.


## How to Run Realtime FCD Application With HERE Traffic API

 - Clone repo using git
 - Go to the root of the cloned directory and run
 **mvn install**
 - Add these entries to etc/hosts
**127.0.0.1 flink-master.tum-idp.local flink-worker.tum-idp.local demo.tum-idp.local kibana.tum-idp.local elasticsearch.tum-idp.local**
 - Edit the fcd-app.yml file and customize the following configs

	 Producer config
 
	As described earlier producer is the flink job which fetches data from HERE 		traffic API in every one minute and match the TMC code with coordinates and insert the raw data in to Kafka topic

	So it needs two parameters to invoke HERE rest API. When you register for the web services provided by HERE you will get two parameters App Id and App Code. Fill those as in below under producer config in “--app_id “ and “--app_code” parameters. And “--bbox” is another parameter that you can specify which will give the traffic data in the given coordinates in rectangular area. At the moment it is fixed because TMC code in to coordinates is hardcoded and you can change the bounded box area by adding more conversions from TMC to coordinates.
	
	**FLINK_APPLICATION_ARGS: "--app_id *** --app_code *** --bbox 48.160250,11.551678;48.159462,11.558652"**
	
	Consumer config

	As mentioned before consumer is another flink job which aggregates events stored in Kafka in every 5 minutes by default and calculate the average speed and store the data in elasticsearch. This time window is configurable, by default it is 5 minutes and you can change it with “--window” parameter.
	
	**FLINK_APPLICATION_ARGS: "--window 3"**

 - Run command docker-compose -f fcd-app.yml up
 This will start the docker containers of all the components. Go to flink-master.tum-idp.local URL in browser to view Flink management UI.
 ![Alt text](images/flink-rt.png?raw=true "Flink Management UI")
 - Go to Kibana UI (kibana.tum-idp.local) and index pattern must be defined so that Kibana can find and retrieve the data from Elasticsearch and index name is "munich" as in the path used to create the index.
 ![Alt text](images/kibana-config-ui.png?raw=true "Kibana Config UI")
 - Then go to “Discover” and define a date range to filter data.
![Alt text](images/kibana-table.png?raw=true "Kibana Search UI")
 - Then go to visualize tab and create a “Tile Map” visualization to view data in the map.
 Elasticsearch provides a web map server (WMS) that is used by default in Kibana. The default WMS doesn't allow to zoom beyond a certain level. So you cannot view road level map. A more detailed view can be achieved using other WMS like [Mundialis WMS](https://www.mundialis.de/en/ows-mundialis/). The default WMS can be changed in the 'Options' of the Tile Map visualization from “WMS compliant map server” setting set the following.
 
				WMS url: http://ows.mundialis.de/services/service 24
				WMS layers: TOPO-OSM-WMS
				WMS version: 1.3.0
				WMS format: image/png

![Alt text](images/kibana-rt-ui.png?raw=true "Kibana Visualize UI")
