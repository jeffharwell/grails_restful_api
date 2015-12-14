package apiservice

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
//import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;

import JWTUtilities

class RabbitMQTestController {
    static responseFormats = ['json']

    // Inject our key service
    def JWTKeyService


    def index() {
        /*
         * The queue name should be in the JWT claims, which was parsed
         * by the CASToJWTInterceptor and placed in request.jwtclaims
         * try to get it out
         */
        def QUEUE_NAME = "none"
        println("\n\n-------------\n")
        println("In RabbitMQTestController/index()")
        println("jwtclaims ${request.jwtclaims}")
        if (request.jwtclaims && request.jwtclaims.queue_name) {
             if (request.jwtclaims.queue_name != "") {
                QUEUE_NAME = request.jwtclaims.queue_name
             }
        }

        /*
         * Make sure we have an authenticated session and a valid queue name
         */
        def rabbit_host = grailsApplication.config.getProperty('rabbitmq_host')
        //def jwt_key = grailsApplication.config.getProperty('jwt_key')
        def jwt_key = JWTKeyService.getKey()
        def expiration_ms = grailsApplication.config.getProperty('jwt_expiration_ms')
        def r = [status:'success', controller:'rabbitMQTest', has_messages:'false', 'messages':[]]

        if (request.authenticated && QUEUE_NAME != "none") {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost(rabbit_host)
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            def args = ["x-expires": 600000]
            channel.queueDeclare(QUEUE_NAME, false, false, false, args)

            GetResponse response = channel.basicGet(QUEUE_NAME, true)
            while (response) { 
                String resp_body = new String(response.getBody(), "UTF-8")
                r.messages << ['body':resp_body]

                // Get the next message, if there is one
                response = channel.basicGet(QUEUE_NAME, true)
            }
            if (r.messages.size > 0) {
                // we got something so set the flag
                r['has_messages'] = 'true'
            }

            /**
             * Close it down
             */
            channel.close()
            connection.close()

            /**
             * Create a new JWT
             * include the queue name
             */
            def jwt = JWTUtilities.createJWTWithQueueName(jwt_key, expiration_ms, request.username, QUEUE_NAME)
            r.jwttoken = jwt

            /**
             * Format as JSON and render the response
             */
            respond r
        } else {
            /*
             * Either authentication went wrong or we arrived at this controller without a Queue Name
             * handle the error and notify the front end
             */
            if (!request.authenticated) {
                // how did that happen
                println("Request was not authenticated, we should never be here")
                redirect(controller: 'authError', action: 'index')
            } else if (QUEUE_NAME == "none") {
                println("Failed to get queue name from JWT")
                def t = JWTUtilities.createJWT(jwt_key, expiration_ms, request.username)
                r = [status:'error', message:"No queue_name in session", jwttoken:t] 
                respond r
            }
        }
        println("\n--------------\n")
    }
}
