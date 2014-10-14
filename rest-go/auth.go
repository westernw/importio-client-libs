/*
import.io REST API - example go code

This file is an example for integrating with the import.io REST API using Go

Dependencies: None outside standard library

@author: dev@import.io
@source: https://github.com/import-io/importio-client-libs/tree/master/rest-go
*/

package main

import "fmt"
import "encoding/json"
import "io/ioutil"
import "net/http"
import "net/url"
import "strings"
import _ "crypto/sha256"
import _ "crypto/sha512"

// Does a login
func login(username string, password string, domain string, connectorGuid string, client *http.Client, userguid string, apikey string) []interface{} {

  // Build the request body we need to log in
  inputString,_ := json.Marshal(map[string]interface{}{
    "loginOnly": true,
    "additionalInput": map[string]interface{}{
      connectorGuid: map[string]interface{}{
        "domainCredentials": map[string]interface{}{
            domain: map[string]interface{}{
              "username": username,
              "password": password,
            },
        },
      },
    },
  })

  // Create the URL to request
  URL,_ := url.Parse("https://api.import.io/store/connector/" + connectorGuid + "/_query")
  parameters := url.Values{}
  parameters.Add("_user",userguid)
  parameters.Add("_apikey",apikey)
  URL.RawQuery = parameters.Encode()

  // Build and dispatch the request
  request, _ := http.NewRequest("POST", URL.String(), strings.NewReader(string(inputString)))
  request.Header.Add("Content-Type", "application/json")
  request.Header.Add("import-io-client", "import.io Go client")
  request.Header.Add("import-io-client-version", "2.0.0")
  resp,_ := client.Do(request)

  defer resp.Body.Close()
  body,_ := ioutil.ReadAll(resp.Body)
  var data map[string]interface{}
  json.Unmarshal(body, &data)
  // Return the cookies that came back from the API call
  return data["cookies"].([]interface{})
}

// Does a single query
func query(input map[string]interface{}, connectorGuid string, client *http.Client, userguid string, apikey string) {

  // Generate the data to be sent to the server
  inputString,_ := json.Marshal(input)

  // Build the URL
  URL,_ := url.Parse("https://api.import.io/store/connector/" + connectorGuid + "/_query")
  parameters := url.Values{}
  parameters.Add("_user",userguid)
  parameters.Add("_apikey",apikey)
  URL.RawQuery = parameters.Encode()

  // Build the request
  request, _ := http.NewRequest("POST", URL.String(), strings.NewReader(string(inputString)))
  request.Header.Add("Content-Type", "application/json")
  request.Header.Add("import-io-client", "import.io Go client")
  request.Header.Add("import-io-client-version", "2.0.0")
  resp,_ := client.Do(request)

  defer resp.Body.Close()
  body,_ := ioutil.ReadAll(resp.Body)

  // Print the results
  fmt.Printf(string(body[:]))
    
}

func main() {

  client := &http.Client{}

  // Configure these settings with the details from My Data (https://import.io/data/mine) and Account (https://import.io/data/account)
  userguid := "YOUR_USER_GUID"
  apikey := "YOUR_API_KEY"
  connectorGuid := "YOUR_CONNECTOR_GUID"
  connectorDomain := "YOUR_CONNECTOR_DOMAIN"
  connectorUsername := "YOUR_CONNECTOR_USERNAME"
  connectorPassword := "YOUR_CONNECTOR_PASSWORD"

  // Do a login to get the cookies
  cookies := login(connectorUsername, connectorPassword, connectorDomain, connectorGuid, client, userguid, apikey)

  // Execute the query using the cookies
  query(map[string]interface{}{
    "input": map[string]interface{}{
      "???": "???",
    },
    "additionalInput": map[string]interface{}{
        connectorGuid: map[string]interface{}{
            "cookies": cookies,
        },
    },
  }, connectorGuid, client, userguid, apikey)

}