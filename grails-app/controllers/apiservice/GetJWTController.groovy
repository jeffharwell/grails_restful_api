package apiservice

import JWTUtilities

/*
 * Nothing fancy here. This controller leverages the work of the CASToJWTIntercetor
 * and returns a valid JWT to the Angular App if the interceptor was successfully able to talk
 * with CAS and get the username.
 */

class GetJWTController {
    static responseFormats = ['json']

    // Inject our key service
    def JWTKeyService

    def index() {
        println("In GetJWT index")
        if (request.authenticated) {
            //def jwt_key = grailsApplication.config.getProperty('jwt_key')
            def jwt_key = JWTKeyService.getKey()
            String expiration_ms = grailsApplication.config.getProperty('jwt_expiration_ms')
            println("Key is: ${jwt_key}")
            println("Expiration is: ${expiration_ms}")

            def token = JWTUtilities.createJWT(jwt_key, expiration_ms, request.username)
            def rd = [status:'success',jwttoken:token]
            respond(rd)
        } else {
            print("Authentication error, redirect to auth error page")
            redirect(controller: 'authError', action: 'index')
            return false
        }
    }
}
