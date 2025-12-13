# Spotify 연동 프론트엔드 수정 가이드

## 📋 백엔드 변경 사항

### SpotifyController.java 수정 완료
- **엔드포인트**: `GET /api/spotify/callback`
- **응답 형식**: HTML 페이지 (이전 JSON → HTML로 변경)
- **Deep Link 포함**: `musiccheck://spotify-callback?success=true&spotifyUserId={spotifyUserId}`

### 동작 흐름
1. 사용자가 "스포티파이 연동" 버튼 클릭
2. 외부 브라우저에서 Spotify 인증 진행
3. Spotify 인증 완료 후 백엔드 콜백 호출
4. **백엔드에서 HTML 페이지 반환** (✅ 완료! + "앱으로 돌아가기" 버튼)
5. 사용자가 "앱으로 돌아가기" 버튼 클릭
6. Deep Link 실행: `musiccheck://spotify-callback?success=true&spotifyUserId=xxx`
7. **프론트엔드에서 Deep Link 처리 필요** ⬅️ 여기서 수정 필요

---

## 🔧 프론트엔드 수정 필요 사항

### 1. Deep Link 리스너 추가 (ProfileScreen.js)

#### 필요한 import
```javascript
import { Linking } from 'react-native';
```

#### useEffect에 Deep Link 리스너 추가

```javascript
useEffect(() => {
  loadUserInfo();
  
  // Deep Link 처리 함수
  const handleDeepLink = (event) => {
    const url = event.url || event;
    console.log('🔗 [Deep Link] 수신:', url);
    
    if (url && url.includes('spotify-callback')) {
      try {
        // URL 파싱
        const urlObj = new URL(url);
        const success = urlObj.searchParams.get('success') === 'true';
        const spotifyUserId = urlObj.searchParams.get('spotifyUserId');
        const message = urlObj.searchParams.get('message');
        
        console.log('🔗 [Deep Link] 파싱 결과:', { success, spotifyUserId, message });
        
        if (success) {
          // 연동 성공
          console.log('✅ [Deep Link] 스포티파이 연동 성공');
          loadUserInfo(); // 사용자 정보 새로고침
          Alert.alert(
            '연동 완료',
            '스포티파이 연동이 완료되었습니다!',
            [{ text: '확인' }]
          );
        } else {
          // 연동 실패
          console.error('❌ [Deep Link] 스포티파이 연동 실패:', message);
          Alert.alert(
            '연동 실패',
            message || '스포티파이 연동에 실패했습니다.',
            [{ text: '확인' }]
          );
        }
      } catch (error) {
        console.error('❌ [Deep Link] 파싱 오류:', error);
        // URL 파싱 실패 시 간단한 문자열 검색으로 처리
        if (url.includes('success=true')) {
          loadUserInfo();
          Alert.alert(
            '연동 완료',
            '스포티파이 연동이 완료되었습니다!',
            [{ text: '확인' }]
          );
        } else if (url.includes('success=false')) {
          Alert.alert(
            '연동 실패',
            '스포티파이 연동에 실패했습니다.',
            [{ text: '확인' }]
          );
        }
      }
    }
  };
  
  // Deep Link 리스너 등록
  const subscription = Linking.addEventListener('url', handleDeepLink);
  
  // 앱이 이미 열려있을 때 처리 (앱 시작 시 Deep Link 확인)
  Linking.getInitialURL().then((url) => {
    if (url) {
      console.log('🔗 [Deep Link] 초기 URL:', url);
      handleDeepLink(url);
    }
  }).catch((error) => {
    console.error('❌ [Deep Link] 초기 URL 가져오기 오류:', error);
  });
  
  // 화면 포커스 시 사용자 정보 새로고침
  const unsubscribe = navigation.addListener('focus', () => {
    loadUserInfo();
  });
  
  return () => {
    subscription.remove();
    unsubscribe();
  };
}, [navigation]);
```

---

### 2. Deep Link URL 형식

**성공 시:**
```
musiccheck://spotify-callback?success=true&spotifyUserId={spotifyUserId}
```

**실패 시:**
```
musiccheck://spotify-callback?success=false&message={에러메시지}
```

---

### 3. app.json 확인

`app.json`에 Deep Link 스킴이 등록되어 있는지 확인:

```json
{
  "expo": {
    "scheme": "musiccheck"
  }
}
```

---

## 📱 Expo Go vs Standalone 앱

### Standalone 앱
- `musiccheck://` 스킴이 정상 작동합니다.

### Expo Go (개발 환경)
- `musiccheck://` 스킴이 작동하지 않을 수 있습니다.
- 이 경우 Expo Go의 `exp://` 스킴을 사용해야 합니다.
- 프론트엔드에서 Expo Go인지 확인하고 스킴 변환:

```javascript
import Constants from 'expo-constants';

const isExpoGo = Constants.executionEnvironment === 'storeClient';

// Deep Link URL 변환 (필요한 경우)
const convertToExpoUrl = (deepLinkUrl) => {
  if (isExpoGo && deepLinkUrl.startsWith('musiccheck://')) {
    // musiccheck://spotify-callback?success=true&spotifyUserId=xxx
    // -> exp://{hostUri}/spotify-callback?success=true&spotifyUserId=xxx
    const params = deepLinkUrl.replace('musiccheck://spotify-callback?', '');
    return `exp://${Constants.expoConfig.hostUri}/spotify-callback?${params}`;
  }
  return deepLinkUrl;
};
```

---

## ✅ 체크리스트

- [ ] `Linking` import 추가
- [ ] `useEffect`에 Deep Link 리스너 추가
- [ ] `Linking.addEventListener('url', ...)` 구현
- [ ] `Linking.getInitialURL()` 구현 (앱 시작 시 Deep Link 처리)
- [ ] URL 파싱 로직 구현
- [ ] 성공 시 `loadUserInfo()` 호출
- [ ] 성공/실패 알림 처리
- [ ] `app.json`에 `scheme: "musiccheck"` 확인

---

## 🧪 테스트 방법

1. 앱에서 "스포티파이 연동" 버튼 클릭
2. 외부 브라우저에서 Spotify 로그인
3. "앱으로 돌아가기" 버튼 클릭
4. 앱이 열리고 Deep Link가 처리되는지 확인
5. 사용자 정보가 업데이트되고 알림이 표시되는지 확인

---

## 📝 참고

- 백엔드는 이미 HTML 페이지를 반환하도록 수정 완료
- 프론트엔드는 Deep Link를 받아서 처리하는 로직만 추가하면 됩니다
- Deep Link가 작동하지 않으면 `app.json`의 `scheme` 설정을 확인하세요

