/**
 * K6 WebSocket Timer Broadcasting 부하테스트 v3
 * 
 * 시나리오: 50개 게임룸 × 8명 플레이어 = 400명 동시접속
 * 목적: 각 방에서 5초마다 브로드캐스팅되는 TimerSyncMessage가
 *       정확히 5초 간격을 유지하는지 검증
 * 
 * EC2 프리티어 (t2.micro: 1 vCPU, 1GB RAM) 기준 Breakpoint 탐색
 * 
 * 테스트 흐름:
 * 1. setup: 50개 방에 각각 Mock 타이머 시작
 * 2. default: 400 VUs가 각자 방에 연결하여 타이머 메시지 수신
 * 3. teardown: 모든 Mock 타이머 종료
 */

import ws from 'k6/ws';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate, Gauge } from 'k6/metrics';

// ==================== 설정 ====================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/ws-raw';
const NUM_ROOMS = 50;               // 50개 방
const PLAYERS_PER_ROOM = 8;         // 방당 8명
const TOTAL_PLAYERS = NUM_ROOMS * PLAYERS_PER_ROOM; // 400명
const TEST_DURATION_MS = 420000;    // 7분 (테스트보다 길게)
const EXPECTED_SYNC_INTERVAL_MS = 5000;  // 5초
const ACCEPTABLE_DRIFT_MS = 500;    // ±500ms

// ==================== 커스텀 메트릭 ====================
const timerMessageInterval = new Trend('timer_message_interval_ms');
const messageReceiveLatency = new Trend('message_receive_latency_ms');
const intervalAccuracy = new Rate('timer_interval_accuracy');
const wsConnectionSuccess = new Counter('ws_connection_success');
const wsConnectionFailed = new Counter('ws_connection_failed');
const timerMessagesReceived = new Counter('timer_messages_received');
const activeRooms = new Gauge('active_rooms');

// ==================== 50개 방 × 8명 시나리오 ====================
export const options = {
    stages: [
        { duration: '30s', target: 80 },   // 10개 방 (10×8=80 VUs)
        { duration: '60s', target: 80 },   // Hold
        { duration: '30s', target: 200 },  // 25개 방 (25×8=200 VUs)
        { duration: '60s', target: 200 },  // Hold
        { duration: '30s', target: 400 },  // 50개 방 (50×8=400 VUs)
        { duration: '60s', target: 400 },  // Hold - Breakpoint 탐색
        { duration: '30s', target: 0 },    // Ramp-down
    ],
    thresholds: {
        'timer_interval_accuracy': ['rate>0.90'],
        'timer_message_interval_ms': ['p(95)<5800'],
        'message_receive_latency_ms': ['p(95)<500'],
    },
};

// ==================== STOMP 함수 ====================
const NULL = '\u0000';
const LF = '\n';

function createConnectFrame() {
    return `CONNECT${LF}accept-version:1.1,1.2${LF}heart-beat:0,0${LF}${LF}${NULL}`;
}

function createSubscribeFrame(destination, id) {
    return `SUBSCRIBE${LF}id:${id}${LF}destination:${destination}${LF}${LF}${NULL}`;
}

function createDisconnectFrame() {
    return `DISCONNECT${LF}${LF}${NULL}`;
}

function parseStompMessage(data) {
    if (!data || data === LF) return { type: 'HEARTBEAT' };
    
    const lines = data.split(LF);
    const command = lines[0];
    if (!command) return null;
    
    let bodyStart = 1;
    for (let i = 1; i < lines.length; i++) {
        if (lines[i] === '') { bodyStart = i + 1; break; }
    }
    
    let body = lines.slice(bodyStart).join(LF).replace(/\u0000/g, '');
    try { return { type: command, body: JSON.parse(body) }; }
    catch { return { type: command, body }; }
}

// ==================== SETUP: 50개 방에 타이머 시작 ====================
export function setup() {
    console.log(`[Setup] Starting ${NUM_ROOMS} room timers...`);
    
    const roomIds = [];
    for (let i = 1; i <= NUM_ROOMS; i++) {
        const roomId = `room-${i.toString().padStart(3, '0')}`;
        roomIds.push(roomId);
        
        const res = http.post(
            `${BASE_URL}/api/test/timer/start?roomId=${roomId}&durationMs=${TEST_DURATION_MS}`
        );
        
        if (res.status !== 200) {
            console.error(`[Setup] Failed to start timer for ${roomId}: ${res.status}`);
        }
    }
    
    console.log(`[Setup] Started ${roomIds.length} room timers`);
    activeRooms.add(roomIds.length);
    
    // 첫 메시지 브로드캐스팅 대기
    sleep(3);
    
    return { roomIds, numRooms: NUM_ROOMS, playersPerRoom: PLAYERS_PER_ROOM };
}

// ==================== TEARDOWN: 모든 타이머 종료 ====================
export function teardown(data) {
    console.log(`[Teardown] Stopping all timers...`);
    http.post(`${BASE_URL}/api/test/timer/stop-all`);
    console.log(`[Teardown] All timers stopped`);
}

// ==================== 메인 테스트 ====================
export default function (data) {
    const vuId = __VU;
    
    // VU를 방에 할당 (VU 1-8 → room-001, VU 9-16 → room-002, ...)
    const roomIndex = Math.floor((vuId - 1) / data.playersPerRoom) % data.numRooms;
    const roomId = data.roomIds[roomIndex];
    const timerChannel = `/topic/game/${roomId}/timer/sync`;
    const subId = `sub-${vuId}`;
    
    let lastMsgTime = null;
    let msgCount = 0;
    
    const res = ws.connect(WS_URL, {}, function (socket) {
        socket.on('open', () => {
            wsConnectionSuccess.add(1);
            socket.send(createConnectFrame());
        });
        
        socket.on('message', (raw) => {
            const msg = parseStompMessage(raw);
            if (!msg) return;
            
            if (msg.type === 'CONNECTED') {
                socket.send(createSubscribeFrame(timerChannel, subId));
            }
            else if (msg.type === 'MESSAGE' && msg.body?.serverTimestamp) {
                const now = Date.now();
                const latency = now - msg.body.serverTimestamp;
                messageReceiveLatency.add(latency);
                
                if (lastMsgTime !== null) {
                    const interval = now - lastMsgTime;
                    timerMessageInterval.add(interval);
                    
                    const inRange = interval >= (EXPECTED_SYNC_INTERVAL_MS - ACCEPTABLE_DRIFT_MS) &&
                                   interval <= (EXPECTED_SYNC_INTERVAL_MS + ACCEPTABLE_DRIFT_MS);
                    intervalAccuracy.add(inRange);
                    
                    if (!inRange) {
                        console.log(`[VU ${vuId}/${roomId}] Drift! interval=${interval}ms`);
                    }
                }
                
                lastMsgTime = now;
                msgCount++;
                timerMessagesReceived.add(1);
            }
        });
        
        socket.on('error', (e) => {
            wsConnectionFailed.add(1);
            console.error(`[VU ${vuId}] WS Error: ${e.error()}`);
        });
        
        socket.on('close', () => {
            if (msgCount > 0) {
                console.log(`[VU ${vuId}/${roomId}] Closed. Msgs: ${msgCount}`);
            }
        });
        
        // 30초간 연결 유지 (5-6개 메시지 수신 예상)
        socket.setTimeout(() => {
            socket.send(createDisconnectFrame());
            socket.close();
        }, 30000);
    });
    
    check(res, { 'WS connected': (r) => r && r.status === 101 });
    sleep(1);
}

// ==================== 결과 요약 ====================
export function handleSummary(data) {
    const out = [];
    out.push('\n========== 50 Rooms × 8 Players Timer Test (EC2 Free Tier) ==========\n');
    out.push(`Configuration: ${NUM_ROOMS} rooms × ${PLAYERS_PER_ROOM} players = ${TOTAL_PLAYERS} clients\n`);
    
    const m = data.metrics;
    
    if (m.timer_message_interval_ms?.values) {
        const i = m.timer_message_interval_ms.values;
        out.push('Timer Message Interval (Expected: 5000ms):');
        out.push(`  Average: ${i.avg?.toFixed(2)}ms (Drift: ${i.avg ? (i.avg-5000).toFixed(2) : 'N/A'}ms)`);
        out.push(`  Min: ${i.min?.toFixed(2)}ms, Max: ${i.max?.toFixed(2)}ms`);
        out.push(`  p(95): ${i['p(95)']?.toFixed(2)}ms`);
    }
    
    if (m.message_receive_latency_ms?.values) {
        const l = m.message_receive_latency_ms.values;
        out.push(`\nMessage Latency: Avg=${l.avg?.toFixed(2)}ms, p(95)=${l['p(95)']?.toFixed(2)}ms`);
    }
    
    if (m.timer_interval_accuracy?.values) {
        out.push(`\nInterval Accuracy (5s±500ms): ${((m.timer_interval_accuracy.values.rate||0)*100).toFixed(2)}%`);
    }
    
    out.push(`\nTotal Messages: ${m.timer_messages_received?.values?.count || 0}`);
    out.push(`WS Connections: OK=${m.ws_connection_success?.values?.count||0}, FAIL=${m.ws_connection_failed?.values?.count||0}`);
    out.push('\n======================================================================\n');
    
    console.log(out.join('\n'));
    
    return {
        'stdout': out.join('\n'),
        'k6-results.json': JSON.stringify(data, null, 2),
    };
}
