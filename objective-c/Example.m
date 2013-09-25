//
//  example.m
//  iOS Client Library
//
//  Created by dev on 23/09/2013.
//  Copyright (c) 2013 importio. All rights reserved.
//

#import "Importio.h"
@implementation Example

- (void) DoExample
{
    
    //Importio* client = [[Importio alloc] initWithUserIdAndApikey:@"1def8c28-7857-4cf5-acbc-257d605d7785" withApikey:@"8ciDZ9nkCwdLnfhPk22vr+8Xt3iMK/1a5jD59OuBIiLv934oSds2pD/x4/6CEKlPPh+KpxdUu3RpBNu4JfwVnw=="];
    
    // To log in with username and password
    Importio* client = [[Importio alloc] init];
    [client login:@"bam4d" withPassword:@"importiome123!" withHost:@"https://api.import.io"];
    
    
    void (^callback)(Query*,NSDictionary* data) = ^(Query* query, NSDictionary* data){

        if ([data[@"type"] isEqual: @"MESSAGE"]) {
            NSLog(@"results: %@",data);
        }
        if(query.finished) {
            NSLog(@"query is complete");
        }
        
    };
    
    [client connect];
    
    NSDictionary* input1 = @{@"input": @{@"query": @"mac mini"}, @"connectorGuids": @[@"39df3fe4-c716-478b-9b80-bdbee43bfbde"]};
    NSDictionary* input2 = @{@"input": @{@"query": @"ubuntu"}, @"connectorGuids": @[@"39df3fe4-c716-478b-9b80-bdbee43bfbde"]};
    NSDictionary* input3 = @{@"input": @{@"query": @"ibm"}, @"connectorGuids": @[@"39df3fe4-c716-478b-9b80-bdbee43bfbde"]};
    
    [client query:input1 withCallback:callback];
    [client query:input2 withCallback:callback];
    [client query:input3 withCallback:callback];
}

@end
