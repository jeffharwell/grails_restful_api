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

    def testJsonWrite() {
        /* Test our JSON write */
        /*
         create table responses (
            id BIGINT NOT NULL AUTO_INCREMENT,
            assignmentid varchar(255) NOT NULL,
            hitid varchar(255) NOT NULL,
            workerid varchar(255) NOT NULL,
            response varchar(4000),
            PRIMARY KEY (id)
         )
        */

        def db_server = grailsApplication.config.getProperty('external.db_server')
        def db_name = grailsApplication.config.getProperty('external.db_name')
        def db_user = grailsApplication.config.getProperty('external.db_user')
        def db_password = grailsApplication.config.getProperty('external.db_password')

        def sql = Sql.newInstance("jdbc:mysql://${db_server}:3306/${db_name}", db_user, db_password, "com.mysql.jdbc.Driver")
        def hit_response = sql.dataSet("responses")
        def data = [assignmentid:'12',hitid:'34',workerid:'56',response:'my response']
        hit_response.add(data)
    }
}
