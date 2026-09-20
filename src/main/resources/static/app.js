const api = {
    summary: "/api/v1/stats/summary",
    groups: "/api/v1/stats/groups",
    services: "/api/v1/stats/services",
    authors: "/api/v1/stats/authors",
    jobs: "/api/v1/sync/jobs",
    importRegistry: "/api/v1/registry/import",
    syncAll: "/api/v1/sync/all",
    syncService: (id) => `/api/v1/sync/services/${id}`
};

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatNumber(value) {
    if (value === null || value === undefined) {
        return "";
    }
    return Number(value).toLocaleString("ru-RU");
}

function formatRatio(value) {
    if (value === null || value === undefined) {
        return "-";
    }
    return Number(value).toFixed(3);
}

function formatDate(value) {
    if (!value) {
        return "-";
    }
    return new Date(value).toLocaleString("ru-RU");
}

function setText(id, text) {
    document.getElementById(id).textContent = text;
}

async function fetchJson(url, options) {
    const response = await fetch(url, options);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(JSON.stringify(data, null, 2));
    }

    return data;
}

function renderSummary(summary) {
    const container = document.getElementById("summary");

    container.innerHTML = `
        <div class="summary-item">
            <div class="label">Групп</div>
            <div class="value">${formatNumber(summary.groupCount)}</div>
        </div>
        <div class="summary-item">
            <div class="label">Сервисов</div>
            <div class="value">${formatNumber(summary.serviceCount)}</div>
        </div>
        <div class="summary-item">
            <div class="label">Код, строк</div>
            <div class="value">${formatNumber(summary.codeLines)}</div>
        </div>
        <div class="summary-item">
            <div class="label">Тесты, строк</div>
            <div class="value">${formatNumber(summary.testLines)}</div>
        </div>
        <div class="summary-item">
            <div class="label">Всего, строк</div>
            <div class="value">${formatNumber(summary.totalLines)}</div>
        </div>
        <div class="summary-item">
            <div class="label">Тесты/код</div>
            <div class="value">${formatRatio(summary.testToCodeRatio)}</div>
        </div>
    `;
}

function renderGroups(rows) {
    const tbody = document.querySelector("#groupsTable tbody");

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td>${escapeHtml(row.groupName)}</td>
            <td>${formatNumber(row.serviceCount)}</td>
            <td>${formatNumber(row.codeLines)}</td>
            <td>${formatNumber(row.testLines)}</td>
            <td>${formatNumber(row.totalLines)}</td>
            <td>${formatRatio(row.testToCodeRatio)}</td>
        </tr>
    `).join("");
}

function renderServices(rows) {
    const tbody = document.querySelector("#servicesTable tbody");

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td>${escapeHtml(row.groupName)}</td>
            <td>${escapeHtml(row.serviceName)}</td>
            <td>${escapeHtml(row.defaultBranch || "-")}</td>
            <td>${formatNumber(row.codeLines)}</td>
            <td>${formatNumber(row.testLines)}</td>
            <td class="${row.syncStatus === "OK" ? "status-ok" : row.syncStatus === "FAILED" ? "status-failed" : ""}">
                ${escapeHtml(row.syncStatus)}
            </td>
            <td>
                <button class="small-button" data-sync-service="${escapeHtml(row.serviceId)}">
                    Синхронизировать
                </button>
            </td>
        </tr>
    `).join("");

    tbody.querySelectorAll("[data-sync-service]").forEach(button => {
        button.addEventListener("click", async () => {
            const serviceId = button.getAttribute("data-sync-service");
            button.disabled = true;
            setText("syncResult", "Синхронизация сервиса " + serviceId + "...");

            try {
                const job = await fetchJson(api.syncService(serviceId), {method: "POST"});
                setText("syncResult", JSON.stringify(job, null, 2));
                await loadAll();
            } catch (error) {
                setText("syncResult", error.message);
            } finally {
                button.disabled = false;
            }
        });
    });
}

function renderAuthors(rows) {
    const tbody = document.querySelector("#authorsTable tbody");

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td>${escapeHtml(row.authorName || "-")}</td>
            <td>${escapeHtml(row.authorEmail)}</td>
            <td>${formatNumber(row.commits)}</td>
            <td>${formatNumber(row.addedCodeLines)}</td>
            <td>${formatNumber(row.removedCodeLines)}</td>
            <td>${formatNumber(row.addedTestLines)}</td>
            <td>${formatNumber(row.removedTestLines)}</td>
            <td>${formatNumber(row.serviceCount)}</td>
        </tr>
    `).join("");
}

function renderJobs(rows) {
    const tbody = document.querySelector("#jobsTable tbody");

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td>${escapeHtml(row.id)}</td>
            <td>${escapeHtml(row.serviceName)}</td>
            <td class="${row.status === "SUCCESS" ? "status-ok" : row.status === "FAILED" ? "status-failed" : ""}">
                ${escapeHtml(row.status)}
            </td>
            <td>${escapeHtml(formatDate(row.startedAt))}</td>
            <td>${escapeHtml(formatDate(row.finishedAt))}</td>
            <td>${formatNumber(row.commitsProcessed)}</td>
            <td>${formatNumber(row.filesProcessed)}</td>
            <td>${escapeHtml(row.errorMessage || "")}</td>
        </tr>
    `).join("");
}

async function loadAll() {
    try {
        const [summary, groups, services, authors, jobs] = await Promise.all([
            fetchJson(api.summary),
            fetchJson(api.groups),
            fetchJson(api.services),
            fetchJson(api.authors),
            fetchJson(api.jobs)
        ]);

        renderSummary(summary);
        renderGroups(groups);
        renderServices(services);
        renderAuthors(authors);
        renderJobs(jobs);
    } catch (error) {
        setText("syncResult", error.message);
    }
}

document.getElementById("importForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const fileInput = document.getElementById("csvFile");
    const file = fileInput.files[0];

    if (!file) {
        setText("importResult", "Выберите CSV-файл");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setText("importResult", "Импорт...");

    try {
        const result = await fetchJson(api.importRegistry, {
            method: "POST",
            body: formData
        });

        setText("importResult", JSON.stringify(result, null, 2));
        await loadAll();
    } catch (error) {
        setText("importResult", error.message);
    }
});

document.getElementById("syncAllButton").addEventListener("click", async () => {
    const button = document.getElementById("syncAllButton");
    button.disabled = true;
    setText("syncResult", "Запущена синхронизация всех сервисов...");

    try {
        const jobs = await fetchJson(api.syncAll, {method: "POST"});
        setText("syncResult", JSON.stringify(jobs, null, 2));
        await loadAll();
    } catch (error) {
        setText("syncResult", error.message);
    } finally {
        button.disabled = false;
    }
});

loadAll();