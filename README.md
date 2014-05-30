ANCS for Google Glass
========

Branched from [orient33/ble_ancs](https://github.com/orient33/ble_ancs)

Google Glass GDK app to receive iPhone notifications using Apple Notification Center Services over Bluetooth LE (a.k.a. magic)

**Current state**
- Creates Bluetooth LE pairing with iPhone
- Connects to ANCS via BluetoothGATT
- Receives ANCS notifications
- Publishes a LiveCard that shows the latest notification (until Google restores TimelineManager for static cards)

**Bugs**
- Sometimes stuck showing old notifications in a queue (iPhone pushes a backlog of notifications)
- Crashes on first notification after initial Bluetooth LE pairing (app restart will work)
