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

  inputString,_ := json.Marshal(input)

  Url,_ := url.Parse("https://api.import.io/store/connector/" + connectorGuid + "/_query")
  parameters := url.Values{}
  parameters.Add("_user",userguid)
  parameters.Add("_apikey",apikey)
  Url.RawQuery = parameters.Encode()

  request, _ := http.NewRequest("POST", Url.String(), strings.NewReader(string(inputString)))
  request.Header.Add("Content-Type","application/json")
  resp,_ := client.Do(request)

  defer resp.Body.Close()
  body,_ := ioutil.ReadAll(resp.Body)

  fmt.Printf(string(body[:]))
    
}

func main() {

  client := &http.Client{}

  userguid := "6d05ddb1-f13d-43f5-a785-2e4314b79fe5"
  apikey := "aRrae3FdmA/0UZ4sl1pVcVr3XQJJrgv/MM3XUQ0u6Px9vkuwOAoFNhpSDanVsOHRctQ73MYXIkJV+gSv9YWPtw=="

  query(map[string]interface{}{
    "input": map[string]interface{}{
      "searchterm": "avengers",
    },
  }, "caff10dc-3bf8-402e-b1b8-c799a77c3e8c", client, userguid, apikey)

}