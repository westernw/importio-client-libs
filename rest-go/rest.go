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

  // Configure these settings with the details from My Account (https://import.io/data/account)
  userguid := "YOUR_USER_GUID"
  apikey := "YOUR_API_KEY"

  // Execute the query
  query(map[string]interface{}{
    "input": map[string]interface{}{
      "searchterm": "avengers",
    },
  }, "caff10dc-3bf8-402e-b1b8-c799a77c3e8c", client, userguid, apikey)

}