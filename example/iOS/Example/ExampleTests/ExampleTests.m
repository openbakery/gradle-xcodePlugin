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

- (void)setUp {
	[super setUp];
}

- (void)tearDown {
	[super tearDown];
}

- (void)testExample {
	//XCTFail(@"fail");
	XCTAssert(YES, @"this test should not fail");
}

- (void)testSkip {
	XCTSkip(@"This test is skipped");
}

@end
