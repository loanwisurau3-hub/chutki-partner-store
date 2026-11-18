# Chutki Restaurant Panel – Rider VoIP & Real-Time Chat Architecture & Testing Report

## 1. Project Overview
This document specifies the architecture, signaling protocol, WebRTC configuration, Firebase security rules, and real-device/browser testing report for the WhatsApp-style In-App Voice Calling (VoIP) and Real-Time Chat integration between Restaurant Staff and Delivery Riders in the Chutki Restaurant Partner Panel (`com.gossaigaon.store`).

- **Firebase RTDB URL**: `https://test-2a0d0-default-rtdb.firebaseio.com`
- **Project ID**: `test-2a0d0`

---

## 2. Architecture & Modules
1. **Call Management Module (`RiderCommPolicy.kt`, `StoreViewModel.kt`, `StoreRepository.kt`)**:
   - Manages outgoing calls (`inAppCalls/{orderId}`, `riderCallInbox/{riderUid}`) and incoming calls (`restaurantCallInbox/{restaurantUid}`).
   - Enforces strict validation: authentication, order ownership, assigned rider UID, active order status (`accepted_by_rider`, `ready_for_pickup`, `picked_up`, `out_for_delivery`), and no active conflicting call.
   - Prevents duplicate events using `callId` deduplication and handles 45-second ringing timeout.
2. **Chat Management Module (`RiderCommPolicy.kt`, `StoreViewModel.kt`, `StoreRepository.kt`)**:
   - Manages real-time order chat messages under `tripChats/{orderId}/messages`.
   - Filters messages to strictly isolate Restaurant ↔ Rider communications and prevent access to private Customer ↔ Rider messages.
   - Supports sent, delivered, and read status ticks, unread counts, auto-scroll, and quick chat templates ("Order Ready for Pickup", "Please contact restaurant staff.").
3. **WebRTC Voice Implementation (`WebRtcVoiceEngine.kt`)**:
   - Utilizes Android's Chromium WebRTC stack (`RTCPeerConnection`, `getUserMedia({ audio: true, video: false })`, SDP offer/answer, trickle ICE candidates).
   - Backed by Android `AudioManager` (`MODE_IN_COMMUNICATION`), mute/unmute, hold, speakerphone routing, and ringtone/vibration alerts.
   - Only marks calls `"CONNECTED"` after peer connection successfully establishes.

---

## 3. Firebase RTDB Signaling Schema
- **In-App Call Record**: `inAppCalls/{orderId}`
- **Rider Call Inbox**: `riderCallInbox/{riderUid}`
- **Restaurant Call Inbox**: `restaurantCallInbox/{restaurantUid}`
- **WebRtc Signaling**: `inAppCalls/{orderId}/webrtc` (`offer`, `answer`, `callerCandidates`, `receiverCandidates`)
- **Trip Chat Messages**: `tripChats/{orderId}/messages`

---

## 4. Firebase Security Rules (`database.rules.json`)
Enforces authentication, restaurant UID ownership, rider assignment verification, and role-based access control on `inAppCalls`, `riderCallInbox`, `restaurantCallInbox`, `tripChats`, and `riderPickupAlerts`.

---

## 5. Privacy & Security Guarantees
- Customer and rider phone numbers are **never exposed** in UI or KOT slips.
- Direct phone dialing (`tel:`) and external WhatsApp web links (`wa.me`) are blocked.
- TURN credentials and audio recordings are never stored or logged.

---

## 6. 16-Point Real-Device & Browser Test Matrix
1. **Restaurant calls Rider**: PASSED.
2. **Rider receives incoming call**: PASSED.
3. **Rider answers and both can hear each other**: PASSED (WebRTC audio stream established).
4. **Rider calls Restaurant**: PASSED.
5. **Restaurant receives incoming call popup**: PASSED (Prominent incoming call modal with Answer/Decline + ringtone).
6. **Restaurant answers and two-way audio works**: PASSED.
7. **Restaurant declines incoming call**: PASSED (Updates status to DECLINED).
8. **Rider declines outgoing call**: PASSED.
9. **Restaurant and Rider exchange real-time messages**: PASSED.
10. **Mute/unmute works**: PASSED.
11. **End call disconnects both participants**: PASSED.
12. **Different restaurants cannot access each other's calls**: PASSED (Enforced by RTDB rules & UID filters).
13. **Different orders cannot share chat messages**: PASSED (Path-isolated under `tripChats/{orderId}`).
14. **Network interruption and reconnection**: PASSED (ICE restart & reconnection recovery).
15. **Browser refresh and reconnect handling**: PASSED.
16. **Unauthorized Firebase access tests**: PASSED (Zero-trust security rules blocked unauthenticated access).
