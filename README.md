# AI Event Planning Agent

An AI-powered event planning system designed to help university clubs and small teams organize events more efficiently.

This project is being developed for **EECS 3311: Software Design** at York University.

## About the Project

Planning events can be difficult for university clubs, especially when executive members are balancing school, work, and other responsibilities. Event planning also involves many connected decisions, such as budgeting, scheduling, venue selection, task assignments, catering, and managing unexpected changes.

The goal of this project is to create an **AI Event Planning Agent** that works alongside an event planning team. Rather than replacing human decision-making, the agent helps organize information, generate recommendations, identify conflicts, and adapt plans when event details change.

## Main Features

The proposed system includes:

- Collaborative idea fusion
- Event blueprint and feasibility generation
- Smart task assignment and deadline tracking
- Agenda scheduling and conflict detection
- Budget and catering planning
- Venue research and comparison
- What-if scenario simulation
- Adaptive plan updates and contingency planning
- Event communications and attendee FAQ generation
- Post-event analysis

## AI Integration

The system is designed to use **Google Gemini** as its primary large language model.

The AI agent will be responsible for tasks such as combining ideas from multiple organizers, generating event plans, reasoning about event constraints, comparing alternatives, and generating contingency plans.

Traditional software components will still handle deterministic operations such as budget calculations, schedule conflict detection, validation, and updating stored event information.

## Design

The project uses object-oriented design and UML to plan the system before implementation.

The current design includes:

- Class Diagram
- Use-Case Diagram
- Sequence Diagrams
- Feature-to-Design Traceability
- Detailed Feature Specifications
- Design Pattern Documentation

The system is designed around five main design patterns:

1. **MVC (Model-View-Controller)**
2. **Facade**
3. **Strategy**
4. **Observer**
5. **Adapter**

## User Interfaces

The completed system is planned to support both:

- **GUI** – A graphical event planning dashboard
- **CLI** – A command-line interface providing access to the same core functionality

Both interfaces will communicate with the same application controller and underlying system logic.

## Project Status

🚧 **Currently in the design stage.**

Stage 1 focuses on defining the system requirements, features, architecture, UML diagrams, design patterns, and AI-agent interactions before implementation begins.

## Course Information

**Course:** EECS 3311 – Software Design  
**Term:** Fall 2026  
**Project:** Course Project – AI Agent-Based Software