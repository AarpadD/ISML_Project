# ISML Project - Multi-Agent Inventory Management System

## About
A multi-agent system built with **JADE** (Java Agent Development Framework) that simulates automated inventory management. Three agents work together:

* **SupplierAgent** - Handles stock replenishment
* **InventoryAgent** - Manages inventory and processes purchases
* **BuyerAgent** - GUI for customers to browse and buy products

Features automatic restocking when inventory falls below threshold levels.

---

## Installation

### Prerequisites
* Java JDK 8+
* JADE library

### Setup
1.  **Install Java JDK**
2.  **Add JADE JAR files** to your project classpath
3.  **Compile** the source files in `src/`

---

## Usage
1.  Run `Main.java`
2.  The **Buyer Agent GUI** will open showing available products
3.  Select quantity and click **"Buy"** to purchase items
4.  Stock automatically restocks when below threshold
5.  Inventory data persists in `inventory.dat`.

---

## Project Structure
```text
src/
├── Main.java            # Entry point
├── Product.java         # Product data model
├── SupplierAgent.java   # Replenishment agent
├── InventoryAgent.java  # Inventory management
└── BuyerAgent.java      # Customer GUI
