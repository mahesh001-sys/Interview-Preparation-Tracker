
const companyForm = document.getElementById("companyForm");
const companyName = document.getElementById("companyName");
const jobRole = document.getElementById("jobRole");
const companyList = document.getElementById("companyList");

companyForm.addEventListener("submit", function (event) {
    event.preventDefault();

    const name = companyName.value.trim();
    const role = jobRole.value.trim();

    if (name === "" || role === "") {
        alert("Please enter the company name and job role.");
        return;
    }

    const item = document.createElement("li");
    item.textContent = name + " - " + role;
    companyList.appendChild(item);

    companyForm.reset();
});