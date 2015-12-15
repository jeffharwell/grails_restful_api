package apiservice

// This is from
// http://stackoverflow.com/questions/29584164/how-to-enable-cors-in-grails-3-0-1
//
// Basically we need to set a cors allow because the grunt server serving the Angular app
// is on a different port than the Grails server. ... It should be possible to deliver
// both with the same server, but I'm not sure it would be worth the trouble.

class CorsInterceptor {
    // We want this to match for all controllers
    CorsInterceptor() {
        matchAll()
    }

    boolean before() {
        println "In CorsInterceptor"
        /**
         * You cannot specify more than one allowed origin in the header, so instead
         * validate from a list and if the clients origin matches an allowed origin 
         * echo it back.
         *
         * http://stackoverflow.com/questions/1653308/access-control-allow-origin-multiple-origin-domains/12414239#12414239
         */
        def origin = request.getHeader("Origin")
        def allowed_origins = ["http://localhost:9000",
                               "http://127.0.0.1:9000",
                               "http://docker1dev.fuller.edu:9000",
                               "http://192.168.27.81:9000",
                               "https://docker1.fuller.edu",
                               "http://triple1.jeffharwell.com",
                               "https://triple1.jeffharwell.com"]
        println "Checking Origin ${origin}"    
        if (origin in allowed_origins) {
            println "Allowing origin ${origin}"
            header ("Access-Control-Allow-Origin", origin )
            header ("Access-Control-Allow-Credentials", "true")
            header ("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
            header ("Access-Control-Max-Age", "3600" )
        }
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

}
