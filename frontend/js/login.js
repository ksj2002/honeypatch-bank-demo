// 프론트 A: 로그인 및 모드 전환 기능
// API_BASE는 팀장이 관리하는 js/config.js에서 정의합니다.

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('loginForm');
  const loginButton = document.getElementById('loginButton');
  const loginMessage = document.getElementById('loginMessage');
  const modeMessage = document.getElementById('modeMessage');
  const currentMode = document.getElementById('currentMode');
  const modeButtons = Array.from(document.querySelectorAll('[data-mode]'));

  function showMessage(element, message, type = 'error') {
    element.textContent = message;
    element.className = `message ${type}`;
  }

  async function readJson(response) {
    try { return await response.json(); }
    catch { return {}; }
  }

  function updateModeUI(mode) {
    currentMode.textContent = mode;
    modeButtons.forEach(button => {
      const selected = button.dataset.mode === mode;
      button.classList.toggle('active', selected);
      button.setAttribute('aria-pressed', String(selected));
    });
  }

  async function loadMode() {
    try {
      const response = await fetch(`${API_BASE}/api/mode`, { credentials: 'include' });
      const data = await readJson(response);
      if (!response.ok) throw new Error(data.message || '모드를 불러오지 못했습니다.');
      updateModeUI(data.mode);
    } catch (error) {
      currentMode.textContent = '연결 실패';
      showMessage(modeMessage, error.message || '서버 연결을 확인하세요.');
    }
  }

  async function changeMode(mode) {
    modeButtons.forEach(button => { button.disabled = true; });
    showMessage(modeMessage, '모드를 변경하고 있습니다.', 'success');
    try {
      const response = await fetch(`${API_BASE}/api/mode`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ mode })
      });
      const data = await readJson(response);
      if (!response.ok) throw new Error(data.message || '모드 변경에 실패했습니다.');
      updateModeUI(data.mode);
      showMessage(modeMessage, `${data.mode} 모드로 변경했습니다.`, 'success');
    } catch (error) {
      showMessage(modeMessage, error.message || '서버 연결을 확인하세요.');
    } finally {
      modeButtons.forEach(button => { button.disabled = false; });
    }
  }

  modeButtons.forEach(button => {
    button.addEventListener('click', () => changeMode(button.dataset.mode));
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const id = document.getElementById('loginId').value.trim();
    const password = document.getElementById('loginPassword').value;
    if (!id || !password) {
      showMessage(loginMessage, '아이디와 비밀번호를 입력하세요.');
      return;
    }
    loginButton.disabled = true;
    loginButton.textContent = '로그인 중...';
    showMessage(loginMessage, '로그인 요청 중입니다.', 'success');
    try {
      const response = await fetch(`${API_BASE}/api/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ id, password })
      });
      const data = await readJson(response);
      if (response.ok && data.success === true) {
        showMessage(loginMessage, data.message || '로그인 성공', 'success');
        setTimeout(() => { window.location.href = 'admin.html'; }, 600);
      } else {
        showMessage(loginMessage, data.message || '로그인에 실패했습니다.');
      }
    } catch {
      showMessage(loginMessage, '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인하세요.');
    } finally {
      loginButton.disabled = false;
      loginButton.textContent = '로그인';
    }
  });

  loadMode();
});

// 프론트 B가 관리자 페이지에서 재사용할 수 있는 로그아웃 함수.
// admin.html에서 login.js를 로드한 뒤 window.logout()을 호출할 수 있습니다.
window.logout = async function logout() {
  try {
    const response = await fetch(`${API_BASE}/api/logout`, {
      method: 'POST',
      credentials: 'include'
    });
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || '로그아웃에 실패했습니다.');
    }
    window.location.href = 'index.html';
  } catch (error) {
    alert(error.message || '서버 연결을 확인하세요.');
  }
};
