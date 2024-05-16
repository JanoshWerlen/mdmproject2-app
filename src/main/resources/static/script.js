document.addEventListener('DOMContentLoaded', function () {
    const serverBaseUrl = "http://mdm-project-2-server.azurewebsites.net";
    const socket = new WebSocket('ws://mdm-project-2-server.azurewebsites.net');

    socket.onopen = function () {
        console.log("WebSocket connection established");
    };

    socket.onmessage = function (event) {
        console.log("WebSocket message received:", event.data);
        if (event.data == 'update') {
            console.log("Update Image triggered")
            const fullImagePath = `http://mdm-project-2-server.azurewebsites.net/display/display.png`;
            updateImage(fullImagePath);
        }
    };

    socket.onerror = function (error) {
        console.log("WebSocket error:", error);
    };

    socket.onclose = function (event) {
        console.log("WebSocket connection closed:", event);
    };

    function updateImage(imagePath) {
        const imgElement = document.getElementById('resultImage');
        if (imgElement) {
            const timestamp = new Date().getTime(); // Cache busting
            imgElement.src = `${imagePath}?${timestamp}`;
            console.log('Image src updated to:', imgElement.src);
        } else {
            console.error('Image element not found');
        }
    }
});

function updateImage(imagePath) {
    const imgElement = document.getElementById('resultImage');
    if (imgElement) {
        const timestamp = new Date().getTime(); // Cache busting
        imgElement.src = `${imagePath}?${timestamp}`;
        console.log('Image src updated to:', imageUrl);
    } else {
        console.error('Image element not found');
    }
}


function checkFiles(files, type) {
    if (files.length !== 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024;
    if (fileSize > 10) {
        alert("Datei zu gross (max. 10Mb)");
        return;
    }

    const file = files[0];
    const formData = new FormData();
    formData.append("image", file);

    if (type == "OD") {
        fetch('/analyze', {
            method: 'POST',
            body: formData
        }).then(response => response.json()).then(data => {
            if (data.imagePath) {
                displayResults(data);
            } else {
                throw new Error('Missing necessary data');
            }
        }).catch(error => {
            document.getElementById('JSON_Display').innerHTML = 'Error processing the request: ' + error.message;
        });
    } else {
        fetch('/analyze_Class', {
            method: 'POST',
            body: formData
        }).then(response => response.json()).then(data => {
            if (data.imagePath) {
                displayResults_Class(data);
            } else {
                throw new Error('Missing necessary data');
            }
        }).catch(error => {
            document.getElementById('JSON_Display').innerHTML = 'Error processing the request: ' + error.message;
        });
    }
}

function checkVideo(files) {
const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net';
    if (files.length !== 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024;
    if (fileSize > 200) {
        alert("Datei zu gross (max. 200Mb)");
        return;
    }

    const file = files[0];
    const formData = new FormData();
    formData.append("video", file);

    fetch('/upload_video', {
        method: 'POST',
        body: formData
    }).then(response => response.json())
        .then(data => {
            console.log("data:", data);
            displayData(data); // Update this function to handle display updates correctly
            //const fullImagePath = `${serverBaseUrl}${data.imagePath}`;
            //updateImage(fullImagePath); // Call updateImage with the new image path
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
        });
}

function displayResults(data) {
    const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net';
    const imageUrl = `${serverBaseUrl}${data.imagePath}`;
    const timestamp = new Date().getTime();
    const imageUrlWithCacheBusting = `${imageUrl}?${timestamp}`;

    document.getElementById('resultImage').src = imageUrlWithCacheBusting;
    console.log("Image Path: " + imageUrlWithCacheBusting);

    const resultsContainer = document.getElementById('JSON_Display');

    // Clear previous results
    resultsContainer.innerHTML = '';

    // Check if there are any detections
    if (data.detections && data.detections.length > 0) {
        data.detections.forEach(detection => {
            const elementDiv = document.createElement('div');
            elementDiv.className = 'result-item';
            elementDiv.innerHTML = `
                <p><strong>Class:</strong> ${detection.className}</p>
                <p><strong>Probability:</strong> ${(detection.probability * 100).toFixed(2)}%</p>
            `;
            resultsContainer.appendChild(elementDiv);
        });
    } else {
        // Display a message if no detections are found
        resultsContainer.innerHTML = '<p>No detections found.</p>';
    }
}

function displayResults_Class(data) {
    const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net';
    const imageUrl = `${serverBaseUrl}${data.imagePath}`;
    const timestamp = new Date().getTime();
    const imageUrlWithCacheBusting = `${imageUrl}?${timestamp}`;

    document.getElementById('resultImage').src = imageUrlWithCacheBusting;
    console.log("Image Path: " + imageUrlWithCacheBusting);

    const resultsContainer = document.getElementById('JSON_Display');

    resultsContainer.innerHTML = '';

    data.detections.forEach(detection => {
        const elementDiv = document.createElement('div');
        elementDiv.className = 'result-item';
        elementDiv.innerHTML = `
            <p><strong>Class:</strong> ${detection.class} / <strong>Probability:</strong> ${(detection.probability * 100).toFixed(2)}% </p>
        `;
        resultsContainer.appendChild(elementDiv);
    });
}
function displayData(data) {
    const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net';
    const imageUrl = `${serverBaseUrl}${data.imagePath}`;
    const timestamp = new Date().getTime();
    const imageUrlWithCacheBusting = `${imageUrl}?${timestamp}`;
    const resultsContainer = document.getElementById('JSON_Display');

    if (!resultsContainer) {
        console.error('Failed to find the results container element');
        return;
    }

    document.getElementById('resultImage').src = imageUrlWithCacheBusting;
    console.log("Image Path: " + imageUrlWithCacheBusting);

    resultsContainer.innerHTML = '';

    for (const className in data.classNameCounts) {
        const detectionElement = document.createElement('div');
        detectionElement.classList.add('results-detail');
        detectionElement.innerHTML = `<p><strong>${className}:</strong> Amount detected: ${data.classNameCounts[className]}</p>`;
        resultsContainer.appendChild(detectionElement);
    }
}