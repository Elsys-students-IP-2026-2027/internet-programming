const BASE_URL = "http://localhost:8000";

async function logResponse(response) {
    console.log("Status:", response.status);
    const data = await response.json().catch(() => null);
    console.log("Response body:", data);
    return data;
}

async function getRequest(path) {
    const response = await fetch(`${BASE_URL}${path}`);
    return logResponse(response);
}

async function postRequest(path, body) {
    const response = await fetch(`${BASE_URL}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    return logResponse(response);
}

async function putRequest(path, body) {
    const response = await fetch(`${BASE_URL}${path}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    return logResponse(response);
}

async function patchRequest(path, body) {
    const response = await fetch(`${BASE_URL}${path}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    return logResponse(response);
}

async function deleteRequest(path) {
    const response = await fetch(`${BASE_URL}${path}`, { method: "DELETE" });
    return logResponse(response);
}

async function testCrudWorkflow() {
    console.log("=== CREATE USER ===");
    const newUser = {
        name: "Alice",
        age: 25,
        email: "alice@example.com",
        status: "processing"
    };
    const created = await postRequest("/users", newUser);
    const userId = created?.id;
    if (!userId) {
        console.error("User creation failed, aborting workflow.");
        return;
    }

    console.log("\n=== GET CREATED USER ===");
    await getRequest(`/users/${userId}`);

    console.log("\n=== PUT UPDATE USER ===");
    const updatedUser = {
        name: "Alice Smith",
        age: 26,
        email: "alice.smith@example.com",
        status: "ready"
    };
    await putRequest(`/users/${userId}`, updatedUser);

    console.log("\n=== PATCH UPDATE STATUS ===");
    await patchRequest(`/users/${userId}`, { status: "active" });

    console.log("\n=== GET UPDATED USER ===");
    await getRequest(`/users/${userId}`);

    console.log("\n=== DELETE USER ===");
    await deleteRequest(`/users/${userId}`);

    console.log("\n=== VERIFY DELETION ===");
    await getRequest(`/users/${userId}`);
}

testCrudWorkflow();
