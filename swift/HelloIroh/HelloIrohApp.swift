//
//  HelloIrohApp.swift
//  HelloIroh
//
//  Created by Rae McKelvey on 5/15/26.
//

import SwiftUI
import Foundation

@main
struct HelloIrohApp: App {
    init() {
        NSLog("[HelloIroh] checkpoint A: HelloIrohApp.init")
    }

    var body: some Scene {
        let _ = NSLog("[HelloIroh] checkpoint B: HelloIrohApp.body")
        WindowGroup {
            ContentView()
        }
    }
}
