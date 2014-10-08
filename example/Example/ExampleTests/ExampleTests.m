//
//  ExampleTests.m
//  ExampleTests
//
//  Created by Rene Pirringer on 06.02.14.
//  Copyright (c) 2014 Rene Pirringer. All rights reserved.
//

#import <XCTest/XCTest.h>

@interface ExampleTests : XCTestCase

@end

@implementation ExampleTests

- (void)setUp
{
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown
{
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testExample
{
	XCTFail(@"fail");
	//XCTAssert(YES, @"this test should not fail");
}

@end
