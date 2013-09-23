<?php


function sign($url,$apiKey) {
    $digest = base64_encode(hash_hmac("sha1", $url, base64_decode($apiKey), true));
    return $digest;
}

$input = '{"input":{"???": "???"}}';
$userGuid = "???";
$apiKey = "???";


$url = "https://api.import.io/store/connector/???/_query?_user=" . urlencode($userGuid) . "&_apikey=" . urlencode($apiKey);

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
curl_setopt($ch, CURLOPT_POSTFIELDS,  $input);
curl_setopt($ch, CURLOPT_HEADER, 1);
curl_setopt($ch, CURLOPT_POST, 1);
$result = curl_exec($ch);
curl_close($ch);


echo 'Query Complete<br/><br/>';
echo $result;

?>
