package apiservice

class AuthErrorController {
    static responseFormats = ['json']

    def index() {
        // We came to chew bubble gum, and give out authorization errors,
        // and we are all out of bubble gum
        println("\n\n--------------\n")
        println("In the AuthError Controller")
        def testmap = ['status':'error', 'message':'authentication failed']
        println("Sending JSON: ${testmap.toString()}")
        println("\n-------------\n")
        respond testmap
    }
}
