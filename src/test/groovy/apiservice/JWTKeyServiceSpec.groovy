package apiservice

import grails.test.mixin.TestFor
import spock.lang.Specification
import org.apache.commons.lang.RandomStringUtils

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(JWTKeyService)
class JWTKeyServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "should return key 123456789"() {
        given:
            // No mocking needed, the reach into the service and change the key
            service.jwt_key = "123456789"
            //RandomStringUtils mockRandom = Mock()
            //mockRandom.random(30, ('0'..'9').join()) >>> "123456789"
        expect:
        service.getKey() == "123456789"
    }

    void "should return a key"() {
        def key = service.getKey()
        if (key) {
            println("Got a key")
        } else {
            println("Key is null")
        }
        println(service.getKey())
        expect:
        service.getKey() != null
    }
}
