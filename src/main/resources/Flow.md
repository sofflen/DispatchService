1. Start Kafka server
```shell
bin/kafka-server-start.sh config/kraft/server.properties
```
2. Start DispatchService in a new terminal window
```shell
mvn spring-boot:run
```
3. Start TrackingService in a new terminal window

4. Start Producer, that sends to 'order.created' topic, in a new terminal window
```shell
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order.created
```
5. Start Consumer, that listens to 'tracking.status' topic, in a new terminal window
```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic tracking.status
```
6. Send payload with console Producer
```json
{
  "orderId": "UUID",
  "item": "Object"
}
```