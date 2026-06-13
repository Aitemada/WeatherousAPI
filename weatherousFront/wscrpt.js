    const citiesData = {
        "Italy": ["Pisa", "Venice", "Rome"],
        "Japan": ["Tokyo", "Kyoto", "Okinawa"],
        "Finland": ["Helsinki", "Lahti", "Nokia"]
    };

    let currentMode = "";

    const countrySelect = document.getElementById("countrySelect");
    const citySelect = document.getElementById("citySelect");
    const datePicker = document.getElementById("datePicker");
    const getWetBtn = document.getElementById("getWetBtn");
    const tooltip = document.getElementById("tooltip");

    //Page Init
    function initializePage() {
        // Force reset dropdowns on page reload
        countrySelect.value = "";
        citySelect.innerHTML = '<option value="" disabled selected>Choose City...</option>';
        citySelect.disabled = true;
        datePicker.value = "";
        getWetBtn.disabled = true;
        getWetBtn.classList.remove("ready");

        // Lock the DatePicker from Jan 1, 2010 to Today
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        const maxDate = `${yyyy}-${mm}-${dd}`;

        datePicker.min = "2010-01-01";
        datePicker.max = maxDate;
    }
    
    // Run init when script loads
    initializePage();

    // Handles Country Change
    countrySelect.addEventListener("change", (e) => {
        const cities = citiesData[e.target.value];
        citySelect.innerHTML = '<option value="" disabled selected>Choose City...</option>';
        cities.forEach(city => {
            citySelect.innerHTML += `<option value="${city}">${city}</option>`;
        });
        citySelect.disabled = false;
        checkReadyState();
    });

    citySelect.addEventListener("change", checkReadyState);
    datePicker.addEventListener("change", checkReadyState);
    datePicker.addEventListener("keyup", checkReadyState); 

    // Handles Mode Selection
    function selectMode(mode) {
        currentMode = mode;
        document.getElementById("btnForecast").classList.remove("active");
        document.getElementById("btnPastcast").classList.remove("active");
        document.getElementById("modeInstruction").style.display = "none";
        
        if (mode === "FORECAST") {
            document.getElementById("btnForecast").classList.add("active");
            tooltip.innerText = "Forecast Mode - Displays 7-day future weather projections.";
            datePicker.classList.add("hidden");
        } else {
            document.getElementById("btnPastcast").classList.add("active");
            tooltip.innerText = `Pastcast Mode - Displays historical weather and moon phase data. Choose dates between 1/1/2010 and ${new Date().toLocaleDateString('en-US')}`;
            datePicker.classList.remove("hidden");
        }
        checkReadyState();
    }

    function checkReadyState() {
        const isCitySelected = citySelect.value !== "";
        let isPastcastValid = true;

        if (currentMode === "PASTCAST") {
            const selectedDate = new Date(datePicker.value);
            const minDate = new Date("2010-01-01");
            const maxDate = new Date(); // Today
            
            // Invalidate if empty, before 2010, or in the future. Limitations of WeatherAPI
            if (datePicker.value === "" || selectedDate < minDate || selectedDate > maxDate) {
                isPastcastValid = false;
            }
        }
        
        if (isCitySelected && currentMode !== "" && isPastcastValid) {
            getWetBtn.disabled = false;
            getWetBtn.classList.add("ready");
        } else {
            getWetBtn.disabled = true;
            getWetBtn.classList.remove("ready");
        }
    }

    // Fetch API
    async function fetchWeather() {
        const city = citySelect.value;
        const resultsContainer = document.getElementById("resultsContainer");
        resultsContainer.innerHTML = "<p>Loading data...</p>";

        try {
            let url = currentMode === "FORECAST" 
                ? `/api/v1/weather/forecast?city=${city}` 
                : `/api/v1/weather/pastcast?city=${city}&date=${datePicker.value}`;
            
            const response = await fetch(url);
            
            // Handle errors from the backend
            if (!response.ok) {
                resultsContainer.innerHTML = `<p style='color:#8b0000; font-weight:bold;'>Error: Unable to fetch data. Ensure your API key is valid.</p>`;
                return;
            }
            
            const data = await response.json();
            renderResults(data);
        } catch (error) {
            resultsContainer.innerHTML = "<p style='color:#8b0000; font-weight:bold;'>Error connecting to backend API.</p>";
        }
    }

    function renderResults(data) {
        const container = document.getElementById("resultsContainer");
        container.innerHTML = "";

        if (currentMode === "FORECAST") {
            data.days.forEach(day => {
                container.innerHTML += `
                    <div class="weather-box">
                        <h4>${day.date}</h4>
                        <img src="/icons/${day.condition.toLowerCase()}.png" alt="${day.condition}">
                        <p><strong>${day.temp}°C</strong></p>
                        <p style="font-size:12px;">Hu ${day.humidity}% | WS ${day.windSpeed}km/h</p>
                    </div>`;
            });
        } else {
            container.innerHTML = `
                <div class="weather-box" style="width: 250px; display:flex; justify-content: space-around;">
                    <div>
                        <h4>Weather</h4>
                        <img src="/icons/${data.condition.toLowerCase().replace(/ /g, '-')}.png" alt="${data.condition}">
                        <p>${data.temp}°C</p>
                    </div>
                    <div>
                        <h4>Moon</h4>
                        <img src="/icons/moon-${data.moonPhase.toLowerCase().replace(/ /g, '-')}.png" alt="${data.moonPhase}">
                        <p>${data.moonPhase}</p>
                    </div>
                </div>`;
        }
    }