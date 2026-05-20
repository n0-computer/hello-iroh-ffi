import Foundation
import IrohLib

struct IdentityStore {
    private static let key = "iroh.helloiroh.secretKey"

    let secretKey: SecretKey
    let endpointId: String

    static func loadOrCreate(defaults: UserDefaults = .standard) -> IdentityStore {
        let secret: SecretKey
        if let b64 = defaults.string(forKey: key),
           let bytes = Data(base64Encoded: b64),
           let parsed = try? SecretKey.fromBytes(bytes: bytes) {
            secret = parsed
        } else {
            secret = SecretKey.generate()
            defaults.set(secret.toBytes().base64EncodedString(), forKey: key)
        }
        return IdentityStore(secretKey: secret, endpointId: secret.public().description)
    }
}
