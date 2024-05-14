const eventSource = new EventSource('/sse');

eventSource.onmessage = function(event) {
    console.log('Received message:', event.data);
    const data = JSON.parse(event.data);
    if (data.message === 'reload') {
        const image = document.getElementById('dynamicImage');
        const imageUrl = 'http://localhost:8080/display/display.png?' + new Date().getTime();
        updateImageSrc(image, imageUrl);
    }
};

eventSource.onerror = function(error) {
    console.error('EventSource failed:', error);
    eventSource.close();
};

function updateImageSrc(image, url) {
    const MAX_RETRIES = 5;
    let attempts = 0;

    function tryUpdate() {
        fetch(url)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Image not found');
                }
                return response.blob();
            })
            .then(blob => {
                const objectURL = URL.createObjectURL(blob);
                image.src = objectURL;
                console.log("Image updated to:", objectURL);
            })
            .catch(error => {
                attempts++;
                if (attempts < MAX_RETRIES) {
                    console.log(`Retry ${attempts}/${MAX_RETRIES}: ${error.message}`);
                    setTimeout(tryUpdate, 200); // Retry after 200ms
                } else {
                    console.error('Failed to load image after multiple attempts:', error);
                }
            });
    }

    tryUpdate();
}

function checkFiles(files, type) {
    console.log("File:", files, "Type:", type);

    if (files.length !== 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024; // in MiB
    if (fileSize > 10) {
        alert("Datei zu groß (max. 10MB)");
        return;
    }

    const file = files[0];

    if(file){
        dynamicImage.src = URL.createObjectURL(file[0])
        console.log("File display locally")
    }

    const formData = new FormData();
    formData.append("image", file);

    fetch('/analyze', {
        method: 'POST',
        body: formData
    }).then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    }).then(data => {
        if (data.detections && data.imagePath) {
            displayResults(data);
        } else {
            throw new Error('Missing necessary data');
        }
    }).catch(error => {
        console.error('Error:', error);
        document.getElementById('JSON_Display').innerHTML = 'Error processing the request: ' + error.message;
    });
}

function displayResults(data) {
    console.log("Detection Results:", data.detections);
    console.log("Image Path:", data.imagePath);

    const resultsContainer = document.getElementById('JSON_Display');
    const imgElement = document.getElementById('dynamicImage');

    if (imgElement) {
        const imageUrl = `http://localhost:8080${data.imagePath}?${new Date().getTime()}`;
        updateImageSrc(imgElement, imageUrl);
    } else {
        console.error('Element with ID "dynamicImage" was not found.');
    }

    // Clear previous results
    resultsContainer.innerHTML = `
        <p><strong>Class:</strong></p>
        <p><strong>Probability:</strong></p>
    `;

    // Display detection results if any
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



function trigger(){
    document.getElementById("dynamicImage").src = "http://localhost:8080/display/display.png";
}



