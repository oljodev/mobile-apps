import AVFoundation
import Foundation
import Shared

/// En samtale med Gemini Live: mikrofonen strømmes til Gemini, og svaret spilles av.
///
/// Lyden går gjennom iOS sin ekkodemping, så Kompis ikke hører seg selv. Appen har lov til å bruke
/// lyd i bakgrunnen, så samtalen fortsetter når skjermen er låst.
final class LiveSession {
    static let shared = LiveSession()

    private struct Config: Decodable {
        let apiKey: String
        let model: String
        let voice: String
        let systemInstruction: String
        var history: [Turn]

        struct Turn: Decodable {
            let role: String
            let text: String
        }
    }

    private var config: Config?
    private var listener: LiveListener?

    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    /// Formatet Gemini sender: 24 kHz, 16-bit, mono. Spilles av som float.
    private let playbackFormat = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: 24_000, channels: 1, interleaved: false)!
    /// Formatet Gemini vil ha: 16 kHz, 16-bit, mono.
    private let sendFormat = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: 16_000, channels: 1, interleaved: true)!
    private var converter: AVAudioConverter?

    private var socket: URLSessionWebSocketTask?
    /// Øker for hver tilkobling, så meldinger fra en gammel tilkobling kan ignoreres.
    private var connection = 0
    private var isReady = false
    private(set) var isRunning = false
    private var isMuted = false
    private var resumptionHandle: String?
    private var reconnectAttempts = 0

    private var pendingBuffers = 0
    private var turnCompleteWhilePlaying = false
    private var isSpeaking = false
    private var lastLevelUpdate = Date.distantPast

    // Taledeteksjon. All lyd som sendes til Gemini koster penger, også stillhet, så bare lyd der
    // noen snakker sendes. Brukes bare fra lydtråden.
    private var noiseFloor: Float = 0.004
    private var isSendingSpeech = false
    private var silentSeconds: Double = 0
    /// Lyden rett før man begynte å snakke, så første stavelse ikke forsvinner.
    private var preRoll: [Data] = []
    private var preRollSeconds: Double = 0
    /// Hvor lenge det må være stille før Gemini får beskjed om at man er ferdig.
    private static let hangoverSeconds = 1.2
    private static let maxPreRollSeconds = 0.4
    /// Lydnivået tale må over, uansett hvor stille det er rundt.
    private static let minSpeechLevel: Float = 0.006

    // Selvreparasjon og diagnose.
    /// Når man sist var ferdig med å snakke uten at Gemini har svart, eller `nil`.
    private var awaitingResponseSince: Date?
    private var watchdog: Timer?
    /// Hvor lenge Kompis venter på svar før den kobler til igjen.
    private static let responseTimeout: TimeInterval = 12
    private var speechSecondsSent: Double = 0
    private var serverMessages = 0
    private var lastEvent = "starter"
    private var lastEventAt = Date()
    /// Hvor mange lydbiter mikrofonen har gitt, og det høyeste nivået, for å se om den virker.
    private var micBuffers = 0
    private var micPeak: Float = 0
    private var micRestarted = false

    // Tekstminne. Lyd i samtalen faktureres på nytt for hver tur, så Kompis starter en ny økt med
    // jevne mellomrom og sender samtalen så langt som tekst, som er mye billigere.
    /// Samtalen så langt, for neste økt. Brukes så snart det er stille.
    private var pendingHistory: [Config.Turn]?
    /// Lyd som tas opp mens Kompis bytter økt. Sendes når den nye økten er klar.
    private let bufferLock = NSLock()
    private var isRolling = false
    private var bufferedAudio: [Data] = []
    private var bufferedStreamEnd = false
    /// Omtrent 15 sekunder med lyd.
    private static let maxBufferedChunks = 350

    // MARK: - Start og stopp

    func start(configJson: String, listener: LiveListener) {
        stop()
        guard let config = try? JSONDecoder().decode(Config.self, from: Data(configJson.utf8)) else {
            listener.onError(message: "Kunne ikke lese innstillingene for samtalen.")
            return
        }
        self.config = config
        self.listener = listener
        isRunning = true
        isMuted = false
        resumptionHandle = nil
        reconnectAttempts = 0
        speechSecondsSent = 0
        serverMessages = 0
        awaitingResponseSince = nil
        // Taledeteksjonen begynner på nytt, så støy fra forrige samtale ikke gjør at tale blir oversett.
        noiseFloor = 0.004
        silentSeconds = 0
        micBuffers = 0
        micPeak = 0
        micRestarted = false
        note("starter")
        listener.onStatus(status: "connecting")

        Task { @MainActor in
            guard await AVAudioApplication.requestRecordPermission() else {
                self.fail("Kompis trenger tilgang til mikrofonen. Slå det på i Innstillinger → Apper → Salat.")
                return
            }
            do {
                try self.startAudio()
            } catch {
                self.fail("Kunne ikke starte lyden: \(error.localizedDescription)")
                return
            }
            self.connect()
        }
    }

    func stop() {
        guard isRunning else { return }
        isRunning = false
        isReady = false
        connection += 1
        socket?.cancel(with: .normalClosure, reason: nil)
        socket = nil
        engine.inputNode.removeTap(onBus: 0)
        player.stop()
        engine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        NotificationCenter.default.removeObserver(self)
        watchdog?.invalidate()
        watchdog = nil
        pendingHistory = nil
        bufferLock.lock()
        isRolling = false
        bufferedAudio = []
        bufferedStreamEnd = false
        bufferLock.unlock()
        pendingBuffers = 0
        turnCompleteWhilePlaying = false
        isSpeaking = false
        isSendingSpeech = false
        preRoll = []
        preRollSeconds = 0
    }

    func setMuted(_ muted: Bool) {
        isMuted = muted
        isSendingSpeech = false
        if muted, isReady {
            send(["realtimeInput": ["audioStreamEnd": true]])
        }
    }

    private func fail(_ message: String) {
        let listener = self.listener
        stop()
        listener?.onError(message: message)
    }

    // MARK: - Lyd

    private func startAudio() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(
            .playAndRecord,
            mode: .voiceChat,
            options: [.allowBluetoothHFP, .allowBluetoothA2DP, .defaultToSpeaker]
        )
        try session.setActive(true)

        let input = engine.inputNode
        // Ekkodemping, så Kompis ikke hører sin egen stemme fra høyttaleren.
        try input.setVoiceProcessingEnabled(true)

        if player.engine == nil {
            engine.attach(player)
        }
        engine.connect(player, to: engine.mainMixerNode, format: playbackFormat)

        let inputFormat = input.outputFormat(forBus: 0)
        converter = AVAudioConverter(from: inputFormat, to: sendFormat)
        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 2048, format: inputFormat) { [weak self] buffer, _ in
            self?.handleMicrophone(buffer)
        }

        engine.prepare()
        try engine.start()
        player.play()
        // Med ekkodemping gir mikrofonen av og til bare stillhet til noe har vært spilt av. Et lite
        // øyeblikk med stillhet får lyden i gang, også når Kompis ikke sier noe først.
        if let silence = AVAudioPCMBuffer(pcmFormat: playbackFormat, frameCapacity: 2400) {
            silence.frameLength = 2400
            silence.floatChannelData?[0].update(repeating: 0, count: 2400)
            player.scheduleBuffer(silence)
        }

        NotificationCenter.default.removeObserver(self)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(routeChanged),
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(interrupted(_:)),
            name: AVAudioSession.interruptionNotification,
            object: nil
        )
        reportRoute()
    }

    private func handleMicrophone(_ buffer: AVAudioPCMBuffer) {
        micBuffers += 1
        guard isReady || isRolling, !isMuted, let converter else { return }

        let ratio = sendFormat.sampleRate / buffer.format.sampleRate
        let capacity = AVAudioFrameCount(Double(buffer.frameLength) * ratio) + 32
        guard let converted = AVAudioPCMBuffer(pcmFormat: sendFormat, frameCapacity: capacity) else { return }
        var consumed = false
        var error: NSError?
        converter.convert(to: converted, error: &error) { _, status in
            if consumed {
                status.pointee = .noDataNow
                return nil
            }
            consumed = true
            status.pointee = .haveData
            return buffer
        }
        guard error == nil, converted.frameLength > 0, let samples = converted.int16ChannelData else { return }

        let frames = Int(converted.frameLength)
        let data = Data(bytes: samples[0], count: frames * 2)
        let seconds = Double(frames) / sendFormat.sampleRate
        let level = rms(int16: samples[0], count: frames)
        micPeak = max(micPeak, level)
        if !isSpeaking {
            reportLevel(level)
        }

        // Tale er tydelig høyere enn bakgrunnsstøyen, som følges med på hele tiden.
        let isSpeech = level > max(noiseFloor * 2.5, Self.minSpeechLevel)
        if !isSpeech {
            noiseFloor = noiseFloor * 0.97 + level * 0.03
        }

        if isSpeech {
            silentSeconds = 0
            if !isSendingSpeech {
                isSendingSpeech = true
                for chunk in preRoll {
                    sendAudio(chunk)
                }
                preRoll = []
                preRollSeconds = 0
                DispatchQueue.main.async {
                    self.awaitingResponseSince = nil
                    self.note("du snakker")
                }
            }
            sendAudio(data)
            speechSecondsSent += seconds
        } else if isSendingSpeech {
            // Litt stillhet etter tale sendes med, så Gemini hører at setningen er ferdig.
            silentSeconds += seconds
            sendAudio(data)
            if silentSeconds >= Self.hangoverSeconds {
                isSendingSpeech = false
                sendStreamEnd()
                DispatchQueue.main.async {
                    self.awaitingResponseSince = Date()
                    self.note("venter på svar")
                }
            }
        } else {
            preRoll.append(data)
            preRollSeconds += seconds
            while preRollSeconds > Self.maxPreRollSeconds, !preRoll.isEmpty {
                preRollSeconds -= Double(preRoll.removeFirst().count / 2) / sendFormat.sampleRate
            }
        }
    }

    private func sendAudio(_ data: Data) {
        bufferLock.lock()
        if isRolling {
            if bufferedAudio.count < Self.maxBufferedChunks {
                bufferedAudio.append(data)
            }
            bufferLock.unlock()
            return
        }
        bufferLock.unlock()
        send(["realtimeInput": ["audio": ["data": data.base64EncodedString(), "mimeType": "audio/pcm;rate=16000"]]])
    }

    private func sendStreamEnd() {
        bufferLock.lock()
        if isRolling {
            bufferedStreamEnd = true
            bufferLock.unlock()
            return
        }
        bufferLock.unlock()
        send(["realtimeInput": ["audioStreamEnd": true]])
    }

    // MARK: - Tekstminne

    /// Starter en ny økt med [historyJson] som tekst, så snart det er stille.
    func roll(historyJson: String) {
        guard isRunning,
              let turns = try? JSONDecoder().decode([Config.Turn].self, from: Data(historyJson.utf8)) else { return }
        pendingHistory = turns
        rollIfIdle()
    }

    private func rollIfIdle() {
        guard let history = pendingHistory, isRunning, isReady, !isSpeaking, !isSendingSpeech,
              awaitingResponseSince == nil, pendingBuffers == 0 else { return }
        pendingHistory = nil
        config?.history = history
        // Uten håndtak starter Gemini en ny økt, og får samtalen som tekst i stedet for lyd.
        resumptionHandle = nil
        bufferLock.lock()
        isRolling = true
        bufferedAudio = []
        bufferedStreamEnd = false
        bufferLock.unlock()
        note("ny økt med tekstminne")
        socket?.cancel(with: .normalClosure, reason: nil)
        connect()
    }

    /// Sender lyden som ble tatt opp mens Kompis byttet økt.
    private func flushBufferedAudio() {
        bufferLock.lock()
        let audio = bufferedAudio
        let streamEnd = bufferedStreamEnd
        isRolling = false
        bufferedAudio = []
        bufferedStreamEnd = false
        bufferLock.unlock()
        for chunk in audio {
            sendAudio(chunk)
        }
        if streamEnd {
            sendStreamEnd()
            awaitingResponseSince = Date()
        }
    }

    // MARK: - Selvreparasjon og diagnose

    /// Kobler til igjen hvis Gemini ikke svarer etter at man er ferdig med å snakke.
    private func startWatchdog() {
        watchdog?.invalidate()
        watchdog = Timer.scheduledTimer(withTimeInterval: 2, repeats: true) { [weak self] _ in
            guard let self, self.isRunning, self.isReady else { return }
            self.rollIfIdle()
            guard let since = self.awaitingResponseSince else { return }
            if Date().timeIntervalSince(since) > Self.responseTimeout {
                self.awaitingResponseSince = nil
                self.note("fikk ikke svar, kobler til igjen")
                self.reconnect()
            }
        }
    }

    /// Husker hva som skjedde sist, og sender en kort oppsummering til Kotlin.
    private func note(_ event: String) {
        lastEvent = event
        lastEventAt = Date()
        reportDebug()
    }

    private func reportDebug() {
        let ago = Int(Date().timeIntervalSince(lastEventAt))
        let microphone = micBuffers == 0 ? "ingen lyd" : micPeak > 0 ? "ok" : "bare stillhet"
        let text = "Mikrofon: \(microphone) · \(Int(speechSecondsSent)) s tale sendt · \(serverMessages) meldinger fra Gemini · sist: \(lastEvent) (\(ago) s siden)"
        listener?.onDebug(text: text)
    }

    /// Spiller av en bit av svaret fra Gemini (16-bit PCM, 24 kHz).
    private func play(_ data: Data) {
        let count = data.count / 2
        guard count > 0, let buffer = AVAudioPCMBuffer(pcmFormat: playbackFormat, frameCapacity: AVAudioFrameCount(count)) else { return }
        buffer.frameLength = AVAudioFrameCount(count)
        let output = buffer.floatChannelData![0]
        data.withUnsafeBytes { raw in
            let samples = raw.bindMemory(to: Int16.self)
            for i in 0..<count {
                output[i] = Float(Int16(littleEndian: samples[i])) / 32768
            }
        }
        pendingBuffers += 1
        if !isSpeaking {
            isSpeaking = true
            listener?.onStatus(status: "speaking")
        }
        let level = rms(float: output, count: count)
        player.scheduleBuffer(buffer, completionCallbackType: .dataPlayedBack) { [weak self] _ in
            DispatchQueue.main.async { self?.bufferPlayed() }
        }
        reportLevel(level)
    }

    private func bufferPlayed() {
        // Avspillingen stoppes også når samtalen avsluttes. Da skal ikke status endres.
        guard isRunning else { return }
        pendingBuffers = max(0, pendingBuffers - 1)
        if pendingBuffers == 0, turnCompleteWhilePlaying {
            turnCompleteWhilePlaying = false
            finishSpeaking()
        }
    }

    private func finishSpeaking() {
        guard isSpeaking else { return }
        isSpeaking = false
        listener?.onLevel(level: 0)
        listener?.onStatus(status: "listening")
        rollIfIdle()
    }

    /// Brukeren avbrøt: kast det som ikke er spilt av ennå.
    private func clearPlayback() {
        player.stop()
        player.play()
        pendingBuffers = 0
        turnCompleteWhilePlaying = false
        finishSpeaking()
    }

    private func reportLevel(_ level: Float) {
        let now = Date()
        guard now.timeIntervalSince(lastLevelUpdate) > 0.06 else { return }
        lastLevelUpdate = now
        // Taletale ligger rundt 0.02–0.2 i RMS, så skaler opp og kapp av.
        let scaled = min(1, sqrt(level) * 2.2)
        DispatchQueue.main.async { self.listener?.onLevel(level: scaled) }
    }

    private func rms(int16 samples: UnsafePointer<Int16>, count: Int) -> Float {
        guard count > 0 else { return 0 }
        var sum: Float = 0
        for i in 0..<count {
            let v = Float(samples[i]) / 32768
            sum += v * v
        }
        return (sum / Float(count)).squareRoot()
    }

    private func rms(float samples: UnsafePointer<Float>, count: Int) -> Float {
        guard count > 0 else { return 0 }
        var sum: Float = 0
        for i in 0..<count {
            sum += samples[i] * samples[i]
        }
        return (sum / Float(count)).squareRoot()
    }

    @objc private func routeChanged() {
        DispatchQueue.main.async { self.reportRoute() }
    }

    private func reportRoute() {
        let output = AVAudioSession.sharedInstance().currentRoute.outputs.first
        let name: String
        switch output?.portType {
        case .builtInSpeaker: name = "Høyttaler"
        case .builtInReceiver: name = "iPhone"
        default: name = output?.portName ?? "iPhone"
        }
        listener?.onAudioRoute(name: name)
    }

    @objc private func interrupted(_ notification: Notification) {
        guard let raw = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: raw) else { return }
        DispatchQueue.main.async {
            switch type {
            case .began:
                self.listener?.onStatus(status: "reconnecting")
            case .ended:
                // Etter f.eks. en telefonsamtale: start lyden igjen.
                try? AVAudioSession.sharedInstance().setActive(true)
                try? self.engine.start()
                self.player.play()
                self.listener?.onStatus(status: "listening")
            @unknown default:
                break
            }
        }
    }

    // MARK: - Mikrofonsjekk

    /// Sjekker at mikrofonen faktisk gir lyd. Gjør den ikke det, startes lyden på nytt én gang.
    private func checkMicrophone(after delay: TimeInterval) {
        let current = connection
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            guard let self, self.isRunning, current == self.connection, !self.isMuted else { return }
            let working = self.micBuffers > 0 && self.micPeak > 0
            if working {
                return
            }
            if self.micRestarted {
                self.note(self.micBuffers == 0 ? "mikrofonen gir ingen lyd" : "mikrofonen gir bare stillhet")
                return
            }
            self.micRestarted = true
            self.note(self.micBuffers == 0 ? "ingen lyd fra mikrofonen, starter den på nytt" : "bare stillhet fra mikrofonen, starter den på nytt")
            self.restartAudio()
            self.checkMicrophone(after: 2.5)
        }
    }

    private func restartAudio() {
        engine.inputNode.removeTap(onBus: 0)
        player.stop()
        engine.stop()
        micBuffers = 0
        micPeak = 0
        do {
            try startAudio()
        } catch {
            fail("Kunne ikke starte lyden: \(error.localizedDescription)")
        }
    }

    // MARK: - Tilkobling

    private func connect() {
        guard isRunning, let config else { return }
        connection += 1
        let current = connection
        isReady = false

        var components = URLComponents(string: "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent")!
        components.queryItems = [URLQueryItem(name: "key", value: config.apiKey)]
        let socket = URLSession.shared.webSocketTask(with: components.url!)
        socket.maximumMessageSize = 16 * 1024 * 1024
        self.socket = socket
        socket.resume()

        var setup: [String: Any] = [
            "model": "models/\(config.model)",
            "generationConfig": [
                "responseModalities": ["AUDIO"],
                "speechConfig": ["voiceConfig": ["prebuiltVoiceConfig": ["voiceName": config.voice]]],
            ],
            "systemInstruction": ["parts": [["text": config.systemInstruction]]],
            "inputAudioTranscription": [String: Any](),
            "outputAudioTranscription": [String: Any](),
            // Uten dette stopper samtalen etter 15 minutter.
            "contextWindowCompression": ["slidingWindow": [String: Any]()],
            "sessionResumption": resumptionHandle.map { ["handle": $0] } ?? [String: Any](),
        ]
        let sendHistory = resumptionHandle == nil && !config.history.isEmpty
        if sendHistory {
            setup["historyConfig"] = ["initialHistoryInClientContent": true]
        }
        send(["setup": setup])
        receive(on: socket, connection: current, sendHistory: sendHistory)
    }

    private func receive(on socket: URLSessionWebSocketTask, connection current: Int, sendHistory: Bool) {
        socket.receive { [weak self] result in
            DispatchQueue.main.async {
                guard let self, current == self.connection else { return }
                switch result {
                case .success(let message):
                    let data: Data?
                    switch message {
                    case .data(let d): data = d
                    case .string(let s): data = s.data(using: .utf8)
                    @unknown default: data = nil
                    }
                    if let data, let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                        self.handle(json, sendHistory: sendHistory)
                    }
                    self.receive(on: socket, connection: current, sendHistory: sendHistory)
                case .failure:
                    self.connectionLost(socket)
                }
            }
        }
    }

    private func handle(_ message: [String: Any], sendHistory: Bool) {
        serverMessages += 1
        if message["setupComplete"] != nil {
            if sendHistory, let config {
                let turns = config.history.map { ["role": $0.role, "parts": [["text": $0.text]]] }
                send(["clientContent": ["turns": turns, "turnComplete": true]])
            }
            isReady = true
            reconnectAttempts = 0
            startWatchdog()
            checkMicrophone(after: 2.5)
            note("koblet til")
            listener?.onStatus(status: "listening")
            flushBufferedAudio()
            return
        }

        if let update = message["sessionResumptionUpdate"] as? [String: Any],
           update["resumable"] as? Bool == true,
           let handle = update["newHandle"] as? String, !handle.isEmpty {
            resumptionHandle = handle
        }

        if message["goAway"] != nil {
            // Tilkoblingen stenger snart. Koble til igjen med en gang, så samtalen fortsetter.
            reconnect()
            return
        }

        guard let content = message["serverContent"] as? [String: Any] else { return }
        awaitingResponseSince = nil

        if content["interrupted"] as? Bool == true {
            clearPlayback()
            listener?.onInterrupted()
        }
        if let text = (content["inputTranscription"] as? [String: Any])?["text"] as? String, !text.isEmpty {
            note("Gemini hørte deg")
            listener?.onUserText(text: text)
        }
        if let text = (content["outputTranscription"] as? [String: Any])?["text"] as? String, !text.isEmpty {
            listener?.onKompisText(text: text)
        }
        if let parts = (content["modelTurn"] as? [String: Any])?["parts"] as? [[String: Any]] {
            for part in parts {
                if let inline = part["inlineData"] as? [String: Any],
                   let base64 = inline["data"] as? String,
                   let audio = Data(base64Encoded: base64) {
                    play(audio)
                }
            }
        }
        if content["turnComplete"] as? Bool == true {
            note("Kompis svarte")
            listener?.onTurnComplete()
            if pendingBuffers > 0 {
                turnCompleteWhilePlaying = true
            } else {
                finishSpeaking()
            }
        }
    }

    private func connectionLost(_ socket: URLSessionWebSocketTask) {
        guard isRunning else { return }
        if let reason = socket.closeReason.flatMap({ String(data: $0, encoding: .utf8) }), !reason.isEmpty,
           socket.closeCode != .normalClosure, socket.closeCode != .goingAway, resumptionHandle == nil {
            // Gemini avviste tilkoblingen, f.eks. på grunn av feil nøkkel.
            note("Gemini stengte: \(reason)")
            fail("Gemini svarte: \(reason)")
            return
        }
        reconnect()
    }

    private func reconnect() {
        guard isRunning else { return }
        reconnectAttempts += 1
        if reconnectAttempts > 5 {
            fail("Mistet kontakten med Gemini. Sjekk nettet og prøv igjen.")
            return
        }
        listener?.onStatus(status: "reconnecting")
        note("kobler til igjen (forsøk \(reconnectAttempts))")
        socket?.cancel(with: .goingAway, reason: nil)
        let delay = reconnectAttempts == 1 ? 0 : Double(reconnectAttempts)
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.connect()
        }
    }

    private func send(_ message: [String: Any]) {
        guard let socket,
              let data = try? JSONSerialization.data(withJSONObject: message),
              let text = String(data: data, encoding: .utf8) else { return }
        socket.send(.string(text)) { _ in }
    }
}
