# TestCLI 🚀

`testcli` is a modern, lightweight, and modular **JSON-Driven E2E Automation CLI** built with **Java 21** and **Playwright**. It bridges the gap between BDD (Behavior-Driven Development) feature files and structured JSON execution payloads, offering an interactive terminal experience for software automation engineers.

---

## 🌟 Core Features

* **JSON-Driven Engine:** Decouples test definitions from execution logic using clean, portable JSON payloads.
* **Interactive BDD Wizard:** Guides you through creating feature files, automatically parsing keywords via an internal dictionary (`bdd.dict`) and converting them into executable JSON payloads.
* **Modular CLI Architecture:** Built using **Picocli** with completely decoupled commands (`init`, `interactive`, `run`, `list`), ensuring clean maintainability and extensibility.
* **Smart Variable Interpolation:** Supports environment variables and `.env` configuration mapping (e.g., `${APP_URL}`) directly within test payloads.
* **Built-in Evidence Capture:** Automatically generates timestamped screenshots per step and stores them neatly in an `evidencias/` directory.
* **Interactive Navigation Menu:** Browse modules and scenarios dynamically with intuitive tree navigation and a back option (`v`).

---

## 🛠️ Tech Stack

* **Java 21** (Records, Pattern Matching, modern APIs)
* **Playwright for Java** (High-performance browser automation)
* **Picocli** (Robust command-line parsing framework)
* **Jackson / Custom Handlers** (JSON payload parsing & environment resolution)

---

## 📋 Prerequisites

Before building and installing `testcli`, ensure you have the following installed on your machine:
* **Java Development Kit (JDK) 21** or higher.
* **Apache Maven** (for building the project package).

---

## 📦 Building the Project

Clone the repository and build the standalone executable JAR using Maven:

```bash
# Clean and package the application (skipping unit tests for a fast build)
mvn clean package -DskipTests
```

This will generate the production-ready artifact in the `target/` directory:
`target/playwright-json-cli-1.0.0.jar`.

---

## 🌐 Global Installation & Setup (Mac, Linux & Windows)

To make `testcli` globally accessible from any terminal window just like a native system command, follow the instructions below for your operating system.

### 🐧 For Linux & 🍏 macOS

1. Create a global bin directory if it doesn't exist:
   ```bash
   mkdir -p ~/.local/bin
   ```
2. Copy your built JAR file to a permanent system location:
   ```bash
   mkdir -p ~/.testcli
   cp target/playwright-json-cli-1.0.0.jar ~/.testcli/testcli.jar
   ```
3. Create a generic wrapper shell script named `testcli` inside `~/.local/bin/`:
   ```bash
   cat << 'EOF' > ~/.local/bin/testcli
   #!/bin/bash
   java -jar ~/.testcli/testcli.jar "$@"
   EOF
   ```
4. Make the script executable:
   ```bash
   chmod +x ~/.local/bin/testcli
   ```
5. Ensure `~/.local/bin` is in your `PATH` (add it to your `~/.bashrc` or `~/.zshrc` if needed):
   ```bash
   export PATH="$HOME/.local/bin:$PATH"
   ```

### 🪟 For Windows

1. Create a folder to house the application, for example: `C:\tools\testcli\`
2. Move your built JAR file (`playwright-json-cli-1.0.0.jar`) into `C:\tools\testcli\` and rename it to `testcli.jar`.
3. Create a batch wrapper script named `testcli.bat` in that same folder (`C:\tools\testcli\testcli.bat`) with the following content:
   ```cmd
   @echo off
   java -jar C:\tools\testcli\testcli.jar %*
   ```
4. Add `C:\tools\testcli\` to your Windows **Environment Variables -> PATH**:
   * Open Start Search -> Type *Environment Variables* -> *Edit the system environment variables*.
   * Click **Environment Variables**, select `Path` under User or System variables, and click **Edit**.
   * Click **New** and add `C:\tools\testcli\`. Click OK on all windows.

---

## 🚀 Usage Guide

Once installed globally, you can execute the following commands:

### 1. Initialize a New Project Structure
Creates local business directories (`features/`, `payloads/`, `evidencias/`), an initial `.env` configuration file, and infrastructure templates (`Makefile`, `docker-compose.yml`):
```bash
testcli init
```

### 2. Interactive BDD Scenario Creation
Launches the wizard to define a module and scenario title, opens your default text editor for your feature steps, and automatically generates the corresponding JSON payload:
```bash
testcli interactive
```

### 3. Interactive Execution Menu
Launches an interactive tree menu to browse modules, pick scenarios, navigate back (`v`), and execute tests on the fly:
```bash
testcli list
```

### 4. Direct Scenario Execution
Runs a specific JSON test scenario directly via command-line option:
```bash
testcli run -s payloads/login/realizar-login-com-sucesso.json
```

---

## 📂 Project Architecture

```text
testcli/
├── src/main/java/com/marciockalves/
│   ├── cli/             # Modular Picocli commands (Init, Interactive, Run, List)
│   ├── core/            # TestEngine, JsonParser, Variable Interpolation
│   └── model/           # TestScenario records
├── src/main/resources/  # BDD dictionary, feature templates, infrastructure templates
├── features/            # Business-readable BDD feature files (.feature)
├── payloads/            # Structured execution JSON payloads (.json)
└── evidencias/          # Auto-captured step screenshots (.png)
```

---

## 📜 License
This project is open-source, designed for high-quality automated testing education and professional engineering workflows.