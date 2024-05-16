document.addEventListener('DOMContentLoaded', function () {
    const socket = new WebSocket('ws://mdm-project-2-server.azurewebsites.net:8081');
    const serverBaseUrl = "http://mdm-project-2-server.azurewebsites.net"

    socket.onmessage = function (event) {
        console.log("WebSocket message received:", event.data);
        if (event.data === 'received: update') {
            console.log("Update command received, updating image.");
            updateImage();
        }
    };

    socket.onopen = function () {
        console.log("WebSocket connection established");
    };

    function updateImage() {
        const imgElement = document.getElementById('resultImage');
        if (imgElement) {
            const imageUrl = 'http://mdm-project-2-server.azurewebsites.net/display/display.png';
            const timestamp = new Date().getTime(); // Cache busting
            imgElement.src = `${imageUrl}?${timestamp}`;
            console.log('Image src updated to:', imgElement.src);
        }
    }
});

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
    console.log(files);

    if (files.length !== 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024; // in MiB
    if (fileSize > 200) {
        alert("Datei zu gross (max. 200Mb)");
        return;
    }

    //answerPart.style.visibility = "visible";
    const file = files[0];

    // Preview
    /*
    if (file) {
        preview.src = URL.createObjectURL(file);
    }
    */

    // Upload
    const formData = new FormData();
    formData.append("video", file);

    fetch('/upload_video', {
        method: 'POST',
        body: formData
    }).then(response => response.json())
        .then(data => {

            console.log("data:" + data)
            displayData(data); // Update this function to handle display updates correctly

        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
        });


}







function displayResults(data) {
    const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net'; // Replace with the actual server address
    const imageUrl = `${serverBaseUrl}${data.imagePath}`;
    const timestamp = new Date().getTime(); // Cache busting
    const imageUrlWithCacheBusting = `${imageUrl}?${timestamp}`;

    document.getElementById('resultImage').src = imageUrlWithCacheBusting;
    console.log("Image Path: " + imageUrlWithCacheBusting);

    const resultsContainer = document.getElementById('JSON_Display');

    resultsContainer.innerHTML = `
        <p><strong>Class:</strong></p>
        <p><strong>Probability:</strong></p>
    `;

    if (data.detections) {
        data.detections.forEach(detection => {
            const elementDiv = document.createElement('div');
            elementDiv.className = 'result-item';
            elementDiv.innerHTML = `
                <p><strong>Class:</strong> ${detection.className}</p>
                <p><strong>Probability:</strong> ${(detection.probability * 100).toFixed(2)}%</p>
            `;

            resultsContainer.appendChild(elementDiv);
        });
    }
}




function displayResults_Class(data) {
    const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net'; // Replace with the actual server address
    const imageUrl = `${serverBaseUrl}${data.imagePath}`;
    const timestamp = new Date().getTime(); // Cache busting
    const imageUrlWithCacheBusting = `${imageUrl}?${timestamp}`;

    document.getElementById('resultImage').src = imageUrlWithCacheBusting;
    console.log("Image Path: " + imageUrlWithCacheBusting);

    const resultsContainer = document.getElementById('JSON_Display');

    // Clear previous results
    resultsContainer.innerHTML = '';

    // Display detection results
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
    const serverBaseUrl = 'http://mdm-project-2-server.azurewebsites.net'; // Replace with the actual server address
    const imageUrl = `${serverBaseUrl}${data.imagePath}`;
    const timestamp = new Date().getTime(); // Cache busting
    const imageUrlWithCacheBusting = `${imageUrl}?${timestamp}`;

    document.getElementById('dynamicImage').src = imageUrlWithCacheBusting;
    console.log("Image Path: " + imageUrlWithCacheBusting);

    // Clear any existing results
    resultsContainer.innerHTML = '';

    // Iterate through the keys of the data object
    for (const className in data) {
        // Create HTML elements for each key-value pair
        const detectionElement = document.createElement('div');
        detectionElement.classList.add('results-detail'); // Ensure this class is styled appropriately
        detectionElement.innerHTML = `<p><strong>${className}:</strong> Amount detected: ${data[className]}</p>`;

        resultsContainer.appendChild(detectionElement);
    }
}



