package apiservice
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*

@Transactional(readOnly = true)
class ApitestoneController {
    static responseFormats = ['json']

    def index() {
        println("index for Apitestone controller")
        if (request.authenticated) {
            def testmap = ['testing':'testing1', 'testing2':'testing2', 'jwttoken':request.jwttoken]
            respond testmap
        } else {
            def testmap = ['error':'authentication failed']
            respond testmap
        }
    }
    def testEnv() {
        /* The yml code
         *     external:
         *        testvar2: ${TestVarTwo}
         */

        //def envvar = 'TestVar'
        //def value = System.getenv(envvar)
        //def value_from_config = grailsApplication.config.getProperty('grails.external.testvar2')
        def banner_server = grailsApplication.config.getProperty('external.banner_server')
        def banner_sid = grailsApplication.config.getProperty('external.banner_sid')
        def banner_user = grailsApplication.config.getProperty('external.banner_user')
        //println("Testing environmental variables ${envvar} = ${value}")
        //println("Testvar2 is ${System.getenv('TestVar2')} in system")
        //println("Testvar2 is ${value_from_config} in config")
        println("banner_server is ${banner_server}")
        println("banner sid is ${banner_sid}")
        println("banner user is ${banner_user}")
        def testmap = [banner_server:banner_server, banner_sid:banner_sid, banner_user:banner_user]
        respond testmap
    }
}
