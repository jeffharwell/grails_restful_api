import io.jsonwebtoken.Jwts
import static io.jsonwebtoken.SignatureAlgorithm.HS256
import io.jsonwebtoken.SignatureException
import io.jsonwebtoken.ExpiredJwtException

/*
 * Two little methods to create JSON Web Tokens as needed. I'm doing all of my session
 * work inside of JWT so I suspect this class will expand
 * 
 * By design this should all be public static
 */

class JWTUtilities {
    /*
     * Create a simple JWT with just the username as the subject and no claims. This simply
     * serves to keep the session alive.
     */
    public static String createJWT(key, expiration_ms, user) {
        def current_time = System.currentTimeMillis()
        def new_time = current_time + expiration_ms.toLong()
        println("new_time ${new_time} = current_time ${current_time} + expiration_ms ${expiration_ms}")
        def expiration = new Date(new_time.toLong())
        println("Setting expiration to new time ${new_time}")
        println("     ${expiration}")
        //def expiration = Date.parse("yyyy-MM-dd", "2015-08-31")
        def token = Jwts.builder().setSubject(user).setExpiration(expiration).signWith(HS256, key).compact()
        println("Token: ${token}")
        return token
    }

    /*
     * Create a JWT with the username as the subject and the RabbitMQ Queue Name as 
     * a claim with the key "queue_name"
     */
    public static String createJWTWithQueueName(key, expiration_ms, user, queue_name) {
        def current_time = System.currentTimeMillis()
        def new_time = current_time + expiration_ms.toLong()
        println("new_time ${new_time} = current_time ${current_time} + expiration_ms ${expiration_ms}")
        println("Setting expiration to new time ${new_time}")
        def expiration = new Date(new_time.toLong())
        println("    ${expiration}")
        //def expiration = Date.parse("yyyy-MM-dd", "2015-08-31")

        /*
         * I think that you can have a per-claim expiration which is different from the
         * JWT "global" expiration. So even through there is an expiration in the payload
         * it still needs to be set for the token. However, if you set it for the token
         * it doesn't seem necessary to set it for the payload specifically.
         */
        def payload = [queue_name:queue_name, sub:user, exp:expiration]
        //def payload = [queue_name:queue_name, sub:user]
        def t = Jwts.builder().setSubject(user).setExpiration(expiration).setClaims(payload).signWith(HS256, key).compact()

        /*
         * This doesn't work, ends up with a expiration in the past, see above comment
         */
        //def t = Jwts.builder().setClaims(payload).signWith(HS256, key).compact()

        println("Created token with queue_name: ${t}")
        return t
    }
}

