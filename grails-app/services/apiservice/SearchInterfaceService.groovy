package apiservice

import grails.transaction.Transactional
import groovy.sql.Sql

@Transactional
class SearchInterfaceService {
    def grailsApplication

    def hit_response

    def openDatabase() {
        // turns out that grailsApplication is null when the service is initialized, so we cannot
        // initialize a database connection there (probably for the best but still.)
        def db_server = grailsApplication.config.getProperty('external.db_server')
        def db_name = grailsApplication.config.getProperty('external.db_name')
        def db_user = grailsApplication.config.getProperty('external.db_user')
        def db_password = grailsApplication.config.getProperty('external.db_password')

        def sql = Sql.newInstance("jdbc:mysql://${db_server}:3306/${db_name}", db_user, db_password, "com.mysql.jdbc.Driver")
        hit_response = sql.dataSet("responses")
    }

    def writeResults(assignmentid, hitid, workerid, response) {
        // Hmm, need to just fold the database into the Groovy config
        // and grab the connection, this feels especially bad
        if (!hit_response) {
            openDatabase()
        }

        def data = [assignmentid:assignmentid,hitid:hitid,workerid:workerid,response:response]
        hit_response.add(data)
    }
}
