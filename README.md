ANCS for Google Glass
========

Branched from [orient33/ble_ancs](https://github.com/orient33/ble_ancs)

Google Glass GDK app to receive iPhone notifications using Apple Notification Center Services over Bluetooth LE (a.k.a. magic)

**Current state**
- Somewhat working
- Creates Bluetooth LE pairing with iPhone
- Connects to ANCS via BluetoothGATT (requires app restart after pairing)
- Receives ANCS notifications (writes to log, Google removed TimelineManager to add static card to timeline in XE16 >_<)
