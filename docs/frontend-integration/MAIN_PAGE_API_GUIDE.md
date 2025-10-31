# 메인 페이지 API 통합 가이드

## 개요

사용자가 메인 페이지에 접속할 때 필요한 모든 정보를 한 번의 API 호출로 제공하는 API 가이드입니다.

## API 명세

### 엔드포인트

```
GET /main
```

### 인증

**선택 사항** - 로그인하지 않은 사용자도 접근 가능합니다.

- 로그인한 경우: `Authorization: Bearer {access_token}` 헤더 포함
- 로그인하지 않은 경우: 헤더 없이 요청 가능

## 응답 구조

### 성공 응답 (200 OK)

```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "isAdmin": false,
    "gameModeStatus": {
      "roadviewEnabled": true,
      "photoEnabled": true,
      "multiplayEnabled": false
    },
    "recentNotices": [
      {
        "noticeId": 3,
        "title": "신규 게임 모드 오픈",
        "createdDate": "2025-10-28T10:00:00"
      },
      {
        "noticeId": 2,
        "title": "서버 정기 점검 안내",
        "createdDate": "2025-10-27T15:30:00"
      },
      {
        "noticeId": 1,
        "title": "KoSpot 서비스 오픈",
        "createdDate": "2025-10-26T09:00:00"
      }
    ],
    "banners": [
      {
        "bannerId": 1,
        "title": "신규 이벤트",
        "imageUrl": "https://s3.amazonaws.com/kospot/banner/image1.jpg",
        "linkUrl": "https://kospot.com/events/1",
        "description": "신규 이벤트에 참여하세요!",
        "displayOrder": 1
      },
      {
        "bannerId": 2,
        "title": "업데이트 안내",
        "imageUrl": "https://s3.amazonaws.com/kospot/banner/image2.jpg",
        "linkUrl": "https://kospot.com/updates",
        "description": "새로운 기능이 추가되었습니다.",
        "displayOrder": 2
      }
    ]
  }
}
```

## 필드 설명

### result 객체

| 필드 | 타입 | 설명 |
|------|------|------|
| `isAdmin` | Boolean | 현재 사용자가 관리자인지 여부 |
| `gameModeStatus` | Object | 게임 모드별 활성화 상태 |
| `recentNotices` | Array | 최근 공지사항 3개 |
| `banners` | Array | 활성화된 배너 목록 |

### gameModeStatus 객체

| 필드 | 타입 | 설명 | 비고 |
|------|------|------|------|
| `roadviewEnabled` | Boolean | 로드뷰 모드 활성화 여부 | 싱글 또는 멀티 중 하나라도 활성화되면 true |
| `photoEnabled` | Boolean | 포토 모드 활성화 여부 | 싱글 또는 멀티 중 하나라도 활성화되면 true |
| `multiplayEnabled` | Boolean | 멀티플레이 모드 활성화 여부 | 모든 멀티 모드 중 하나라도 활성화되면 true |

### recentNotices[] 객체

| 필드 | 타입 | 설명 |
|------|------|------|
| `noticeId` | Number | 공지사항 ID |
| `title` | String | 공지사항 제목 |
| `createdDate` | String | 생성일시 (ISO 8601) |

### banners[] 객체

| 필드 | 타입 | 설명 |
|------|------|------|
| `bannerId` | Number | 배너 ID |
| `title` | String | 배너 제목 |
| `imageUrl` | String | 배너 이미지 S3 URL |
| `linkUrl` | String | 배너 클릭 시 이동할 URL |
| `description` | String | 배너 설명 |
| `displayOrder` | Number | 배너 노출 순서 (오름차순 정렬) |

## 프론트엔드 통합 예시

### React + TypeScript

```typescript
// types.ts
export interface MainPageData {
  isAdmin: boolean;
  gameModeStatus: GameModeStatus;
  recentNotices: Notice[];
  banners: Banner[];
}

export interface GameModeStatus {
  roadviewEnabled: boolean;
  photoEnabled: boolean;
  multiplayEnabled: boolean;
}

export interface Notice {
  noticeId: number;
  title: string;
  createdDate: string;
}

export interface Banner {
  bannerId: number;
  title: string;
  imageUrl: string;
  linkUrl: string;
  description: string;
  displayOrder: number;
}

// api.ts
import axios from 'axios';

export const fetchMainPageData = async (): Promise<MainPageData> => {
  const token = localStorage.getItem('accessToken');
  
  const response = await axios.get('/main', {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  
  return response.data.result;
};

// MainPage.tsx
import React, { useEffect, useState } from 'react';
import { fetchMainPageData, MainPageData } from './api';

const MainPage: React.FC = () => {
  const [data, setData] = useState<MainPageData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const mainPageData = await fetchMainPageData();
        setData(mainPageData);
      } catch (error) {
        console.error('Failed to load main page data:', error);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  if (loading) return <div>Loading...</div>;
  if (!data) return <div>Failed to load data</div>;

  return (
    <div>
      {/* 네비게이션 바 */}
      <nav>
        {data.isAdmin && (
          <button onClick={() => window.location.href = '/admin'}>
            관리자 페이지
          </button>
        )}
      </nav>

      {/* 배너 섹션 */}
      <section className="banner-section">
        {data.banners.map(banner => (
          <a key={banner.bannerId} href={banner.linkUrl}>
            <img src={banner.imageUrl} alt={banner.title} />
            <p>{banner.description}</p>
          </a>
        ))}
      </section>

      {/* 게임 모드 섹션 */}
      <section className="game-modes">
        <h2>게임 모드</h2>
        
        {data.gameModeStatus.roadviewEnabled && (
          <div className="game-mode-card">
            <h3>로드뷰</h3>
            <p>실제 거리 뷰를 보고 위치를 맞춰보세요</p>
            <button onClick={() => window.location.href = '/game/roadview'}>
              시작하기
            </button>
          </div>
        )}
        
        {data.gameModeStatus.photoEnabled && (
          <div className="game-mode-card">
            <h3>포토</h3>
            <p>사진을 보고 위치를 맞춰보세요</p>
            <button onClick={() => window.location.href = '/game/photo'}>
              시작하기
            </button>
          </div>
        )}
        
        {data.gameModeStatus.multiplayEnabled && (
          <div className="game-mode-card">
            <h3>멀티플레이</h3>
            <p>친구들과 함께 즐겨보세요</p>
            <button onClick={() => window.location.href = '/game/multi'}>
              시작하기
            </button>
          </div>
        )}
      </section>

      {/* 공지사항 섹션 */}
      <section className="notices">
        <h2>최근 공지사항</h2>
        <ul>
          {data.recentNotices.map(notice => (
            <li key={notice.noticeId}>
              <a href={`/notices/${notice.noticeId}`}>
                {notice.title}
              </a>
              <span>{new Date(notice.createdDate).toLocaleDateString()}</span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
};

const startGame = (configId: number) => {
  // 게임 시작 로직
  console.log(`Starting game with config ID: ${configId}`);
};

export default MainPage;
```

### Vue.js 3 + Composition API

```typescript
// composables/useMainPage.ts
import { ref, onMounted } from 'vue';
import axios from 'axios';

export interface MainPageData {
  isAdmin: boolean;
  gameModes: GameMode[];
  recentNotices: Notice[];
  banners: Banner[];
}

export const useMainPage = () => {
  const data = ref<MainPageData | null>(null);
  const loading = ref(true);
  const error = ref<Error | null>(null);

  const fetchData = async () => {
    try {
      loading.value = true;
      const token = localStorage.getItem('accessToken');
      
      const response = await axios.get('/main', {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      
      data.value = response.data.result;
    } catch (e) {
      error.value = e as Error;
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    fetchData();
  });

  return {
    data,
    loading,
    error,
    refetch: fetchData,
  };
};

// MainPage.vue
<template>
  <div v-if="loading">Loading...</div>
  <div v-else-if="error">Failed to load data</div>
  <div v-else-if="data">
    <!-- 네비게이션 -->
    <nav>
      <button v-if="data.isAdmin" @click="goToAdmin">
        관리자 페이지
      </button>
    </nav>

    <!-- 배너 -->
    <section class="banners">
      <a 
        v-for="banner in data.banners" 
        :key="banner.bannerId"
        :href="banner.linkUrl"
      >
        <img :src="banner.imageUrl" :alt="banner.title" />
        <p>{{ banner.description }}</p>
      </a>
    </section>

    <!-- 게임 모드 -->
    <section class="game-modes">
      <h2>게임 모드</h2>
      
      <div v-if="data.gameModeStatus.roadviewEnabled" class="game-mode-card">
        <h3>로드뷰</h3>
        <p>실제 거리 뷰를 보고 위치를 맞춰보세요</p>
        <button @click="goToGame('roadview')">시작하기</button>
      </div>
      
      <div v-if="data.gameModeStatus.photoEnabled" class="game-mode-card">
        <h3>포토</h3>
        <p>사진을 보고 위치를 맞춰보세요</p>
        <button @click="goToGame('photo')">시작하기</button>
      </div>
      
      <div v-if="data.gameModeStatus.multiplayEnabled" class="game-mode-card">
        <h3>멀티플레이</h3>
        <p>친구들과 함께 즐겨보세요</p>
        <button @click="goToGame('multi')">시작하기</button>
      </div>
    </section>

    <!-- 공지사항 -->
    <section class="notices">
      <h2>최근 공지사항</h2>
      <ul>
        <li v-for="notice in data.recentNotices" :key="notice.noticeId">
          <router-link :to="`/notices/${notice.noticeId}`">
            {{ notice.title }}
          </router-link>
          <span>{{ formatDate(notice.createdDate) }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useMainPage } from '@/composables/useMainPage';

const { data, loading, error } = useMainPage();

const goToAdmin = () => {
  window.location.href = '/admin';
};

const goToGame = (gameType: string) => {
  window.location.href = `/game/${gameType}`;
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString();
};
</script>
```

## UI 구성 권장사항

### 1. 네비게이션 바
- `isAdmin`이 `true`일 때만 "관리자 페이지" 버튼 표시
- 로그인 여부와 관계없이 일반 메뉴는 항상 표시

### 2. 배너 섹션
- Carousel 또는 Slider 형태로 구현 권장
- `displayOrder` 순서대로 정렬하여 표시
- 클릭 시 `linkUrl`로 이동
- 이미지 로딩 실패 시 fallback 이미지 제공

### 3. 게임 모드 섹션
- 3개의 큰 카테고리로 표시: 로드뷰, 포토, 멀티플레이
- 각 모드의 `enabled` 상태에 따라 조건부 렌더링
- 활성화된 모드만 버튼으로 표시
- 세부 모드 선택은 각 게임 페이지 내부에서 처리

### 4. 공지사항 섹션
- 최대 3개만 표시 (서버에서 제한됨)
- 날짜 포맷은 사용자 로케일에 맞게 변환
- "더보기" 버튼으로 전체 공지사항 페이지 링크

## 데이터 처리 시 주의사항

### 1. 게임 모드 상태 확인
게임 모드 상태를 확인하여 UI 표시:

```typescript
const { gameModeStatus } = data;

// 로드뷰 모드가 활성화되어 있는지 확인
if (gameModeStatus.roadviewEnabled) {
  // 로드뷰 게임 버튼 표시
}

// 포토 모드가 활성화되어 있는지 확인
if (gameModeStatus.photoEnabled) {
  // 포토 게임 버튼 표시
}

// 멀티플레이 모드가 활성화되어 있는지 확인
if (gameModeStatus.multiplayEnabled) {
  // 멀티플레이 게임 버튼 표시
}

// 활성화된 모드가 하나도 없는 경우
const hasAnyMode = gameModeStatus.roadviewEnabled || 
                   gameModeStatus.photoEnabled || 
                   gameModeStatus.multiplayEnabled;

if (!hasAnyMode) {
  // "현재 이용 가능한 게임이 없습니다" 메시지 표시
}
```

### 2. 빈 데이터 처리
모든 게임 모드가 비활성화된 경우 처리:

```typescript
const hasAnyGameMode = data.gameModeStatus.roadviewEnabled || 
                       data.gameModeStatus.photoEnabled || 
                       data.gameModeStatus.multiplayEnabled;

{hasAnyGameMode ? (
  <GameModesSection status={data.gameModeStatus} />
) : (
  <EmptyState message="현재 이용 가능한 게임 모드가 없습니다." />
)}
```

### 3. 날짜 포맷
ISO 8601 형식의 날짜를 사용자 친화적으로 변환:

```typescript
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';

const formatNoticeDate = (dateString: string) => {
  return format(new Date(dateString), 'yyyy년 MM월 dd일', { locale: ko });
};
```

### 4. 이미지 최적화
배너 이미지 로딩 최적화:

```typescript
<img 
  src={banner.imageUrl} 
  alt={banner.title}
  loading="lazy"
  onError={(e) => {
    e.currentTarget.src = '/images/default-banner.jpg';
  }}
/>
```

## 에러 처리

### 일반적인 에러 케이스

1. **네트워크 오류**: 사용자에게 재시도 옵션 제공
2. **인증 만료**: 로그인 페이지로 리다이렉트 (선택적)
3. **서버 오류**: 사용자 친화적인 에러 메시지 표시

```typescript
try {
  const data = await fetchMainPageData();
  setData(data);
} catch (error) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 401) {
      // 인증 만료 (선택적 처리)
      console.log('Not authenticated - showing public data');
    } else if (error.response?.status === 500) {
      showErrorMessage('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } else {
      showErrorMessage('데이터를 불러오는데 실패했습니다.');
    }
  }
}
```

## 성능 최적화 팁

### 1. 데이터 캐싱
메인 페이지 데이터는 자주 변경되지 않으므로 클라이언트 캐싱 권장:

```typescript
// React Query 사용 예시
import { useQuery } from '@tanstack/react-query';

const useMainPageData = () => {
  return useQuery({
    queryKey: ['mainPage'],
    queryFn: fetchMainPageData,
    staleTime: 5 * 60 * 1000, // 5분
    cacheTime: 10 * 60 * 1000, // 10분
  });
};
```

### 2. 이미지 Lazy Loading
배너 이미지는 lazy loading 적용:

```html
<img loading="lazy" src="..." alt="..." />
```

### 3. 조건부 렌더링
데이터가 없는 경우 불필요한 DOM 생성 방지

## 요약

- **엔드포인트**: `GET /main`
- **인증**: 선택 사항 (로그인 여부 무관)
- **응답 데이터**: 게임 모드 활성화 상태, 공지사항, 배너, 관리자 여부
- **게임 모드**: 3가지 카테고리(로드뷰, 포토, 멀티플레이)의 활성화 여부만 제공
- **주요 용도**: 메인 페이지 초기 로딩 시 한 번의 API 호출로 모든 데이터 조회
- **데이터 갱신**: 페이지 새로고침 또는 주기적인 refetch로 최신 상태 유지

## 참고 문서

- [관리자 API 가이드](../admin-api/ADMIN_API_GUIDE.md)
- [로드뷰 싱글 게임 API 가이드](./ROADVIEW_SOLO_API_GUIDE.md)
- [공지사항 API 문서](./NOTICE_API_GUIDE.md) (향후 작성 예정)

