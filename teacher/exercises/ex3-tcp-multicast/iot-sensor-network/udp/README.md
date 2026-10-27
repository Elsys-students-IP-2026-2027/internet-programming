# IoT sensor network — UDP reference solution

The UDP server listens on port `5555`. Sensors send UTF-8 datagrams in the form
`DEVICE_ID:TEMP:HUMIDITY` every five seconds. Each valid message replaces that
device's previous reading, and the server responds to the sender with
`AVG:<averageTemp>:<averageHumidity> (devices=<activeCount>)`.

The averages are calculated by dividing the temperature and humidity totals by
the number of active devices. A device is active for 30 seconds after its most
recent valid reading. Temperatures outside `-40..50 °C`, non-finite numbers,
missing fields, and non-numeric values are rejected with `ERROR:invalid data`.

## Run

```bash
javac SensorServer.java SensorClient.java

# Terminal 1
java SensorServer

# Other terminals (use unique device IDs)
java SensorClient localhost sensor1
java SensorClient localhost sensor2
java SensorClient localhost sensor3
```

UDP is connectionless, so the server handles incoming datagrams in one receive
loop rather than creating a thread per client. The loop periodically removes
inactive devices even when no datagrams arrive.
