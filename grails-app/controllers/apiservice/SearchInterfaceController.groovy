package apiservice

import groovy.json.*

class SearchInterfaceController {
    static responseFormats = ['json']

    // Inject our key service
    def SearchInterfaceService

    def index() { }

    def recordResults() {
        println params
        def jsonObject = request.JSON
        def r = ["status":"success","message":"No JSON was passed"]
        if (request.JSON) {
            println jsonObject

            // Can be tested with curl like so
            //
            // curl -i -L -H "Content-Type: application/json" -X POST -d
            // '{"testdata":{"assignmentid":"99","hitid":"98","workerid":"97","query":{"nodes":"node structure
            // here","query_duration":"duration in seconds"}}}' triple2.jeffharwell.com/SearchInterface/recordResults

            // Want to store the user's query as a JSON string, so convert it back
            // hacky I know
            try {
                def userquery = new JsonBuilder( jsonObject.testdata.query ).toString()

                // Call the service and write in the results we received
                SearchInterfaceService.writeResults(jsonObject.testdata.assignmentid,
                                                    jsonObject.testdata.hitid,
                                                    jsonObject.testdata.workerid,
                                                    userquery)

                def whichsurvey = SearchInterfaceService.whichSurvey()
                r = ["status":"success","survey_number":"${whichsurvey}"]
            } catch (java.lang.NullPointerException e) {
                r = ["status":"error","message":"JSON Format was Invalid"]
            }
        }
        respond r
    }

}
