# Phone Fleet Dashboard

A comprehensive phone fleet monitoring and management system consisting of a Python backend API, mobile data collection app, and a dashboard for real-time device monitoring and analytics.

##  ---  Project Overview  ---

This project is a three-tier system designed to monitor, manage, and analyse data from multiple mobile devices in a centralised location. The system enables tracking device status, performance metrics, and operational data from a fleet of phones.

### Architecture

```

┌─────────────────┐                 ┌──────────────┐                 ┌──────────────┐
│  Mobile Devices |  → HTTP/JSON →  |    Backend   |  → REST API →   │   Dashboard  │
│  (Data Source)  |                 |   (Python)   |                 │  (Frontend)  │
└─────────────────┘                 └──────────────┘                 └──────────────┘
```

##  ---  Technology Stack  ---

### Backend (Completed)
- **Language**: Python
- **Framework**: FastAPI
- **Database**: Flat-file JSON
- **API**: RESTful JSON endpoints

### Mobile App (In Development)
- **Platform**: Android
- **Language**: Kotlin
- **Framework**: Jetpack Compose
- **Communication**: OkHttp

### Frontend Dashboard (Planned)
- **Language**: TypeScript
- **Framework**: Vue.js
- **Styling**: Tailwind CSS
- **Visualisation**: Real-time status cards

##  ---  Getting Started  ---

### Prerequisites

- Python 3.8 or higher
- pip (Python package manager)
- Virtual environment (recommended)

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/ndrewta/Phone-Fleet-Dashboard.git
   cd Phone-Fleet-Dashboard
   ```

2. **Set up virtual environment**
   ```bash
   python -m venv .venv
   
   # On Windows
   venv\Scripts\activate
   
   # On macOS/Linux
   source venv/bin/activate
   ```

3. **Install dependencies**
   ```bash
   cd backend
   pip install -r requirements.txt
   ```

4. **Configure environment variables**
   ```bash
   Create .env file in root folder
   Edit .env with your configuration

   --- AUTHENTICATION ---
   TAPO_USERNAME= 'example@dot.com'
   TAPO_PASSWORD= 'password'

   --- NETWORK ---
   TAPO_P300_IP='192.168.xx.xxx'

   --- CONFIG ---
   BATTERY_LOW_THRESHOLD= xx
   BATTERY_HIGH_THRESHOLD= xx

5. **Testing the API**
   ```bash
   # Test health endpoint
    http://localhost:8000/

##  ---  Roadmap  ---

   ##  Mobile App (Coming Soon)

   The mobile app will collect and transmit device data to the backend including:

   - Device identifier
   - Battery status and health
   - App usage statistics
   - TP300 plug position
   - Connected SIMs

   ### Data Transmission Format
   ```bash
      json
   {
   "device_id": "unique-device-identifier",
   "battery_level": 85,
   "is_connected": "True",
   "plug_slot": 1,
   "sims": {k:v}
   "app_data": {k:v}
      }
```

##  Frontend Dashboard (Coming Soon)

The web dashboard will provide:

- Device status overview (online/offline)
- Realtime metrics
- Performance metrics visualization
- Device grouping and filtering
- SIM expiry
- App performance
- TP300 status

### Features
-  Interactive charts and graphs
-  Real-time notifications
-  Customisable views
-  Modifiable data

##  Development Roadmap

### Phase 1: Backend 
- [x] API endpoint structure
- [x] Database models
- [x] Device registration
- [x] Data collection endpoints
- [x] Basic authentication

### Phase 2: Mobile App 
- [ ] App architecture design
- [ ] Data collection modules
- [ ] API integration
- [ ] Background service
- [ ] Testing on multiple devices

### Phase 3: Frontend Dashboard 
- [ ] Dashboard framework setup
- [ ] Real-time data visualization
- [ ] Device management interface
- [ ] Alert system
- [ ] User authentication
- [ ] Responsive design

### Phase 4: Deployment 
- [x] Docker containerisation
- [ ] CI/CD pipeline
- [ ] Production server setup
- [ ] Monitoring and logging
- [ ] Documentation


This project is under active development. The mobile app and frontend dashboard components are currently being developed. Check back for updates!
---
