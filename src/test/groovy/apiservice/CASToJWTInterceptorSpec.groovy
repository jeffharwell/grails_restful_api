package apiservice


import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(CASToJWTInterceptor)
class CASToJWTInterceptorSpec extends Specification {

    def setup() {
    }

    def cleanup() {

    }

    void "Test CASToJWT interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"CASToJWT")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
