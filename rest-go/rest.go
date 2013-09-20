package main

import "fmt"
import "io/ioutil"
import "net/http"
import "net/url"
import "bytes"

func main() {

	client := &http.Client{}

	input := "{\"input\":{\"???\": \"???\"}}"

	userguid := "???"
	apikey := "???"

	
    Url,_ := url.Parse("http://api.import.io/store/connector/???/_query")
    parameters := url.Values{}
    parameters.Add("_user",userguid)
    parameters.Add("_apikey",apikey)
    Url.RawQuery = parameters.Encode()

	request, _ := http.NewRequest("POST", Url.String(), bytes.NewBufferString(input))
	request.Header.Add("Content-Type","application/json")
    resp,_ := client.Do(request)

    defer resp.Body.Close()
    body,_ := ioutil.ReadAll(resp.Body)

    fmt.Printf(string(body[:]))
}