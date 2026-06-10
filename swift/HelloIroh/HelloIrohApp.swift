//
//  HelloIrohApp.swift
//  HelloIroh
//
//  Created by Rae McKelvey on 5/15/26.
//

import SwiftUI

@main
struct HelloIrohApp: App {
    init() {
        print("[HelloIroh] checkpoint A: HelloIrohApp.init")
    }

    var body: some Scene {
        let _ = print("[HelloIroh] checkpoint B: HelloIrohApp.body")
        WindowGroup {
            ContentView()
        }
    }
}
