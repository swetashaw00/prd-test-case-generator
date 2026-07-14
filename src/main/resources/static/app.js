async function submitFile(endpoint, formData, statusEl) {
  statusEl.textContent = "Generating… this can take up to a minute.";
  statusEl.classList.remove("error");
  document.getElementById("result-section").style.display = "none";

  try {
    const response = await fetch(endpoint, { method: "POST", body: formData });
    const data = await response.json();

    if (!response.ok) {
      statusEl.textContent = data.error || `Request failed (${response.status})`;
      statusEl.classList.add("error");
      return;
    }

    statusEl.textContent = "Done.";
    renderResult(data);
  } catch (err) {
    statusEl.textContent = "Request failed: " + err.message;
    statusEl.classList.add("error");
  }
}

function renderResult(result) {
  document.getElementById("result-summary").textContent = result.summary || "";
  document.getElementById("result-scope").textContent = result.scope || "";

  const assumptionsEl = document.getElementById("result-assumptions");
  assumptionsEl.innerHTML = "";
  if (result.assumptions && result.assumptions.length > 0) {
    const heading = document.createElement("strong");
    heading.textContent = "Assumptions:";
    const list = document.createElement("ul");
    result.assumptions.forEach((a) => {
      const li = document.createElement("li");
      li.textContent = a;
      list.appendChild(li);
    });
    assumptionsEl.appendChild(heading);
    assumptionsEl.appendChild(list);
  }

  const body = document.getElementById("result-body");
  body.innerHTML = "";
  (result.testCases || []).forEach((tc) => {
    const row = document.createElement("tr");

    const steps = document.createElement("ol");
    steps.className = "steps";
    (tc.steps || []).forEach((s) => {
      const li = document.createElement("li");
      li.textContent = s;
      steps.appendChild(li);
    });

    row.innerHTML = `
      <td>${escapeHtml(tc.id)}</td>
      <td>${escapeHtml(tc.title)}</td>
      <td>${escapeHtml(tc.type)}</td>
      <td class="priority-${escapeHtml(tc.priority)}">${escapeHtml(tc.priority)}</td>
      <td>${escapeHtml(tc.preconditions)}</td>
      <td></td>
      <td>${escapeHtml(tc.expectedResult)}</td>
    `;
    row.children[5].appendChild(steps);
    body.appendChild(row);
  });

  document.getElementById("result-section").style.display = "block";
}

function escapeHtml(value) {
  if (value === undefined || value === null) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

document.getElementById("prd-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const fileInput = document.getElementById("prd-file");
  const formData = new FormData();
  formData.append("file", fileInput.files[0]);
  submitFile("/api/prd/analyze", formData, document.getElementById("prd-status"));
});

document.getElementById("screenshot-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const fileInput = document.getElementById("screenshot-file");
  const prompt = document.getElementById("screenshot-prompt").value;
  const formData = new FormData();
  formData.append("file", fileInput.files[0]);
  if (prompt) formData.append("prompt", prompt);
  submitFile("/api/screenshot/analyze", formData, document.getElementById("screenshot-status"));
});
