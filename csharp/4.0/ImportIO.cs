using System;
using System.Collections.Generic;
using System.Net;
using System.IO;
using System.Text;
using System.Web;

// Download the Newtonsoft JSON library here http://james.newtonking.com/projects/json-net.aspx
using Newtonsoft.Json;
using System.Threading;
using System.Collections.Concurrent;

namespace MinimalCometLibrary
{
    public delegate void QueryHandler(Query query, Dictionary<string, object> data);

    public class Query
    {
        private readonly QueryHandler queryCallback;

        private int jobsCompleted;
        private int jobsStarted;
        private int jobsSpawned;

        public Query(QueryHandler queryCallback)
        {
            this.queryCallback = queryCallback;
        }

        public bool IsFinished { get; private set; }

        public void OnMessage(Dictionary<string, object> data)
        {
            var messageType = (string)data["type"];

            Console.WriteLine((string)data["type"]);

            switch (messageType)
            {
                case "SPAWN":
                    jobsSpawned++;
                    break;
                case "INIT":
                case "START":
                    jobsStarted++;
                    break;
                case "STOP":
                    jobsCompleted++;
                    break;
            }

            IsFinished = jobsStarted == jobsCompleted && jobsSpawned + 1 == jobsStarted && jobsStarted > 0;

            if (messageType.Equals("ERROR") || messageType.Equals("UNAUTH") || messageType.Equals("CANCEL"))
            {
                IsFinished = true;
            }

            queryCallback(this, data);
        }
    }

    public class ImportIO
    {
        private const string MessagingChannel = "/messaging";

        private readonly string apiKey;
        private readonly string url;
        private readonly CookieContainer cookieContainer = new CookieContainer();
        private readonly Dictionary<Guid, Query> queries = new Dictionary<Guid, Query>();
        private readonly BlockingCollection<Dictionary<string, object>> messageQueue = new BlockingCollection<Dictionary<string, object>>();

        private Guid userGuid;
        private int msgId;
        private string clientId;
        private bool isConnected;

        public ImportIO(string host = "http://query.import.io", Guid userGuid = default(Guid), string apiKey = null)
        {
            this.userGuid = userGuid;
            this.apiKey = apiKey;

            url = host + "/query/comet/";
            clientId = null;
        }

        public void Login(string username, string password, string host = "http://api.import.io")
        {
            Console.WriteLine("Logging in");
            var loginParams = "username=" + HttpUtility.UrlEncode(username) + "&password=" + HttpUtility.UrlEncode(password);
            var searchUrl = host + "/auth/login";
            var loginRequest = (HttpWebRequest)WebRequest.Create(searchUrl);

            loginRequest.Method = "POST";
            loginRequest.ContentType = "application/x-www-form-urlencoded";
            loginRequest.ContentLength = loginParams.Length;

            loginRequest.CookieContainer = cookieContainer;

            using (var dataStream = loginRequest.GetRequestStream())
            {
                dataStream.Write(Encoding.UTF8.GetBytes(loginParams), 0, loginParams.Length);

                var loginResponse = (HttpWebResponse)loginRequest.GetResponse();

                if (loginResponse.StatusCode != HttpStatusCode.OK)
                {
                    throw new Exception("Could not log in, code:" + loginResponse.StatusCode);
                }

                foreach (Cookie cookie in loginResponse.Cookies)
                {
                    if (cookie.Name.Equals("AUTH"))
                    {
                        // Login was successful
                        Console.WriteLine("Login Successful");
                    }
                }
            }
        }

        public List<Dictionary<string, object>> Request(string channel, Dictionary<string, object> data = null, string path = "", bool doThrow = true)
        {
            var dataPacket = new Dictionary<string, object>();
            dataPacket.Add("channel", channel);
            dataPacket.Add("connectionType", "long-polling");
            dataPacket.Add("id", (msgId++).ToString());

            if (this.clientId != null)
                dataPacket.Add("clientId", this.clientId);

            if (data != null)
            {
                foreach (KeyValuePair<string, object> entry in data)
                {
                    dataPacket.Add(entry.Key, entry.Value);
                }
            }

            string url = this.url + path;

            if (apiKey != null)
            {
                url += "?_user=" + HttpUtility.UrlEncode(userGuid.ToString()) + "&_apikey=" + HttpUtility.UrlEncode(apiKey);
            }

            var request = (HttpWebRequest)WebRequest.Create(url);
            request.AutomaticDecompression = DecompressionMethods.GZip;
            request.Method = "POST";
            request.ContentType = "application/json;charset=UTF-8";
            request.Headers.Add(HttpRequestHeader.AcceptEncoding, "gzip");
            var dataJson = JsonConvert.SerializeObject(new List<object>() { dataPacket });

            request.ContentLength = dataJson.Length;

            request.CookieContainer = cookieContainer;

            using (var dataStream = request.GetRequestStream())
            {
                dataStream.Write(System.Text.UTF8Encoding.UTF8.GetBytes(dataJson), 0, dataJson.Length);
                try
                {
                    var response = (HttpWebResponse)request.GetResponse();

                    using (var responseStream = new StreamReader(response.GetResponseStream()))
                    {
                        var responseJson = responseStream.ReadToEnd();
                        var responseList = JsonConvert.DeserializeObject<List<Dictionary<string, object>>>(responseJson);
                        foreach (Dictionary<string, object> responseDict in responseList)
                        {
                            if (responseDict.ContainsKey("successful") && (bool)responseDict["successful"] != true)
                            {
                                if (doThrow)
                                    throw new Exception("Unsucessful request");
                            }

                            if (!responseDict["channel"].Equals(MessagingChannel)) continue;

                            if (responseDict.ContainsKey("data"))
                            {
                                messageQueue.Add(((Newtonsoft.Json.Linq.JObject)responseDict["data"]).ToObject<Dictionary<string, object>>());
                            }

                        }

                        return responseList;
                    }
                }
                catch (Exception e)
                {
                    Console.WriteLine("Error occurred {0}", e.Message);
                    return new List<Dictionary<string, object>>();
                }
                
            }
        }

        public void Handshake()
        {
            var handshakeData = new Dictionary<string, object>();
            handshakeData.Add("version", "1.0");
            handshakeData.Add("minimumVersion", "0.9");
            handshakeData.Add("supportedConnectionTypes", new List<string> { "long-polling" });
            handshakeData.Add("advice", new Dictionary<string, int>() { { "timeout", 60000 }, { "interval", 0 } });
            var responseList = Request("/meta/handshake", handshakeData, "handshake");
            clientId = (string)responseList[0]["clientId"];
        }

        public void Connect()
        {
            if(isConnected) {
                return ;
            }
            
            Handshake();

            var subscribeData = new Dictionary<string, object>();
            subscribeData.Add("subscription", MessagingChannel);
            Request("/meta/subscribe", subscribeData);

            isConnected = true;

            new Thread(new ThreadStart(Poll)).Start();

            new Thread(new ThreadStart(PollQueue)).Start();
        }

        public void Disconnect()
        {
            Request("/meta/disconnect", null, "", true);
            isConnected = false;
        }

        private void Poll()
        {
            while (isConnected)
            {
                Request("/meta/connect", null, "connect", false);
            }
        }

        private void PollQueue()
        {
            while (isConnected)
            {
                ProcessMessage(messageQueue.Take());
            }
        }

        private void ProcessMessage(Dictionary<string, object> data)
        {
            var requestId = Guid.Parse((string)data["requestId"]);
            var query = queries[requestId];

            query.OnMessage(data);
            if (query.IsFinished)
            {
                queries.Remove(requestId);
            }
        }

        public void DoQuery(Dictionary<string, object> query, QueryHandler queryHandler)
        {
            var requestId = Guid.NewGuid();
            queries.Add(requestId, new Query(queryHandler));
            query.Add("requestId", requestId);
            Request("/service/query", new Dictionary<string, object>() { { "data", query } });
        }
    }
}
