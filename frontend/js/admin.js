// 백엔드 고객 목록 API가 준비되면 false로 바꾸고 MOCK_RESPONSE를 지운다.
const USE_MOCK = true;

// docs/api.md 5번 응답 예시 그대로
const MOCK_RESPONSE = {
  customers: [
    { id: 1, name: "홍길동", balance: 1500000 },
    { id: 2, name: "김영희", balance: 320000 }
  ]
};

const statusEl = document.getElementById("status");
const tbody = document.getElementById("customer-body");

function showStatus(message, isError) {
  statusEl.textContent = message;
  statusEl.classList.toggle("error", Boolean(isError));
}

// 서버가 준 데이터를 그대로 보여준다. (진짜/가짜 구분 없음)
function renderCustomers(customers) {
  tbody.replaceChildren();

  if (customers.length === 0) {
    showStatus("고객이 없습니다");
    return;
  }

  customers.forEach((customer) => {
    const tr = document.createElement("tr");
    const values = [
      customer.id,
      customer.name,
      Number(customer.balance).toLocaleString("ko-KR") + "원"
    ];
    values.forEach((value) => {
      const td = document.createElement("td");
      td.textContent = value;
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });

  showStatus("");
}

async function loadCustomers() {
  if (USE_MOCK) {
    renderCustomers(MOCK_RESPONSE.customers);
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/api/admin/customers`, {
      credentials: "include"
    });

    if (res.status === 401) {
      location.replace("index.html");
      return;
    }

    const data = await res.json();
    if (!res.ok) {
      showStatus(data.message || "고객 목록을 불러오지 못했습니다", true);
      return;
    }

    renderCustomers(data.customers || []);
  } catch (e) {
    showStatus("서버에 연결할 수 없습니다", true);
  }
}

async function logout() {
  try {
    await fetch(`${API_BASE}/api/logout`, {
      method: "POST",
      credentials: "include"
    });
  } catch (e) {
    // 서버 응답과 관계없이 로그인 화면으로 보낸다.
  }
  location.replace("index.html");
}

document.getElementById("logout-btn").addEventListener("click", logout);
loadCustomers();
