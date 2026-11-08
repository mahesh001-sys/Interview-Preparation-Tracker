const API_URL = "/api/companies";
const ROUND_API_URL = "/api/interview-rounds";
const QUESTION_API_URL = "/api/questions";

const companyForm = document.getElementById("companyForm");
const companyList = document.getElementById("companyList");
const formHeading = document.getElementById("formHeading");
const submitButton = document.getElementById("submitButton");
const cancelEditButton = document.getElementById("cancelEditButton");
const formMessage = document.getElementById("formMessage");
const listMessage = document.getElementById("listMessage");
const loadingMessage = document.getElementById("loadingMessage");
const emptyMessage = document.getElementById("emptyMessage");
const searchInput = document.getElementById("searchInput");
const statusFilter = document.getElementById("statusFilter");

const roundForm = document.getElementById("roundForm");
const roundCompanyId = document.getElementById("roundCompanyId");
const roundName = document.getElementById("roundName");
const roundType = document.getElementById("roundType");
const roundStatus = document.getElementById("roundStatus");
const roundDate = document.getElementById("roundDate");
const roundFeedback = document.getElementById("roundFeedback");
const roundSubmitButton = document.getElementById("roundSubmitButton");
const cancelRoundEditButton = document.getElementById("cancelRoundEditButton");
const roundFormHeading = document.getElementById("roundFormHeading");
const roundFormMessage = document.getElementById("roundFormMessage");
const roundList = document.getElementById("roundList");
const roundCountLabel = document.getElementById("roundCountLabel");
const roundCompanyFilter = document.getElementById("roundCompanyFilter");
const roundLoadingMessage = document.getElementById("roundLoadingMessage");
const roundEmptyMessage = document.getElementById("roundEmptyMessage");
const roundListMessage = document.getElementById("roundListMessage");

let companies = [];
let interviewRounds = [];
let editingId = null;
let editingRoundId = null;

let questions = [];
let editingQuestionId = null;

const questionForm = document.getElementById("questionForm");
const questionCategory = document.getElementById("questionCategory");
const questionText = document.getElementById("questionText");
const questionAnswerNotes = document.getElementById("questionAnswerNotes");
const questionDifficulty = document.getElementById("questionDifficulty");
const questionIsCompleted = document.getElementById("questionIsCompleted");
const questionSubmitButton = document.getElementById("questionSubmitButton");
const cancelQuestionEditButton = document.getElementById("cancelQuestionEditButton");
const questionFormHeading = document.getElementById("questionFormHeading");
const questionFormMessage = document.getElementById("questionFormMessage");
const questionListMessage = document.getElementById("questionListMessage");
const questionList = document.getElementById("questionList");
const questionLoadingMessage = document.getElementById("questionLoadingMessage");
const questionEmptyMessage = document.getElementById("questionEmptyMessage");
const questionSearch = document.getElementById("questionSearch");
const questionCategoryFilter = document.getElementById("questionCategoryFilter");
const questionCompletionFilter = document.getElementById("questionCompletionFilter");
const questionCountLabel = document.getElementById("questionCountLabel");


function showMessage(element, message, type = "success") {
    element.textContent = message;
    element.className = `message ${type}`;
    element.setAttribute("role", type === "error" ? "alert" : "status");
    element.setAttribute("aria-live", type === "error" ? "assertive" : "polite");
    element.setAttribute("aria-atomic", "true");
}

function clearMessage(element) {
    element.textContent = "";
    element.className = "message";
    element.setAttribute("role", "status");
    element.setAttribute("aria-live", "polite");
    element.setAttribute("aria-atomic", "true");
}

async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });

    const text = await response.text();
    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            throw new Error("The server returned an invalid response.");
        }
    }

    if (!response.ok) {
        throw new Error(data?.error || `Request failed (${response.status}).`);
    }

    return data;
}

/* =========================
   COMPANY MANAGEMENT
========================= */

async function loadCompanies() {
    loadingMessage.classList.remove("hidden");
    companyList.replaceChildren();
    emptyMessage.classList.add("hidden");
    clearMessage(listMessage);

    try {
        companies = await apiRequest(API_URL);
        updateDashboard();
        renderCompanies();
        populateRoundCompanyOptions();
        await loadInterviewRounds();
    } catch (error) {
        showMessage(listMessage, error.message, "error");
        companies = [];
        updateDashboard();
        populateRoundCompanyOptions();
        roundList.replaceChildren();
        roundEmptyMessage.classList.remove("hidden");
    } finally {
        loadingMessage.classList.add("hidden");
    }
}

function updateDashboard() {
    document.getElementById("totalCount").textContent = companies.length;
    document.getElementById("appliedCount").textContent =
        companies.filter(c => c.applicationStatus === "APPLIED").length;
    document.getElementById("interviewCount").textContent =
        companies.filter(c => c.applicationStatus === "INTERVIEW").length;
    document.getElementById("offerCount").textContent =
        companies.filter(c => c.applicationStatus === "OFFER").length;

    document.getElementById("companyCountLabel").textContent =
        `${companies.length} ${companies.length === 1 ? "record" : "records"}`;
}

function createElement(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined && text !== null) {
        element.textContent = text;
    }
    return element;
}

function renderCompanies() {
    companyList.replaceChildren();

    const searchTerm = searchInput.value.trim().toLowerCase();
    const selectedStatus = statusFilter.value;

    const filtered = companies.filter(company => {
        const matchesSearch =
            (company.companyName || "").toLowerCase().includes(searchTerm) ||
            (company.jobRole || "").toLowerCase().includes(searchTerm) ||
            (company.jobLocation || "").toLowerCase().includes(searchTerm);

        const matchesStatus =
            !selectedStatus || company.applicationStatus === selectedStatus;

        return matchesSearch && matchesStatus;
    });

    emptyMessage.classList.toggle("hidden", filtered.length !== 0);

    filtered.forEach(company => {
        const card = createElement("article", "company-card");

        const top = createElement("div", "company-card-top");
        const identity = createElement("div", "company-identity");
        const initial = createElement(
            "div",
            "company-initial",
            (company.companyName || "?").trim().charAt(0).toUpperCase()
        );

        const nameBlock = createElement("div");
        nameBlock.append(
            createElement("h3", "company-name", company.companyName),
            createElement("p", "company-role", company.jobRole)
        );

        identity.append(initial, nameBlock);

        const status = createElement(
            "span",
            `status-badge status-${(company.applicationStatus || "APPLIED").toLowerCase()}`,
            formatStatus(company.applicationStatus)
        );

        top.append(identity, status);
        card.append(top);

        const details = createElement("div", "company-details");

        if (company.jobLocation) {
            details.append(createElement("span", "", `📍 ${company.jobLocation}`));
        }
        if (company.jobType) {
            details.append(createElement("span", "", `💼 ${company.jobType}`));
        }
        if (company.applicationDate) {
            details.append(createElement("span", "", `📅 ${company.applicationDate}`));
        }

        if (details.childElementCount) card.append(details);

        if (company.jobDescription) {
            card.append(createElement("p", "company-description", company.jobDescription));
        }

        if (company.websiteUrl) {
            const link = createElement("a", "company-link", "Career page ↗");
            link.href = company.websiteUrl;
            link.target = "_blank";
            link.rel = "noopener noreferrer";
            card.append(link);
        }

        const actions = createElement("div", "card-actions");

        const editButton = createElement("button", "btn-small btn-edit", "Edit");
        editButton.type = "button";
        editButton.addEventListener("click", () => startEdit(company));

        const deleteButton = createElement("button", "btn-small btn-delete", "Delete");
        deleteButton.type = "button";
        deleteButton.addEventListener("click", () => deleteCompany(company.id));

        actions.append(editButton, deleteButton);
        card.append(actions);
        companyList.append(card);
    });
}

function formatStatus(status) {
    const labels = {
        APPLIED: "Applied",
        ASSESSMENT: "Assessment",
        INTERVIEW: "Interview",
        OFFER: "Offer",
        REJECTED: "Rejected",
        WITHDRAWN: "Withdrawn"
    };

    return labels[status] || status || "Applied";
}

function getFormData() {
    const formData = new FormData(companyForm);

    return {
        companyName: formData.get("companyName").trim(),
        jobRole: formData.get("jobRole").trim(),
        jobLocation: formData.get("jobLocation").trim() || null,
        jobType: formData.get("jobType") || null,
        applicationStatus: formData.get("applicationStatus"),
        applicationDate: formData.get("applicationDate") || null,
        jobDescription: formData.get("jobDescription").trim() || null,
        websiteUrl: formData.get("websiteUrl").trim() || null
    };
}


/* =========================
   ACCESSIBLE FORM VALIDATION
========================= */

function validateRequiredFields(form) {
    const requiredFields = form.querySelectorAll("[required]");
    let firstInvalidField = null;

    requiredFields.forEach(field => {
        const isTextField =
            field.tagName === "TEXTAREA" ||
            (
                field.tagName === "INPUT" &&
                field.type !== "checkbox" &&
                field.type !== "radio"
            );

        if (isTextField) {
            field.setCustomValidity(
                field.value.trim()
                    ? ""
                    : "Please enter a value. Spaces alone are not accepted."
            );
        }

        if (!field.checkValidity() && !firstInvalidField) {
            firstInvalidField = field;
        }
    });

    if (firstInvalidField) {
        firstInvalidField.reportValidity();
        firstInvalidField.focus();
        return false;
    }

    return true;
}

document.querySelectorAll("form [required]").forEach(field => {
    const clearValidation = () => {
        if (field.value.trim()) field.setCustomValidity("");
    };

    field.addEventListener("input", clearValidation);
    field.addEventListener("change", clearValidation);
});

companyForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (!validateRequiredFields(companyForm)) return;
    clearMessage(formMessage);

    const payload = getFormData();
    const isEditing = editingId !== null;

    submitButton.disabled = true;
    submitButton.textContent = isEditing ? "Updating..." : "Saving...";

    try {
        if (isEditing) {
            await apiRequest(`${API_URL}/${editingId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showMessage(formMessage, "Company updated successfully.");
        } else {
            await apiRequest(API_URL, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showMessage(formMessage, "Company added successfully.");
        }

        resetForm();
        await loadCompanies();

    } catch (error) {
        showMessage(formMessage, error.message, "error");
    } finally {
        submitButton.disabled = false;
        submitButton.textContent =
            editingId !== null ? "Update company" : "Save company";
    }
});

function startEdit(company) {
    editingId = company.id;

    document.getElementById("companyName").value = company.companyName || "";
    document.getElementById("jobRole").value = company.jobRole || "";
    document.getElementById("jobLocation").value = company.jobLocation || "";
    document.getElementById("jobType").value = company.jobType || "";
    document.getElementById("applicationStatus").value =
        company.applicationStatus || "APPLIED";
    document.getElementById("applicationDate").value =
        company.applicationDate || "";
    document.getElementById("websiteUrl").value = company.websiteUrl || "";
    document.getElementById("jobDescription").value =
        company.jobDescription || "";

    formHeading.textContent = "Edit company";
    submitButton.textContent = "Update company";
    cancelEditButton.classList.remove("hidden");
    clearMessage(formMessage);

    document.getElementById("companyName").focus();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function resetForm() {
    companyForm.reset();
    editingId = null;
    formHeading.textContent = "Add a company";
    submitButton.textContent = "Save company";
    cancelEditButton.classList.add("hidden");
}

cancelEditButton.addEventListener("click", () => {
    resetForm();
    clearMessage(formMessage);
});

async function deleteCompany(id) {
    const company = companies.find(item => item.id === id);
    const name = company?.companyName || "this company";

    if (!window.confirm(`Delete ${name} from your tracker? Its interview rounds will also be deleted.`)) {
        return;
    }

    clearMessage(listMessage);

    try {
        await apiRequest(`${API_URL}/${id}`, { method: "DELETE" });

        if (editingId === id) resetForm();
        if (Number(roundCompanyId.value) === Number(id)) resetRoundForm();

        showMessage(listMessage, `${name} deleted successfully.`);
        await loadCompanies();
    } catch (error) {
        showMessage(listMessage, error.message, "error");
    }
}

searchInput.addEventListener("input", renderCompanies);
statusFilter.addEventListener("change", renderCompanies);

/* =========================
   INTERVIEW ROUND MANAGEMENT
========================= */

function populateRoundCompanyOptions() {
    const previousCompany = roundCompanyId.value;
    const previousFilter = roundCompanyFilter.value;

    roundCompanyId.replaceChildren(
        createElement("option", "", "Select a company")
    );
    roundCompanyFilter.replaceChildren(
        createElement("option", "", "All companies")
    );

    companies.forEach(company => {
        const formOption = createElement(
            "option",
            "",
            `${company.companyName} — ${company.jobRole}`
        );
        formOption.value = company.id;
        roundCompanyId.append(formOption);

        const filterOption = createElement(
            "option",
            "",
            company.companyName
        );
        filterOption.value = company.id;
        roundCompanyFilter.append(filterOption);
    });

    if (companies.some(c => String(c.id) === previousCompany)) {
        roundCompanyId.value = previousCompany;
    }

    if (companies.some(c => String(c.id) === previousFilter)) {
        roundCompanyFilter.value = previousFilter;
    }

    roundCompanyId.disabled = companies.length === 0;

    if (companies.length === 0) {
        roundCompanyId.value = "";
        roundCompanyId.options[0].textContent = "Add a company first";
    }
}

async function loadInterviewRounds() {
    roundLoadingMessage.classList.remove("hidden");
    roundEmptyMessage.classList.add("hidden");
    roundList.replaceChildren();
    clearMessage(roundListMessage);

    try {
        interviewRounds = await apiRequest(ROUND_API_URL);
        renderInterviewRounds();
    } catch (error) {
        interviewRounds = [];
        roundCountLabel.textContent = "0 rounds";
        showMessage(roundListMessage, error.message, "error");
    } finally {
        roundLoadingMessage.classList.add("hidden");
    }
}

function formatRoundType(type) {
    const labels = {
        TECHNICAL: "Technical",
        HR: "HR",
        CODING: "Coding",
        MANAGERIAL: "Managerial",
        BEHAVIORAL: "Behavioral",
        APTITUDE: "Aptitude",
        OTHER: "Other"
    };

    return labels[type] || type || "Other";
}

function formatRoundStatus(status) {
    const labels = {
        PENDING: "Pending",
        SCHEDULED: "Scheduled",
        COMPLETED: "Completed",
        CANCELLED: "Cancelled"
    };

    return labels[status] || status || "Pending";
}

function formatInterviewDate(value) {
    if (!value) return "Date not scheduled";
    return value.replace("T", " at ");
}

function renderInterviewRounds() {
    roundList.replaceChildren();

    const selectedCompany = roundCompanyFilter.value;

    const filtered = interviewRounds.filter(round =>
        !selectedCompany ||
        selectedCompany === "All companies" ||
        String(round.companyId) === selectedCompany
    );

    roundCountLabel.textContent =
        `${filtered.length} ${filtered.length === 1 ? "round" : "rounds"}`;

    roundEmptyMessage.classList.toggle("hidden", filtered.length > 0);

    filtered.forEach(round => {
        const company = companies.find(
            item => Number(item.id) === Number(round.companyId)
        );

        const card = createElement("article", "round-card");
        const top = createElement("div", "round-card-top");

        const titleBlock = createElement("div", "round-title-block");
        titleBlock.append(
            createElement("h3", "round-name", round.roundName),
            createElement(
                "p",
                "round-company",
                company ? company.companyName : "Company unavailable"
            )
        );

        const status = createElement(
            "span",
            `round-status status-${(round.status || "PENDING").toLowerCase()}`,
            formatRoundStatus(round.status)
        );

        top.append(titleBlock, status);
        card.append(top);

        const details = createElement("div", "round-details");
        details.append(
            createElement("span", "", `▣ ${formatRoundType(round.roundType)}`),
            createElement("span", "", `◷ ${formatInterviewDate(round.interviewDate)}`)
        );
        card.append(details);

        if (round.feedback) {
            card.append(
                createElement("p", "round-feedback", round.feedback)
            );
        }

        const actions = createElement("div", "card-actions");

        const editButton = createElement("button", "btn-small btn-edit", "Edit");
        editButton.type = "button";
        editButton.addEventListener("click", () => startRoundEdit(round));

        const deleteButton = createElement("button", "btn-small btn-delete", "Delete");
        deleteButton.type = "button";
        deleteButton.addEventListener("click", () => deleteInterviewRound(round));

        actions.append(editButton, deleteButton);
        card.append(actions);
        roundList.append(card);
    });
}

function getRoundFormData() {
    let dateValue = roundDate.value || null;

    // datetime-local supplies YYYY-MM-DDTHH:mm.
    // The Java API accepts LocalDateTime and the DAO stores seconds.
    if (dateValue && dateValue.length === 16) {
        dateValue += ":00";
    }

    return {
        companyId: Number(roundCompanyId.value),
        roundName: roundName.value.trim(),
        roundType: roundType.value,
        interviewDate: dateValue,
        status: roundStatus.value,
        feedback: roundFeedback.value.trim() || null
    };
}

roundForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (!validateRequiredFields(roundForm)) return;
    clearMessage(roundFormMessage);

    if (!companies.length) {
        showMessage(roundFormMessage, "Add a company before creating an interview round.", "error");
        return;
    }

    const payload = getRoundFormData();
    const isEditing = editingRoundId !== null;

    roundSubmitButton.disabled = true;
    roundSubmitButton.textContent = isEditing ? "Updating..." : "Saving...";

    try {
        if (isEditing) {
            await apiRequest(`${ROUND_API_URL}/${editingRoundId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showMessage(roundFormMessage, "Interview round updated successfully.");
        } else {
            await apiRequest(ROUND_API_URL, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showMessage(roundFormMessage, "Interview round added successfully.");
        }

        resetRoundForm();
        await loadInterviewRounds();
    } catch (error) {
        showMessage(roundFormMessage, error.message, "error");
    } finally {
        roundSubmitButton.disabled = false;
        roundSubmitButton.textContent =
            editingRoundId !== null ? "Update interview round" : "Save interview round";
    }
});

function startRoundEdit(round) {
    editingRoundId = round.id;

    roundCompanyId.value = String(round.companyId);
    roundName.value = round.roundName || "";
    roundType.value = round.roundType || "OTHER";
    roundStatus.value = round.status || "PENDING";
    roundDate.value = round.interviewDate
        ? round.interviewDate.slice(0, 16)
        : "";
    roundFeedback.value = round.feedback || "";

    roundFormHeading.textContent = "Edit interview round";
    roundSubmitButton.textContent = "Update interview round";
    cancelRoundEditButton.classList.remove("hidden");
    clearMessage(roundFormMessage);

    roundName.focus();
    document.querySelector(".rounds-panel").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function resetRoundForm() {
    roundForm.reset();
    editingRoundId = null;
    roundFormHeading.textContent = "Add an interview round";
    roundSubmitButton.textContent = "Save interview round";
    cancelRoundEditButton.classList.add("hidden");
}

cancelRoundEditButton.addEventListener("click", () => {
    resetRoundForm();
    clearMessage(roundFormMessage);
});

async function deleteInterviewRound(round) {
    if (!window.confirm(`Delete "${round.roundName}"?`)) return;

    clearMessage(roundListMessage);

    try {
        await apiRequest(`${ROUND_API_URL}/${round.id}`, {
            method: "DELETE"
        });

        if (editingRoundId === round.id) {
            resetRoundForm();
        }

        showMessage(roundListMessage, "Interview round deleted successfully.");
        await loadInterviewRounds();
    } catch (error) {
        showMessage(roundListMessage, error.message, "error");
    }
}

roundCompanyFilter.addEventListener("change", renderInterviewRounds);


/* =========================
   PREPARATION QUESTIONS
========================= */

async function loadQuestions() {
    questionLoadingMessage.classList.remove("hidden");
    questionList.replaceChildren();
    questionEmptyMessage.classList.add("hidden");
    clearMessage(questionListMessage);

    try {
        questions = await apiRequest(QUESTION_API_URL);
        updateQuestionStats();
        populateQuestionCategoryFilter();
        renderQuestions();
    } catch (error) {
        questions = [];
        updateQuestionStats();
        renderQuestions();
        showMessage(questionListMessage, error.message, "error");
    } finally {
        questionLoadingMessage.classList.add("hidden");
    }
}

function updateQuestionStats() {
    const total = questions.length;
    const completed = questions.filter(q => q.isCompleted).length;
    const pending = total - completed;
    const percentage = total ? Math.round((completed / total) * 100) : 0;

    document.getElementById("questionTotalCount").textContent = total;
    document.getElementById("questionCompletedCount").textContent = completed;
    document.getElementById("questionPendingCount").textContent = pending;
    document.getElementById("questionProgressPercent").textContent = `${percentage}%`;
    questionCountLabel.textContent =
        `${total} ${total === 1 ? "question" : "questions"}`;
}

function populateQuestionCategoryFilter() {
    const selected = questionCategoryFilter.value;
    const categories = [...new Set(
        questions.map(q => q.category).filter(Boolean)
    )].sort((a, b) => a.localeCompare(b));

    questionCategoryFilter.replaceChildren(
        createElement("option", "", "All categories")
    );
    questionCategoryFilter.options[0].value = "";

    categories.forEach(category => {
        const option = createElement("option", "", category);
        option.value = category;
        questionCategoryFilter.append(option);
    });

    questionCategoryFilter.value = categories.includes(selected) ? selected : "";
}

function formatDifficulty(difficulty) {
    const value = (difficulty || "MEDIUM").toUpperCase();
    return value.charAt(0) + value.slice(1).toLowerCase();
}

function renderQuestions() {
    questionList.replaceChildren();

    const search = questionSearch.value.trim().toLowerCase();
    const category = questionCategoryFilter.value;
    const completion = questionCompletionFilter.value;

    const filtered = questions.filter(question => {
        const searchable = [
            question.category,
            question.questionText,
            question.answerNotes
        ].join(" ").toLowerCase();

        const matchesSearch = !search || searchable.includes(search);
        const matchesCategory = !category || question.category === category;
        const matchesCompletion =
            !completion ||
            (completion === "completed" && question.isCompleted) ||
            (completion === "pending" && !question.isCompleted);

        return matchesSearch && matchesCategory && matchesCompletion;
    });

    questionEmptyMessage.classList.toggle("hidden", filtered.length > 0);

    filtered.forEach(question => {
        const card = createElement("article", "question-card");
        const top = createElement("div", "question-card-top");

        const categoryBadge = createElement(
            "span",
            "question-category",
            question.category
        );
        const difficultyBadge = createElement(
            "span",
            `question-difficulty difficulty-${(question.difficulty || "MEDIUM").toLowerCase()}`,
            formatDifficulty(question.difficulty)
        );

        const badges = createElement("div", "question-badges");
        badges.append(categoryBadge, difficultyBadge);

        const completionBadge = createElement(
            "span",
            `question-completion ${question.isCompleted ? "is-completed" : "is-pending"}`,
            question.isCompleted ? "Completed" : "Pending"
        );

        top.append(badges, completionBadge);
        card.append(top);

        card.append(createElement("h4", "question-card-title", question.questionText));

        if (question.answerNotes) {
            const answer = createElement("details", "question-answer");
            const summary = createElement("summary", "", "View answer notes");
            const content = createElement(
                "p",
                "question-answer-content",
                question.answerNotes
            );
            answer.append(summary, content);
            card.append(answer);
        }

        const actions = createElement("div", "card-actions");

        const completionButton = createElement(
            "button",
            "btn-small btn-completion",
            question.isCompleted ? "Mark pending" : "Mark completed"
        );
        completionButton.type = "button";
        completionButton.addEventListener("click", () => {
            updateQuestionCompletion(question);
        });

        const editButton = createElement("button", "btn-small btn-edit", "Edit");
        editButton.type = "button";
        editButton.addEventListener("click", () => startQuestionEdit(question));

        const deleteButton = createElement("button", "btn-small btn-delete", "Delete");
        deleteButton.type = "button";
        deleteButton.addEventListener("click", () => deleteQuestion(question));

        actions.append(completionButton, editButton, deleteButton);
        card.append(actions);
        questionList.append(card);
    });
}

function getQuestionFormData() {
    return {
        category: questionCategory.value.trim(),
        questionText: questionText.value.trim(),
        answerNotes: questionAnswerNotes.value.trim() || null,
        difficulty: questionDifficulty.value,
        isCompleted: questionIsCompleted.checked
    };
}

questionForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (!validateRequiredFields(questionForm)) return;
    clearMessage(questionFormMessage);

    const payload = getQuestionFormData();
    const isEditing = editingQuestionId !== null;

    questionSubmitButton.disabled = true;
    questionSubmitButton.textContent = isEditing ? "Updating..." : "Saving...";

    try {
        if (isEditing) {
            await apiRequest(`${QUESTION_API_URL}/${editingQuestionId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showMessage(questionFormMessage, "Question updated successfully.");
        } else {
            await apiRequest(QUESTION_API_URL, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showMessage(questionFormMessage, "Question added successfully.");
        }

        resetQuestionForm();
        await loadQuestions();
    } catch (error) {
        showMessage(questionFormMessage, error.message, "error");
    } finally {
        questionSubmitButton.disabled = false;
        questionSubmitButton.textContent =
            editingQuestionId !== null ? "Update question" : "Save question";
    }
});

function startQuestionEdit(question) {
    editingQuestionId = question.id;

    questionCategory.value = question.category || "";
    questionText.value = question.questionText || "";
    questionAnswerNotes.value = question.answerNotes || "";
    questionDifficulty.value = question.difficulty || "MEDIUM";
    questionIsCompleted.checked = Boolean(question.isCompleted);

    questionFormHeading.textContent = "Edit question";
    questionSubmitButton.textContent = "Update question";
    cancelQuestionEditButton.classList.remove("hidden");
    clearMessage(questionFormMessage);

    questionCategory.focus();
    document.querySelector(".questions-panel").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function resetQuestionForm() {
    questionForm.reset();
    editingQuestionId = null;
    questionFormHeading.textContent = "Add a question";
    questionSubmitButton.textContent = "Save question";
    questionDifficulty.value = "MEDIUM";
    cancelQuestionEditButton.classList.add("hidden");
}

cancelQuestionEditButton.addEventListener("click", () => {
    resetQuestionForm();
    clearMessage(questionFormMessage);
});

async function updateQuestionCompletion(question) {
    clearMessage(questionListMessage);

    try {
        await apiRequest(`${QUESTION_API_URL}/${question.id}`, {
            method: "PUT",
            body: JSON.stringify({
                category: question.category,
                questionText: question.questionText,
                answerNotes: question.answerNotes,
                difficulty: question.difficulty,
                isCompleted: !question.isCompleted
            })
        });

        await loadQuestions();
        showMessage(
            questionListMessage,
            question.isCompleted
                ? "Question marked as pending."
                : "Question marked as completed."
        );
    } catch (error) {
        showMessage(questionListMessage, error.message, "error");
    }
}

async function deleteQuestion(question) {
    if (!window.confirm(`Delete this question: "${question.questionText}"?`)) {
        return;
    }

    clearMessage(questionListMessage);

    try {
        await apiRequest(`${QUESTION_API_URL}/${question.id}`, {
            method: "DELETE"
        });

        if (editingQuestionId === question.id) {
            resetQuestionForm();
        }

        await loadQuestions();
        showMessage(questionListMessage, "Question deleted successfully.");
    } catch (error) {
        showMessage(questionListMessage, error.message, "error");
    }
}

questionSearch.addEventListener("input", renderQuestions);
questionCategoryFilter.addEventListener("change", renderQuestions);
questionCompletionFilter.addEventListener("change", renderQuestions);



/* =========================
   DYNAMIC WORKSPACE NAVIGATION
========================= */

const navigationLinks = [...document.querySelectorAll(".nav-link")];
const pageTitle = document.getElementById("topbarPageTitle");

const navigationSections = [
    { id: "overview", title: "Overview" },
    { id: "applications", title: "Applications" },
    { id: "interviews", title: "Interviews" },
    { id: "knowledge-base", title: "Knowledge Base" }
].map(item => ({
    ...item,
    element: document.getElementById(item.id)
})).filter(item => item.element);

function setActiveNavigation(sectionId) {
    const current = navigationSections.find(section => section.id === sectionId);
    if (!current) return;

    navigationLinks.forEach(link => {
        const isActive = link.getAttribute("href") === `#${sectionId}`;

        link.classList.toggle("active", isActive);

        if (isActive) {
            link.setAttribute("aria-current", "page");
        } else {
            link.removeAttribute("aria-current");
        }
    });

    if (pageTitle) {
        pageTitle.textContent = current.title;
    }
}

function updateNavigationFromHash() {
    const sectionId = window.location.hash.slice(1) || "overview";
    setActiveNavigation(sectionId);
}

navigationLinks.forEach(link => {
    link.addEventListener("click", () => {
        const sectionId = link.getAttribute("href").slice(1);
        setActiveNavigation(sectionId);
    });
});

window.addEventListener("hashchange", updateNavigationFromHash);

let navigationScrollPending = false;

window.addEventListener("scroll", () => {
    if (navigationScrollPending) return;

    navigationScrollPending = true;

    window.requestAnimationFrame(() => {
        const visibleSections = navigationSections.filter(
            section => section.element.getBoundingClientRect().top <= 180
        );

        const currentSection = visibleSections.length
            ? visibleSections[visibleSections.length - 1]
            : navigationSections[0];

        if (currentSection) {
            setActiveNavigation(currentSection.id);
        }

        navigationScrollPending = false;
    });
}, { passive: true });

updateNavigationFromHash();


/* =========================
   INITIAL LOAD
========================= */

loadCompanies();
loadQuestions();
