package apiservice

class AuthForbiddenController {
    static responseFormats = ['json']

    def index() {
        // All we ever do is say no!
        println("\n\n--------------\n")
        println("In the AuthForbidden Controller")
        def testmap = ['status':'error', 'message':'forbidden']
        println("Sending JSON: ${testmap.toString()}")
        println("\n-------------\n")
        respond testmap
    }
}

