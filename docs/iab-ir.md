# Iranian App Store IAB Emulation Specification (P-IAB-IR, Part 9 & Part 23)

HEXFORGE treats Iranian Android ecosystems as first-class citizens (`PD-9`). Module `M2 PatchForge` (Milestone M5) and `M14 Plugins` implement an extensible `IabProviderAdapter` registry for local offline billing emulation.

## Supported Built-In Adapters (Planned M5)
1. **Cafe Bazaar (`com.farsitel.bazaar`)**
   - AIDL Interface: `com.bazaar.IInAppBillingService`
   - Intercepted Methods: `isBillingSupported`, `getSkuDetails`, `getBuyIntent`, `getPurchases`, `consumePurchase`, `getBuyIntentToReplaceSkus`
   - Local Emulation: Returns `RESPONSE_CODE = 0` (`BILLING_RESPONSE_RESULT_OK`), constructs valid RTL-friendly purchase confirmation dialog, echoes `developerPayload`, and neutralizes local RSA signature verification when needed for user-owned offline games.
2. **Myket (`ir.mservices.myket`)**
   - AIDL Interface: `com.myket.IInAppBillingService`
   - Intercepted Methods: Same AIDL v3 surface + Myket `PurchaseData` JSON schema (`PURCHASE_STATE = 0`), local `Security.verifyPurchase` bypass.

## Custom Store Adapter SDK (`< 100 lines`)
Third-party or community adapters register an `IabAdapterManifest`:
- Target store package name
- AIDL interface descriptor
- Response-code mapping table
- Purchase JSON template & RSA verification hook signature
