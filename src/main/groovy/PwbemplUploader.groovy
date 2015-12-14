import java.sql.*
import groovy.sql.Sql
import com.jeffharwell.utilities.CSVUtilities;
import java.math.RoundingMode

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

class PwbemplUploader {

    private static String end_of_output_str = "======ENDOFOUTPUT======"

    private static String fzbodir_clear_e = """
    delete from fts.fzbodir where fzbodir_type = 'E'
    """

    private static String fzbodir_populate_sql = """
    INSERT INTO fts.fzbodir
    (
      FZBODIR_TYPE,
      FZBODIR_DEPT,
      FZBODIR_BUILDING,
      FZBODIR_PHONE_EXT,
      FZBODIR_PHONE_DIRECT,
      FZBODIR_PHONE_TOLLFREE,
      FZBODIR_FAX,
      FZBODIR_EMAIL_1,
      FZBODIR_EMAIL_2,
      FZBODIR_MAILBOX,
      FZBODIR_TITLE,
      FZBODIR_LAST_NAME,
      FZBODIR_FIRST_NAME,
      FZBODIR_GEN,
      FZBODIR_NICKNAME
    )
    (
      SELECT 
        'E',
        pwbempl_dept_desc,
        pwbempl_building,
        NVL(SUBSTR(pwbempl_work, -18), pwbempl_work),
        NULL,
        NULL,
        NULL,
        NULL,
        pwbempl_email,
        pwbempl_mailbox,
        NVL(SUBSTR(pwbempl_function, 50), pwbempl_function),
        pwbempl_last_name,
        pwbempl_first_name,
        NULL,
        pwbempl_preferred -- updated 10/9/12 MS from pwbempl_sal
      FROM
        fts.pwbempl_base
      WHERE
        pwbempl_pidm IS NOT NULL -- Talked to Roxanne on 26-OCT-2010. She asked me to remove it
        AND pwbempl_campus_dir = 'Y'
        AND pwbempl_status = 'Active'
    )
    """

    private static HashMap<String,String> csv_to_sql_translation = [
    'Associate ID':'PWBEMPL_ASSOCIATE_ID',
    'First Name':'PWBEMPL_FIRST_NAME',
    'Last Name':'PWBEMPL_LAST_NAME',
    'Middle Name':'PWBEMPL_MI',
    'Generation Suffix Description':'PWBEMPL_SUFFIX',
    'Preferred Name':'PWBEMPL_PREFERRED',
    'Position Status':'PWBEMPL_STATUS',
    'Address: Street':'PWBEMPL_STREET_LINE1',
    'Address: City':'PWBEMPL_CITY',
    'Address: State / Territory Code':'PWBEMPL_STAT_CODE',
    'Address: Zip / Postal Code':'PWBEMPL_ZIP',
    'Personal Contact: Home Phone':'PWBEMPL_PHONE',
    'Personal Contact: Personal Mobile':'PWBEMPL_MOBILE',
    'Gender':'PWBEMPL_SEX',
    'Regular Staff Override':'PWBEMPL_STAFF_OVERRIDE',
    'Administrator Override':'PWBEMPL_ADMIN_OVERRIDE',
    'Hire Date':'PWBEMPL_OHDATE',
    'Rehire Date':'PWBEMPL_RHDATE',
    'Work Contact: Work Phone':'PWBEMPL_WORK',
    'Work Contact: Work Email':'PWBEMPL_EMAIL',
    'Work Contact: Work Mail Stop':'PWBEMPL_MAILBOX',
    'Division':'PWBEMPL_VPAREA',
    'Division Description':'PWBEMPL_VPAREA_DESC',
    'Class':'PWBEMPL_CLASS_CODE',
    'Class Description':'PWBEMPL_CLASS_DESC',
    'Home Department':'PWBEMPL_DEPT',
    'Home Department Description':'PWBEMPL_DEPT_DESC',
    'Employee Type Description':'PWBEMPL_FULLPARTTIME',
    'File Number':'PWBEMPL_FILENUMBER',
    'Job Title Description':'PWBEMPL_FUNCTION',
    'Pay Grade Description':'PWBEMPL_GRADE',
    'Length of Service Date':'PWBEMPL_LENGTH_SERVE',
    'Scheduled Hours':'PWBEMPL_HOURS',
    'Building':'PWBEMPL_BUILDING',
    'Home Cost Number Code':'PWBEMPL_HOME_COST_CENTER',
    'Include on Directory':'PWBEMPL_CAMPUS_DIR',
    'Role':'PWBEMPL_ROLE'
    //'Payroll Company Code':'PWBEMPL_COMPANY_CODE',
    //'Clock Full Code':'PWBEMPL_CLOCK_CODE',
    //'Pay Group Code':'PWBEMPL_PAY_GROUP_CODE',
    //'Regular Pay Rate Code':'PWBEMPL_R_PAY_RATE_CODE',
    //'Standard Hours':'PWBEMPL_STANDARD_HOURS',
    //'Pay Frequency Code':'PWBEMPL_PAY_FREQ_CODE',
    //"NAICS Worker\'s Comp Code":"PWBEMPL_WORKERS_COMP_CODE",
    //'Annual Total Compensation':"PWBEMPL_ANNUAL_COMPENSATION",
    //'SUI/SDI Tax Code':'PWBEMPL_SUI_SDI_TAX_CODE',
    //'Federal/W4 Exemptions':'PWBEMPL_W4_EXEMPTIONS',
    //'Worked In State Tax Code':'PWBEMPL_STATE_TAX_CODE',
    //'Do not calculate Social Security':'PWBEMPL_DNC_SOCIAL_SECURITY',
    //'Do not calculate Medicare':'PWBEMPL_DNC_MEDICARE',
    //'Premium Only Plan 1':'PWBEMPL_POP1_CODE',
    //'Premium Only Plan 2':'PWBEMPL_POP2_CODE',
    //'Premium Only Plan 3':'PWBEMPL_POP3_CODE',
    //'Premium Only Plan 4':'PWBEMPL_POP4_CODE',
    //'Premium Only Plan 5':'PWBEMPL_POP5_CODE',
    //'Federal/W4 Effective Date':'PWBEMPL_W4_EFFECTIVE_DATE',
    //'U.S. Work Authorization Status':'PWBEMPL_US_WORK_AUTH_STATUS',
    //'Pay Group Description':'PWBEMPL_PAY_GROUP_DESCRIPTION'
    ]

    private static ArrayList<String> headers_to_require = ['Associate ID','First Name','Last Name','Position Status','Role','Tax ID (SSN)']

    private static ArrayList<String> date_fields = ['PWBEMPL_OHDATE','PWBEMPL_RHDATE','PWBEMPL_LENGTH_SERVE','PWBEMPL_W4_EFFECTIVE_DATE']
    private static ArrayList<String> active_roles = ['Faculty','Faculty and Manager','Manager','Staff']

    private static HashMap<String, Integer> field_lengths_to_reject = [
    'PWBEMPL_ACCT':5,
    'PWBEMPL_MAILBOX':5,
    'PWBEMPL_CLASS_CODE':12,
    'PWBEMPL_DEPT':6,
    'PWBEMPL_GRADE':13,
    'PWBEMPL_STAT_CODE':2
    ]

    private static HashMap<String, Integer> field_lengths_to_truncate = [
    //'PWBEMPL_DEPT_DESC':255,
    //'PWBEMPL_CLASS_DESC':255,
    //'PWBEMPL_FUNCTION':255,
    //'PWBEMPL_VPAREA_DESC':255,
    //'PWBEMPL_VPAREA':255
    'PWBEMPL_DEPT_DESC':35,
    'PWBEMPL_CLASS_DESC':52,
    'PWBEMPL_FUNCTION':70,
    'PWBEMPL_VPAREA_DESC':38,
    'PWBEMPL_VPAREA':3
    ]

    /*
     * This subroutine verifies the each record has at least most of the required headers
     * If a record does not have the correct headers we should throw a Runtime Exception, as this
     * most likely means that there is something wrong with the file.
     */
    public static void verifyHeaders(headers_to_require, record) {
        headers_to_require.each() { header ->
            if (!record.containsKey(header)) {
                def msg = "The file is missing the header \"${header}\". Are sure that the file you uploaded is a CSV file of the correct format? No changes have been made to Banner."
                throw new RuntimeException(msg)
            }
        }
    }

    private static String checkRejectFields(entry_hash) {
        ArrayList<String> reject_fields_list = []
        entry_hash.each() { key, value ->
            if (field_lengths_to_reject.containsKey(key) && field_lengths_to_reject[key] < value.length()) {
                reject_fields_list += "${key} - Max Size ${field_lengths_to_reject[key]}"
            }
        }
        return reject_fields_list.join(", ")
    }

    private static String truncateFields(entry_hash) {
        ArrayList<String> truncate_fields_list = []
        entry_hash.each() { key, value ->
            if (field_lengths_to_truncate.containsKey(key) && field_lengths_to_truncate[key] < value.length()) {
                truncate_fields_list += "${key} - Max Size ${field_lengths_to_truncate[key]}"
                // because the slice notation is inclusive ...
                def max_index = field_lengths_to_truncate[key] - 1
                // change the hash in place, because we are not messing with keys this is safe
                entry_hash[key] = value[0..max_index]
            }
        }
        return truncate_fields_list.join(", ")
    }

    /*
     * Gets PIDM from an SSN and Birthdate, if there is an error it will push the 
     * error onte the message queue
     */
    private static String get_pidm(sql, ssn, birthdate, pushMsg) {
        if (!ssn || !birthdate) {
            // really
            pushMsg("Error: either SSN or Birthdate is missing, cannot match")
            return false
        }
        def to_ignore = ['514130730']
        def only_num_ssn = ssn.replaceAll('-','')

        // We know they are duplicates, ignore until they are cleaned up
        if (only_num_ssn in to_ignore) {
            return false
        }

        if (birthdate ==~ /[0-9]+\/[0-9]+\/[0-9]{2}/) {
            // this is bad
            // so RRRR will say anything before 49 is 2049, not 1949. This doesn't 
            // work so hot for birthdates.
            // Rewrite it for the 20th century :(
            def a = birthdate.split('/')
            birthdate = "${a[0]}/${a[1]}/19${a[2]}".toString()
        }
        def pattern = 'mm/dd/RRRR'
        String query = """
            select spbpers_pidm
             from spbpers
            where spbpers_ssn = :ssn
              and spbpers_birth_date = to_date(:bd_string, '${pattern}')
        """.toString()
        //println "${query}\n${only_num_ssn}, ${birthdate}"

        def rows = sql.rows(query, [ssn:only_num_ssn, bd_string:birthdate])
        if (rows.size > 1) {
            pushMsg("Error: query for pidm returned more than one row for the given SSN and birth date")
            pushMsg("   ${only_num_ssn}, ${birthdate}")
            return false
        }
        def r = rows[0]
        def pidm = false
        if (r && !r.isEmpty()) {
            if (r.containsKey('SPBPERS_PIDM')) {
                pidm = r['SPBPERS_PIDM']
            }
        }
        return pidm
    }

    /*
     * Takes the queue_name and the rabbitmq_host and creates 
     * or subscribes to the channel we will be using to publish
     * the output of the processing of the CSV file
     */
    public static List<Object> createChannel(rabbitmq_host, queue_name) {
        Channel channel = null;
        Connection connection = null;
        try {
            // Create the connection to the RabbitMQ host
            ConnectionFactory factory = new ConnectionFactory()
            factory.setHost(rabbitmq_host) // this is the docker container running on the local computer
            connection = factory.newConnection()
            channel = connection.createChannel()

            // Configure and open the channel
            def args = ["x-expires": 600000]
            channel.queueDeclare(queue_name, false, false, false, args);
        } catch (java.io.IOException e) {
            // Rethrow ... I guess
            println "   PwbemplUploader: Failed to create channel: ${e}"
            throw e
        }
        return [channel, connection]
    }

    public static notifyAndExit(pushMesg, channel, connection, error_message, Exception exception) {
        println error_message.class
        println error_message.toString().class
        println error_message.toString()
	pushMesg("*****")
        pushMesg(error_message.toString())
	pushMesg("*****")
	pushMesg("")
        pushMesg("** The Upload Failed with errors **")
        pushMesg("======ENDOFOUTPUT======")
        channel.close()
        connection.close()
        throw exception
    }

    public static upload(InputStream inputStream, String rabbitmq_host, String queue_name, String ban_server, String ban_database, String ban_username, String ban_password ) {
        // Connect to RabbitMQ
        def (Channel channel, Connection connection) = createChannel(rabbitmq_host, queue_name)
        // Define a little closure to replace all of our print statements 
        // This will take a string and push it into the message bus
        def pushMsg = { String msg ->
            // Need to make sure msg is actually a string, basicPublish doesn't
            // like it if it is not
            String m = msg.toString()
            try {
                channel.basicPublish("", queue_name, null, m.getBytes())
            } catch (java.io.IOException e) {
                println "    PwbemplUploader: Publishing fail with message ${e}"
                throw e
            }
        }

        //pushMsg("Getting Headers from CSV File") 

        def csvr = new CSVUtilities(inputStream)
        csvr.setDelimiter(",")
        /*
         * cannot double dip like this on an input stream
         * need to rearchitect the library a bit
         *
        String[] file_headers = csvr.getHeaders()

        if (file_headers.size() < 5) {
            // Something went wrong
            def msg = "Got less than five headers, something has gone wrong, the CSV file likely didn't parse or has a format error"
            pushMsg(msg)
            pushMsg(end_of_output_str)
            //throw new RuntimeException(msg)
            return false
        }
        */
        
        // Oracle Connection
        pushMsg("Connecting to Banner: ${ban_database}@${ban_server}")
        def sql
        try {
            sql = Sql.newInstance("jdbc:oracle:thin:@${ban_server}:1521:${ban_database}", ban_username, ban_password, "oracle.jdbc.pool.OracleDataSource")
        } catch (all) {
            def msg = "Unable to connect to Banner! Call IT and tell them \"${all.toString()}\""
            notifyAndExit(pushMsg, channel, connection, msg, all)
        }

        List<HashMap<String,String>> hm
        try {
            hm = csvr.parseAsCSV();
        } catch (all) {
            def msg = "Unable to read the file you uploaded: ${all.toString()}"
            notifyAndExit(pushMsg, channel, connection, msg, all)
        }


        ArrayList<String> test_data = ['123-45-6789','000-00-0000','000-00-0005','123-45-6788','111-11-1112']
        def entries = [] // this will hold all the data we want to enter
        try {
            hm.eachWithIndex() { h, index ->
                // Make sure this record has the correct headers, if it doesn't throw a runtime error
                verifyHeaders(headers_to_require, h)
                def ssn = h['Tax ID (SSN)'] // xxx-xx-xxxx
                if (!(ssn in test_data)) { // don't push ADP test data into Banner.
                    def birthdate_string = h['Birth Date'] // MM/DD/YYYY
                    HashMap entry = [:]
                    //println("---")
                    h.each() { key, value ->
                        if (h[key] != '' && csv_to_sql_translation.containsKey(key)) {
                            //println("${key} -> ${value}")    
                            if (csv_to_sql_translation[key] in date_fields) {
                                // Need to convert to a date
                                def date_value
                                if (value ==~ /[0-9]+\/[0-9]+\/[0-9]{2}/) {
                                    // uugh, got a year with only two digits
                                    date_value = new java.sql.Date(Date.parse("d/M/yy", value).getTime())
                                } else {
                                    date_value = new java.sql.Date(Date.parse("d/M/yyyy", value).getTime())
                                }
                                entry[csv_to_sql_translation[key]] = date_value
                            } else if (key == 'Home Department') {
                                // ADP is currently add a "--" to the end for some reason
                                def v = value.replaceAll(~/--$/,'')
                                entry[csv_to_sql_translation[key]] = v
                            } else if (key == 'Scheduled Hours') {
                                def v = new BigDecimal(value).setScale(0, RoundingMode.HALF_UP)
                                entry[csv_to_sql_translation[key]] = v
                            } else if (key == 'Home Cost Number Code') {
                                entry[csv_to_sql_translation[key]] = value
                                // now get the acct number ... uugh
                                def fund, orgn, acct
                                (fund, orgn, acct) = value.split('-')
                                if (acct) {
                                    entry['PWBEMPL_ACCT'] = acct
                                }
                            } else {
                                // Just a normal string/varchar
                                entry[csv_to_sql_translation[key]] = value
                            }
                            //println("${csv_to_sql_translation[key]} -> ${entry[csv_to_sql_translation[key]]}")
                        }
                    }
                    //println("\n--- Printing processed entry after value translation")
                    //println(entry)
                    def reject_on_fields = checkRejectFields(entry)
                    def truncate_fields = truncateFields(entry)
                    //def person_string = "${entry['PWBEMPL_LAST_NAME']}, ${entry['PWBEMPL_FIRST_NAME']}, ${entry['PWBEMPL_ASSOCIATE_ID']}, ${entry['PWBEMPL_STATUS']}"
                    def person_string = "${entry['PWBEMPL_LAST_NAME']}, ${entry['PWBEMPL_ASSOCIATE_ID']}, ${entry['PWBEMPL_STATUS']}"

                    // If they have a valid role we want to consider them as active regardless of the status as per Eric
                    // This will mainly happen when we want to add someone in advance of their actual hire date.
                    if (entry['PWBEMPL_ROLE'] in active_roles) {
                        entry['PWBEMPL_STATUS'] = 'Active'
                    }
                    if (!(entry['PWBEMPL_ROLE'] in active_roles) && entry['PWBEMPL_ACCT'] == '') {
                        pushMsg("Warning: ${person_string} has an active role but no Home Account Center, they will not receive the Employee role")
                    }
                    if (!(entry['PWBEMPL_ROLE'] in active_roles) && entry['PWBEMPL_ROLE'] == 'Active') {
                        pushMsg("Warning: ${person_string} has a status of 'Active' but an invalid role of ${entry['PWBEMPL_ROLE']}")
                    }

                    if (reject_on_fields) {
                        // These are data entry errors, reject the record
                        pushMsg("Rejected: The value in field(s) ${reject_on_fields} is/are too long for ${person_string}")
                    } else {
                        def pidm = get_pidm(sql, ssn, birthdate_string, pushMsg)
                        if (truncate_fields) {
                            // We want to warn about truncated fields, but go ahead and push the entry anyways
                            //pushMsg("Values in fields ${truncate_fields} truncated for ${person_string}")
                        }
                        if (!pidm || pidm == 'false') {
                            if (entry['PWBEMPL_STATUS'] == 'Active') {
                                //def msg = "Rejected: Couldn't find a PIDM for ${person_string}, ${ssn}, ${birthdate_string}"
                                def msg = "Rejected: Couldn't find a PIDM for ${person_string}, no SSN and Birthday Match"
                                pushMsg(msg)
                            } 
                            // If they are inactive with no PIDM ... we don't care ... keep on keeping on
                        } else {
                            // Add These People
                            entry['PWBEMPL_PIDM'] = pidm
                            entries << entry
                        }
                    }
                }
            }
        } catch (all) {
            def msg = "Unable to read the file .. the error is \"${all.toString()}\""
            notifyAndExit(pushMsg, channel, connection, msg, all)
        }


	/*
	 * A bit of error checking
         */
        if (entries.size < 300) {
            def msg = "We only found ${entries.size} records in the file you uploaded. This is (hopefully) an error. Please check your file and upload again. No Changes have been to Banner."
            notifyAndExit(pushMsg, channel, connection, msg, new RuntimeException(msg))
        }
             

        /*
         * Populate PWBEMPL_BASE
         */

	def added_records = 0
        sql.withTransaction {
            pushMsg("Clearing Existing Data from Banner")
            sql.execute('delete from FTS.PWBEMPL_BASE')
            def pwbempl_base_table = sql.dataSet("FTS.PWBEMPL_BASE")
            entries.each { entry ->
                if (!entry.containsKey('PWBEMPL_PIDM') || !entry['PWBEMPL_PIDM'] || entry['PWBEMPL_PIDM'] == 'false') {
                    pushMsg("Error: no pidm in entry ${entry.toString()} ... this should not have happened")
                } else { 
                    try {
                        pwbempl_base_table.add(entry)
			added_records++
                    } catch (java.sql.SQLException e) {
                        pushMsg("Failed to insert entry: ")
                        pushMsg(entry.toString())
                        pushMsg("With the following error: ")
                        pushMsg(e.toString())
                        pushMsg("This record will not got into the database")
                    }
                }
            }
        }

        /*
         * Populate the Directory
         *   This is based on the code from FTS.TWGZHR.UPDATE_FZBODIR
         *
         */

        // Do this in a transaction, it either needs to fail or roll back completely
        //   Don't want to blow out the director with a bad entry
        sql.withTransaction {
            sql.execute(fzbodir_clear_e)
            sql.execute(fzbodir_populate_sql)
        }

        // All Done
        pushMsg("Upload Complete")
	pushMsg("Uploaded ${added_records} records to PWBEMPL_BASE")
        pushMsg(end_of_output_str)

        // Close it down
        channel.close()
        connection.close()

    } // end upload(InputStream inputStream)

}
