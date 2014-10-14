<?php
 
/*
import.io REST API - example PHP code

This file is an example for integrating with the import.io REST API using PHP

Dependencies: Requires cURL module to be installed

@author: dev@import.io
@source: https://github.com/import-io/importio-client-libs/tree/master/rest-php
*/

// Populate your credentials from My Account (https://import.io/data/account)
$userGuid = "YOUR_USER_GUID";
$apiKey = "YOUR_API_KEY";
// Populate your sources from My Data (https://import.io/data/mine)
$connectorGuid = "YOUR_CONNECTOR_GUID";
$connectorDomain = "YOUR_CONNECTOR_DOMAIN";
$username = "YOUR_SITE_USERNAME";
$password = "YOUR_SITE_PASSWORD";

// Issues a query request to import.io
function query($connectorGuid, $input, $userGuid, $apiKey, $additionalInput, $login) {
 
  $url = "https://api.import.io/store/connector/" . $connectorGuid . "/_query?_user=" . urlencode($userGuid) . "&_apikey=" . urlencode($apiKey);
 
  $data = array();
  if ($input) {
    $data["input"] = $input;
  }
  if ($additionalInput) {
    $data["additionalInput"] = $additionalInput;
  }
  if ($login) {
    $data["loginOnly"] = true;
  }
 
  $ch = curl_init($url);
  curl_setopt($ch, CURLOPT_HTTPHEADER, array(
    "Content-Type: application/json",
    "import-io-client: import.io PHP client",
    "import-io-client-version: 2.0.0"
  ));
  curl_setopt($ch, CURLOPT_POSTFIELDS,  json_encode($data));
  curl_setopt($ch, CURLOPT_POST, 1);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
  curl_setopt($ch, CURLOPT_HEADER, 0);
  $result = curl_exec($ch);
  curl_close($ch);
 
  return json_decode($result);
}

// Build credentials request
$creds = array();
$creds[$connectorDomain] = array(
  "username" => $username,
  "password" => $password
);

// Execute credentials request
$login = query($connectorGuid, false, $userGuid, $apiKey, array(
  $connectorGuid => array(
    "domainCredentials" => $creds
  )
), false);

// Execute the query with the credentials
$result = query($connectorGuid, array(
  "search" => "google",
), $userGuid, $apiKey, array(
  $connectorGuid => array(
    "cookies" => $login->cookies
  )
), false);

var_dump($result);