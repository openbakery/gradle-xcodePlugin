//
//  main.swift
//  Echo
//
//  Created by Rene Pirringer on 19/12/15.
//  Copyright © 2015 Rene Pirringer. All rights reserved.
//

import Foundation


var result = ""
for (var i=1; i<Process.arguments.count; i++) {
	if (i>1) {
		result = result + " "
	}
	result = result + Process.arguments[i]
}

print(result)