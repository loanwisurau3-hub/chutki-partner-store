package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.BuildConfig
import com.example.model.WebRtcIceCandidateData
import com.example.model.WebRtcSessionDescriptionData
import org.json.JSONArray
import org.json.JSONObject

interface WebRtcPeerCallbacks {
    fun onLocalDescription(description: WebRtcSessionDescriptionData)
    fun onLocalIceCandidate(candidate: WebRtcIceCandidateData)
    fun onConnectionStateChanged(state: String)
    fun onPeerConnected()
    fun onPeerFailure(reason: String)
}

interface WebRtcPeerEngine {
    fun startOutgoingOffer(callbacks: WebRtcPeerCallbacks)
    fun startIncomingAnswer(remoteOfferSdp: String, callbacks: WebRtcPeerCallbacks)
    fun applyRemoteAnswer(remoteAnswerSdp: String)
    fun addRemoteIceCandidate(candidate: WebRtcIceCandidateData)
    fun setMuted(muted: Boolean)
    fun setOnHold(onHold: Boolean)
    fun setSpeakerphone(enabled: Boolean)
    fun triggerIceRestart()
    fun startIncomingRingAlert()
    fun stopIncomingRingAlert()
    fun releaseSession()
}

/**
 * Real two-way WebRTC Voice Calling engine backed by Android's Chromium WebRTC stack
 * (`RTCPeerConnection`, `getUserMedia({ audio: true, video: false })`, STUN/TURN ICE negotiation)
 * combined with Android `AudioManager` voice communication routing and ringtone/vibration alerts.
 */
class AndroidWebRtcVoiceEngine(
    context: Context
) : WebRtcPeerEngine {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    private var activeCallbacks: WebRtcPeerCallbacks? = null

    private var webView: WebView? = null
    private var pageReady = false
    private val pendingJsQueue = mutableListOf<String>()

    private var activeRingtone: Ringtone? = null
    private var ringPulseToneGen: ToneGenerator? = null

    companion object {
        /**
         * Builds the JSON array of STUN and optional TURN servers without ever logging
         * or exposing TURN credentials in UI or logs.
         */
        fun buildIceServersJson(
            turnUrl: String = BuildConfig.WEBRTC_TURN_URL,
            turnUsername: String = BuildConfig.WEBRTC_TURN_USERNAME,
            turnCredential: String = BuildConfig.WEBRTC_TURN_CREDENTIAL
        ): String {
            val servers = JSONArray()
            servers.put(
                JSONObject().apply {
                    put(
                        "urls",
                        JSONArray().apply {
                            put("stun:stun.l.google.com:19302")
                            put("stun:stun1.l.google.com:19302")
                            put("stun:stun2.l.google.com:19302")
                        }
                    )
                }
            )
            val cleanTurn = turnUrl.trim()
            if (cleanTurn.isNotEmpty() &&
                (cleanTurn.startsWith("turn:", ignoreCase = true) ||
                    cleanTurn.startsWith("turns:", ignoreCase = true))
            ) {
                val turnObj = JSONObject().apply {
                    put("urls", cleanTurn)
                    if (turnUsername.isNotBlank()) put("username", turnUsername.trim())
                    if (turnCredential.isNotBlank()) put("credential", turnCredential.trim())
                }
                servers.put(turnObj)
            }
            return servers.toString()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebViewOnMain(onReady: () -> Unit) {
        mainHandler.post {
            if (webView != null && pageReady) {
                onReady()
                return@post
            }
            if (webView != null && !pageReady) {
                pendingJsQueue.add("__READY_CALLBACK__")
                return@post
            }

            runCatching {
                val wv = WebView(appContext)
                wv.settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                wv.webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return
                        // Grant ONLY audio capture for voice-only WebRTC calls; never grant video/camera.
                        val audioResources = request.resources.filter {
                            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                        }.toTypedArray()
                        if (audioResources.isNotEmpty()) {
                            request.grant(audioResources)
                        } else {
                            request.deny()
                        }
                    }
                }
                wv.addJavascriptInterface(JsBridge(), "AndroidWebRtcBridge")
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageReady = true
                        onReady()
                        val copy = pendingJsQueue.toList()
                        pendingJsQueue.clear()
                        copy.forEach { script ->
                            if (script != "__READY_CALLBACK__") {
                                wv.evaluateJavascript(script, null)
                            }
                        }
                    }
                }
                webView = wv
                wv.loadDataWithBaseURL(
                    "https://chutki-webrtc.local/",
                    buildWebRtcHostHtml(buildIceServersJson()),
                    "text/html",
                    "UTF-8",
                    null
                )
            }.onFailure { err ->
                activeCallbacks?.onPeerFailure("WebRTC audio initialization error: ${err.localizedMessage ?: "Unavailable"}")
            }
        }
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            val wv = webView
            if (wv != null && pageReady) {
                wv.evaluateJavascript(script, null)
            } else {
                pendingJsQueue.add(script)
            }
        }
    }

    override fun startOutgoingOffer(callbacks: WebRtcPeerCallbacks) {
        activeCallbacks = callbacks
        configureVoiceAudioMode(active = true)
        ensureWebViewOnMain {
            evalJs("window.ChutkiRtc && window.ChutkiRtc.startCallerOffer();")
        }
    }

    override fun startIncomingAnswer(remoteOfferSdp: String, callbacks: WebRtcPeerCallbacks) {
        activeCallbacks = callbacks
        stopIncomingRingAlert()
        configureVoiceAudioMode(active = true)
        val escapedSdp = JSONObject.quote(remoteOfferSdp)
        ensureWebViewOnMain {
            evalJs("window.ChutkiRtc && window.ChutkiRtc.startReceiverAnswer($escapedSdp);")
        }
    }

    override fun applyRemoteAnswer(remoteAnswerSdp: String) {
        val escapedSdp = JSONObject.quote(remoteAnswerSdp)
        evalJs("window.ChutkiRtc && window.ChutkiRtc.applyRemoteAnswer($escapedSdp);")
    }

    override fun addRemoteIceCandidate(candidate: WebRtcIceCandidateData) {
        val candJson = JSONObject().apply {
            put("candidate", candidate.candidate)
            put("sdpMid", candidate.sdpMid.ifBlank { "0" })
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }.toString()
        evalJs("window.ChutkiRtc && window.ChutkiRtc.addRemoteCandidate($candJson);")
    }

    override fun setMuted(muted: Boolean) {
        runCatching {
            audioManager?.isMicrophoneMute = muted
        }
        evalJs("window.ChutkiRtc && window.ChutkiRtc.setAudioMuted($muted);")
    }

    override fun setOnHold(onHold: Boolean) {
        evalJs("window.ChutkiRtc && window.ChutkiRtc.setCallHold($onHold);")
    }

    override fun setSpeakerphone(enabled: Boolean) {
        runCatching {
            val am = audioManager ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enabled) {
                    val speaker = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        am.setCommunicationDevice(speaker)
                    }
                } else {
                    am.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = enabled
            }
        }
    }

    override fun triggerIceRestart() {
        evalJs("window.ChutkiRtc && window.ChutkiRtc.restartIce();")
    }

    override fun startIncomingRingAlert() {
        mainHandler.post {
            runCatching {
                if (activeRingtone?.isPlaying == true) return@runCatching
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(appContext, uri)
                activeRingtone = ringtone
                ringtone?.play()
            }.onFailure {
                runCatching {
                    val tg = ringPulseToneGen ?: ToneGenerator(AudioManager.STREAM_RING, 90).also {
                        ringPulseToneGen = it
                    }
                    tg.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
                }
            }

            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator?.hasVibrator() == true) {
                    val pattern = longArrayOf(0L, 500L, 400L, 500L, 400L, 500L)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(pattern, 0)
                    }
                }
            }
        }
    }

    override fun stopIncomingRingAlert() {
        mainHandler.post {
            runCatching {
                activeRingtone?.stop()
                activeRingtone = null
            }
            runCatching {
                ringPulseToneGen?.stopTone()
            }
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.cancel()
            }
        }
    }

    override fun releaseSession() {
        stopIncomingRingAlert()
        activeCallbacks = null
        configureVoiceAudioMode(active = false)
        mainHandler.post {
            runCatching {
                webView?.evaluateJavascript("window.ChutkiRtc && window.ChutkiRtc.closeSession();", null)
            }
        }
    }

    private fun configureVoiceAudioMode(active: Boolean) {
        runCatching {
            val am = audioManager ?: return@runCatching
            if (active) {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isMicrophoneMute = false
            } else {
                am.isMicrophoneMute = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.clearCommunicationDevice()
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                }
                am.mode = AudioManager.MODE_NORMAL
            }
        }
    }

    private inner class JsBridge {
        @JavascriptInterface
        fun onLocalSdp(type: String, sdp: String) {
            activeCallbacks?.onLocalDescription(
                WebRtcSessionDescriptionData(type = type, sdp = sdp)
            )
        }

        @JavascriptInterface
        fun onIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int) {
            if (candidate.isBlank()) return
            activeCallbacks?.onLocalIceCandidate(
                WebRtcIceCandidateData(
                    candidate = candidate,
                    sdpMid = sdpMid ?: "0",
                    sdpMLineIndex = sdpMLineIndex
                )
            )
        }

        @JavascriptInterface
        fun onStateChange(state: String) {
            val normalized = state.uppercase()
            activeCallbacks?.onConnectionStateChanged(normalized)
            if (normalized == "CONNECTED" || normalized == "COMPLETED") {
                activeCallbacks?.onPeerConnected()
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            activeCallbacks?.onPeerFailure(message.ifBlank { "WebRTC peer connection failed" })
        }
    }

    private fun buildWebRtcHostHtml(iceServersJson: String): String = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8" /></head>
        <body>
        <audio id="remoteAudio" autoplay playsinline></audio>
        <script>
        (function() {
          const ICE_SERVERS = $iceServersJson;
          let pc = null;
          let localStream = null;
          let remoteDescReady = false;
          let pendingCandidates = [];
          let connectedNotified = false;

          async function acquireAudioStream() {
            if (localStream) return localStream;
            localStream = await navigator.mediaDevices.getUserMedia({
              audio: {
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true
              },
              video: false
            });
            return localStream;
          }

          function createPeer() {
            closeSession();
            pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
            remoteDescReady = false;
            pendingCandidates = [];
            connectedNotified = false;

            pc.onicecandidate = function(event) {
              if (event.candidate && event.candidate.candidate) {
                AndroidWebRtcBridge.onIceCandidate(
                  event.candidate.candidate,
                  event.candidate.sdpMid || "0",
                  event.candidate.sdpMLineIndex || 0
                );
              }
            };

            pc.ontrack = function(event) {
              const audioEl = document.getElementById("remoteAudio");
              if (audioEl && event.streams && event.streams[0]) {
                audioEl.srcObject = event.streams[0];
                audioEl.play().catch(function(){});
              }
            };

            pc.onconnectionstatechange = function() {
              if (!pc) return;
              const st = pc.connectionState || "unknown";
              AndroidWebRtcBridge.onStateChange(st);
              if (st === "connected" && !connectedNotified) {
                connectedNotified = true;
                AndroidWebRtcBridge.onStateChange("CONNECTED");
              } else if (st === "failed") {
                AndroidWebRtcBridge.onError("Peer connection failed");
              }
            };

            pc.oniceconnectionstatechange = function() {
              if (!pc) return;
              const ice = pc.iceConnectionState || "";
              if ((ice === "connected" || ice === "completed") && !connectedNotified) {
                connectedNotified = true;
                AndroidWebRtcBridge.onStateChange("CONNECTED");
              } else if (ice === "disconnected") {
                AndroidWebRtcBridge.onStateChange("RECONNECTING");
                try { pc.restartIce(); } catch (e) {}
              } else if (ice === "failed") {
                AndroidWebRtcBridge.onError("ICE connection failed");
              }
            };

            return pc;
          }

          async function flushPendingCandidates() {
            if (!pc || !remoteDescReady) return;
            while (pendingCandidates.length > 0) {
              const item = pendingCandidates.shift();
              try {
                await pc.addIceCandidate(new RTCIceCandidate(item));
              } catch (e) {}
            }
          }

          async function startCallerOffer() {
            try {
              const peer = createPeer();
              const stream = await acquireAudioStream();
              stream.getAudioTracks().forEach(function(track) {
                peer.addTrack(track, stream);
              });
              const offer = await peer.createOffer({
                offerToReceiveAudio: true,
                offerToReceiveVideo: false
              });
              await peer.setLocalDescription(offer);
              AndroidWebRtcBridge.onLocalSdp("offer", offer.sdp);
            } catch (err) {
              AndroidWebRtcBridge.onError((err && err.message) ? err.message : "Failed to create audio offer");
            }
          }

          async function startReceiverAnswer(offerSdp) {
            try {
              const peer = createPeer();
              const stream = await acquireAudioStream();
              stream.getAudioTracks().forEach(function(track) {
                peer.addTrack(track, stream);
              });
              await peer.setRemoteDescription(new RTCSessionDescription({
                type: "offer",
                sdp: offerSdp
              }));
              remoteDescReady = true;
              await flushPendingCandidates();
              const answer = await peer.createAnswer();
              await peer.setLocalDescription(answer);
              AndroidWebRtcBridge.onLocalSdp("answer", answer.sdp);
            } catch (err) {
              AndroidWebRtcBridge.onError((err && err.message) ? err.message : "Failed to create audio answer");
            }
          }

          async function applyRemoteAnswer(answerSdp) {
            try {
              if (!pc) return;
              if (pc.signalingState === "stable") return;
              await pc.setRemoteDescription(new RTCSessionDescription({
                type: "answer",
                sdp: answerSdp
              }));
              remoteDescReady = true;
              await flushPendingCandidates();
            } catch (err) {
              AndroidWebRtcBridge.onError((err && err.message) ? err.message : "Failed to apply remote answer");
            }
          }

          async function addRemoteCandidate(candObj) {
            try {
              if (!pc) {
                pendingCandidates.push(candObj);
                return;
              }
              if (!remoteDescReady) {
                pendingCandidates.push(candObj);
                return;
              }
              await pc.addIceCandidate(new RTCIceCandidate(candObj));
            } catch (e) {}
          }

          function setAudioMuted(muted) {
            if (!localStream) return;
            localStream.getAudioTracks().forEach(function(t) {
              t.enabled = !muted;
            });
          }

          function setCallHold(onHold) {
            if (localStream) {
              localStream.getAudioTracks().forEach(function(t) {
                t.enabled = !onHold;
              });
            }
            const audioEl = document.getElementById("remoteAudio");
            if (audioEl) {
              audioEl.muted = !!onHold;
            }
          }

          function restartIce() {
            if (pc && typeof pc.restartIce === "function") {
              try { pc.restartIce(); } catch (e) {}
            }
          }

          function closeSession() {
            remoteDescReady = false;
            pendingCandidates = [];
            connectedNotified = false;
            if (pc) {
              try { pc.onicecandidate = null; pc.ontrack = null; pc.close(); } catch (e) {}
              pc = null;
            }
            if (localStream) {
              try {
                localStream.getTracks().forEach(function(t) { t.stop(); });
              } catch (e) {}
              localStream = null;
            }
            const audioEl = document.getElementById("remoteAudio");
            if (audioEl) {
              audioEl.srcObject = null;
            }
          }

          window.ChutkiRtc = {
            startCallerOffer: startCallerOffer,
            startReceiverAnswer: startReceiverAnswer,
            applyRemoteAnswer: applyRemoteAnswer,
            addRemoteCandidate: addRemoteCandidate,
            setAudioMuted: setAudioMuted,
            setCallHold: setCallHold,
            restartIce: restartIce,
            closeSession: closeSession
          };
        })();
        </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Deterministic fallback WebRTC peer engine used when no Android Activity WebView context is
 * attached (e.g., headless JVM unit tests or Robolectric tests where ShadowWebView does not run JS).
 */
class DefaultWebRtcPeerEngine(
    private val autoConnectOnAnswer: Boolean = true
) : WebRtcPeerEngine {

    var currentCallbacks: WebRtcPeerCallbacks? = null
        private set
    var isMuted: Boolean = false
        private set
    var isOnHold: Boolean = false
        private set
    var isSpeakerphoneOn: Boolean = false
        private set
    var isRingingAlertActive: Boolean = false
        private set
    var sessionReleasedCount: Int = 0
        private set

    override fun startOutgoingOffer(callbacks: WebRtcPeerCallbacks) {
        currentCallbacks = callbacks
        callbacks.onConnectionStateChanged("connecting")
        callbacks.onLocalDescription(
            WebRtcSessionDescriptionData(
                type = "offer",
                sdp = "v=0\r\no=- 1001 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n"
            )
        )
        callbacks.onLocalIceCandidate(
            WebRtcIceCandidateData(
                candidate = "candidate:1 1 UDP 2122260223 192.0.2.1 54400 typ host",
                sdpMid = "0",
                sdpMLineIndex = 0
            )
        )
    }

    override fun startIncomingAnswer(remoteOfferSdp: String, callbacks: WebRtcPeerCallbacks) {
        currentCallbacks = callbacks
        callbacks.onConnectionStateChanged("connecting")
        callbacks.onLocalDescription(
            WebRtcSessionDescriptionData(
                type = "answer",
                sdp = "v=0\r\no=- 2002 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n"
            )
        )
        callbacks.onLocalIceCandidate(
            WebRtcIceCandidateData(
                candidate = "candidate:2 1 UDP 2122260223 192.0.2.2 54401 typ host",
                sdpMid = "0",
                sdpMLineIndex = 0
            )
        )
        if (autoConnectOnAnswer) {
            callbacks.onConnectionStateChanged("connected")
            callbacks.onPeerConnected()
        }
    }

    override fun applyRemoteAnswer(remoteAnswerSdp: String) {
        if (remoteAnswerSdp.isNotBlank() && autoConnectOnAnswer) {
            currentCallbacks?.onConnectionStateChanged("connected")
            currentCallbacks?.onPeerConnected()
        }
    }

    override fun addRemoteIceCandidate(candidate: WebRtcIceCandidateData) {
        // Accepted into peer ice state
    }

    override fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    override fun setOnHold(onHold: Boolean) {
        this.isOnHold = onHold
    }

    override fun setSpeakerphone(enabled: Boolean) {
        isSpeakerphoneOn = enabled
    }

    override fun triggerIceRestart() {
        currentCallbacks?.onConnectionStateChanged("reconnecting")
    }

    override fun startIncomingRingAlert() {
        isRingingAlertActive = true
    }

    override fun stopIncomingRingAlert() {
        isRingingAlertActive = false
    }

    override fun releaseSession() {
        isRingingAlertActive = false
        isMuted = false
        isOnHold = false
        currentCallbacks = null
        sessionReleasedCount++
    }
}

