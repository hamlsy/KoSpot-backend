/**
 * K6 Timer Interval Accuracy 집중 테스트
 * 
 * 목적: 50개 방에서 타이머가 실제로 5초 간격으로 클라이언트에게 전달되는지 검증
 * 
 * 측정 방식:
 * 1. 각 VU가 타이머 채널 구독
 * 2. 연속된 타이머 메시지 수신 시간 측정
 * 3. 메시지 간 실제 간격 = 5초인지 확인
 * 
 * 성공 기준:
 * - 메시지 간격이 5000ms ± 500ms (4.5초 ~ 5.5초) 내
 * - 95% 이상의 메시지가 정확한 간격으로 수신
 */

import ws from 'k6/ws';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate, Gauge } from 'k6/metrics';

// ==================== 설정 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/ws-raw';
const NUM_ROOMS = 50;
const PLAYERS_PER_ROOM = 8;
const TEST_DURATION_MS = 480000; // 8분
const EXPECTED_INTERVAL_MS = 5000;
const ACCEPTABLE_DRIFT_MS = 500; // ±500ms

// ==================== 커스텀 메트릭 ====================
// 핵심 메트릭: 타이머 메시지 간 실제 간격
const timerInterval = new Trend('timer_interval_ms', true);

// 간격 정확도 (5초 ± 500ms 내 비율)
const intervalAccuracy = new Rate('interval_accuracy');

// 서버 → 클라이언트 수신 지연
const receiveLatency = new Trend('receive_latency_ms', true);

// 드리프트 (예상 간격과의 차이)
const intervalDrift = new Trend('interval_drift_ms', true);

// 수신 메시지 카운터
const messagesReceived = new Counter('timer_messages_total');
const wsConnected = new Counter('ws_connected');
const wsFailed = new Counter('ws_failed');

// ==================== 테스트 시나리오 ====================
export const options = {
    stages: [
        { duration: '30s', target: 80 },   // 10개 방 (80 VUs)
        { duration: '60s', target: 80 },   // Hold - 메시지 수신 관찰
        { duration: '30s', target: 200 },  // 25개 방 (200 VUs)
        { duration: '90s', target: 200 },  // Hold - 부하 중 간격 측정
        { duration: '30s', target: 400 },  // 50개 방 (400 VUs)
        { duration: '90s', target: 400 },  // Hold - 최대 부하 테스트
        { duration: '30s', target: 0 },    // Ramp-down
    ],
    thresholds: {
        'interval_accuracy': ['rate>0.95'],           // 95% 이상 정확
        'timer_interval_ms': ['p(95)<5500'],          // p95 < 5.5초
        'interval_drift_ms': ['p(95)<500', 'avg<200'], // 드리프트 평균 200ms 이내
    },
};

// ==================== STOMP 프레임 ====================
const NULL = '\u0000';
const LF = '\n';

function createConnectFrame() {
    return `CONNECT${LF}accept-version:1.1,1.2${LF}heart-beat:0,0${LF}${LF}${NULL}`;
}

function createSubscribeFrame(dest, id) {
    return `SUBSCRIBE${LF}id:${id}${LF}destination:${dest}${LF}${LF}${NULL}`;
}

function createDisconnectFrame() {
    return `DISCONNECT${LF}${LF}${NULL}`;
}

function parseStompMessage(raw) {
    if (!raw || raw === LF) return { type: 'HEARTBEAT' };
    const lines = raw.split(LF);
    const cmd = lines[0];
    if (!cmd) return null;
    
    let bodyStart = 1;
    for (let i = 1; i < lines.length; i++) {
        if (lines[i] === '') { bodyStart = i + 1; break; }
    }
    
    let body = lines.slice(bodyStart).join(LF).replace(/\u0000/g, '');
    try { return { type: cmd, body: JSON.parse(body) }; }
    catch { return { type: cmd, body }; }
}

// ==================== SETUP ====================
export function setup() {
    console.log(`[Setup] Starting ${NUM_ROOMS} room timers...`);
    
    const roomIds = [];
    let successCount = 0;
    
    for (let i = 1; i <= NUM_ROOMS; i++) {
        const roomId = `timer-test-${i.toString().padStart(3, '0')}`;
        roomIds.push(roomId);
        
        const res = http.post(
            `${BASE_URL}/api/test/timer/start?roomId=${roomId}&durationMs=${TEST_DURATION_MS}`
        );
        
        if (res.status === 200) {
            successCount++;
        }
    }
    
    console.log(`[Setup] Started ${successCount}/${NUM_ROOMS} timers`);
    sleep(3); // 첫 메시지 대기
    
    return { roomIds, numRooms: NUM_ROOMS, playersPerRoom: PLAYERS_PER_ROOM };
}

// ==================== TEARDOWN ====================
export function teardown(data) {
    console.log(`[Teardown] Stopping all timers...`);
    http.post(`${BASE_URL}/api/test/timer/stop-all`);
}

// ==================== 메인 테스트 ====================
export default function (data) {
    const vuId = __VU;
    const roomIndex = Math.floor((vuId - 1) / data.playersPerRoom) % data.numRooms;
    const roomId = data.roomIds[roomIndex];
    const timerChannel = `/topic/game/${roomId}/timer/sync`;
    const subId = `timer-${vuId}`;
    
    let lastMsgTime = null;
    let msgCount = 0;
    let connected = false;
    
    const res = ws.connect(WS_URL, {}, function (socket) {
        socket.on('open', () => {
            wsConnected.add(1);
            socket.send(createConnectFrame());
        });
        
        socket.on('message', (raw) => {
            const msg = parseStompMessage(raw);
            if (!msg) return;
            
            if (msg.type === 'CONNECTED') {
                connected = true;
                socket.send(createSubscribeFrame(timerChannel, subId));
            }
            else if (msg.type === 'MESSAGE' && msg.body && msg.body.serverTimestamp) {
                const now = Date.now();
                const serverTs = msg.body.serverTimestamp;
                
                // 서버 → 클라이언트 수신 지연
                const latency = now - serverTs;
                if (latency >= 0 && latency < 10000) { // 10초 이내만 유효
                    receiveLatency.add(latency);
                }
                
                // 연속 메시지 간 간격 측정 (핵심!)
                if (lastMsgTime !== null) {
                    const interval = now - lastMsgTime;
                    
                    // 유효한 간격인지 확인 (1초 ~ 10초 범위)
                    if (interval >= 1000 && interval <= 10000) {
                        timerInterval.add(interval);
                        
                        // 드리프트 계산 (예상 5000ms와의 차이)
                        const drift = Math.abs(interval - EXPECTED_INTERVAL_MS);
                        intervalDrift.add(drift);
                        
                        // 정확도 체크 (4.5초 ~ 5.5초)
                        const isAccurate = drift <= ACCEPTABLE_DRIFT_MS;
                        intervalAccuracy.add(isAccurate);
                        
                        // 드리프트가 클 때만 로깅
                        if (drift > ACCEPTABLE_DRIFT_MS) {
                            console.log(`[VU ${vuId}/${roomId}] DRIFT! interval=${interval}ms, drift=${drift}ms`);
                        }
                    }
                }
                
                lastMsgTime = now;
                msgCount++;
                messagesReceived.add(1);
            }
        });
        
        socket.on('error', (e) => {
            wsFailed.add(1);
        });
        
        socket.on('close', () => {
            if (msgCount > 2) {
                console.log(`[VU ${vuId}] Closed. Messages: ${msgCount}`);
            }
        });
        
        // 60초간 연결 유지 (약 12개 메시지 수신 예상)
        socket.setTimeout(() => {
            if (connected) {
                socket.send(createDisconnectFrame());
            }
            socket.close();
        }, 60000);
    });
    
    check(res, { 'WS connected': (r) => r && r.status === 101 });
    sleep(2);
}

// ==================== 결과 요약 ====================
export function handleSummary(data) {
    const out = [];
    out.push('\n============= 5초 타이머 간격 정확도 테스트 결과 =============\n');
    out.push(`테스트 환경: ${NUM_ROOMS}개 방 × ${PLAYERS_PER_ROOM}명 = ${NUM_ROOMS * PLAYERS_PER_ROOM} 클라이언트\n`);
    out.push(`예상 간격: ${EXPECTED_INTERVAL_MS}ms (허용 오차: ±${ACCEPTABLE_DRIFT_MS}ms)\n`);
    
    const m = data.metrics;
    
    // 핵심 결과: 타이머 간격
    if (m.timer_interval_ms?.values) {
        const i = m.timer_interval_ms.values;
        out.push('\n📊 타이머 메시지 간격:');
        out.push(`   평균: ${i.avg?.toFixed(2)}ms (예상 대비 ${(i.avg - 5000).toFixed(2)}ms 차이)`);
        out.push(`   최소: ${i.min?.toFixed(2)}ms`);
        out.push(`   최대: ${i.max?.toFixed(2)}ms`);
        out.push(`   p(90): ${i['p(90)']?.toFixed(2)}ms`);
        out.push(`   p(95): ${i['p(95)']?.toFixed(2)}ms`);
    }
    
    // 드리프트 분석
    if (m.interval_drift_ms?.values) {
        const d = m.interval_drift_ms.values;
        out.push('\n📏 간격 드리프트 (5초와의 차이):');
        out.push(`   평균: ${d.avg?.toFixed(2)}ms`);
        out.push(`   최대: ${d.max?.toFixed(2)}ms`);
        out.push(`   p(95): ${d['p(95)']?.toFixed(2)}ms`);
    }
    
    // 정확도
    if (m.interval_accuracy?.values) {
        const acc = (m.interval_accuracy.values.rate * 100).toFixed(2);
        out.push(`\n✅ 간격 정확도: ${acc}% (5초 ± 500ms 내 비율)`);
        if (parseFloat(acc) >= 95) {
            out.push('   → 목표 달성! 95% 이상 정확');
        } else {
            out.push('   → ⚠️ 목표 미달 (95% 미만)');
        }
    }
    
    // 수신 지연
    if (m.receive_latency_ms?.values) {
        const l = m.receive_latency_ms.values;
        out.push(`\n🕐 서버→클라이언트 수신 지연: 평균 ${l.avg?.toFixed(2)}ms, p(95) ${l['p(95)']?.toFixed(2)}ms`);
    }
    
    out.push(`\n📬 총 수신 메시지: ${m.timer_messages_total?.values?.count || 0}`);
    out.push(`🔌 WebSocket: 성공=${m.ws_connected?.values?.count||0}, 실패=${m.ws_failed?.values?.count||0}`);
    out.push('\n===========================================================\n');
    
    console.log(out.join('\n'));
    
    return {
        'stdout': out.join('\n'),
        'k6-timer-interval-results.json': JSON.stringify(data, null, 2),
    };
}
