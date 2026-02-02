ISML Project - Multi-Agent Inventory Management System

About
A multi-agent system built with JADE (Java Agent Development Framework) that simulates automated inventory management. Three agents work together:

SupplierAgent - Handles stock replenishment
InventoryAgent - Manages inventory and processes purchases
BuyerAgent - GUI for customers to browse and buy products
Features automatic restocking when inventory falls below threshold levels.

Installation
Prerequisites
Java JDK 8+
JADE library
Setup
Install Java JDK
Add JADE JAR files to your project classpath
Compile the source files in src/
Usage
Run Main.java
The Buyer Agent GUI will open showing available products
Select quantity and click "Buy" to purchase items
Stock automatically restocks when below threshold
Inventory data persists in inventory.dat.

Project Structure
src/
├── Main.java            # Entry point
├── Product.java         # Product data model
├── SupplierAgent.java   # Replenishment agent
├── InventoryAgent.java  # Inventory management
└── BuyerAgent.java      # Customer GUI
