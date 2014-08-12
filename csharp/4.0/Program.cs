using System;
using System.Collections.Generic;

// Download the Newtonsoft JSON library here http://james.newtonking.com/projects/json-net.aspx
using Newtonsoft.Json;
using System.Threading;

namespace MinimalCometLibrary
{
    public class Program
    {
        private static CountdownEvent countdownLatch;
        
        public static void Main(string[] args)
        {
            // If using an API key
            //ImportIO io = new ImportIO("http://api.import.io",Guid.parse("d08d14f3-6c98-44af-a301-f8d4288ecce3"),"tMFNJzaaLe8sgYF9hFNhKI7akyiPLMhfu8U2omNVCVr5hqWWLyiQMApDDyUucQKF++BAoVi6jnGnavYqRKP/9g==");
            
            // If using a username and password
            var io = new ImportIO();
            io.Login("xxx", "xxx");
            

            io.Connect();
            var query1 = new Dictionary<string,object>();
            query1.Add("input",new Dictionary<string, string> { { "query", "mac mini" } });
            query1.Add("connectorGuids", new List<string> { "39df3fe4-c716-478b-9b80-bdbee43bfbde" });

            var query2 = new Dictionary<string, object>();
            query2.Add("input", new Dictionary<string, string> { { "query", "ubuntu" } });
            query2.Add("connectorGuids", new List<string> { "39df3fe4-c716-478b-9b80-bdbee43bfbde" });

            var query3 = new Dictionary<string, object>();
            query3.Add("input", new Dictionary<string, string> { { "query", "ibm" } });
            query3.Add("connectorGuids", new List<string> { "39df3fe4-c716-478b-9b80-bdbee43bfbde" });

            countdownLatch = new CountdownEvent(3);

            io.DoQuery(query1, HandleQuery);
            io.DoQuery(query2, HandleQuery);
            io.DoQuery(query3, HandleQuery);

            countdownLatch.Wait();

            io.Disconnect();
        }

        private static void HandleQuery(Query query, Dictionary<string, object> message)
        {
            if(message["type"].Equals("MESSAGE"))
            {
                Console.WriteLine("Got data!");
                Console.WriteLine(JsonConvert.SerializeObject(message["data"]));
            }

            if (query.IsFinished) countdownLatch.Signal();
        }
    }
}
