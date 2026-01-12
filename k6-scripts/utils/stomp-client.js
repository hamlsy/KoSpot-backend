/**
 * K6 WebSocket STOMP 유틸리티
 * 
 * k6는 STOMP 프로토콜을 기본 지원하지 않으므로,
 * Raw WebSocket 위에서 STOMP 프레임을 직접 생성/파싱합니다.
 */

// STOMP 프레임 구분자
const NULL_CHAR = '\u0000';
const LF = '\n';

/**
 * STOMP CONNECT 프레임 생성
 * @param {object} headers - 연결 헤더 (accept-version, heart-beat 등)
 * @returns {string} STOMP CONNECT 프레임
 */
export function createConnectFrame(headers = {}) {
    const defaultHeaders = {
        'accept-version': '1.1,1.2',
        'heart-beat': '0,0'
    };
    const mergedHeaders = { ...defaultHeaders, ...headers };
    
    let frame = 'CONNECT' + LF;
    for (const [key, value] of Object.entries(mergedHeaders)) {
        frame += `${key}:${value}${LF}`;
    }
    frame += LF + NULL_CHAR;
    
    return frame;
}

/**
 * STOMP SUBSCRIBE 프레임 생성
 * @param {string} destination - 구독할 목적지 (예: /topic/game/{roomId}/timer/sync)
 * @param {string} subscriptionId - 구독 ID
 * @returns {string} STOMP SUBSCRIBE 프레임
 */
export function createSubscribeFrame(destination, subscriptionId) {
    let frame = 'SUBSCRIBE' + LF;
    frame += `id:${subscriptionId}${LF}`;
    frame += `destination:${destination}${LF}`;
    frame += LF + NULL_CHAR;
    
    return frame;
}

/**
 * STOMP DISCONNECT 프레임 생성
 * @returns {string} STOMP DISCONNECT 프레임
 */
export function createDisconnectFrame() {
    return 'DISCONNECT' + LF + LF + NULL_CHAR;
}

/**
 * STOMP 메시지 파싱
 * @param {string} data - Raw WebSocket 데이터
 * @returns {object|null} 파싱된 STOMP 메시지 또는 null
 */
export function parseStompMessage(data) {
    if (!data || data === LF) {
        // 하트비트
        return { type: 'HEARTBEAT' };
    }
    
    const lines = data.split(LF);
    const command = lines[0];
    
    if (!command) {
        return null;
    }
    
    const headers = {};
    let bodyStartIndex = 1;
    
    // 헤더 파싱
    for (let i = 1; i < lines.length; i++) {
        const line = lines[i];
        if (line === '') {
            bodyStartIndex = i + 1;
            break;
        }
        const colonIndex = line.indexOf(':');
        if (colonIndex > 0) {
            const key = line.substring(0, colonIndex);
            const value = line.substring(colonIndex + 1);
            headers[key] = value;
        }
    }
    
    // 바디 파싱 (NULL 문자 제거)
    let body = lines.slice(bodyStartIndex).join(LF);
    body = body.replace(/\u0000/g, '');
    
    // JSON 파싱 시도
    let parsedBody = null;
    if (body && body.trim()) {
        try {
            parsedBody = JSON.parse(body);
        } catch (e) {
            parsedBody = body;
        }
    }
    
    return {
        type: command,
        headers: headers,
        body: parsedBody
    };
}

/**
 * 서버 타임스탬프와 현재 시간 차이 계산 (ms)
 * @param {number} serverTimestamp - 서버에서 보낸 타임스탬프
 * @returns {number} 지연 시간 (ms)
 */
export function calculateLatency(serverTimestamp) {
    return Date.now() - serverTimestamp;
}
