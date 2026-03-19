<div align="center">

# 🛰️ DRISHTI
### *From Space to Street — Every Cleanup Verified*

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Made with Flutter](https://img.shields.io/badge/Mobile-Flutter-blue?logo=flutter)](https://flutter.dev)
[![Backend: Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![AI: TensorFlow](https://img.shields.io/badge/AI-TensorFlow-orange?logo=tensorflow)](https://www.tensorflow.org/)
[![DB: MongoDB](https://img.shields.io/badge/DB-MongoDB-green?logo=mongodb)](https://www.mongodb.com/)
[![Hackathon Project](https://img.shields.io/badge/Built%20for-Hackathon-purple)](https://github.com)

> **DRISHTI** (Sanskrit: *दृष्टि* — Vision) is a smart, multi-layer waste management verification system that ensures every garbage cleanup is **real**, **verified**, and **trustworthy**.

</div>

---

## 📌 Table of Contents

- [🌍 The Problem](#-the-problem)
- [💡 Our Solution](#-our-solution)
- [🔄 How It Works](#-how-it-works)
- [🧠 Key Features](#-key-features)
- [🏗️ Tech Stack](#️-tech-stack)
- [📁 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
- [🔥 What Makes Us Different](#-what-makes-us-different)
- [🎯 Use Cases](#-use-cases)
- [🔭 Future Scope](#-future-scope)
- [👨‍💻 Team](#-team)
- [📄 License](#-license)

---

## 🌍 The Problem

Urban waste management suffers from a critical lack of **accountability and transparency**. Garbage is frequently marked as "cleaned" without any real proof, leading to:

| Issue | Impact |
|-------|--------|
| ❌ Illegal dumping | Waste accumulates in unauthorized spots |
| ❌ Data manipulation | Fake cleanup reports by field workers |
| ❌ Persistent dirty hotspots | No follow-up on recurring problem areas |
| ❌ No citizen feedback loop | Residents have no voice in verification |
| ❌ Lack of trust | Citizens lose faith in municipal systems |

Existing systems rely on **manual reporting** with no verification layer — DRISHTI changes that.

---

## 💡 Our Solution

**DRISHTI** is a smart, multi-layer verification system powered by AI, geo-tracking, and community participation.

We combine four powerful layers to ensure every cleanup is genuine:

```
🤖 AI Image Validation  +  📍 Geo-Tracking  +  👤 Citizen Verification  +  🛰️ Space-Tech Monitoring
```

This creates a **trustworthy, tamper-resistant** pipeline from garbage report to verified cleanup — with a quantified **Trust Score** at the end.

---

## 🔄 How It Works

```
┌─────────────────────────────────────────────────────────────────────┐
│                        DRISHTI WORKFLOW                             │
└─────────────────────────────────────────────────────────────────────┘

  👤 CITIZEN                🏛️ AUTHORITY             🚛 DRIVER
     │                          │                        │
     │── Reports Garbage ──────▶│                        │
     │   (Image + Location)     │── Assigns Task ───────▶│
     │                          │                        │── Completes Cleanup
     │                          │                        │── Uploads Proof Image
     │                          │                        │
     │                    🤖 AI ANALYSIS                 │
     │                    ┌──────────────┐               │
     │                    │ CLEAN ✅ or  │◀──────────────┘
     │                    │ DIRTY ❌     │
     │                    └──────────────┘
     │                          │
     │◀── Notified for Verification ────────────────────┘
     │
     │── ✅ Confirms Clean / ❌ Disputes ──▶ 📊 TRUST SCORE GENERATED
```

### Step-by-Step

1. **👤 Report** — Citizen spots garbage, uploads image + GPS location via the app
2. **🏛️ Assign** — Municipal authority receives the task and assigns it to a cleanup driver
3. **🚛 Cleanup** — Driver completes the cleanup and submits a proof image
4. **🤖 AI Validate** — System analyzes the image and classifies: `CLEAN` or `DIRTY`
5. **👤 Verify** — Original citizen is notified to confirm or dispute the result
6. **📊 Score** — System generates a **Trust Score** based on all verification layers

---

## 🧠 Key Features

### ✅ Multi-Layer Verification
No single point of failure. AI + Citizen + Geo-location all must align for a cleanup to be marked verified.

### 📸 AI Image Classification
Automated analysis of before/after images using trained ML models to determine cleanliness status.

### 📍 Real-Time Geo-Tracking
GNSS/GPS-based location verification ensures cleanup is reported at the correct location.

### 👥 Citizen Participation
Citizens are not just reporters — they're active verifiers, creating a participatory civic loop.

### ⚖️ Conflict Detection
When AI and citizen assessments disagree, the system flags the conflict for manual review by authorities.

### 📊 Trust Score
Each cleanup gets a quantified Trust Score based on all verification inputs — making data manipulation extremely difficult.

### 🛰️ Space-Tech Inspired Monitoring
Architecture inspired by satellite-based monitoring for future hotspot detection from aerial/satellite data.

---

## 🏗️ Tech Stack

```
┌──────────────────────────────────────────────────────────┐
│                     DRISHTI ARCHITECTURE                 │
├──────────────┬───────────────────────┬───────────────────┤
│  📱 MOBILE   │    ⚙️ BACKEND          │    🤖 AI/ML        │
├──────────────┼───────────────────────┼───────────────────┤
│ Flutter /    │ Spring Boot           │ TensorFlow /      │
│ React Native │ (Microservices)       │ PyTorch           │
│              │ MongoDB               │ Image Classifier  │
│              │ REST APIs             │ (Clean vs Dirty)  │
└──────────────┴───────────────────────┴───────────────────┘
```

| Layer | Technology | Purpose |
|-------|-----------|---------|
| 📱 Mobile App | Flutter / React Native | Citizen & Driver interface |
| ⚙️ Backend | Spring Boot (Microservices) | Business logic, task management |
| 🗄️ Database | MongoDB | Flexible document-based storage |
| 🤖 AI/ML | TensorFlow / PyTorch | Image classification (Clean vs Dirty) |
| 📍 Geo | GNSS/GPS | Location verification |
| 🔔 Notifications | Push Notifications | Citizen alerts & driver updates |

---

## 📁 Project Structure

```
drishti/
├── 📱 mobile/                  # Flutter / React Native app
│   ├── citizen/                # Citizen-facing screens
│   └── driver/                 # Driver-facing screens
│
├── ⚙️ backend/                  # Spring Boot microservices
│   ├── task-service/           # Task creation & assignment
│   ├── verification-service/   # AI + citizen verification logic
│   ├── trust-score-service/    # Trust Score computation
│   └── notification-service/  # Push notification dispatch
│
├── 🤖 ai-model/                # ML model for image classification
│   ├── training/               # Model training scripts
│   ├── inference/              # Inference API
│   └── datasets/               # Training data (labeled images)
│
├── 🗄️ database/                 # MongoDB schemas & migrations
│
└── 📄 docs/                    # Documentation & diagrams
```

---

## 🚀 Getting Started

### Prerequisites

- Node.js ≥ 18 / Flutter SDK ≥ 3.x
- Java 17+ (for Spring Boot)
- Python 3.9+ (for AI model)
- MongoDB (local or Atlas)

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/drishti.git
cd drishti
```

### 2. Backend Setup

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

> Configure your MongoDB connection in `application.properties`:
> ```properties
> spring.data.mongodb.uri=mongodb://localhost:27017/drishti
> ```

### 3. AI Model Setup

```bash
cd ai-model
pip install -r requirements.txt
python inference/server.py
```

### 4. Mobile App Setup

```bash
cd mobile
flutter pub get
flutter run
```

---

## 🔥 What Makes Us Different

| Feature | Traditional Systems | DRISHTI |
|---------|-------------------|---------|
| Cleanup Proof | Self-reported | AI-verified image |
| Location Check | Manual | GPS-verified |
| Citizen Role | Passive reporter | Active verifier |
| Trust Metric | None | Quantified Trust Score |
| Conflict Handling | Ignored | Detected & flagged |
| Data Integrity | Low | Multi-layer verified |

> **We don't just track garbage — we ensure TRUST in cleanup operations.**

---

## 🎯 Use Cases

- 🏙️ **Smart Cities** — Integrate into city-wide waste management platforms
- 🏛️ **Municipal Corporations** — Replace paper-based or unverified reporting
- 🌿 **Environmental Monitoring** — Track persistent dirty zones over time
- 🤝 **Civic Engagement Platforms** — Empower citizens as accountability partners
- 📊 **Government Auditing** — Reliable data for policy and budget decisions

---

## 🔭 Future Scope

- 🛰️ **Satellite-Based Hotspot Detection** — Use remote sensing imagery to detect large-scale illegal dumping zones
- 📊 **Advanced Analytics Dashboard** — Heatmaps, trends, and predictive waste forecasting for municipal planners
- 🧠 **Improved AI Accuracy** — Expand training datasets and explore segmentation models for finer analysis
- 🌍 **Multi-City Scalability** — Federated architecture to deploy across different municipalities
- 🔗 **Blockchain Audit Trail** — Immutable records of every cleanup for maximum transparency
- 🌐 **Open Data API** — Allow researchers and NGOs to access anonymized cleanup data

---

## 👨‍💻 Team

Built with 💡, ☕ and a lot of passion for cleaner cities.

| Name | Role |
|------|------|
| **Vaibhav Malviya** | System Design & Team Lead |
| **Shreya Bilagi** | Mobile App & UI/UX Design |
| **Manabendu Karfa** | AI/ML Model & Backend Development |

*Crafted for Hackathon Innovation — because clean cities deserve verified data.*

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

⭐ **If you believe in smarter, cleaner cities — give DRISHTI a star!** ⭐

*Made with 💚 for a cleaner, accountable world*

</div>
