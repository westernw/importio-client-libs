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

// Issues a query request to import.io
function query($connectorGuid, $input, $userGuid, $apiKey) {

	$url = "https://query.import.io/store/connector/" . $connectorGuid . "/_query?_user=" . urlencode($userGuid) . "&_apikey=" . urlencode($apiKey);

	$ch = curl_init($url);
	curl_setopt($ch, CURLOPT_HTTPHEADER, array(
		"Content-Type: application/json",
		"import-io-client: import.io PHP client",
		"import-io-client-version: 2.0.0"
	));
	curl_setopt($ch, CURLOPT_POSTFIELDS,  json_encode(array("input" => $input)));
	curl_setopt($ch, CURLOPT_POST, 1);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch, CURLOPT_HEADER, 0);
	$result = curl_exec($ch);
	curl_close($ch);

	return json_decode($result);
}

// Example of doing a query
$result = query("39df3fe4-c716-478b-9b80-bdbee43bfbde", array(
	"input" => "query",
), $userGuid, $apiKey);

var_dump($result);
