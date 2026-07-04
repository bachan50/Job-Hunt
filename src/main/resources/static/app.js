const $ = (sel) => document.querySelector(sel);

let currentTab = "all";

async function api(path, options = {}) {
    const res = await fetch(path, options);
    if (res.status === 204) return null;
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        throw new Error((data && data.error) || `Request failed (${res.status})`);
    }
    return data;
}

async function loadActiveResume() {
    try {
        const resume = await api("/api/resume");
        if (resume) renderResume(resume);
    } catch (e) {
        console.warn(e);
    }
}

function renderResume(resume) {
    const box = $("#resumeInfo");
    box.classList.remove("hidden");
    const chips = (resume.keywords || [])
        .filter((k) => k && k.trim())
        .map((k) => `<span class="chip">${escapeHtml(k)}</span>`) 
        .join("");
    box.innerHTML = `
        <strong>${escapeHtml(resume.fileName)}</strong> analyzed.
        <div>Search query: <em>${escapeHtml(resume.searchQuery || "-")}</em>
             &middot; Location: ${escapeHtml(resume.preferredLocation || "any")}</div>
        <div class="chips">${chips || "<span class='hint'>No keywords detected</span>"}</div>`;
}

$("#uploadForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const file = $("#resumeFile").files[0];
    if (!file) return;
    const fd = new FormData();
    fd.append("file", file);
    fd.append("location", $("#location").value || "");
    const btn = e.target.querySelector("button");
    btn.disabled = true;
    btn.textContent = "Analyzing...";
    try {
        const resume = await api("/api/resume", { method: "POST", body: fd });
        renderResume(resume);
    } catch (err) {
        alert("Upload failed: " + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = "Upload & analyze";
    }
});

$("#runNow").addEventListener("click", async () => {
    const status = $("#runStatus");
    status.className = "status";
    status.classList.remove("hidden");
    status.textContent = "Running search...";
    try {
        const result = await api("/api/run", { method: "POST" });
        status.classList.add("ok");
        status.textContent = `Search complete: ${result.matched} new match(es), ${result.applied} auto-applied.`;
        await loadJobs();
    } catch (err) {
        status.classList.add("err");
        status.textContent = err.message;
    }
});

document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");
        currentTab = tab.dataset.tab;
        loadJobs();
    });
});

async function loadJobs() {
    const path = currentTab === "applied" ? "/api/jobs/applied" : "/api/jobs";
    const jobs = await api(path);
    renderJobs(jobs || []);
}

function renderJobs(jobs) {
    const body = $("#jobsBody");
    const empty = $("#emptyState");
    body.innerHTML = "";
    if (!jobs.length) {
        empty.classList.remove("hidden");
        return;
    }
    empty.classList.add("hidden");
    for (const j of jobs) {
        const tr = document.createElement("tr");
        const applyCell = j.status === "APPLIED"
            ? `<span class="hint">${fmt(j.appliedAt)}</span>`
            : `<button class="link" data-apply="${j.id}">Mark applied</button>`;
        tr.innerHTML = `
            <td class="score">${j.matchScore}%</td>
            <td><a href="${escapeHtml(j.url)}" target="_blank" rel="noopener">${escapeHtml(j.title)}</a></td>
            <td>${escapeHtml(j.company || "")}</td>
            <td>${escapeHtml(j.location || "")}</td>
            <td>${escapeHtml(j.source || "")}</td>
            <td><span class="badge ${j.status}">${j.status}</span></td>
            <td>${applyCell}</td>`;
        body.appendChild(tr);
    }
    body.querySelectorAll("[data-apply]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            btn.disabled = true;
            try {
                await api(`/api/jobs/${btn.dataset.apply}/apply`, { method: "POST" });
                await loadJobs();
            } catch (err) {
                alert(err.message);
                btn.disabled = false;
            }
        });
    });
}

function fmt(iso) {
    if (!iso) return "";
    try { return new Date(iso).toLocaleString(); } catch (e) { return iso; }
}

function escapeHtml(s) {
    return String(s == null ? "" : s)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

loadActiveResume();
loadJobs();
