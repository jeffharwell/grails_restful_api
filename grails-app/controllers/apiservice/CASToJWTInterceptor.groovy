package apiservice

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureException
import io.jsonwebtoken.ExpiredJwtException
import static io.jsonwebtoken.SignatureAlgorithm.HS256

import groovy.json.*
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovy.xml.*

import JWTUtilities

/*
 * This Interceptor is fired in front of every controller except the authError controller
 * It's job is to make sure the request is authenticated and authorized.
 *
 * The interceptor will first look to see if a JSON Web Ticket (JWT) is embedded in the request,
 * it looks in the jwttoken key. If the key exists it will try to validate the token. If the
 * token validates it will load the username into the request data (request.username = user) 
 * set request.authenticated = true. The code also sets request.jwtclaims to be the value of the
 * the claims HashMap (really a Claims object) in the JWT so that the controller down the line
 * can access and process the claims appropriately.
 *
 * If there is no token, or the token doesn't validate the code will look for the presence of 
 * ticket = and service = in the request params. If those two exist it will attempt to contact
 * the CAS server and validate the ticket. If the ticket validates it will use the username
 * from the CAS response to set request.username and also set request.authenticated.
 *
 * The interceptor does not create a new JWT, that is the responsibility of the 
 *
 * If neither authentication method works the code will redirect to the index method of the 
 * AuthError controller and then halt executing of the action. The AuthError controller simply
 * returns a JSON string ['error':'authentication failed'] for the Angular app to deal with 
 * appropriately, presumably by sending the user back to CAS for another go-round.
 */

class CASToJWTInterceptor {

    // Inject our key service
    def JWTKeyService

    // We want this to match for all controllers
    CASToJWTInterceptor() {
        //See https://grails.github.io/grails-doc/latest/guide/single.html#interceptorMatching
        // The matcher expects lower case >:| ... at least for the first letter >:(
        matchAll().except( controller: "authError" ).except( controller: "apitestone", action: "testEnv" ).except( controller: "authForbidden" )
        //matchAll().except( controller: "authError" )
        //match( controller: "authError" )
        //match( controller: "apitestone" )
    }

    /*
    boolean before() {
        request.authenticated = true
        request.username = 'jharwell'
        request.jwtclaims = [:] // no claims
        true
    }
    */

    boolean before() { 
        /*
         * First check for the presence of a Java Web Token (JWT). If one exists
         * then validate it. If it doesn't exist then check of the existence of
         * a CAS Ticket and Service. If those exist validate them and create a new
         * JWT
         */
        request.authenticated = false
        request.forbidden = false

        //def key = grailsApplication.config.getProperty('jwt_key')
        def key = JWTKeyService.getKey()
        def valid_user = grailsApplication.config.getProperty('valid_user')

        // This is wrong, wrong, all wrong ... FIX ME PLEASE!!!
        // this should pull out of a role in the Fuller ID
        def valid_users = ['bob', 'joe', 'fred', 'george']
        def ticket_expiration_ms = grailsApplication.config.getProperty('jwt_expiration_ms')


        /*
         * Check for a JSON Web Ticket in the Request body (body??)
         */
        println("\n\n---------")
        println("In CASToJWT Interceptor")
        println("JSON is: ${request.JSON}")
        println("Raw jwttoken in params is: ${params.jwttoken}")
        if ((request.JSON && request.JSON.jwttoken) || params.jwttoken) {
            println("JSON has jwttoken")
            def jwttoken = request.JSON.jwttoken
            if (params.jwttoken) {
                jwttoken = params.jwttoken
            }
            request.jwtoken_from_user = jwttoken // stash it away for reference later
            try {
                def jwtclaims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwttoken).getBody()
                def user = jwtclaims.getSubject()
                println("Settings claims as ${jwtclaims}")
                request.jwtclaims = jwtclaims
                println("Got User: ${user}")
                request.authenticated = true
                request.username = user
            } catch (SignatureException e) {
                println("JWTToken failed validation with error: ${e}")
            } catch (ExpiredJwtException e) {
                println("JWTToken expired: ${e}")
            }
        } else {
            println("No JWTToken Found")
        }

        /*
         * No Valid JWT was found. Look for a ticket and service to authenticate using
         * CAS
         */
        if (!request.authenticated && params.ticket && params.service) {
            println("Has a ticket: ${params.ticket}")
            println("Has a service: ${params.service}")
            //def auth_response = getUserFromTicket(params.ticket, params.service)
            def auth_response = getUserFromTicket(params.ticket, params.service)
            if (auth_response.authenticated && auth_response.user in valid_users) {
                request.authenticated = true
                request.username = auth_response.user
                request.jwtclaims = [:] // no claims
            } else {
                request.authenticated = false
                if (auth_response.authenticated && !(auth_response.user in valid_users)) {
                    println("User is forbidden")
                    request.forbidden = true
                } else {
                    println("Failed to authenticate ticket")
                }
            }
        } else {
            println("No Ticket and Service!")
        }

        // If authentication fails we should probably just bail out so that we don't have to
        // code the failure condition into the controllers 50 times ... not very DRY
        if (!request.authenticated) {
            if (request.forbidden) {
                print("Authentication forbidden, redirect to auth forbidden page")
                redirect(controller: 'authForbidden', action: 'index')
            } else {
                print("Authentication error, redirect to auth error page")
                redirect(controller: 'authError', action: 'index')
            }
            return false
        } else {
            return true
        }
        println("------------\n\n")
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    HashMap<String,Object> getUserFromTicket(String ticket, String service) {
        /*
         * Actually talks to CAS and does the authentication. Returns the Username
         */
        def r = [authenticated:false, user:'none']

        def http = new HTTPBuilder( 'https://login.fuller.edu:8443' )
        http.request( GET, XML) {
            uri.path = '/cas/serviceValidate'
            uri.query = [service:service, ticket:ticket]
            //uri.query = [service:service, ticket:"badticket"]
            response.success = { resp, xml ->
                //println resp.statusLine
                //println "XML is ${XmlUtil.serialize(xml)}"
                def failure = xml.authenticationFailure.text().trim()
                def success = xml.authenticationSuccess.user.text().trim()
                if (failure != "") {
                    println ("Invalid ticket or other login failure: ${failure}")
                } else if (success != "") {
                    println ("Successfull Login for user: ${success}")
                    r.authenticated = true
                    r.user = success
                }
            }
        }
        return r
    }
}
