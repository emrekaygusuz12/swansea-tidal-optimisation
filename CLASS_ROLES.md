
# Real-World Roles of Classes in NSGA-II Tidal Optimisation Project

This document maps each Java class to its real-world counterpart or analogy to help understand its function in the simulation system.

---

## Lagoon.java

**Represents the physical tidal lagoon infrastructure.**  
*Like an engineer’s blueprint of the Swansea Bay lagoon.*

- Contains static configuration data (e.g., turbine specs, sluice area, capital cost)
- Treated as unchangeable during simulation — a constant reference
- Used by all other classes to access design parameters

---

## Individual.java

**Represents one complete operational strategy for the lagoon.**  
*Like a tidal plant operator writing a full daily schedule: when and how to open turbines.*

- Encodes [Hs, He] control parameters for each half-tide
- Tracks energy output and unit cost (the objectives)
- Holds NSGA-II metadata (rank, crowding distance)
- Core object for evaluating and evolving strategies

---

## IndividualGenerator.java

**Represents the idea of “strategy brainstorming”.**  
*Like an engineer testing random combinations of start/end heads to explore viable plans.*

- Generates random but valid Individual objects
- Ensures constraints like min/max head are respected
- Forms the starting population for the NSGA-II algorithm

---

## TideDataReader.java

**Represents the data interface to the real world.**  
*Like importing tide level data from a sensor or file into the simulation.*

- Reads tidal elevation values from a text file
- Extracts and parses only the height values
- Converts external data into an internal simulation-ready list

---

## TidalSimulator.java

**Represents the real-time simulation engine of the lagoon.**  
*Like running a virtual twin of the plant on a computer to estimate energy output.*

- Uses real tide data + an operational strategy (`Individual`)
- Applies physics:
  - Orifice theory (flow through turbines)
  - Power = ρ * g * Q * h * η
  - Updates lagoon level after flow
- Includes realistic bounds on lagoon level
- Limits power to installed capacity
- Returns total energy output (used in fitness evaluation)

---

## Main.java

**Represents the control room operator or test engineer.**  
*The person running the simulations.*

- Loads tide data
- Creates random strategies (Individuals)
- Runs simulations using `TidalSimulator`
- Prints outputs for debugging or inspection
