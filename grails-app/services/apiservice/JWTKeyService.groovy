package apiservice

import grails.transaction.Transactional
//import java.util.Random
import org.apache.commons.lang.RandomStringUtils


@Transactional
class JWTKeyService {

    String charset = ('0'..'9').join()    
    Integer length = 30
    String jwt_key = RandomStringUtils.random(length, charset.toCharArray())
 
    def getKey() {
        return jwt_key
    }
}
