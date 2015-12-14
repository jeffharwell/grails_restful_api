import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

class FileProcessor {
    public static processFile(inputstream, rabbitmq_host, queue_name) {
        try {
            // Create the connection to the RabbitMQ host
            ConnectionFactory factory = new ConnectionFactory()
            println("Connecting to rabbitmq host ${rabbitmq_host}")
            factory.setHost(rabbitmq_host) // this is the docker container running on the local computer
            Connection connection = factory.newConnection()
            Channel channel = connection.createChannel()

            // Configure and open the channel
            def args = ["x-expires": 600000]
            channel.queueDeclare(queue_name, false, false, false, args);

            println "FileProcessor is processing file"
            inputstream.eachLine { line ->
                sleep(2000)
                println "Sending ${line}"
                channel.basicPublish("", queue_name, null, line.getBytes())
            }

            println " Done Sending"
            def end_marker = "======ENDOFOUTPUT======"
            channel.basicPublish("", queue_name, null, end_marker.getBytes())

            // Close it down
            channel.close()
            connection.close()
        } catch (java.io.IOException e) {
            println "Publishing fail with message ${e}"
        }
    }
}

