# 플레이리스트 조회 API (좋아요/싫어요 기능 포함)

## 📌 플레이리스트 조회 API

책을 선택한 후 해당 책 기반으로 추천된 음악 플레이리스트를 조회하는 API입니다.
- 벡터 유사도 기준으로 30개 음악 조회
- 좋아요 개수 기준으로 내림차순 정렬
- 로그인한 유저가 싫어요한 음악은 제외

### 엔드포인트
```
GET /api/books/{isbn}/playlist
```

### 요청

#### URL 파라미터
- `isbn` (String, 필수): 선택한 책의 ISBN

#### 헤더
```
Authorization: Bearer {JWT_TOKEN}  (필수)
```

#### 예시
```
GET /api/books/9788937461234/playlist
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 응답

#### 성공 응답 (200 OK)
```json
[
  {
    "trackId": "4uLU6hMCjMI75M1A2tKUQC",
    "trackName": "곡 제목",
    "artistName": "아티스트명",
    "imageUrl": "https://i.scdn.co/image/...",
    "externalUrl": "https://open.spotify.com/track/...",
    "likeCount": 15
  },
  {
    "trackId": "5uLU6hMCjMI75M1A2tKUQC",
    "trackName": "다른 곡 제목",
    "artistName": "다른 아티스트",
    "imageUrl": "https://i.scdn.co/image/...",
    "externalUrl": "https://open.spotify.com/track/...",
    "likeCount": 12
  }
]
```

#### 응답 필드 설명
- `trackId`: Spotify 트랙 ID
- `trackName`: 곡 제목
- `artistName`: 아티스트명
- `imageUrl`: 앨범 커버 이미지 URL
- `externalUrl`: Spotify 외부 링크 URL
- `likeCount`: 해당 책에 대한 좋아요 개수 (해당 책 기준)

### 동작 방식

1. **벡터 유사도 기반 조회**: 책의 임베딩 벡터와 유사한 음악 30개 조회
2. **싫어요 필터링**: 로그인한 유저가 싫어요한 음악은 제외 (비로그인 시 모든 음악 포함)
3. **좋아요 순 정렬**: 좋아요 개수가 많은 순서대로 정렬
4. **결과 반환**: 정렬된 플레이리스트 반환

### 에러 응답

**401 Unauthorized** - 로그인이 필요한 경우
```json
{
  "error": "로그인이 필요합니다."
}
```

**400 Bad Request** - 사용자를 찾을 수 없는 경우
```json
{
  "error": "사용자 없음"
}
```

### 주의사항

- **인증 필수**: 로그인이 필수입니다. JWT 토큰이 필요합니다
- **좋아요 개수**: `likeCount`는 해당 책(`isbn`)에 대한 좋아요 개수입니다
- **정렬 순서**: 좋아요 개수가 많은 순서대로 정렬되어 반환됩니다
- **싫어요 필터링**: 로그인한 유저가 싫어요한 음악은 자동으로 제외됩니다

