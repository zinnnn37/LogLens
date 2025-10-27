// src/mocks/dummyLogSearch.ts

import type { LogRow } from '@/components/LogResultsTable';

const BASE_TIMESTAMP = new Date('2024-06-05T19:55:00.000Z').getTime();

const systemOptions: LogRow['layer'][] = ['FE', 'BE', 'INFRA'];
const levelOptions: LogRow['level'][] = ['INFO', 'WARN', 'ERROR'];

const messages = {
    FE: [
        '초기 로드 완료: initial_load_to_https://www.cholog.com/',
        'XHR 응답: POST https://www.cholog.com/api/v1/user/login',
        '클릭 이벤트: div.header > button.login',
        '페이지 라우팅: /dashboard/project/123',
        '웹소켓 연결 성공: wss://realtime.cholog.com/connect',
    ],
    BE: [
        'Request Finished: GET /api/vendor/phpunittest',
        '[ResourceNotFound] GET /api/vendor/phpunittest/Util/Getopt.php',
        'Request Finished: GET /status=403 duration=0.04ms',
        'Request Finished: GET /favicon.ico status=403',
        'Error parsing HTTP request header Note: further occurrences...',
        'Request Finished: GET /api/log/22/timeline?range=6h',
        'Request Finished: POST /api/report/22/stat',
        'Request Finished: POST /api/log/42/analysis',
        'Request Finished: GET /api/log/42/trace/60a1b2c3d4e5',
    ],
    INFRA: [
        'New deployment detected: v2.1.5 scaling up pods.',
        'DB connection pool overflow warning (pool_size=100)',
        'Redis cache miss rate > 20%',
        '[Critical] Disk space usage at 95% on volume /data',
        'ELB health check failed (target: 10.0.1.5:80)',
    ],
};

export const DUMMY_LOG_SEARCH_RESULTS: LogRow[] = Array.from({
    length: 100,
}).map((_, i) => {
    const system = systemOptions[i % 3];
    let level: LogRow['level'];

    if (system === 'INFRA') {
        level = levelOptions[(i % 3) + 1] || 'ERROR'; // WARN, ERROR
    } else {
        level = i % 7 === 0 ? 'WARN' : 'INFO'; // 대부분 INFO, 가끔 WARN
    }
    if (system !== 'INFRA' && i % 10 === 0) {
        level = 'ERROR';
    }

    const messageList = messages[system] || messages.BE;
    const message = messageList[i % messageList.length];
    const timestamp = new Date(BASE_TIMESTAMP - i * 1000 * 5); // 5초 간격

    return {
        id: `trace-search-${(900000 + i).toString(16)}`,
        level: level,
        layer: system,
        env: system === 'FE' ? 'prod' : 'secret.prod', 
        date: timestamp.toISOString(),
        message: `${message} (ID=${(100 + i) * 3})`,
    };
});