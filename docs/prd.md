# Hardwired: Product Requirements

## How To Read This Document

This document says what Hardwired is: what a player can do with it, who may do what, which side decides each rule, and what the mod deliberately does not do. Every requirement has an identifier such as `WIRE-3`, so issues, commits and reviews can cite it.

It holds no status. Nothing here says whether a requirement is built, in progress or waiting. Progress lives on the project board.

It changes only when a requirement changes or turns out to be unclear. The reasoning behind architectural choices lives in the decision records in [`docs/adr/`](adr/). The engineering baseline lives in [`docs/standards.md`](standards.md). Numbers in this document are starting values, to be tuned in play and then held as requirements.

## 1. Product Summary

### The Problem

Redstone can build a working computer, but it struggles to wire a house. Dust fills a whole block space, cannot run up a wall, fades after 15 blocks and needs its own structure of blocks around it. A hidden door ends up with a machine bolted to the side of the building. The rules are hard to see, so when a circuit fails a player has no way to tell why. Most players place a lever beside a door and stop there, and redstone becomes something they use in potions.

### What The Mod Is

Hardwired is practical 1920s electrics for Minecraft, built from copper and redstone. You drag wire across any wall, floor or ceiling and it disappears when you put it away. It takes up no space and follows simple rules. You wire up lamps, change their colour from a panel, build a hidden door without a machine behind it, and pack a whole circuit into one block. Redstone becomes the material everything is made from, so it is worth mining again.

### Experience It Aims For

- **Thirty seconds to light a room.** Wiring is a quick job, not a puzzle.
- **Nothing in the way.** Wiring never changes how a build looks or what can be placed in it. Wire is hidden, and even when shown it is a hair-thin line.
- **You can always see why.** Holding wire shows every connection and whether it is on.
- **Depth when you want it.** A homeowner never has to meet a logic gate, and an engineer can still build real circuits.
- **Early electrics, in vanilla's style.** Everything looks like it belongs in the game, and captures how exciting early electricity was: the showmanship and elegance of old world meeting new. It is sleek, never bulky, high resolution or plastic.
- **Vanilla at heart.** Recipes and behaviour echo what vanilla already teaches: copper ages, honeycomb waxes, lightning strikes rods, dyes colour things.

### What Makes It Different

| Claim | Why it holds |
|---|---|
| Wire takes up no space | It lives on block faces, not in block spaces (WIRE-1) |
| Rules a player can predict | A wire is on or off everywhere, with no fading, repeaters or direction rules (WIRE-6 to WIRE-9) |
| Faults are visible | Holding wire shows every connection live (VIEW-1 to VIEW-5) |
| Smart lights | One panel changes the colour and brightness of every lamp on a wire (PANEL-1 to PANEL-6) |
| A hidden door with no machine | A controller runs pistons from a simple timeline (SEQ-1 to SEQ-6) |
| A circuit in one block | Design it on a board and it becomes one block you can copy (CIRC-1 to CIRC-8) |
| Redstone is worth mining | Everything is built from it, and cells burn it (POWER-1, POWER-3) |

### Non-Goals

- **Not a computer mod.** No programmable computers or scripting languages.
- **Not an energy mod.** No voltages, generators to balance or power tiers. A lamp and a door never need a power plant.
- **Not a logistics mod.** It moves signals, never items.
- **Not a change to vanilla redstone.** Dust, repeaters and machines built from them behave as they always have.
- **Not a modern decoration pack.** Devices follow the period looks in LOOK-8 and LOOK-9, not today's plastic and flat screens.

## 2. Release Scope

This document specifies the Hardwired alpha line for Minecraft 1.21.1 on Fabric.

| An alpha release accepts | An alpha release refuses |
|---|---|
| Features that are thin, as long as what is there works | Any item or block that can be duplicated or lost by a game mechanic |
| Settings that need a world restart to apply, if the setting says so | A client being able to change anything the server decides |
| Missing optional integrations | A crash, or a server that will not start, from a bad config file |
| Art still being refined, if it reads clearly in game | Wiring lost from a world by a reload or an unloaded chunk |
| Fewer devices than this document lists | Any untranslated text in a shipped language |

### Deferred

| Out | Why |
|---|---|
| Replacing vanilla dust | Starting beside vanilla is safer. A world setting could later turn placed dust into new wire |
| Televisions and video streaming | Available video libraries bring licence, platform and stability problems, and the mod does not need them |
| Coloured light on nearby blocks | Vanilla light has no colour. A lamp changes its own colour, and a glow on the world must first be proven alongside Sodium and shaders |
| Signal levels from 0 to 15 | On and off keeps wiring simple |
| Visible decorative wire | Hidden wire comes first |
| Wireless links | Wire is the core of the mod |

### Product Decisions

| Question | Decision | Requirement |
|---|---|---|
| Does the mod replace vanilla redstone? | No. It sits beside it and connects to it | LINK-3, LINK-4 |
| Is a signal on or off, or a level? | On or off | WIRE-6 |
| Do lights and doors need power? | No. Only bigger devices need a cell | POWER-2 |
| When can a player start? | Early game, about when vanilla redstone becomes available | WIRE-2 |
| What is the first wow moment? | Wiring up lamps, then changing their colour | ADV-2, ADV-3 |
| Which look? | Early analog electrics in copper and redstone, with 1980s and 1990s computing, in vanilla's style | LOOK-1 to LOOK-4, LOOK-8, LOOK-9 |

## 3. Actors

| Actor | What they need |
|---|---|
| **Homeowner** | To light and control a build quickly, without learning circuits |
| **Tinkerer** | To build hidden doors and timed machines without a pile of redstone |
| **Engineer** | To design real logic in a small space and share it |
| **Wire owner** | To keep their wiring working and, when they choose, private |
| **Visitor** | To use switches placed for them. A seal stops them cutting or joining wire, but it does not protect the blocks wire sits on |
| **Server operator** | To configure every option from the command line and trust that clients cannot cheat |
| **Pack author** | To tune costs, limits and data-driven content without code |
| **Vanilla redstone** | Keeps working. Dust and components connect to wire that touches them and behave as they do in vanilla |
| **A vanilla client** | Cannot join: the mod adds blocks and items, so the client must have it installed |
| **A modified client** | Assumed hostile. It can send any packet with any value, and tries to read wiring it should not see |
| **Reduced settings** | A player on Minimal particles or with sound off still sees what wiring and devices are doing |

## 4. Domain Model

| Term | Definition |
|---|---|
| Face | One of the six sides of a block |
| Wire | A line along a block face, in plain copper or one of the 16 dye colours. It is saved with the world and takes no block space |
| Network | All wire of one colour joined together, plus everything connected to it |
| Touching | Wire touches the block it lies on and the block sitting on that face. A wire end also touches the blocks around it on every side except back along itself |
| Wire end | The last face of a line of wire. A branching network has many |
| Device | Any block in this mod that connects to wire |
| On | A network with at least one active input |
| Input | Anything that turns a network on: a switch, a sensor, a Junction, a Controller or Circuit Block output, or vanilla redstone power |
| Output | Anything a network drives: a device or a vanilla component |
| Junction | A block that joins different colours under a rule the player sets |
| Copper Lamp | A wired lamp whose colour and brightness can be set |
| Control Panel | A wall block that sets lamps on its network and stores scenes |
| Scene | A saved set of colours and brightness |
| Controller | A block that runs a timeline of steps on its outputs |
| Circuit Board | The screen and block where a circuit is designed |
| Circuit Block | One block holding a finished circuit |
| Redstone Cell | A block that stores charge for devices that need it |
| Charge | What a cell stores and big devices use |
| Holding wire | Having wire in the main hand or off hand |
| Sealed wire | Wire waxed with honeycomb by a player. Only that player, or an operator using PERM-6, can see or change it |
| Tripped | A network shut off by LINK-8 |

### What Wire Can Sit On

| Face | Allowed | Why |
|---|---|---|
| A full solid face | Yes | The normal case |
| The flat sides of slabs and stairs | Yes | Builders use them on every wall and floor |
| Glass and other see-through blocks | Yes | Wire is hidden, so it does not spoil them |
| Air, liquids and plants | No | Nothing to attach to |
| A bedrock or barrier face | No | The block can never be broken, so the wire could be stuck there |

## 5. Use Cases

### 5.1 Lay Wire

**Actor:** Homeowner.

You craft Copper Wire from copper and redstone. While you hold it, you click a start point, drag along any surface and release. Wire follows the faces you crossed, around corners and up walls. You put it away and it disappears.

| Gesture | What happens |
|---|---|
| Hold wire | Shows nearby wiring (5.2) |
| Click and drag on faces | Lays wire along them |
| Sneak and click a wire | Removes that piece and returns it to you |
| Surround a dye with eight wire in the crafting grid | Makes eight coloured wire |
| With wire in the other hand, use honeycomb on a wire | Seals that network to you |
| With wire in the other hand, use an axe on sealed wire | Removes your seal, as an axe scrapes wax from copper, and wears the axe the same way |

| ID | Requirement |
|---|---|
| WIRE-1 | Wire sits on block faces, takes no block space and never stops a block being placed |
| WIRE-2 | Copper Wire is crafted from copper ingots and redstone dust, with no tool better than vanilla redstone needs |
| WIRE-3 | Laying wire uses one item per face and refuses to continue when the player runs out. Creative players use none |
| WIRE-4 | Wire runs across any face in the eligibility table, around inner and outer corners |
| WIRE-5 | Wires of different colours may share a face and never join. Wires of one colour join wherever they touch, in any direction, so a line can branch freely, except as WIRE-13 says |
| WIRE-6 | A network is on or off. Every part of it reads the same state |
| WIRE-7 | A signal has no length limit, up to the network size limit (PERF-2) |
| WIRE-8 | A change at an input reaches every loaded output on its network on the next game tick, whatever the distance, the shape of the build or where the wire was laid, except as ACC-3 says |
| WIRE-9 | The result never depends on update order, the direction of a run or where in the world it sits |
| WIRE-10 | Breaking a block drops the wire on its faces. So does an explosion, a piston that destroys the block, or the block starting to fall |
| WIRE-11 | A block moved by a piston takes its wire with it, and connections re-form where it lands |
| WIRE-12 | A network crossing chunks behaves as one network. An input in an unloaded chunk keeps the state it had when it unloaded, and an output in an unloaded chunk catches up when it loads |
| WIRE-13 | Wire never joins a sealed network unless its owner, or an operator using PERM-6, lays it. Anyone else's wire sits beside it, or on the same face in the same colour, with no sign that it is there |
| WIRE-14 | Wire that joins a sealed network becomes sealed, and any open wire it connects to is sealed along with it at no cost. Networks sealed by different players never join, and an operator's wire laid between two seals joins only one of them: their own if they have one there, otherwise the same one every time |

**Not supported:** wire that decays; wire as a block; repeaters.

**Enforcement:** server. The client only sends the requested path, and the server checks reach, faces and items.

### 5.2 See The Wiring

**Actor:** Any player.

| ID | Requirement |
|---|---|
| VIEW-1 | Wire draws nothing until a player is holding wire. It then shows within range, through blocks, on every face, as a line as thin as tripwire |
| VIEW-2 | Each network shows its colour, and whether it is on, with a shape as well as a colour: solid when on and dashed when off |
| VIEW-3 | Looking at a wire shows its colour and lists what is driving it |
| VIEW-4 | Inputs and outputs are marked, so a player can trace why something is on or off |
| VIEW-5 | A broken connection is marked on the wire where it ends, and a tripped network is marked as tripped |
| VIEW-6 | The server sends a player wiring only while they are holding wire, within range, and never sends sealed wire to anyone but its owner or an operator using PERM-6 |

**Not supported:** seeing wire without holding it; seeing another player's sealed wire.

**Enforcement:** server decides what is sent. The client draws it.

### 5.3 Connect Things

**Actor:** Homeowner, tinkerer.

| ID | Requirement |
|---|---|
| LINK-1 | Every device connects to wire that touches it, except a Circuit Block (CIRC-2) |
| LINK-2 | The mod adds a Wall Switch, a Push Button, a Motion Sensor and a Dusk Sensor, all of which turn a network on |
| LINK-3 | Wire touching a vanilla component powers it fully: pistons, doors, lamps, note blocks, dispensers, hoppers and the like |
| LINK-4 | Any vanilla redstone power in a block wire touches turns its network on. Wire ignores power that wire supplied, so wiring into dust does not hold a network on, and dust never carries a signal between colours |
| LINK-5 | A Junction joins colours with AND, OR or NOT, chosen from its screen |
| LINK-6 | A network drives no vanilla block that it does not touch, so quasi-connectivity plays no part in wiring |
| LINK-7 | A network that feeds itself through a longer path of vanilla redstone or a Junction stays on until the loop is broken. That is allowed, and can be used as memory |
| LINK-8 | A network that changes state on more than 10 ticks in any 20 trips. It stays off until its wire is changed, including through a world reload |

**Not supported:** wire giving part-strength power.

**Enforcement:** server.

### 5.4 Light A Home

**Actor:** Homeowner.

This is the first wow moment. A player wires lamps to a switch, then changes all of them at once.

| ID | Requirement |
|---|---|
| LIGHT-1 | A Copper Lamp comes in block, ceiling and wall forms, made from copper, redstone and a torch, so the first lamp needs no trip to the Nether |
| LIGHT-2 | A Light Strip is a rail of small bulbs along a wall, floor or ceiling, and a Streetlight stands on a post |
| LIGHT-3 | A lamp starts as warm white and can show that or any of the 16 dye colours. Using a dye on a lamp sets its colour |
| LIGHT-4 | Brightness runs from the vanilla light level 0 to 15 |
| LIGHT-5 | Exposed lamps age like copper over time, and honeycomb stops it. Age changes the look, not the light |
| LIGHT-6 | A lamp on a network with no panel turns on at full brightness in the last colour it was given, by a dye or a panel |
| LIGHT-7 | A Streetlight with a Dusk Sensor on its network turns on at dusk and off at dawn |

**Not supported:** coloured light cast on other blocks (Section 2).

**Enforcement:** server. The client draws the colour it is told.

### 5.5 Set The Mood

**Actor:** Homeowner.

| ID | Requirement |
|---|---|
| PANEL-1 | A Control Panel on a network sets colour and brightness for all its lamps at once |
| PANEL-2 | Colours are picked from warm white and the 16 dye colours, shown by name as well as swatch |
| PANEL-3 | A panel stores up to 8 scenes, each with a player-given name |
| PANEL-4 | A wired Wall Switch can be set to recall a scene |
| PANEL-5 | A change reaches every lamp on the network within one game tick (WIRE-8), except as ACC-3 says |
| PANEL-6 | Two players, or two panels on one network, changing lamps at once give one result, and the last change wins |

**Not supported:** a colour wheel outside the dye colours.

**Enforcement:** server.

### 5.6 Hide A Door

**Actor:** Tinkerer.

A player builds a wall section with pistons behind it, wires them up, and sets the order on a Controller. No circuit is needed.

| ID | Requirement |
|---|---|
| SEQ-1 | A Controller reads an open colour and a close colour, and drives up to 8 output colours. All of them are set on its screen. A colour it reads cannot also be one it drives |
| SEQ-2 | Its screen is a timeline of up to 32 steps: each step sets outputs and waits a set time, from 0.1 seconds to one minute |
| SEQ-3 | The open colour turning on runs the timeline forwards, and the close colour turning on runs it backwards, undoing each step in reverse order so every output ends off. If both colours are the same, one switch works the door: on runs it forwards and off runs it backwards |
| SEQ-4 | A timeline can be told to reverse by itself after a set time, up to five minutes, counted from its last step finishing. Another open order restarts the time |
| SEQ-5 | A Controller that is interrupted finishes its current step, then follows the new order, so it never strands a piston halfway through a sequence |
| SEQ-6 | A Controller keeps its place through a chunk unload and a world reload |

**Not supported:** branching timelines. That is what circuits are for.

**Enforcement:** server.

### 5.7 Build A Circuit

**Actor:** Engineer.

| ID | Requirement |
|---|---|
| CIRC-1 | A Circuit Board block opens a grid screen where a player places gates, latches, timers and counters and joins them with drawn lines |
| CIRC-2 | A finished design is saved onto a Circuit Block. Each of its four sides is one port, set on the board as input, output or unused. Wire connects only to sides that are in use, and its top and bottom are unused |
| CIRC-3 | A Circuit Block takes one block space and works anywhere |
| CIRC-4 | A design can be tested live on the board before it is made |
| CIRC-5 | A design can be copied onto a blank block for the cost of the materials |
| CIRC-6 | A design can be exported as text and pasted on another world |
| CIRC-7 | The cost of a block rises with the number of parts in its design |
| CIRC-8 | A Circuit Block runs on the server at a fixed rate, and a design that fails a check for loops without delay is refused at the board. Loops through wire outside the block follow LINK-7 and LINK-8 |

**Not supported:** scripts or programming languages; circuits that read inventories.

**Enforcement:** server. It recomputes every design it receives, including the cost and the size limit.

### 5.8 Keep Devices Running

**Actor:** Tinkerer.

| ID | Requirement |
|---|---|
| POWER-1 | A Redstone Cell stores charge. Using redstone dust or a block of redstone on it fills it, using the item up |
| POWER-2 | Only the Lift, Alarm, Radio, Speaker and Intercom need charge. Wire, lamps, panels, switches, Controllers, Circuit Blocks and vanilla blocks never do |
| POWER-3 | A device that needs charge uses it from a cell on its network, only while running |
| POWER-4 | A Solar Panel, built from daylight detectors, slowly fills cells on its network by day |
| POWER-5 | Lightning striking a lightning rod wired to a cell fills that cell |
| POWER-6 | A device that runs out of charge stops, says why when looked at, and starts again when charged |
| POWER-7 | A cell shows how full it is in its look, without a number |

**Not supported:** generators that burn other fuels; cables with limited capacity; voltage.

**Enforcement:** server.

### 5.9 Add Electronics

**Actor:** Homeowner, tinkerer.

| ID | Requirement |
|---|---|
| DEV-1 | A Lift moves players between Lift Floors on a wired shaft |
| DEV-2 | An Alarm sounds when its network turns on |
| DEV-3 | A Speaker plays a music disc from a Radio on its network, so one Radio can fill a house |
| DEV-4 | An Intercom shares a chat line with other Intercoms on its network |
| DEV-5 | A Main Panel on a network lists its lamps, Controllers and switches and works them from one screen, with up to 128 rows. It flips switches, runs or stops Controllers, and sets the colour and brightness of one lamp. A stopped Controller finishes its step and holds there, and Run carries on forwards, or backwards from the end. A lamp set this way keeps that look until a Control Panel next changes its network |
| DEV-6 | Every device has a plain name, a recipe in copper and redstone, and a sound that fits vanilla |
| DEV-7 | A Main Panel lists only networks the player may see (PERM-4), and every action checks reach of the panel, build rights and that the device is still on a network the player may see. A device in an unloaded chunk shows its last known state and cannot be worked, and other wire points there are counted as unknown |

**Not supported:** video screens (Section 2); anything needing an internet connection.

**Enforcement:** server.

### 5.10 Configure The Mod

**Actor:** Server operator, pack author.

| ID | Requirement |
|---|---|
| CFG-1 | Every server option can be set with `/hardwired`, by an operator of level 2 or above |
| CFG-2 | Settings are saved per world as JSON in the world save directory |
| CFG-3 | A missing file gives defaults. A broken file gives defaults, is logged and is kept |
| CFG-4 | Out-of-range values are clamped on load |
| CFG-5 | Every balance value is a setting with a balanced default, never a fixed number in code. That covers wire view range, network size, circuit size, cell capacity, charge use, lamp ageing, Motion Sensor range and Push Button hold time. Accessibility and safety limits such as ACC-3 and LINK-8 are requirements, not settings |
| CFG-6 | A settings screen through Mod Menu shows the same server options and shares their validation with the commands |
| CFG-7 | An option that only changes what one player sees, such as how wire is drawn, is a client setting saved on that player's device |

**Enforcement:** server.

### 5.11 Work Through The Advancements

**Actor:** Any player.

The mod has its own tab, with a background of copper and redstone traces and a wire spool as its icon. Early advancements lead a new player to the first wow moment, then the tab branches by theme and can be followed in any order.

| ID | Advancement | Earned by |
|---|---|---|
| ADV-1 | Live Wire | Crafting Copper Wire |
| ADV-2 | Let There Be Light | Turning on a wired lamp from a switch |
| ADV-3 | Mood Lighting | Changing a lamp's colour from a panel |
| ADV-4 | Street Smart | A Streetlight turning on at dusk |
| ADV-5 | Scene Stealer | Saving a scene |
| ADV-6 | Full Spectrum | Using all 16 colours |
| ADV-7 | Now You See Me | Running a Controller that moves a piston |
| ADV-8 | Going Up | Riding a Lift |
| ADV-9 | Smart Home | Working 8 different devices from a Main Panel |
| ADV-10 | Logic Gate | Saving a first design |
| ADV-11 | Pocket Circuit | Making a Circuit Block |
| ADV-12 | Share The Love | Copying a Circuit Block |
| ADV-13 | Charged Up | Filling a Redstone Cell |
| ADV-14 | Sun Power | Charging a cell with a Solar Panel |
| ADV-15 | Caught Lightning | Charging a cell by lightning |
| ADV-16 | Short Circuit | Cutting a wire that is on |
| ADV-17 | Vintage | A lamp reaching full age |
| ADV-18 | Lights Out | Every device on a network that needs charge stopping for lack of it |
| ADV-19 | Hardwired | Granted on joining a world: the root of the tab |

| ID | Requirement |
|---|---|
| ADV-20 | Hardwired, Live Wire, Let There Be Light and Mood Lighting are not hidden and chain in that order. Their descriptions tell the player what to do next |
| ADV-21 | Lighting (ADV-4 to ADV-6), Mechanisms (ADV-7 to ADV-9), Circuits (ADV-10 to ADV-12) and Power (ADV-13 to ADV-15) each branch from Let There Be Light, in any order |
| ADV-22 | Short Circuit (under Live Wire), Vintage (under Let There Be Light) and Lights Out (under Charged Up) stay hidden until earned |
| ADV-23 | Let There Be Light, Mood Lighting, Full Spectrum and Share The Love are goals. Smart Home and Caught Lightning are challenges. Goals give 50 experience and challenges give 100 |
| ADV-24 | An advancement earned in steps shows its progress |
| ADV-25 | Icons use the mod's own items where one fits |

**Enforcement:** server.

## 6. Permissions

A modified client is assumed. Hiding a control is never the enforcement of a rule. Wire has no owner until it is sealed, so open wire belongs to the world, as a placed block does.

### Capability Matrix

| Capability | Any player | Owner of sealed wire | Operator |
|---|---|---|---|
| See open wire while holding wire | Yes | Yes | Yes |
| Cut or join open wire | Yes | Yes | Yes |
| See sealed wire | No | Yes | Yes, through PERM-6 |
| Cut or join sealed wire | No | Yes | Yes, through PERM-6 |
| Break a block with sealed wire on it | Yes, the wire drops | Yes | Yes |
| Use a switch, panel or Controller | Yes | Yes | Yes |
| Seal open wire | Yes | Yes | Yes |
| Remove a seal | No | Yes | Yes, through PERM-6 |
| Change server settings | No | No | Yes |

### Requirements

| ID | Requirement |
|---|---|
| PERM-1 | Every action in the matrix is checked on the server |
| PERM-2 | Sealing a network seals every wire in it. A player cannot seal a network that already has a seal. If a sealed network is split, each part stays sealed to the same owner. A seal that would join a player's own wire past the size limit (PERF-2) is refused and uses no honeycomb |
| PERM-3 | Sealing a network uses up one honeycomb. A seal is a mark on the wire, not an item: removing it or breaking the wire returns no honeycomb. Only the owner, or an operator using PERM-6, can remove it. Open wire left on the same face in the same colour joins it when the seal comes off, and the spare wire drops |
| PERM-4 | Sealed wire data never reaches a client that may not see it (VIEW-6) |
| PERM-5 | An owner who leaves the server keeps their seals. Nobody inherits them, and an operator can clear them |
| PERM-6 | An operator sees and changes sealed wire only after turning on `/hardwired override`, which stays on for that operator through a world reload until they turn it off. It counts only while the player is still an operator |

## 7. Client And Server

There is no singleplayer mode in this mod. A single-player world is an integrated server with one client, on the same code path as a dedicated server. No requirement here depends on player count.

### The Boundary

| Server owns | Client does |
|---|---|
| Wire, networks and their state | Draws wire it is sent |
| Which wire each player may see | Sends drag paths as requests |
| Lamp colour and brightness | Draws lamps |
| Circuit designs, checks and cost | Offers the board screen |
| Charge | Shows it in the cell's look |

### Concurrency

| ID | Requirement |
|---|---|
| CS-1 | Two players laying wire of one colour on one face place one wire and pay once |
| CS-2 | Two inputs turning a network on and off in one tick give a defined result: the network is on while any input is on |
| CS-3 | A wire cut in the same tick as a network change leaves two correct networks |
| CS-4 | Two players filling one cell never lose or duplicate items or charge |
| CS-5 | Two players editing one Circuit Board save one design, and the last save wins |

### Per-Player State

| State | Scope |
|---|---|
| Which wire is sent to a client | That player, while holding wire |
| Drag in progress | That player, until released |

### Side Separation

| ID | Requirement |
|---|---|
| CS-6 | A dedicated server starts without loading a client class |
| CS-7 | Optional integrations load only when the other mod is present |

## 8. Non-Functional Requirements

### Safety

| ID | Requirement |
|---|---|
| SAFE-1 | No game mechanic duplicates or deletes wire or any item the mod handles. Laying and removing wire conserves items |
| SAFE-2 | Charge is never created except by POWER-1, POWER-4 and POWER-5 |
| SAFE-3 | A bad config file never stops a server starting (CFG-3, CFG-4) |
| SAFE-4 | Every payload is size-limited: drag paths, scene names, timelines, designs and pasted text |
| SAFE-5 | A pasted design is checked in full before it is accepted |

### Performance

| ID | Requirement |
|---|---|
| PERF-1 | A network with no changing input costs nothing per tick |
| PERF-2 | A network has a size limit, set by the server. Wire beyond it is refused with a message |
| PERF-3 | A network change updates only blocks touching that network |
| PERF-4 | Wire adds no render cost for a player who is not holding it |
| PERF-5 | Chunk load adds no noticeable cost for chunks with no wire |
| PERF-6 | Finding a network's parts never loads a chunk |

### Persistence

| What | Chunk unload | World reload | Piston move | Block broken | Power loss |
|---|---|---|---|---|---|
| Wire | kept | kept | moves with its block | drops | kept to the last save |
| Seal | kept | kept | moves with its wire | lost | kept to the last save |
| Lamp colour, age and wax | kept | kept | lamp does not move | drops as item data | kept to the last save |
| Lamp brightness | kept | kept | lamp does not move | set again by its panel | kept to the last save |
| Scenes | kept | kept | panel does not move | drop with the panel as item data | kept to the last save |
| A lamp's own look from a Main Panel | kept | kept | lamp does not move | lost | kept to the last save |
| A Main Panel's last known devices | kept | kept | panel does not move | lost | kept to the last save |
| Devices worked towards Smart Home | kept | kept | not applicable | not applicable | kept to the last save |
| Controller timeline and place | kept | kept | Controller does not move | timeline drops as item data | kept to the last save |
| Circuit design | kept | kept | block does not move | drops as item data | kept to the last save |
| Charge | kept | kept | cell does not move | drops as item data | kept to the last save |
| Network state, including a trip | kept | kept | not applicable | not applicable | kept to the last save |
| Junction rule and a switch's scene | kept | kept | block does not move | drops as item data | kept to the last save |
| Settings | not applicable | kept | not applicable | not applicable | written atomically |

### Look And Sound

| ID | Requirement |
|---|---|
| LOOK-1 | Block and item textures are 16x16, in vanilla's palette and shading |
| LOOK-2 | Models are simple and blocky, sitting comfortably beside vanilla blocks |
| LOOK-3 | Copper and redstone are the main materials, in vanilla's colours |
| LOOK-4 | Tile and icon art for the advancement tab follows the same rules |
| LOOK-5 | Blocks and items look like vanilla ones wherever they are held or shown: GUI, first and third person, item frames and the ground |
| LOOK-6 | Sounds use vanilla first, and real recordings next |
| LOOK-7 | A lit device shows a lit texture, as a redstone lamp does |
| LOOK-8 | Every analog block, item, screen and icon is styled on real electrics of the 1920s: filament bulbs in smoked glass, marquee bulb rails, cloth-covered wire on wooden spools, porcelain knobs, tumbler switches, doorbell presses, slate switchboards with brass dials and knife switches, and Art Deco stepped and fan shapes. Accents use vanilla's gold, blackstone and calcite tones |
| LOOK-9 | Anything that computes, such as Junctions, Circuit Boards and Circuit Blocks, is styled on 1980s and 1990s computing: beige cases, green circuit boards, chips and green phosphor glyphs |

### Accessibility

| ID | Requirement |
|---|---|
| ACC-1 | No state relies on colour alone: wire shows on and off by shape, and colours carry names |
| ACC-2 | No meaning relies on particles alone |
| ACC-3 | A lamp never changes more than 3 times a second, however fast its network flips |
| ACC-4 | Every string is translated into every shipped language |

### Compatibility

| ID | Requirement |
|---|---|
| COMPAT-1 | Minecraft 1.21.1, Java 21, Fabric Loader 0.16.5 or later, Fabric API 0.107.0+1.21.1 or later |
| COMPAT-2 | Mod Menu, JEI and EMI are optional. Without them, the mod works fully. Every recipe is a plain crafting recipe, so JEI and EMI show them with no code in the mod |
| COMPAT-3 | Hardwired needs no other mod in the family. With More Doors, wire opens its doors like any other. With Total Storage, wire powers its terminals |
| COMPAT-4 | Vanilla redstone behaves as it does without the mod |
| COMPAT-5 | Costs, sensors and loot can be changed by data packs |
| COMPAT-6 | Shipped languages: English, German, Spanish, French, Brazilian Portuguese, Russian and Simplified Chinese |
| COMPAT-7 | Every declared dependency is used |

## 9. Release Criteria

- Every use case's requirements hold on a dedicated server with two clients and in a singleplayer world, through the same code.
- A new player can wire a switch to a lamp, without outside help, by following the advancements.
- No item, wire or charge can be duplicated or lost by any mechanic the mod adds (SAFE-1, SAFE-2).
- A dedicated server starts with no client classes loaded (CS-6).
- A vanilla redstone machine behaves the same with the mod installed (COMPAT-4).
- Every string is translated in every shipped language.
- Every server setting is reachable from the command line.
- A world from the previous release opens with all its wiring intact.

## 10. References

- **Progress:** the project board. This document holds no status.
- **Acceptance criteria:** each requirement's owning issue. This document does not repeat them.
- **Reasoning:** the decision records in [`docs/adr/`](adr/).
- **Engineering standards:** [`docs/standards.md`](standards.md).
- **Player-facing summary:** the [README](../README.md).
