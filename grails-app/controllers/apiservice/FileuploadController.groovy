package apiservice

import io.jsonwebtoken.Jwts
import static io.jsonwebtoken.SignatureAlgorithm.HS256

import groovy.json.*
import java.util.UUID
import FileProcessor
import JWTUtilities


class FileuploadController {
    static responseFormats = ['json']

    // Inject our key service
    def JWTKeyService

    // Inject our service
    // see ../services/apiservice/CometService.groovy
    //def cometService

    def index() {
        println params
        def uploadedFile = request.getFile(params.filedata)

        cometService.run()
        respond([status: 200])
    }

    def upload() {
        // request.authenticated was reset to false by the CASToJWTInterceptor
        // before the authentication code executed, so it cannot be set manualy
        // by the user when they make the request.

        println("\n\n-----------\n")
        if (!request.authenticated) {
            print("Authentication error, redirect to auth error page")
            redirect(controller: 'authError', action: 'index')
            return false
        } else if (params.file && request.authenticated) {
            println("User has authenticated successfully with token ${request.jwtoken_from_user}");
            def jwt_key = JWTKeyService.getKey()
            def expiration_ms = grailsApplication.config.getProperty('jwt_expiration_ms')

            if (params.file != "undefined") {
                def filenames = params.file.getName()
                println "Filename ${filenames} has been uploaded"
                def uploadedFile = params.file.getInputStream()
        
                // Start up a thread to process the file, this will return
                // immediately
                def rabbitmq_host = grailsApplication.config.getProperty('rabbitmq_host')
                //def jwt_key = grailsApplication.config.getProperty('jwt_key')
                String queue_name = UUID.randomUUID().toString()

                // Get our Banner connection information from our config
                def server = grailsApplication.config.getProperty('external.banner_server')
                def database = grailsApplication.config.getProperty('external.banner_sid')
                def username = grailsApplication.config.getProperty('external.banner_user')
                def password = grailsApplication.config.getProperty('external.banner_password')


                def t = Thread.start {
                    //FileProcessor.processFile(uploadedFile, rabbitmq_host, queue_name, server, database, username, password)
                    PwbemplUploader.upload(uploadedFile, rabbitmq_host, queue_name, server, database, username, password)

                }

                // Send back the queue name, so that the app knows where
                // to listen
                println("In Fileupload/upload, creating token with queue name ${queue_name} to expire in ${expiration_ms}")
                //expiration_ms = "-10000";
                def token = JWTUtilities.createJWTWithQueueName(jwt_key, expiration_ms, request.username, queue_name)
                println("Passing new token to user: ${token}")
                println("\n---------------\n")
                def rd = [status:'success',jwttoken:token]
                respond(rd)
            } else {
                def token = JWTUtilities.createJWT(jwt_key, expiration_ms, request.username)
                println "No file was uploaded (the box was blank)"
                def rd = [status:'error',message:'No file was selected',jwttoken:token]
                respond(rd)
            }
        }
    }
}
