// Sample Groovy mapping script
// Input: Map containing REST API responses
// Output: SOAP response object

// Log input data
logger.info("Groovy mapping input: {}, correlationId: {}", input, correlationId)

// Create response map
def response = [:]

// Extract data from first REST call
def restCall1 = input.get("call_1")
if (restCall1) {
    def statusCode = restCall1.statusCode ?: 200
    def body = restCall1.body

    response.status = statusCode

    if (statusCode == 200) {
        try {
            def jsonSlurper = new groovy.json.JsonSlurper()
            def parsedBody = jsonSlurper.parseText(body)

            response.data = parsedBody.toString()
            response.message = "Data successfully retrieved and mapped"
        } catch (Exception e) {
            response.data = body
            response.message = "Raw data returned"
        }
    } else {
        response.data = "Error occurred"
        response.message = "HTTP " + statusCode + " error"
    }
} else {
    response.status = 500
    response.data = "No data"
    response.message = "No REST call results found"
}

logger.info("Groovy mapping output: {}, correlationId: {}", response, correlationId)

// Return the mapped response
return response
